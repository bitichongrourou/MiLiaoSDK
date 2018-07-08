
package com.mi.milink.sdk.speedtest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Build;
import android.text.TextUtils;
import android.util.Base64;

import com.mi.milink.sdk.account.manager.MiAccountManager;
import com.mi.milink.sdk.aidl.PacketData;
import com.mi.milink.sdk.base.Global;
import com.mi.milink.sdk.base.data.Convert;
import com.mi.milink.sdk.base.os.SimpleRequest;
import com.mi.milink.sdk.base.os.SimpleRequest.LengthPair;
import com.mi.milink.sdk.base.os.SimpleRequest.StringContent;
import com.mi.milink.sdk.base.os.info.DeviceDash;
import com.mi.milink.sdk.base.os.info.NetworkDash;
import com.mi.milink.sdk.config.ConfigManager;
import com.mi.milink.sdk.data.ClientAppInfo;
import com.mi.milink.sdk.data.Const;
import com.mi.milink.sdk.debug.MiLinkLog;
import com.mi.milink.sdk.debug.TrafficMonitor;
import com.mi.milink.sdk.session.common.StreamUtil;
import com.mi.milink.sdk.util.CommonUtils;

/**
 * 测速，主要是为了采集数据，上传给服务器
 * 
 * @author MK
 */
public class SpeedTestManager {

    public static final String TAG = "SpeedTestManager";

    private static final int TCP_PING_LENGTH = 36;

    private static final int SOCKET_RETRY_TIMES = 3;

    private static final int UDP_PACKAGE_HEAD_LENGTH = 8;

    private static final short BIG_UDP_CONTENT_LENGTH = 1024;

    private static final short SMALL_UDP_CONTENT_LENGTH = 256;

    private static final int STATUS_SUCCESS = 0;

    private static final int STATUS_FAILURE = 1;

    private static final int STATE_NOT_DONE = 0;

    private static final int STATE_IN_PROGRESS = 1;

    private static final int STATE_DONE = 2;

    private static SpeedTestManager sInstance = new SpeedTestManager();

    private volatile int mState = STATE_NOT_DONE;

    public static SpeedTestManager getInstance() {
        return sInstance;
    }

    private SpeedTestManager() {
    }

    public synchronized void start() {
        if (!MiAccountManager.getInstance().appHasLogined()) {
            MiLinkLog.w(TAG, "speed test start, app not login");
            return;
        }
        MiLinkLog.w(TAG, "speed test start");
        if (mState == STATE_IN_PROGRESS) {
            MiLinkLog.i(TAG, "speed test is in progress");
            return;
        }

        mState = STATE_IN_PROGRESS;
        new Thread(new Runnable() {

            @Override
            public void run() {
                long testStart = System.currentTimeMillis();
                ArrayList<SpeedTestReportTcpTestInfo> tcpTestInfoList = testServerForTcp(ConfigManager
                        .getInstance().getSpeedTestTcpIps());
                ArrayList<SpeedTestReportUdpTestInfo> udpTestInfoList = testServerForUdp(ConfigManager
                        .getInstance().getSpeedTestUdpIps());
                if (tcpTestInfoList != null || udpTestInfoList != null) {
                    doPostData(tcpTestInfoList, udpTestInfoList);
                }
                MiLinkLog.v(TAG, "speed test cost=" + (System.currentTimeMillis() - testStart)
                        + " ms");
            }
        }, "MilinkSpeedTest").start();
    }

    private void doPostData(ArrayList<SpeedTestReportTcpTestInfo> tcpTestInfoList,
            ArrayList<SpeedTestReportUdpTestInfo> udpTestInfoList) {
        if (!TextUtils.isEmpty(MiAccountManager.getInstance().getUserId())) {
            String json = toJson(tcpTestInfoList, udpTestInfoList);
            String base64Data = new String(Base64.encode(json.getBytes(), Base64.DEFAULT));
            MiLinkLog.v(TAG, "ThreadId=" + Thread.currentThread().getId()
                    + ", SpeedTest doPostData: data=" + json);
            if (!TextUtils.isEmpty(base64Data)) {
                Map<String, String> params = new HashMap<String, String>();
                params.put("data", base64Data);
                if (!ClientAppInfo.isTestChannel()) { // 只有在非test版本中才上传数据
                    // 先使用域名
                    try {
                        LengthPair lengthPair = new LengthPair();
                        StringContent result = SimpleRequest.postAsString(
                                Const.SPEED_TEST_SERVER_ADDR, params, null, true, lengthPair);
                        TrafficMonitor.getInstance().traffic("tr.do", lengthPair.compressLength);
                        if (result != null) {
                            MiLinkLog.v(TAG, "SpeedTest doPostData use host report succeed: "
                                    + result.getBody());
                            mState = STATE_DONE;
                            return;// 成功之后返回
                        }
                    } catch (Exception e) {
                        MiLinkLog.v(TAG, "SpeedTest doPostData use host report failed");
                        mState = STATE_NOT_DONE;
                    }
                    // 走保底IP的逻辑，在传一下
                    try {
                        LengthPair lengthPair = new LengthPair();
                        StringContent result = SimpleRequest.postAsString(
                                Const.SPEED_TEST_SERVER_ADDR_IP, params, null, true,
                                Const.STASTIC_SERVER_HOST, lengthPair);
                        TrafficMonitor.getInstance().traffic("tr.do", lengthPair.compressLength);
                        if (result != null) {
                            MiLinkLog.v(TAG, "SpeedTest doPostData use ip report succeed: "
                                    + result.getBody());
                            mState = STATE_DONE;
                            return;// 成功之后返回
                        }
                    } catch (Exception e) {
                        MiLinkLog.v(TAG, "SpeedTest doPostData use ip report failed");
                        mState = STATE_NOT_DONE;
                    }
                }
            }
        }
    }

    private String toJson(ArrayList<SpeedTestReportTcpTestInfo> tcpTestInfoList,
            ArrayList<SpeedTestReportUdpTestInfo> udpTestInfoList) {
        String json = "";
        JSONObject root = new JSONObject();
        try {
            root.put(Const.PARAM_APP_ID, String.valueOf(getSpeedTestAppId()));
            root.put(Const.PARAM_PACKET_VID, MiAccountManager.getInstance().getUserId());
            root.put(Const.PARAM_CLIENT_VERSION,
                    String.valueOf(Global.getClientAppInfo().getVersionCode()));
            root.put(Const.PARAM_MI_LINK_VERSION, String.valueOf(Global.getMiLinkVersion()));
            root.put(Const.PARAM_SYSTEM_VERSION, "Android" + String.valueOf(Build.VERSION.RELEASE));
            root.put(Const.PARAM_DEVICE_ID,
                    CommonUtils.miuiSHA1(DeviceDash.getInstance().getDeviceId()));
            root.put(Const.PARAM_DEVICE_INFO, Build.MODEL);
            root.put(Const.PARAM_CHANNEL, Global.getClientAppInfo().getReleaseChannel());
            JSONArray dataArray = new JSONArray();
            if (tcpTestInfoList != null) {
                for (int i = 0; i < tcpTestInfoList.size(); i++) {
                    dataArray.put(tcpTestInfoList.get(i).toJSONObject());
                }
            }
            if (udpTestInfoList != null) {
                for (int i = 0; i < udpTestInfoList.size(); i++) {
                    dataArray.put(udpTestInfoList.get(i).toJSONObject());
                }
            }
            if (dataArray.length() > 0) {
                root.put(Const.PARAM_DATA, dataArray);
                json = root.toString();
            }
        } catch (JSONException e) {
            MiLinkLog.e(TAG, "toJson", e);
        }
        return json;
    }

    // TCP
    private ArrayList<SpeedTestReportTcpTestInfo> testServerForTcp(String ips) {
        MiLinkLog.i(TAG, "start speed test tcp, ips=" + ips);
        ArrayList<SpeedTestReportTcpTestInfo> dataList = null;
        if (!TextUtils.isEmpty(ips)) {
            if (NetworkDash.isAvailable()) {
                dataList = new ArrayList<SpeedTestReportTcpTestInfo>();
                String[] ipAry = ips.split("#");
                for (String ipPort : ipAry) {
                    if (!TextUtils.isEmpty(ipPort)) {
                        String[] address = ipPort.split(":");
                        if (address != null && address.length == 2) {
                            try {
                                int port = Integer.parseInt(address[1]);
                                SpeedTestReportTcpTestInfo info = testServerForTcpByIpPort(
                                        address[0], port);
                                if (info != null) {
                                    dataList.add(info);
                                }
                            } catch (NumberFormatException e) {
                            }
                        }
                    }
                }
            }
        }
        return dataList;
    }

    // TCP
    private SpeedTestReportTcpTestInfo testServerForTcpByIpPort(String ip, int port) {
        SpeedTestReportTcpTestInfo info = null;
        if (!TextUtils.isEmpty(ip) && port > 0) {
            if (NetworkDash.isAvailable()) {
                int seq = Global.getSequence();
                byte[] ping = buildTcpPingPackage(seq);
                if (ping != null) {
                    MiLinkLog.i(TAG, "speed test tcp, IP = " + ip + " port = " + port);
                    info = new SpeedTestReportTcpTestInfo();
                    Socket socket = new Socket();
                    try {
                        InetSocketAddress socketAddress = new InetSocketAddress(
                                InetAddress.getByName(ip), port);
                        int connectTimeout = ConfigManager.getInstance().getConnetionTimeout();
                        int readTimeout = ConfigManager.getInstance().getRequestTimeout();
                        info.connectTimeout = connectTimeout;
                        info.readTimeout = readTimeout;
                        info.serverIp = ip;
                        info.port = port;
                        long connectBegin = System.currentTimeMillis();
                        socket.connect(socketAddress, connectTimeout);
                        info.connectTime = System.currentTimeMillis() - connectBegin;
                        MiLinkLog.i(TAG, "tcp connectTime=" + info.connectTime);
                        socket.setSoTimeout(readTimeout);
                        OutputStream os = socket.getOutputStream();

                        long sendBegin = System.currentTimeMillis();
                        os.write(ping);
                        os.flush();
                        byte[] readBuffer = new byte[256];
                        int hasReadLength = 0;
                        int retryTimes = 0;
                        InputStream is = socket.getInputStream();
                        do {
                            int count = is.read(readBuffer, hasReadLength, TCP_PING_LENGTH
                                    - hasReadLength);
                            if (count < 0) {
                                break;
                            }
                            retryTimes++;
                            hasReadLength += count;
                        } while (hasReadLength < TCP_PING_LENGTH && retryTimes < SOCKET_RETRY_TIMES);
                        info.status = STATUS_SUCCESS;
                        info.rtt = System.currentTimeMillis() - sendBegin;
                    } catch (UnknownHostException e) {
                        info.status = STATUS_FAILURE;
                        MiLinkLog.w(TAG, "testServerForTcpByIpPort UnknownHostException");
                    } catch (SocketTimeoutException e) {
                        info.status = STATUS_FAILURE;
                        MiLinkLog.w(TAG, "testServerForTcpByIpPort SocketTimeoutException");
                    } catch (Exception e) {
                        info.status = STATUS_FAILURE;
                        MiLinkLog.e(TAG, "testServerForTcpByIpPort", e);
                    } finally {
                        try {
                            if (socket != null) {
                                socket.close();
                                socket = null;
                            }
                        } catch (IOException e) {
                            MiLinkLog.w(TAG, "testServerForTcpByIpPort , close socket fail");
                        }
                    }
                } else {
                    MiLinkLog.i(TAG, "speed test tcp, ping = null, IP = " + ip + " port = " + port);
                }
            } else {
                MiLinkLog.i(TAG, "speed test tcp, network.is unavailable, IP = " + ip + " port = "
                        + port);
            }
        }
        return info;
    }

    // UDP
    private ArrayList<SpeedTestReportUdpTestInfo> testServerForUdp(String ips) {
        MiLinkLog.i(TAG, "start speed test udp, ips=" + ips);
        ArrayList<SpeedTestReportUdpTestInfo> dataList = null;
        if (!TextUtils.isEmpty(ips)) {
            if (NetworkDash.isAvailable()) {
                dataList = new ArrayList<SpeedTestReportUdpTestInfo>();
                String[] ipAry = ips.split("#");
                for (String ipPort : ipAry) {
                    if (!TextUtils.isEmpty(ipPort)) {
                        String[] address = ipPort.split(":");
                        if (address != null && address.length == 2) {
                            try {
                                int port = Integer.parseInt(address[1]);
                                SpeedTestReportUdpTestInfo info = testServerForUdpByIpPort(
                                        address[0], port);
                                if (info != null) {
                                    dataList.add(info);
                                }
                            } catch (NumberFormatException e) {
                            }
                        }
                    }
                }
            }
        }
        return dataList;
    }

    // UDP
    private SpeedTestReportUdpTestInfo testServerForUdpByIpPort(String ip, int port) {
        SpeedTestReportUdpTestInfo info = null;
        if (!TextUtils.isEmpty(ip) && port > 0) {
            if (NetworkDash.isAvailable()) {
                short smallSeq = 1;
                byte[] small = buildUdpPingPackage(SMALL_UDP_CONTENT_LENGTH, smallSeq);
                short bigSeq = 2;
                byte[] big = buildUdpPingPackage(BIG_UDP_CONTENT_LENGTH, bigSeq);
                if (small != null || big != null) {
                    MiLinkLog.i(TAG, "speed test udp, IP = " + ip + " port = " + port);
                    info = new SpeedTestReportUdpTestInfo();
                    DatagramSocket socket = null;
                    try {
                        socket = new DatagramSocket();
                        InetAddress serverAddress = InetAddress.getByName(ip);
                        info.serverIp = ip;
                        info.port = port;
                        int readTimeout = ConfigManager.getInstance().getRequestTimeout();
                        info.readTimeout = readTimeout;
                        socket.setSoTimeout(readTimeout);

                        DatagramPacket outputPackage = null;

                        byte[] receiveBuffer = new byte[UDP_PACKAGE_HEAD_LENGTH
                                + BIG_UDP_CONTENT_LENGTH];

                        DatagramPacket inputPackage = new DatagramPacket(receiveBuffer, 0,
                                UDP_PACKAGE_HEAD_LENGTH + SMALL_UDP_CONTENT_LENGTH);
                        if (small != null) {
                            outputPackage = new DatagramPacket(small, small.length, serverAddress,
                                    port);
                            try {
                                long sendStart = System.currentTimeMillis();
                                socket.send(outputPackage);
                                info.smallStatus = STATUS_SUCCESS;
                                socket.receive(inputPackage);
                                info.smallRtt = System.currentTimeMillis() - sendStart;
                            } catch (SocketTimeoutException e) {
                                info.smallStatus = STATUS_FAILURE;
                                MiLinkLog.w(TAG,
                                        "testServerForUdpByIpPort small SocketTimeoutException");
                            } catch (IOException e) {
                                info.smallStatus = STATUS_FAILURE;
                                MiLinkLog.e(TAG, "testServerForUdpByIpPort small", e);
                            }
                        }

                        if (big != null) {
                            outputPackage = new DatagramPacket(big, big.length, serverAddress, port);
                            inputPackage.setData(receiveBuffer, 0, UDP_PACKAGE_HEAD_LENGTH
                                    + BIG_UDP_CONTENT_LENGTH);
                            try {
                                long sendStart = System.currentTimeMillis();
                                socket.send(outputPackage);
                                info.bigStatus = STATUS_SUCCESS;
                                socket.receive(inputPackage);
                                info.bigRtt = System.currentTimeMillis() - sendStart;
                            } catch (SocketTimeoutException e) {
                                info.bigStatus = STATUS_FAILURE;
                                MiLinkLog.w(TAG,
                                        "testServerForUdpByIpPort big SocketTimeoutException");
                            } catch (IOException e) {
                                info.bigStatus = STATUS_FAILURE;
                                MiLinkLog.e(TAG, "testServerForUdpByIpPort big", e);
                            }
                        }

                    } catch (UnknownHostException e) {
                        info.smallStatus = STATUS_FAILURE;
                        info.bigStatus = STATUS_FAILURE;
                        MiLinkLog.w(TAG, "testServerForUdpByIpPort UnknownHostException");
                    } catch (SocketException e) {
                        info.smallStatus = STATUS_FAILURE;
                        info.bigStatus = STATUS_FAILURE;
                        MiLinkLog.w(TAG, "testServerForUdpByIpPort SocketException");
                    } finally {
                        if (socket != null) {
                            socket.close();
                        }
                    }
                } else {
                    MiLinkLog.i(TAG, "speed test udp, package = null, IP = " + ip + " port = "
                            + port);
                }
            } else {
                MiLinkLog.i(TAG, "speed test udp, network is unavailable, IP = " + ip + " port = "
                        + port);
            }
        }
        return info;
    }

    /**
     * 获取ping包的数据流
     *
     * @return
     */
    private byte[] buildTcpPingPackage(int seq) {
        PacketData data = new PacketData();
        data.setCommand(Const.MnsCmd.MNS_PING_CMD);
        data.setSeqNo(seq);
        return StreamUtil.toUpBytes(String.format("[%s]", TAG), data, true,
                StreamUtil.MNS_ENCODE_NONE,MiAccountManager.getInstance().getCurrentAccount());
    }

    private byte[] buildUdpPingPackage(short contentLength, short seq) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            // HEAD
            String flagStr = "png\0";
            baos.write(flagStr.getBytes()); // udp 固定的flag，4个字节
            baos.write(Convert.shortToBytes(contentLength));// len，2个字节
            baos.write(Convert.shortToBytes(seq));// seq，2个字节
            // CONTENT
            byte[] content = new byte[contentLength];
            for (int i = 0; i < contentLength; i++) {
                content[i] = (byte) (i % 128);
            }
            baos.write(content);
            return baos.toByteArray();
        } catch (IOException e) {
        }
        return null;
    }

    // speed test 使用特殊的appId
    private int getSpeedTestAppId() {
        return 80000 + Global.getClientAppInfo().getAppId();
    }

}

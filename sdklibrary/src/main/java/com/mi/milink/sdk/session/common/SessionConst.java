
package com.mi.milink.sdk.session.common;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

import android.util.SparseArray;

import com.mi.milink.sdk.debug.MiLinkLog;

/**
 * @author MK
 */
public class SessionConst {
    private static final String TAG = "SessionConst";

    // 连接类型
    public static final int NONE_CONNECTION_TYPE = 0;

    public static final int TCP_CONNECTION_TYPE = 1;

    // public static final int HTTP_CONNECTION_TYPE = 2;

    // 服务器类型
    public static final int OPTI_IP = 1;

    public static final int REDIRECT_IP = 2;

    public static final int RECENTLY_IP = 3;

    public static final int DOMAIN_IP = 4;

    public static final int BACKUP_IP = 5;

    public static final int CDN_IP = 6;

    public static final int TEST_IP = 7;

    public static final int SCORE_IP = 8;

   
    // 失败原因
    public static final int CONN_FAILED = 1;

    public static final int HANDSHAKE_OTHERERROR_FAILED = 2;

    public static final int HANDSHAKE_PACKERROR_FAILED = 3;

    // 包的类型
    public static final int PACKET_TYPE_DOWNSTREAM = 0;

    public static final int PACKET_TYPE_TLV = 1;

    // 心跳包发送的场景
    public static final byte HEART_BEAT_SCENE_AFTER_HANDSHAKE_START = 1;

    public static final byte HEART_BEAT_SCENE_AFTER_HANDSHAKE_NORMAL = 2;

    public static final byte HEART_BEAT_SCENE_SHORT_TIMEOUT = 3;

    public static final byte HEART_BEAT_SCENE_LONG_TIMEOUT = 4;

    public static final byte HEART_BEAT_SCENE_EXIT_POWERSAVING = 5;

    public final static String ERROR_MSG = "ERROR_MSG";

    private static AtomicInteger uniqueSessionNO = new AtomicInteger(1);

    public static final String CDN_PICTURE_URL = "http://d.g.mi.com/t.html";

    private static volatile boolean sIsNewApn = true; // 切换到新APN

    public static final int TIME_OUT_MAX_RETRY_TIMES = 2; // 内部命令字超时最大重试次数
    
    public static final int ACC_NEED_CLIENT_RETRY_TIMES = 1; // acc需要带上缓存信息重试次数
    
    public static final int CONTINUOUS_RECV_TIMEOUT_PACKAGE_MAX_COUNT = 3; // 允许连续收到109超时包的次数
    
    public static int generateSessionNO() {
        return uniqueSessionNO.getAndIncrement();
    }

    public static String getProtocol(int conType) {
        return conTypeMap.get(conType);
    }

    public static String getSeverType(int serverType) {
        return serverTypeMap.get(serverType);
    }

    private static SparseArray<String> conTypeMap = new SparseArray<String>();

    private static SparseArray<String> serverTypeMap = new SparseArray<String>();

    static {
        conTypeMap.put(NONE_CONNECTION_TYPE, "none");
        conTypeMap.put(TCP_CONNECTION_TYPE, "tcp");
        // conTypeMap.put(HTTP_CONNECTION_TYPE, "http");

        serverTypeMap.put(OPTI_IP, "opt");
        serverTypeMap.put(REDIRECT_IP, "redirect");
        serverTypeMap.put(RECENTLY_IP, "recently");
        serverTypeMap.put(DOMAIN_IP, "dns");
        serverTypeMap.put(BACKUP_IP, "bak");
        serverTypeMap.put(CDN_IP, "cdn");
        serverTypeMap.put(TEST_IP, "test");
    }

    public static final int TEST_PIC_OK = 0;

    public static final int TEST_PIC_FAIL = 1;

    public static final int TEST_PIC_NEED_REDIRECT = 2;

    private static final int TEST_PIC_CONNECT_TIMEOUT = 5000;/* milliseconds */

    private static final int TEST_PIC_READ_TIMEOUT = 15000;/* milliseconds */

    public static boolean isInternetAvailable() {
        HttpURLConnection conn = null;
        try {
            StringBuilder urlSb = new StringBuilder();
            urlSb.append(CDN_PICTURE_URL);
            urlSb.append("?time=");
            urlSb.append(System.currentTimeMillis());
            URL url = new URL(urlSb.toString());
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(TEST_PIC_CONNECT_TIMEOUT);
            conn.setReadTimeout(TEST_PIC_READ_TIMEOUT);
            conn.setRequestMethod("GET");
            conn.addRequestProperty("Cache-Control", "no-cache");
            conn.setUseCaches(false);
            conn.setDefaultUseCaches(false);
            conn.setDoInput(true);
            conn.connect();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            StringBuilder result = new StringBuilder();
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
            MiLinkLog.v(TAG, "isInternetAvailable result=" + result.toString());
            if (result.toString().contains("milink.test")) {
                return true;
            } else {
                return false;
            }
        } catch (Throwable e) {
            MiLinkLog.v(TAG, "isInternetAvailable error");
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    public static void setNewApn(boolean isNew) {
        sIsNewApn = isNew;
    }

    public static boolean isNewApn() {
        boolean result = sIsNewApn;
        if (result) {
            sIsNewApn = false;
        }
        return result;
    }
}

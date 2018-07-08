
package com.mi.milink.sdk.client.ipc;

import java.util.Vector;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Pair;

import com.mi.milink.sdk.account.manager.MiAccountManager;
import com.mi.milink.sdk.aidl.IService;
import com.mi.milink.sdk.aidl.PacketData;
import com.mi.milink.sdk.base.Global;
import com.mi.milink.sdk.base.MessageTask;
import com.mi.milink.sdk.client.IEventListener;
import com.mi.milink.sdk.client.IPacketListener;
import com.mi.milink.sdk.client.MiLinkException;
import com.mi.milink.sdk.client.MiLinkObserver;
import com.mi.milink.sdk.client.SendPacketListener;
import com.mi.milink.sdk.client.ipc.internal.MiLinkServiceHost;
import com.mi.milink.sdk.client.ipc.internal.MnsSendPacketListener;
import com.mi.milink.sdk.data.ClientAppInfo;
import com.mi.milink.sdk.data.Const;
import com.mi.milink.sdk.mipush.MiPushManager;
import com.mi.milink.sdk.mipush.MiPushManager.MiPushRegisterListener;
import com.mi.milink.sdk.mipush.MiPushMessageListener;

/**
 * 实现异步发消息，同步发消息等接口
 *
 * @author CSM
 */

public class MiLinkClientIpc extends MiLinkServiceHost {

    private static MiLinkClientIpc INSTANCE;

    private MiLinkClientIpc() {
        super(Global.getApplicationContext());
    }

    public static final String ACTION_NO_LISTENER = "com.mi.milink.sdk.client.ipc.ACTION_NO_LISTENER";

    private static MiLinkClientIpc getInstance() {
        if (INSTANCE == null) {
            synchronized (MiLinkClientIpc.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MiLinkClientIpc();
                }
            }
        }
        return INSTANCE;
    }

    public static void setMiPushMessageListener(MiPushMessageListener listener) {
        MiPushManager.getInstance().setMessageListener(listener);
    }

    public static void clearNotification(int notifyId) {
        MiPushManager.getInstance().clearNotification(notifyId);
    }

    static boolean mPassportInit = false;

    /**
     * 所有init操作最终都调用到这
     *
     * @param appUserId
     * @param serviceToken
     * @param sSecurity
     * @param fastLoginExtra
     * @param passportInit              是否是带密码登陆
     * @return
     */
    public static boolean init(final String appUserId, final String serviceToken, final String sSecurity,
                               byte[] fastLoginExtra, boolean passportInit) {
        ClientLog.w(TAG, "init, milinkversion=" + Global.getMiLinkVersion() + "_" + Global.getMiLinkSubVersion());
        ClientLog.w(TAG,
                "init service,passportInit=" + passportInit + " ,app user id is " + appUserId + "serviceToken="
                        + serviceToken + ", serviceToken.length= " + serviceToken.length() + "security=" + sSecurity
                        + ", security.length= " + sSecurity.length());
        if (!mPassportInit) {
            mPassportInit = passportInit;
        }
        // 在app进程中设置userId
        MiAccountManager.getInstance().setUserId(appUserId);
        if (ClientAppInfo.isSupportMiPush()) {
            MiPushManager.getInstance().registerMiPush(appUserId, new MiPushRegisterListener() {

                @Override
                public void onSetMiPushRegId(String regId) {
                    setMiPushRegId(regId);
                }
            });
        }
        try {
            IService service = getRemoteService();
            if (service != null) {
                service.init(appUserId, serviceToken, sSecurity, fastLoginExtra, mPassportInit);
                mPassportInit = false;
                return true;
            } else {
                ClientLog.v(TAG, "init but remote service = null");
            }
        } catch (RemoteException e) {
            ClientLog.e(TAG, "error when init", e);
        }
        return false;
    }

    public static void setAllowAnonymousLoginSwitch(boolean on) {
        try {
            IService service = getRemoteService();
            if (service != null) {
                service.setAllowAnonymousLoginSwitch(on);
            } else {
                ClientLog.v(TAG, "setAllowAnonymousLoginSwitch but remote service = null");
            }
        } catch (RemoteException e) {
            ClientLog.e(TAG, "error when setAllowAnonymousLoginSwitch", e);
        }
    }

    public static void initUseAnonymousMode() {
        try {
            IService service = getRemoteService();
            if (service != null) {
                service.initUseAnonymousMode();
            } else {
                ClientLog.v(TAG, "initUseAnonymousMode but remote service = null");
            }
        } catch (RemoteException e) {
            ClientLog.e(TAG, "error when initUseAnonymousMode", e);
        }
    }

    /**
     * 设置IPacketCallback
     *
     * @param packetListener
     */
    public static void setPacketListener(final IPacketListener packetListener) {
        getInstance().mPacketListener = packetListener;
    }

    public static void setEventListener(final IEventListener eventListener) {
        getInstance().mEventListener = eventListener;
    }

    public static void setMilinkStateObserver(MiLinkObserver l) {
        ClientLog.v(TAG, "setMilinkStateObserver");
        getInstance().deleteObservers();
        getInstance().addObserver(l);
    }

    /**
     * 注销
     */
    public static void logoff() {
        if (ClientAppInfo.isSupportMiPush()) {
            MiPushManager.getInstance().logoff();
        }
        try {
            IService service = getRemoteService();
            if (service != null) {
                service.logoff();
                getInstance().stopService();
            } else {
                ClientLog.v(TAG, "logoff but remote service = null");
            }
        } catch (RemoteException e) {
            ClientLog.e(TAG, "error when logoff", e);
        }
    }

    /**
     * 手动设置milink连接的ip
     *
     * @param ip
     */
    public static void setIpAndPortInManualMode(String ip, int port) {
        try {
            IService service = getRemoteService();
            if (service != null) {
                service.setIpAndPortInManualMode(ip, port);
            } else {
                ClientLog.v(TAG, "setIpAndPortInManualMode but remote service = null");
            }
        } catch (RemoteException e) {
            ClientLog.e(TAG, "error when setIpAndPortInManualMode", e);
        }
    }

    public static IService getRemoteService() {
        return getInstance().getRemoteServiceProxy();
    }

    /**
     * 强制重连的接口
     */
    public static void forceReconnet() {
        try {
            IService service = getRemoteService();
            if (service != null) {
                service.forceReconnet();
            } else {
                ClientLog.v(TAG, "forceReconnet but remote service = null");
            }
        } catch (RemoteException e) {
            ClientLog.e(TAG, "error when forceReconnet", e);
        }
    }

    public static void fastLogin(final String appUserId, final String serviceToken, final String sSecurity,
                                 byte[] fastLoginExtra) {
        ClientLog.v(TAG, "fastLogin");
        try {
            IService service = getRemoteService();
            if (service != null) {
                service.fastLogin(appUserId, serviceToken, sSecurity, fastLoginExtra);
            } else {
                ClientLog.v(TAG, "registerBind but remote service = null");
            }
        } catch (RemoteException e) {
            ClientLog.e(TAG, "error when registerBind", e);
        }
    }

    public static String getSuid() {
        try {
            IService service = getRemoteService();
            if (service != null) {
                return service.getSuid();
            } else {
                ClientLog.v(TAG, "getSuid but remote service = null");
            }
        } catch (RemoteException e) {
            ClientLog.e(TAG, "error when getSuid", e);
        }
        return "";
    }

    public static long getAnonymousAccountId() {
        try {
            IService service = getRemoteService();
            if (service != null) {
                return service.getAnonymousAccountId();
            } else {
                ClientLog.v(TAG, "getSuid but remote service = null");
            }
        } catch (RemoteException e) {
            ClientLog.e(TAG, "error when getSuid", e);
        }
        return 0;
    }

    public static void sendAsync(PacketData packet) {
        sendAsync(packet, 0);
    }

    public static void sendAsync(PacketData packet, int timeout) {
        sendAsync(packet, timeout, null);
    }

    public static void sendAsync(PacketData packet, int timeout, final SendPacketListener l) {
        sendAsync(packet, timeout, l, false);
    }

    /**
     * 异步发送消息
     *
     * @param packet  发送的业务数据
     * @param timeout 等response的超时时间
     * @param l       服务器回包或者超时的listener
     */
    public static void sendAsync(PacketData packet, int timeout, final SendPacketListener l, boolean fromCache) {
        if (packet == null) {
            throw new IllegalArgumentException("Ary you kidding me ? packet is null");
        }
        if (TextUtils.isEmpty(packet.getCommand())) {
            throw new IllegalArgumentException("Packet's command is null");
        }
        try {
            IService service = getRemoteService();
            if (service != null) {
                if (l == null) {
                    service.sendAsyncWithResponse(packet, timeout, null);
                } else {
                    service.sendAsyncWithResponse(packet, timeout, new MnsSendPacketListener(l));
                }
            } else {
                if (!fromCache) {
                    // service 为空时，缓存住
                    getInstance().addToServiceNotReadyCache(new Pair<PacketData, SendPacketListener>(packet, l));
                    ClientLog.v(TAG, "sendAsync but remote service = null,try add to cache");
                } else {
                    if (l != null) {
                        l.onFailed(-3, "milink-service not ready!");
                    }
                }
            }
        } catch (RemoteException e) {
            ClientLog.e(TAG, "error when sendAsync", e);
        }
    }

    Vector<Pair<PacketData, SendPacketListener>> mServiceNotReadyCache = new Vector<Pair<PacketData, SendPacketListener>>();

    public void addToServiceNotReadyCache(Pair<PacketData, SendPacketListener> pair) {
        try {
            if (mServiceNotReadyCache.size() > 100) {
                Pair<PacketData, SendPacketListener> first = mServiceNotReadyCache.remove(0);
                if (first != null) {
                    first.second.onFailed(-4, "milink-service not ready and cache queue is full!!abandon");
                }
            }
            mServiceNotReadyCache.add(pair);
        } catch (Exception e) {
        }
    }

    boolean sendingMilinkServiceReadyCache = false;

    @Override
    protected void onMilinkServiceReady() {
        if (!sendingMilinkServiceReadyCache && !mServiceNotReadyCache.isEmpty()) {
            sendingMilinkServiceReadyCache = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    ClientLog.d(TAG, "onMilinkServiceReady send cache size:" + mServiceNotReadyCache.size());
                    try {
                        for (Pair<PacketData, SendPacketListener> pair : mServiceNotReadyCache) {
                            sendAsync(pair.first, 10 * 1000, pair.second, true);
                        }
                        mServiceNotReadyCache.clear();
                    } catch (Exception e) {
                    }
                }
            }).start();
        }
    }

    /**
     * 发送消息之后等待服务器返回结果，根据消息的seqNo判断是同一个消息的返回
     *
     * @param packet  发送的业务数据
     * @param timeout 等待服务器返回的超时时间
     * @return 服务器返回的结果
     */
    public static PacketData sendSync(final PacketData packet, final int timeout) {
        // if (getInstance().isServiceAvailable()) {
        MessageTask result = new MessageTask() {

            @Override
            public void doSendWork() {
                sendAsync(packet, timeout, new SendPacketListener() {

                    @Override
                    public void onResponse(PacketData packet) {
                        if (!isCancelled() && !isDone()) {
                            set(packet);
                        }
                    }

                    @Override
                    public void onFailed(int errCode, String errMsg) {
                        if (!isCancelled() && !isDone()) {
                            setException(new MiLinkException(errCode, errMsg));
                        }
                    }

                });
            }
        }.start();
        try {
            // client也加一个超时保护，防止service没有回调
            return result.getResult(timeout + 5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            ClientLog.e(TAG, "task InterruptedException", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause != null && (cause instanceof MiLinkException)) {
                // 如果能确定milink的具体业务，打印出来。防止什么错误抛出TimeoutException。
                ClientLog.e(TAG, "", cause);
            } else {
                ClientLog.e(TAG, "task ExecutionException", e);
            }
        } catch (CancellationException e) {
            ClientLog.e(TAG, "task CancellationException", e);
        } catch (TimeoutException e) {
            ClientLog.e(TAG, "task TimeoutException, detailName=" + e.getClass().getName());
        }
        // }
        return null;
    }

    /**
     * 得到连接状态，值在 Const.SessionState 中，0、1、2分别代表未连接，正在连接，已连接
     *
     * @return 连接状态的int值
     */
    public static int getMiLinkConnectState() {
        try {
            IService service = getRemoteService();
            if (service != null) {
                return service.getServerState();
            } else {
                ClientLog.v(TAG, "getMiLinkConnectState but remote service = null");
            }
        } catch (RemoteException e) {
            ClientLog.e(TAG, "error when getMiLinkConnectState", e);
        }
        return Const.SessionState.Disconnected;
    }

    /**
     * @return milink若已经登录，返回true，否则返回false；
     */
    public static boolean isMiLinkLogined() {
        try {
            IService service = getRemoteService();
            if (service != null) {
                return service.isMiLinkLogined();
            } else {
                ClientLog.v(TAG, "isMiLinkLogined but remote service = null");
            }
        } catch (RemoteException e) {
            ClientLog.e(TAG, "error when isMiLinkLogined", e);
        }
        return false;
    }

    /**
     * 所有的超时时间都会乘以这个值，例如通话时可以将这个值增大，如2，即原来15超时就变为30s超时
     *
     * @param timeoutMultiply
     */
    public static boolean setTimeoutMultiply(float timeoutMultiply) {
        if (timeoutMultiply < 1 || timeoutMultiply > 10) {
            ClientLog.e(TAG, "illegal timeoutMultiply，timeoutMultiply between 1-10");
            return false;
        }
        try {
            IService service = getRemoteService();
            if (service != null) {
                service.setTimeoutMultiply(timeoutMultiply);
                return true;
            } else {
                ClientLog.v(TAG, "setTimeoutMultiply but remote service = null");
            }
        } catch (RemoteException e) {
            ClientLog.e(TAG, "error when setBackground", e);
        }
        return false;
    }

    public static boolean enableConnectModeManual(boolean enable) {
        try {
            IService service = getRemoteService();
            if (service != null) {
                return service.enableConnectionManualMode(enable);
            } else {
                ClientLog.v(TAG, "enableConnectModeManual but remote service = null");
            }
        } catch (RemoteException e) {
            ClientLog.e(TAG, "error when setConnectModeAuto", e);
        }
        return false;
    }

    public static boolean setMiPushRegId(String regId) {
        ClientLog.v(TAG, "mMiPushRegId=" + regId);
        getInstance().mMiPushRegId = regId;
        try {
            IService service = getRemoteService();
            if (service != null) {
                service.setMipushRegId(regId);
            } else {
                ClientLog.v(TAG, "setMiPushRegId but remote service = null");
            }
        } catch (RemoteException e) {
            ClientLog.e(TAG, "error when setMiPushRegId", e);
        }
        return false;
    }

    /**
     * 怀疑sdk是坏的连接，会导致sdk重连，但底层会有保护。
     */
    public static void suspectBadConnection() {
        try {
            IService service = getRemoteService();
            if (service != null) {
                service.suspectBadConnection();
            } else {
                ClientLog.v(TAG, "suspectBadConnection but remote service = null");
            }
        } catch (RemoteException e) {
            ClientLog.e(TAG, "error when suspectBadConnection", e);
        }
    }

    public static void setMilinkLogLevel(int level) {
        getInstance().mLogLevel = level;
        try {
            IService service = getRemoteService();
            if (service != null) {
                service.setMilinkLogLevel(level);
            } else {
                ClientLog.v(TAG, "setMilinkLogLevel but remote service = null");
            }
        } catch (RemoteException e) {
            ClientLog.e(TAG, "error when setMilinkLogLevel", e);
        }
    }

    public static void setGlobalPushFlag(boolean enable) {
        getInstance().mGlobalPushFlag = enable;
        try {
            IService service = getRemoteService();
            if (service != null) {
                service.setGlobalPushFlag(enable);
            } else {
                ClientLog.v(TAG, "setMilinkLogLevel but remote service = null");
            }
        } catch (RemoteException e) {
            ClientLog.e(TAG, "error when setMilinkLogLevel", e);
        }
    }

    public static void setLanguage(String language) {
        try {
            IService service = getRemoteService();
            if (service != null) {
                service.setLanguage(language);
            } else {
                ClientLog.v(TAG, "setMilinkLogLevel but remote service = null");
            }
        } catch (RemoteException e) {
            ClientLog.e(TAG, "error when setMilinkLogLevel", e);
        }
    }
}

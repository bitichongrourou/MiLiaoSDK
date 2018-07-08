
package com.mi.milink.sdk.client;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.text.TextUtils;
import android.util.Log;

import com.mi.milink.sdk.account.AnonymousAccount;
import com.mi.milink.sdk.account.manager.MiAccountManager;
import com.mi.milink.sdk.aidl.IService;
import com.mi.milink.sdk.aidl.PacketData;
import com.mi.milink.sdk.base.Global;
import com.mi.milink.sdk.base.MessageTask;
import com.mi.milink.sdk.base.os.timer.AlarmClockService;
import com.mi.milink.sdk.client.ipc.ClientLog;
import com.mi.milink.sdk.config.ConfigManager;
import com.mi.milink.sdk.data.ClientAppInfo;
import com.mi.milink.sdk.debug.MiLinkLog;
import com.mi.milink.sdk.event.MiLinkEvent.ClientActionEvent;
import com.mi.milink.sdk.event.MiLinkEvent.SessionManagerNotificationEvent;
import com.mi.milink.sdk.event.MiLinkEvent.SessionManagerStateChangeEvent;
import com.mi.milink.sdk.mipush.MiPushManager;
import com.mi.milink.sdk.mipush.MiPushManager.MiPushRegisterListener;
import com.mi.milink.sdk.mipush.MiPushMessageListener;
import com.mi.milink.sdk.proto.PushPacketProto.KickMessage;
import com.mi.milink.sdk.service.MiLinkExceptionHandler;
import com.mi.milink.sdk.session.common.ResponseListener;
import com.mi.milink.sdk.session.persistent.MnsPacketDispatcher;
import com.mi.milink.sdk.session.persistent.SessionManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

/**
 * @author chengsimin
 */
public class MiLinkClient {
    private final static String TAG = "MiLinkClient";

    private static MiLinkClient INSTANCE;

    private MiLinkObserver mMiLinkObserver;

    private IEventListener mEventCallback;

    private boolean mMiPushSwitch = false;

    private MiLinkClient() {
        EventBus.getDefault().register(this);
        // PreloadClearUtil.clearResources();
        Thread.setDefaultUncaughtExceptionHandler(new MiLinkExceptionHandler());
        MiLinkLog.w(TAG, "MiLinkClient no ipc build");
        AlarmClockService.start();
    }

    private static MiLinkClient getInstance() {
        if (INSTANCE == null) {
            synchronized (MiLinkClient.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MiLinkClient();
                }
            }
        }
        return INSTANCE;
    }

    public static void setMilinkStateObserver(MiLinkObserver l) {
        getInstance().mMiLinkObserver = l;
    }

    public static void setPacketListener(IPacketListener l) {
        Log.w("wangchuntao","MiLinkClient setPacketListener:"+(l == null));
        MnsPacketDispatcher.getInstance().setCallback(l);
    }

    public static void setEventListener(IEventListener l) {
        getInstance().mEventCallback = l;
    }

    public static void setDispatchPacketDelayMillis(int delayTime) {
        MnsPacketDispatcher.getInstance().setDispatchPacketDelayTime(delayTime);
    }

    /**
     * 匿名登录
     */
    public static void initUseAnonymousMode() {
        SessionManager.getInstance().initApp();
        MiAccountManager.getInstance().initUseAnonymousMode();
    }

    /**
     * 设置匿名模式开关，如果设置开启，实名登出后会自动用匿名模式登录。
     * 
     * @param on
     */
    public static void setAllowAnonymousLoginSwitch(boolean on) {
        MiAccountManager.getInstance().setAnonymousModeSwitch(on);
    }

    /**
     * 实名初始化接口
     * 
     * @param appUserId
     * @param serviceToken
     * @param sSecurity
     * @param fastLoginExtra
     * @param passportInit 是否是带密码登陆，服务端互踢依据之一
     * @return
     */
    public static void init(final String appUserId, final String serviceToken,
            final String sSecurity, byte[] fastLoginExtra, boolean passportInit) {
        MiLinkLog.w(
                TAG,
                "init, milinkversion=" + Global.getMiLinkVersion() + "_"
                        + Global.getMiLinkSubVersion());
        MiLinkLog.v(TAG, "init service,passportInit=" + passportInit + " ,app user id is "
                + appUserId + "serviceToken=" + serviceToken + ", serviceToken.length= "
                + serviceToken.length() + "security=" + sSecurity + ", security.length= "
                + sSecurity.length());
        if (ClientAppInfo.isSupportMiPush()) {
            MiPushManager.getInstance().registerMiPush(appUserId,new MiPushRegisterListener() {

                @Override
                public void onSetMiPushRegId(String regId) {
                    MiAccountManager.getInstance().setMipushRegId(regId);
                }
            });
        }
        // 在app进程中设置userId
        MiAccountManager.getInstance().setUserId(appUserId);
        SessionManager.getInstance().initApp();
        MiAccountManager.getInstance().login(appUserId, serviceToken, sSecurity, fastLoginExtra,
                passportInit);
    }

    public static void init(final String appUserId, final String serviceToken,
            final String sSecurity, byte[] fastLoginExtra, boolean passportInit, boolean isIpModle) {
        MiLinkLog.w(
                TAG,
                "init, milinkversion=" + Global.getMiLinkVersion() + "_"
                        + Global.getMiLinkSubVersion());
        MiLinkLog.v(TAG, "init service,passportInit=" + passportInit + " ,app user id is "
                + appUserId + "serviceToken=" + serviceToken + ", serviceToken.length= "
                + serviceToken.length() + "security=" + sSecurity + ", security.length= "
                + sSecurity.length());
        // 在app进程中设置userId
        MiAccountManager.getInstance().setUserId(appUserId);
        SessionManager.getInstance().initApp();
        MiAccountManager.getInstance().login(appUserId, serviceToken, sSecurity, fastLoginExtra,
                passportInit);
    }

    /**
     * 异步发包
     * 
     * @param data 发送的数据
     * @param timeout 超时时间
     * @param l 发包成功或者失败的回调监听，如果设置为null，包会从IPacketListener的onReceive中回来。
     */
    public static void sendAsync(PacketData data, int timeout, final SendPacketListener l) {
        SessionManager.getInstance().sendData(data, timeout, new ResponseListener() {
            @Override
            public void onDataSendSuccess(int errCode, PacketData data) {
                l.onResponse(data);
            }

            @Override
            public void onDataSendFailed(int errCode, String errMsg) {
                l.onFailed(errCode, errMsg);
            }
        });
    }

    public static void sendAsync(PacketData data, int timeout) {
        SessionManager.getInstance().sendData(data, timeout, null);
    }

    public static void sendAsync(PacketData data) {
        SessionManager.getInstance().sendData(data, 0, null);
    }

    /**
     * 同步阻塞发包
     * 
     * @param packet 数据
     * @param timeout 超时时间
     * @return 返回端返回的包
     */
    public static PacketData sendSync(final PacketData packet, final int timeout) {
        if (packet == null) {
            throw new IllegalArgumentException(" packet is null");
        }
        if (TextUtils.isEmpty(packet.getCommand())) {
            throw new IllegalArgumentException("Packet's command is null");
        }
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
            MiLinkLog.e(TAG, "task InterruptedException", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause != null && (cause instanceof MiLinkException)) {
                // 如果能确定milink的具体业务，打印出来。防止什么错误抛出TimeoutException。
                MiLinkLog.e(TAG, "", cause);
            } else {
                MiLinkLog.e(TAG, "task ExecutionException", e);
            }
        } catch (CancellationException e) {
            MiLinkLog.e(TAG, "task CancellationException", e);
        } catch (TimeoutException e) {
            MiLinkLog.e(TAG, "task TimeoutException, detailName=" + e.getClass().getName());
        }
        return null;
    }

    /**
     * 登出，会断开连接，并清空token等信息
     */
    public static void logoff() {
        MiLinkLog.i(TAG, "logoff");
        if (ClientAppInfo.isSupportMiPush()) {
            MiPushManager.getInstance().logoff();
        }
        MiAccountManager.getInstance().userLogoff();
    }

    /**
     * 强制重连，会使milinksdk断开连接再重新连接
     */
    public static void forceReconnect() {
        MiLinkLog.i(TAG, "forceReconnet");
        EventBus.getDefault().post(
                new ClientActionEvent(ClientActionEvent.EventType.ClientForceOpen));
    }

    public static void setMilinkLogLevel(int level) {
        MiLinkLog.setLogcatTraceLevel(level);
        MiLinkLog.setFileTraceLevel(level);
        
        ClientLog.setLogcatTraceLevel(level);
        ClientLog.setFileTraceLevel(level);
    }

    // public static void setIpModle(boolean ipModle) {
    //
    // Global.setIPModle(ipModle);
    // MiLinkLog.d(TAG, "IpModle = "+ Global.getIpModle());
    // }

    /**
     * 得到连接状态，值在 Const.SessionState 中，0、1、2分别代表未连接，正在连接，已连接
     * 
     * @return 连接状态的int值
     */
    public static int getMiLinkConnectState() {
        return SessionManager.getInstance().getSessionState();
    }

    /**
     * @return milink若已经登录，返回true，否则返回false；
     */
    public static boolean isMiLinkLogined() {
        MiLinkLog.i(TAG, "isMiLinkLogined");
        return SessionManager.getInstance().isMiLinkLogined();
    }

    public static boolean enableConnectModeManual(boolean enable) {
        return SessionManager.getInstance().enableConnectionManualMode(enable);
    }

    /**
     * 手动设置milink连接的ip
     *
     * @param ip
     */
    public static void setIpAndPortInManualMode(String ip, int port) {
        SessionManager.getInstance().setIpAndPortInManualMode(ip, port);
    }

    /**
     * 
     * @return 匿名模式下的id
     */
    public static long getAnonymousAccountId(){
        try {
            return Long.parseLong(AnonymousAccount.getInstance().getUserId());
        } catch (Exception e) {
            return 0;
        }
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
        ConfigManager.getInstance().setTimeoutMultiply(timeoutMultiply);
        return true;
    }

    public static void setMiPushMessageListener(MiPushMessageListener listener) {
        MiPushManager.getInstance().setMessageListener(listener);
    }

    public static void clearNotification(int notifyId) {
        MiPushManager.getInstance().clearNotification(notifyId);
    }

    public static void setLanguage(String language){
        Global.getClientAppInfo().setLanguageCode(language);
    }
    
    /** 回调 **/
    protected void onSessionStateChanged(int oldState, int newState) {
        if (mMiLinkObserver != null) {
            mMiLinkObserver.onServerStateUpdate(oldState, newState);
        }
    }

    protected void onLoginStateChanged(int newState) {
        if (mMiLinkObserver != null) {
            mMiLinkObserver.onLoginStateUpdate(newState);
        }
    }

    protected void onEventShouldUpdate() {
        if (mEventCallback != null) {
            mEventCallback.onEventShouldCheckUpdate();
        }
    }

    protected void onEventServiceTokenExpired() {
        if (mEventCallback != null) {
            mEventCallback.onEventServiceTokenExpired();
        }
    }

    protected void onEventKickByServer(int type, long time, String device) {
        if (mEventCallback != null) {
            mEventCallback.onEventKickedByServer(type, time, device);
        }
    }

    protected void onEventGetServiceToken() {
        if (mEventCallback != null) {
            mEventCallback.onEventGetServiceToken();
        }
    }

    protected void onEventInvilidPacket() {
        if (mEventCallback != null) {
            mEventCallback.onEventInvalidPacket();
        }
    }

    @Subscribe
    public void onEvent(SessionManagerStateChangeEvent event) {
        switch (event.mEventType) {
            case LoginStateChange: {
                onLoginStateChanged(event.mNewState);
            }
                break;
            case SessionStateChange: {
                onSessionStateChanged(event.mOldState, event.mNewState);
            }
                break;
            default:
                break;
        }
    }

    @Subscribe
    public void onEvent(SessionManagerNotificationEvent event) {
        switch (event.mEventType) {
            case GetServiceToken: {
                onEventGetServiceToken();
            }
                break;
            case KickByServer: {
                int type = 0;
                long kicktime = 0;
                String device = "";
                if (event.mObject != null) {
                    KickMessage kickMsg = (KickMessage) event.mObject;
                    type = kickMsg.getType();
                    kicktime = kickMsg.getTime();
                    device = kickMsg.getDevice();
                }
                onEventKickByServer(type, kicktime * 1000, device);
            }
                break;
            case ServiceTokenExpired: {
                onEventServiceTokenExpired();
            }
                break;
            case ShouldUpdate: {
                onEventShouldUpdate();
            }
                break;
            case RecvInvalidPacket:
                onEventInvilidPacket();
            default:
                break;
        }
    }

}

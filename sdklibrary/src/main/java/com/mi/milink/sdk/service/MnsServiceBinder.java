
package com.mi.milink.sdk.service;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.os.Messenger;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import com.mi.milink.sdk.account.AnonymousAccount;
import com.mi.milink.sdk.account.manager.MiAccountManager;
import com.mi.milink.sdk.aidl.IEventCallback;
import com.mi.milink.sdk.aidl.IPacketCallback;
import com.mi.milink.sdk.aidl.ISendCallback;
import com.mi.milink.sdk.aidl.IService;
import com.mi.milink.sdk.aidl.PacketData;
import com.mi.milink.sdk.base.Global;
import com.mi.milink.sdk.client.ClientConstants;
import com.mi.milink.sdk.client.IPacketListener;
import com.mi.milink.sdk.config.ConfigManager;
import com.mi.milink.sdk.data.Const;
import com.mi.milink.sdk.debug.MiLinkLog;
import com.mi.milink.sdk.event.MiLinkEvent.ClientActionEvent;
import com.mi.milink.sdk.event.MiLinkEvent.SessionManagerNotificationEvent;
import com.mi.milink.sdk.event.MiLinkEvent.SessionManagerStateChangeEvent;
import com.mi.milink.sdk.event.MiLinkEvent.SystemNotificationEvent;
import com.mi.milink.sdk.proto.PushPacketProto.KickMessage;
import com.mi.milink.sdk.session.common.ResponseListener;
import com.mi.milink.sdk.session.persistent.MnsPacketDispatcher;
import com.mi.milink.sdk.session.persistent.SessionManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

/**
 * service端对应client实现的接口
 *
 * @author MK
 */

public class MnsServiceBinder extends IService.Stub implements IPacketListener {

    private static final String TAG = "MnsServiceBinder";

    private static final MnsServiceBinder sInstance = new MnsServiceBinder();

    private final RemoteCallbackList<IPacketCallback> mPacketCallBackList = new RemoteCallbackList<IPacketCallback>();

    private final RemoteCallbackList<IEventCallback> mEventCallBackList = new RemoteCallbackList<IEventCallback>();

    private MnsServiceBinder() {
        EventBus.getDefault().register(this);
        Log.w("wangchuntao","MnsServiceBinder setPacketListener");
        MnsPacketDispatcher.getInstance().setCallback(this);
        /** 在这里实例化SM更合适，说明bind成功了 **/
        SessionManager.getInstance();
        EventBus.getDefault().post(
                new SystemNotificationEvent(SystemNotificationEvent.EventType.ServiceCreated));
        long end = System.currentTimeMillis();
    }

    public static MnsServiceBinder getInstance() {
        return sInstance;
    }

    /**
     * 初始化需要传入接收消息的callback
     */
    @Override
    public void init(String appUserId, String serviceToken, String sSecurity,
            byte[] fastLoginExtra, boolean passportInit) throws RemoteException {
        MiLinkLog.w(TAG, "init,passportInit=" + passportInit);
        SessionManager.getInstance().initApp();
        MiAccountManager.getInstance().login(appUserId, serviceToken, sSecurity, fastLoginExtra,
                passportInit);
    }

    @Override
    public void setEventCallBack(IEventCallback eCallback) {
        mEventCallBackList.register(eCallback);
    }

    @Override
    public void setPacketCallBack(IPacketCallback pCallback) throws RemoteException {
        mPacketCallBackList.register(pCallback);
    }

    /**
     * 异步发送，service仅支持异步发，同步发实现在client中，根据seqNo判断
     */
    @Override
    public void sendAsyncWithResponse(PacketData data, int timeout, final ISendCallback callback)
            throws RemoteException {
        if (callback == null) {
            SessionManager.getInstance().sendData(data, timeout, null);
        } else {
            SessionManager.getInstance().sendData(data, timeout, new ResponseListener() {

                @Override
                public void onDataSendSuccess(int errCode, PacketData res) {
                    if (callback != null) {
                        try {
                            callback.onRsponse(res);
                        } catch (RemoteException e) {
                            // app死了就没必要response了
                        }
                    }
                }

                @Override
                public void onDataSendFailed(int errCode, String errMsg) {
                    if (callback != null) {
                        try {
                            callback.onFailed(errCode, errMsg);
                        } catch (RemoteException e) {
                            // app死了就没必要response了
                        }
                    }
                }
            });
        }
    }

    @Override
    public void logoff() throws RemoteException {
        MiLinkLog.i(TAG, "logoff");
        MiAccountManager.getInstance().userLogoff();
    }

    @Override
    public int setClientInfo(Bundle clientInfo) throws RemoteException {
        try {
            if (clientInfo == null) {
                return Integer.MIN_VALUE;
            }
            Messenger messenger = clientInfo.getParcelable(Const.IPC.ClientNotifier);

            if (messenger == null) {
                return Integer.MIN_VALUE;
            }
            start(messenger);
            return Process.myPid();
        } catch (Exception e) {
            throw new java.lang.Error(e);
        }
    }

    private MnsServiceBinder start(Messenger messenger) {
        MnsNotify.setMessenger(messenger);
        // 立刻通知客户端关于启动时间
        MnsNotify.sendEvent(Const.Event.SERVICE_CONNECTED, 0, System.currentTimeMillis());

        return this;
    }

    @Override
    public int getServerState() {
        try {
            return SessionManager.getInstance().getSessionState();
        } catch (Exception e) {
            throw new java.lang.Error(e);
        }
    }

    @Override
    public boolean isMiLinkLogined() {
        MiLinkLog.i(TAG, "isMiLinkLogined");
        try {
            return SessionManager.getInstance().isMiLinkLogined();
        } catch (Exception e) {
            throw new java.lang.Error(e);
        }
    }

    public boolean onError(int errorCode, String errMsg, Object obj) {
        // 通知WNS全局错误
        MnsNotify.sendEvent(Const.Event.MNS_INTERNAL_ERROR, errorCode, errMsg);
        return true;
    }

    @Override
    public void setIpAndPortInManualMode(String ip, int port) throws RemoteException {
        SessionManager.getInstance().setIpAndPortInManualMode(ip, port);
    }

    @Override
    public String getSuid() throws RemoteException {
        MiLinkLog.i(TAG, "getSuid");
        return ConfigManager.getInstance().getSuid();
    }

    @Override
    public void forceReconnet() throws RemoteException {
        MiLinkLog.i(TAG, "forceReconnet");
        EventBus.getDefault().post(
                new ClientActionEvent(ClientActionEvent.EventType.ClientForceOpen));
    }

    @Override
    public void fastLogin(String appUserId, String serviceToken, String sSecurity,
            byte[] fastLoginExtra) throws RemoteException {
        MiLinkLog.i(TAG, "fastLogin");
        MiAccountManager.getInstance().login(appUserId, serviceToken, sSecurity, fastLoginExtra,
                false);
    }

    @Override
    public boolean enableConnectionManualMode(boolean enable) throws RemoteException {
        return SessionManager.getInstance().enableConnectionManualMode(enable);
    }

    @Override
    public void setTimeoutMultiply(float timeoutMultiply) throws RemoteException {
        ConfigManager.getInstance().setTimeoutMultiply(timeoutMultiply);
    }

    /**
     * 接受push包，运行在MnsPacketDispatcher线程
     * 
     * @param list
     * @return
     */
    @Override
    public void onReceive(ArrayList<PacketData> list) {
        List<IPacketCallback> deadCallback = new ArrayList<IPacketCallback>();
        MiLinkLog.v(TAG, "delivery data, data size=" + list.size());
        boolean aidlSuccess = false;
        int n = mPacketCallBackList.beginBroadcast();
        for (int i = 0; i < n; i++) {
            IPacketCallback callback = mPacketCallBackList.getBroadcastItem(i);
            try {
                if (callback.onReceive(list)) {
                    aidlSuccess = true;
                    MiLinkLog.v(TAG, "delivery data success");
                } else {
                    aidlSuccess = false;
                    MiLinkLog.v(TAG, "onReceive return false,try delivery data by broadcast");
                }
            } catch (Exception e) {
                MiLinkLog.w(TAG, "dead callback.");
                deadCallback.add(callback);
            }
        }
        mPacketCallBackList.finishBroadcast();
        for (IPacketCallback callback : deadCallback) {
            MiLinkLog.v(TAG, "unregister callback.");
            mPacketCallBackList.unregister(callback);
        }
        if (!aidlSuccess) {
            long time = System.currentTimeMillis();
            MiLinkLog.v(TAG, "app not run, delivery data by broadcast, data size=" + list.size()
                    + ", time=" + time);
            // 唤醒app
            Intent intent = new Intent(ClientConstants.ACTION_DISPATCH_MSG);
            intent.putParcelableArrayListExtra(ClientConstants.EXTRA_ACTION_DISPATCH_MSG_ARRAY,
                    list);
            intent.putExtra(ClientConstants.EXTRA_ACTION_TIME, time);
            intent.setPackage(Global.getClientAppInfo().getPackageName());
            Global.sendBroadcast(intent);
        }
    }

    private boolean onSessionStateChanged(int oldState, int newState) {
        MiLinkLog.i(TAG, "Session State Changed From " + oldState + " → " + newState);

        if (oldState != newState) {
            // 通知服务器连接状态变更
            MnsNotify.sendEvent(Const.Event.SERVER_STATE_UPDATED, oldState, newState);
        }
        return true;
    }

    private boolean onMiLinkLoginStateChanged(int state) {
        MiLinkLog.i(TAG, "onMiLinkLoginStateChanged state=" + state);
        MnsNotify.sendEvent(Const.Event.MI_LINK_LOGIN_STATE_CHANGED, 0, state);
        return true;
    }

    @Subscribe
    public void onEvent(SessionManagerStateChangeEvent event) {
        switch (event.mEventType) {
            case LoginStateChange: {
                onMiLinkLoginStateChanged(event.mNewState);
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

    private void onEventGetServiceToken() {
        MiLinkLog.v(TAG, "no service token, call app onEventGetServiceToken");
        List<IEventCallback> deadCallback = new ArrayList<IEventCallback>();
        boolean aidlSuccess = false;
        int n = mEventCallBackList.beginBroadcast();
        for (int i = 0; i < n; i++) {
            IEventCallback callback = mEventCallBackList.getBroadcastItem(i);
            try {
                callback.onEventGetServiceToken();
                aidlSuccess = true;
                MiLinkLog.v(TAG, " notify app get service token success");
            } catch (Exception e) {
                MiLinkLog.v(TAG, "dead callback.");
                deadCallback.add(callback);
            }
        }
        mEventCallBackList.finishBroadcast();
        for (IEventCallback callback : deadCallback) {
            MiLinkLog.v(TAG, "unregister event callback.");
            mEventCallBackList.unregister(callback);
        }
        if (!aidlSuccess) {
            long time = System.currentTimeMillis();
            MiLinkLog.v(TAG, "app does not run, broadcast get service token, time=" + time);
            // 唤醒app
            Intent intent = new Intent(ClientConstants.ACTION_EVENT_GET_SERVICE_TOKEN);
            intent.putExtra(ClientConstants.EXTRA_ACTION_TIME, time);
            intent.setPackage(Global.getClientAppInfo().getPackageName());
            Global.sendBroadcast(intent);
        }
    }

    private void onEventServiceTokenExpired() {
        MiLinkLog.v(TAG, "service token expired, call app onEventServiceTokenExpired");
        List<IEventCallback> deadCallback = new ArrayList<IEventCallback>();
        boolean aidlSuccess = false;
        int n = mEventCallBackList.beginBroadcast();
        for (int i = 0; i < n; i++) {
            IEventCallback callback = mEventCallBackList.getBroadcastItem(i);
            try {
                callback.onEventServiceTokenExpired();
                aidlSuccess = true;
                MiLinkLog.v(TAG, " notify app service token expired success");
            } catch (Exception e) {
                MiLinkLog.v(TAG, "dead callback.");
                deadCallback.add(callback);
            }
        }
        mEventCallBackList.finishBroadcast();
        for (IEventCallback callback : deadCallback) {
            MiLinkLog.v(TAG, "unregister event callback.");
            mEventCallBackList.unregister(callback);
        }
        if (!aidlSuccess) {
            long time = System.currentTimeMillis();
            MiLinkLog.v(TAG, "app does not run, broadcast service token expired, time=" + time);
            // 唤醒app
            Intent intent = new Intent(ClientConstants.ACTION_EVENT_SERVICE_TOKEN_EXPIRED);
            intent.putExtra(ClientConstants.EXTRA_ACTION_TIME, time);
            intent.setPackage(Global.getClientAppInfo().getPackageName());
            Global.sendBroadcast(intent);
        }
    }

    private void onEventKickByServer(int type, long kicktime, String device) {
        MiLinkLog.v(TAG, String.format(
                "kicked by server, type is %d,time is %d s,device is %s call app onKickedByServer",
                type, kicktime, device));
        List<IEventCallback> deadCallback = new ArrayList<IEventCallback>();
        boolean aidlSuccess = false;
        int n = mEventCallBackList.beginBroadcast();
        for (int i = 0; i < n; i++) {
            IEventCallback callback = mEventCallBackList.getBroadcastItem(i);
            try {
                callback.onEventKickedByServer(type, kicktime * 1000, device);
                aidlSuccess = true;
                MiLinkLog.v(TAG, " notify app service kicked by server success");
            } catch (Exception e) {
                MiLinkLog.v(TAG, "dead callback.");
                deadCallback.add(callback);
            }
        }
        mEventCallBackList.finishBroadcast();
        for (IEventCallback callback : deadCallback) {
            MiLinkLog.v(TAG, "unregister event callback.");
            mEventCallBackList.unregister(callback);
        }
        if (!aidlSuccess) {
            long time = System.currentTimeMillis();
            MiLinkLog.v(TAG, "app does not run, broadcast kicked by server, time=" + time);
            // 唤醒app
            Intent intent = new Intent(ClientConstants.ACTION_EVENT_KICKED_BY_SERVER);
            intent.putExtra(ClientConstants.EXTRA_ACTION_TIME, time);
            intent.putExtra(ClientConstants.EXTRA_KICKED_BY_SERVER_TYPE, device);
            intent.putExtra(ClientConstants.EXTRA_KICKED_BY_SERVER_TIME, kicktime);
            intent.putExtra(ClientConstants.EXTRA_KICKED_BY_SERVER_DEVICE, device);
            intent.setPackage(Global.getClientAppInfo().getPackageName());
            Global.sendBroadcast(intent);
        }
    }

    private void onEventShouldUpdate() {
        MiLinkLog.v(TAG, "app should check update.");
        List<IEventCallback> deadCallback = new ArrayList<IEventCallback>();
        boolean aidlSuccess = false;
        int n = mEventCallBackList.beginBroadcast();
        for (int i = 0; i < n; i++) {
            IEventCallback callback = mEventCallBackList.getBroadcastItem(i);
            try {
                callback.onEventShouldCheckUpdate();
                aidlSuccess = true;
                MiLinkLog.v(TAG, " notify app check update success");
            } catch (Exception e) {
                MiLinkLog.v(TAG, "dead callback.");
                deadCallback.add(callback);
            }
        }
        mEventCallBackList.finishBroadcast();
        for (IEventCallback callback : deadCallback) {
            MiLinkLog.v(TAG, "unregister event callback.");
            mEventCallBackList.unregister(callback);
        }
        if (!aidlSuccess) {
            long time = System.currentTimeMillis();
            MiLinkLog.v(TAG, "app does not run, broadcast check update, time=" + time);
            // 唤醒app
            Intent intent = new Intent(ClientConstants.ACTION_EVENT_CHECK_UPDATE);
            intent.putExtra(ClientConstants.EXTRA_ACTION_TIME, time);
            intent.setPackage(Global.getClientAppInfo().getPackageName());
            Global.sendBroadcast(intent);
        }
    }

    private void onEventRecvInvalidPacket() {
        MiLinkLog.v(TAG, "invalid packet, call app onEventInvalidPacket ");
        List<IEventCallback> deadCallback = new ArrayList<IEventCallback>();
        boolean aidlSuccess = false;
        int n = mEventCallBackList.beginBroadcast();
        for (int i = 0; i < n; i++) {
            IEventCallback callback = mEventCallBackList.getBroadcastItem(i);
            try {
                callback.onEventInvalidPacket();
                aidlSuccess = true;
                MiLinkLog.v(TAG, " notify app invalid packet success");
            } catch (Exception e) {
                MiLinkLog.v(TAG, "dead callback.");
                deadCallback.add(callback);
            }
        }
        mEventCallBackList.finishBroadcast();
        for (IEventCallback callback : deadCallback) {
            MiLinkLog.v(TAG, "unregister event callback.");
            mEventCallBackList.unregister(callback);
        }
        if (!aidlSuccess) {
            long time = System.currentTimeMillis();
            MiLinkLog.v(TAG, "app does not run, broadcast invalid packet, time=" + time);
            // 唤醒app
            Intent intent = new Intent(ClientConstants.ACTION_EVENT_INVALID_PACKET);
            intent.putExtra(ClientConstants.EXTRA_ACTION_TIME, time);
            intent.setPackage(Global.getClientAppInfo().getPackageName());
            Global.sendBroadcast(intent);
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
                onEventKickByServer(type, kicktime, device);
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
            case RecvInvalidPacket: {
                onEventRecvInvalidPacket();
            }
                break;
            default:
                break;
        }
    }

    @Override
    public void setAllowAnonymousLoginSwitch(boolean on) throws RemoteException {
        MiAccountManager.getInstance().setAnonymousModeSwitch(on);
    }

    @Override
    public void initUseAnonymousMode() throws RemoteException {
        SessionManager.getInstance().initApp();
        MiAccountManager.getInstance().initUseAnonymousMode();
    }

    @Override
    public void setMipushRegId(String regId) throws RemoteException {
        MiAccountManager.getInstance().setMipushRegId(regId);
    }

    @Override
    public void suspectBadConnection() throws RemoteException {
        SessionManager.getInstance().suspectBadConnection();
    }

    @Override
    public void setMilinkLogLevel(int level) throws RemoteException {
        MiLinkLog.setLogcatTraceLevel(level);
        MiLinkLog.setFileTraceLevel(level);
    }

    @Override
    public void setLanguage(String language) throws RemoteException {
        Global.getClientAppInfo().setLanguageCode(language);
    }

    @Override
    public long getAnonymousAccountId() throws RemoteException {
        try {
            return Long.parseLong(AnonymousAccount.getInstance().getUserId());
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public void setGlobalPushFlag(boolean enable) throws RemoteException {
        SessionManager.getInstance().setGlobalPushFlag(enable);
    }

}


package com.mi.milink.sdk.session.persistent;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.mi.milink.sdk.account.AnonymousAccount;
import com.mi.milink.sdk.account.manager.MiAccountManager;
import com.mi.milink.sdk.aidl.PacketData;
import com.mi.milink.sdk.base.CustomHandlerThread;
import com.mi.milink.sdk.base.Global;
import com.mi.milink.sdk.base.os.Device.Network;
import com.mi.milink.sdk.base.os.Device.Network.NetworkDetailInfo;
import com.mi.milink.sdk.base.os.info.NetworkDash;
import com.mi.milink.sdk.base.os.info.WifiDash;
import com.mi.milink.sdk.base.os.timer.AlarmClockService;
import com.mi.milink.sdk.config.HeartBeatManager;
import com.mi.milink.sdk.config.MiLinkIpInfoManager;
import com.mi.milink.sdk.connection.DomainManager;
import com.mi.milink.sdk.data.Const;
import com.mi.milink.sdk.debug.BaseDataMonitor;
import com.mi.milink.sdk.debug.InternalDataMonitor;
import com.mi.milink.sdk.debug.MiLinkLog;
import com.mi.milink.sdk.debug.TrafficMonitor;
import com.mi.milink.sdk.event.MiLinkEvent.ChannelStatusChangeEvent;
import com.mi.milink.sdk.event.MiLinkEvent.ClientActionEvent;
import com.mi.milink.sdk.event.MiLinkEvent.ServerNotificationEvent;
import com.mi.milink.sdk.event.MiLinkEvent.SessionConnectEvent;
import com.mi.milink.sdk.event.MiLinkEvent.SessionLoginEvent;
import com.mi.milink.sdk.event.MiLinkEvent.SessionManagerNotificationEvent;
import com.mi.milink.sdk.event.MiLinkEvent.SessionManagerStateChangeEvent;
import com.mi.milink.sdk.event.MiLinkEvent.SessionOtherEvent;
import com.mi.milink.sdk.event.MiLinkEvent.SystemNotificationEvent;
import com.mi.milink.sdk.proto.DataExtraProto.DataLoglevel;
import com.mi.milink.sdk.proto.PushPacketProto.MilinkLogReq;
import com.mi.milink.sdk.proto.PushPacketProto.PushLogLevel;
import com.mi.milink.sdk.proto.SystemPacketProto.MnsCmdChannelNewPubKeyRsp;
import com.mi.milink.sdk.proto.SystemPacketProto.PublicKeyInfo;
import com.mi.milink.sdk.session.common.IServerManager;
import com.mi.milink.sdk.session.common.OpenSessionSucessReturnInfo;
import com.mi.milink.sdk.session.common.Request;
import com.mi.milink.sdk.session.common.ResponseListener;
import com.mi.milink.sdk.session.common.ServerProfile;
import com.mi.milink.sdk.session.common.SessionConst;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.text.TextUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

/**
 * @author CSM
 */
public class SessionManager extends CustomHandlerThread {
    private static final String TAG = "SessionManager";

    private static final int MEDIUM_CONNECTION_CLOSE_INTERNAL = 10 * 60 * 1000;

    /***********************
     * SessionManager 内部要处理的消息类型
     *****************************/
    private static final int MSG_TYPE_SESSION_EVENT = 1;

    private static final int MSG_TYPE_SESSION_LOGIN_EVENT = 2;

    private static final int MSG_TYPE_SESSION_OTHER_EVENT = 3;

    private static final int MSG_TYPE_SERVER_NOTIFICATON_EVENT = 4;

    private static final int MSG_TYPE_CLIENT_ACTION_EVENT = 5;

    private static final int MSG_TYPE_SYSTEM_NOTIFICATION_EVENT = 6;

    private static final int MSG_CHECK_SESSION_TIMER = 20;

    private static final int MSG_TYPE_RELEASE_WAKE_LOCK = 22;

    private static final int MSG_TYPE_GET_SERVICE_TOKEN = 23;

    private static final int MSG_TYPE_LOGIN_RETRY = 24;

    private static final int MSG_TYPE_SEND_MSG = 25;

    private static final int MSG_TYPE_APP_NOT_INIT = 26;

    /********************************* 会话状态 **************************************/
    private static final int NO_SESSION_STATE = Const.SessionState.Disconnected;

    private static final int TRING_SESSION_STATE = Const.SessionState.Connecting;

    private static final int SINGLE_SESSION_STATE = Const.SessionState.Connected;

    private static final int NOLOGIN_SESSION_STATE = Const.LoginState.NotLogin;

    private static final int LOGINED_SESSION_STATE = Const.LoginState.Logined;

    /********************************* 上报返回码 **************************************/
    public static final int MILINK_OPEN_RET_CODE_OK = 0;

    public static final int MILINK_OPEN_RET_CODE_FAIL = 1;

    public static final int MILINK_OPEN_RET_CODE_NO_ROUTE = 2;

    public static final int MILINK_OPEN_RET_CODE_REFUSED = 3;

    public static final int MILINK_OPEN_RET_CODE_NETWORK_CHANGE = 4;

    public static final int MILINK_OPEN_RET_CODE_NET_UNREACHABLE = 5;

    public static final int MILINK_OPEN_RET_CODE_ALL_TIME_OUT = 6;

    public static final int MILINK_OPEN_RET_CODE_MULTI_UNAVAILABLE = 7;

    public static final int MILINK_OPEN_RET_CODE_PERMISSION_DENIED = 8;

    public static final int MILINK_OPEN_RET_CODE_LOAD_SO_FAILED = 9;

    /********************************* 失败错误码 **************************************/
    private static final int ERRNO_PERMISSION_DENIED = 13;

    private static final int ERRNO_NET_UNREACHABLE = 101;

    private static final int ERRNO_CONNECT_TIME_OUT = 110;

    private static final int ERRNO_REFUSED = 111;

    private static final int ERRNO_NO_ROUTE = 113;

    private static final int OPEN_SESSION_TRY_TIMES = 5;

    private static final int LOGIN_TRY_TIMES = 5;

    private static final int SESSION_RECONNECT_TIMES = 2;

    // 检查队列的时间间隔
    private static final int CHECK_SESSION_INTERVAL = 3000; // 3s

    private boolean mCheckTimeOutTimerOpen = false;

    private static final int CHECK_WAKE_LOCK_TIMEOUT = 5; // 5ms

    private static final int AUTO_INTERNAL_OPEN_DELAY = 3 * 1000;

    private static SessionManager sInstance;

    private final ConcurrentLinkedQueue<Request> mSendQueue = new ConcurrentLinkedQueue<Request>();

    private static final int FLAG_TRTING_SESSION = 1;

    private static final int FLAG_ABANDON_SESSION = 3;

    private static final int FLAG_MASTER_SESSION = 4;

    private final List<Session> mSessionList = new ArrayList<Session>(); // 所有由SessionManager维护的Session的列表

    private final HashMap<String, Integer> mSessionAddress2ErrorCodeMap = new HashMap<String, Integer>(); // 地址与错误码

    private Session mMasterSession; // 目前主要一个主session

    private Session mAssistSession; // 辅助的session

    private boolean mAllowAssitSessionWork = false;

    private IServerManager mServerManager = null;

    private WakeLock mWakeLock = null; // 后台时唤醒

    private NetworkChangeReceiver mNetworkReveiver = null;// 网络变化

    private ScreenOnChangeReceiver mScreenOnReveiver = null;// 屏幕变化

    private NetworkDetailInfo mNetworkDetailInfoOnOpen;

    private Object mLock = null;

    /**
     * 当前链接模式，手动或自动。默认为自动跑马。
     */
    private boolean mEnableConnectionManualMode = false;

    private int mState = NO_SESSION_STATE;

    private int mLoginState = NOLOGIN_SESSION_STATE;

    private int mAssistSessionState = NO_SESSION_STATE;

    private int mOpenSessionTryTimes = 0; // 跑马重试次数

    private int mSessionReconnectTryTimes = 0; // 连接次数

    private int mLoginTryTimes = 0; // login重试次数

    private boolean mAppInited = false;

    private long mOpenStartTime = 0;

    private ThreadPoolExecutor threadPool = new ThreadPoolExecutor(1, 1, 1, TimeUnit.SECONDS,
            new ArrayBlockingQueue<Runnable>(3), new ThreadPoolExecutor.DiscardPolicy());

    private boolean isMultiUnavailable() {
        if (mSessionAddress2ErrorCodeMap.isEmpty()) {
            return false;
        }
        for (String address : mSessionAddress2ErrorCodeMap.keySet()) {
            Integer code = mSessionAddress2ErrorCodeMap.get(address);
            if (code == null || (code != ERRNO_REFUSED && code != ERRNO_NET_UNREACHABLE && code != ERRNO_NO_ROUTE
                    && code != ERRNO_CONNECT_TIME_OUT)) {
                return false;
            }
        }
        return true;
    }

    private boolean isAllSessionErrorCode(int errorCode) {
        if (mSessionAddress2ErrorCodeMap.isEmpty()) {
            return false;
        }
        for (String address : mSessionAddress2ErrorCodeMap.keySet()) {
            Integer code = mSessionAddress2ErrorCodeMap.get(address);
            if (code == null || code != errorCode) {
                return false;
            }
        }
        return true;
    }

    public void initApp() {
        mAppInited = true;
        // 移除登出请求
        mHandler.removeCallbacks(mLogoffRunnable);
        resetAllTryTimes();
    }

    private SessionManager() {
        super(TAG);
        MiLinkLog.e(TAG, "SessionManager created, milinkversion=" + Global.getMiLinkVersion() + "_"
                + Global.getMiLinkSubVersion());// 这行日志还隐含一个意思就是初始化MiLinkLog
        EventBus.getDefault().register(this);
        TrafficMonitor.getInstance().start();
        mServerManager = MiLinkServerManager.getInstance();
        setState(NO_SESSION_STATE);
        mLock = new Object();
        // 设置心跳
        mNetworkReveiver = new NetworkChangeReceiver();
        mScreenOnReveiver = new ScreenOnChangeReceiver();
        mNetworkReveiver.setCurrentNetworkInfo();
        Global.registerReceiver(mNetworkReveiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        {
            IntentFilter intent = new IntentFilter(Intent.ACTION_SCREEN_ON);
            intent.addAction(Intent.ACTION_SCREEN_OFF);
            Global.registerReceiver(mScreenOnReveiver, intent);
        }
        String defaultHost = MiLinkIpInfoManager.getInstance().getDefaultHost();
        if (MiAccountManager.getInstance().appHasLogined()) {
            DomainManager.getInstance().startResolve(defaultHost);
        }
        // 怀疑这个地方会导致进程崩溃，onserviceconnect没有回调。等待新版本复现。
        // ConnectionImpl.addConnectPrintLogCallback(mConnectPrintLogCallback);
        MiLinkLog.w(TAG, "SessionManager created finish");
    }

    public synchronized static SessionManager getInstance() {
        if (sInstance == null) {
            sInstance = new SessionManager();
        }
        return sInstance;
    }

    @Override
    protected void processMessage(Message msg) {
        switch (msg.what) {
            case MSG_TYPE_SESSION_EVENT: {
                processEvent((SessionConnectEvent) msg.obj);
            }
            break;
            case MSG_TYPE_SESSION_LOGIN_EVENT: {
                processEvent((SessionLoginEvent) msg.obj);
            }
            break;
            case MSG_TYPE_SESSION_OTHER_EVENT: {
                processEvent((SessionOtherEvent) msg.obj);
            }
            break;
            case MSG_TYPE_SERVER_NOTIFICATON_EVENT: {
                processEvent((ServerNotificationEvent) msg.obj);
            }
            break;
            case MSG_TYPE_CLIENT_ACTION_EVENT: {
                processEvent((ClientActionEvent) msg.obj);
            }
            break;
            case MSG_TYPE_SYSTEM_NOTIFICATION_EVENT: {
                processEvent((SystemNotificationEvent) msg.obj);
            }
            break;
            case MSG_CHECK_SESSION_TIMER: {
                MiLinkLog.w(TAG, "MSG_CHECK_SESSION_TIMER");
                Session session = getSession();
                if (session != null) {
                    session.checkRequestsTimeout();
                }
                // 所有的tryingsession都应该被检查，防止一直处于 state=1的状态
                for (Session s : mSessionList) {
                    if (s.mFlagForSessionManager == FLAG_TRTING_SESSION) {
                        s.checkRequestsTimeout();
                    }
                }
                if (mAllowAssitSessionWork) {
                    Session assistSession = getAssistSession();
                    if (assistSession != null) {
                        assistSession.checkRequestsTimeout();
                    }
                }
                // 继续
                mHandler.sendEmptyMessageDelayed(MSG_CHECK_SESSION_TIMER, CHECK_SESSION_INTERVAL);
            }
            break;
            case MSG_TYPE_RELEASE_WAKE_LOCK: {
                MiLinkLog.w(TAG, "release wake lock");
                releaseWakeLock();
            }
            break;

            case MSG_TYPE_LOGIN_RETRY: // 登录重试
            {
                login("LOGIN_RETRY");
            }
            break;
            case MSG_TYPE_GET_SERVICE_TOKEN: {
                MiLinkLog.v(TAG, "MSG_TYPE_GET_SERVICE_TOKEN,no service token, call app onEventGetServiceToken");
                EventBus.getDefault().post(
                        new SessionManagerNotificationEvent(SessionManagerNotificationEvent.EventType.GetServiceToken));
            }
            break;
            case MSG_TYPE_APP_NOT_INIT: {
                MiLinkLog.v(TAG, "MSG_TYPE_APP_NOT_INIT,app not init, call app init by onEventGetServiceToken");
                EventBus.getDefault().post(
                        new SessionManagerNotificationEvent(SessionManagerNotificationEvent.EventType.GetServiceToken));
            }
            break;
            case MSG_TYPE_SEND_MSG: {

                // 所有业务包的入口
                Request request = (Request) msg.obj;
                if (!NetworkDash.isAvailable()) {
                    request.onDataSendFailed(Const.InternalErrorCode.MNS_NOT_LOGIN,
                            "abandon package,network not available state=" + mState);
                    return;
                }
                if (mAllowAssitSessionWork) {
                    Session session = getAssistSession();
                    // 已经连接就可以发包
                    if (session == null || !session.isConnected()) {
                        internalAssistSessionOpen();
                    } else {
                        MiLinkLog.v(TAG, "send data to assistsession, seq=" + request.getSeqNo());
                        // 判定死链接问题
                        if (session.isDeadConnection(60 * 100, 5 * 60 * 1000)) {
                            MiLinkLog.w(TAG, "assistsession isDeadConnection=true");
                            setAssistSessionState(NO_SESSION_STATE);
                        } else {
                            session.handleRequest(request);
                            return;
                        }
                    }
                }
                Session session = getSession();
                MiLinkLog.v(TAG, "send data, session manager state: " + mState);

                // 判定死链接问题
                if (session != null && session.isDeadConnection(60 * 100, 5 * 60 * 1000)) {
                    MiLinkLog.w(TAG, "session isDeadConnection=true");
                    setState(NO_SESSION_STATE);
                    session = null;
                }
                if (session == null || !session.isAvailable()) {
                    // cache request
                    if (request.requestShouldCached()) {
                        MiLinkLog.v(TAG, "push request in cache, seq=" + request.getSeqNo());
                        mSendQueue.add(request);
                    } else {
                        MiLinkLog.v(TAG, "abandon data because session is not available, seq=" + request.getSeqNo());
                        // 记录一下，此时属于session not ready
                        if (request.getData() != null) {
                            request.onDataSendFailed(Const.InternalErrorCode.MNS_NOT_LOGIN,
                                    "abandon package,session is not available state=" + mState);
                            InternalDataMonitor.getInstance().trace("", 0, request.getData().getCommand(),
                                    BaseDataMonitor.RET_CODE_ABANDONED_SESSION_NOT_READY, request.getCreatedTime(),
                                    System.currentTimeMillis(), request.getSize(), 0, request.getSeqNo());
                        }
                    }
                    login("handleRequest");
                    return;
                }
                MiLinkLog.v(TAG, "send data to session, seq=" + request.getSeqNo());
                session.handleRequest(request);
            }
            break;
            default:
                break;
        }
    }

    @Subscribe
    public void onEvent(SessionConnectEvent event) {
        Message localMessage = mHandler.obtainMessage(MSG_TYPE_SESSION_EVENT, event);
        mHandler.sendMessage(localMessage);
    }

    @Subscribe
    public void onEvent(SessionLoginEvent event) {
        Message localMessage = mHandler.obtainMessage(MSG_TYPE_SESSION_LOGIN_EVENT, event);
        mHandler.sendMessage(localMessage);
    }

    @Subscribe
    public void onEvent(SessionOtherEvent event) {
        Message localMessage = mHandler.obtainMessage(MSG_TYPE_SESSION_OTHER_EVENT, event);
        mHandler.sendMessage(localMessage);
    }

    @Subscribe
    public void onEvent(ServerNotificationEvent event) {
        Message localMessage = mHandler.obtainMessage(MSG_TYPE_SERVER_NOTIFICATON_EVENT, event);
        mHandler.sendMessage(localMessage);
    }

    @Subscribe
    public void onEvent(ClientActionEvent event) {
        Message localMessage = mHandler.obtainMessage(MSG_TYPE_CLIENT_ACTION_EVENT, event);
        mHandler.sendMessage(localMessage);
    }

    @Subscribe
    public void onEvent(SystemNotificationEvent event) {
        Message localMessage = mHandler.obtainMessage(MSG_TYPE_SYSTEM_NOTIFICATION_EVENT, event);
        mHandler.sendMessage(localMessage);
    }

    Runnable channelIdleReset = new Runnable() {

        @Override
        public void run() {
            mAllowAssitSessionWork = false;
        }
    };

    @Subscribe
    public void onEvent(ChannelStatusChangeEvent event) {
        switch (event.mEventType) {
            case channelBusy: {
                mAllowAssitSessionWork = true;
                mHandler.removeCallbacks(channelIdleReset);
                // 30s后恢复
                mHandler.postDelayed(channelIdleReset, 30 * 1000);
            }
            break;
            case channelIdle: {
                if (mAllowAssitSessionWork) {
                    MiLinkLog.w(TAG, "mode change,mAllowAssitSessionWork==false");
                    mAllowAssitSessionWork = false;
                    mHandler.removeCallbacks(channelIdleReset);
                }
            }
            break;
            default:
                break;
        }
    }

    private void processEvent(SessionConnectEvent event) {
        Session session = event.mSession;
        int errCode = event.mRetCode;
        switch (event.mEventType) {
            case SessionBuildFailed: {
                MiLinkLog.w(TAG, "SessionConnectEvent SessionBuildFailed");
                synchronized (mReportLock) {
                    mReportLock.notifyAll();
                }
                // 错误码统计的唯一入口
                ServerProfile sp = session.getServerProfileForStatistic();
                MiLinkLog.w(TAG, "SessionBuildFailed ServerProfile " + sp.toString());
                if (sp != null) {
                    String address = String.format("%s:%s", sp.getServerIP(), sp.getServerPort());
                    mSessionAddress2ErrorCodeMap.put(address, errCode);
                }
                // 先检查是否是废弃的session
                if (isAbandonSession(session)) {
                    return;
                }

                MiLinkLog.w(TAG, "MSG_TYPE_OPEN_SESSION_FAIL errCode:" + errCode);
                if (session.mFlagForSessionManager == FLAG_MASTER_SESSION) {
                    MiLinkLog.e(TAG,
                            String.format("handleMessage MSG_TYPE_OPEN_SESSION_FAIL is mMasterSession No:%d, mState = %d",
                                    session.getSessionNO(), mState));
                    setState(NO_SESSION_STATE);
                    if (NetworkDash.isAvailable()) {
                        mInternalAutoOpenRunnable.run();
                    }
                    return;
                }
                if (session.mFlagForSessionManager == FLAG_TRTING_SESSION) {
                    MiLinkLog.e(TAG,
                            String.format("handleMessage MSG_TYPE_OPEN_SESSION_FAIL is isTryingSession No:%d, mState = %d",
                                    session.getSessionNO(), mState));
                    getNextServerProfile(session, errCode);
                    return;
                }

                // 无法识别的sesion，统统关闭
                MiLinkLog.e(TAG,
                        "handleMessage MSG_TYPE_OPEN_SESSION_FAIL is unknown session No:" + session.getSessionNO());
                session.close();
            }
            break;
            case SessionBuildSuccess: {
                MiLinkLog.w(TAG, "SessionConnectEvent SessionBuildSuccess");
                if (isAbandonSession(session)) {
                    return;
                }
                MiLinkLog.v(TAG, "handleMessage OPEN_SESSION_SUCCESS No:" + session.getSessionNO());
                if (session.mFlagForSessionManager == FLAG_TRTING_SESSION
                        || session.mFlagForSessionManager == FLAG_MASTER_SESSION) {
                    MiLinkLog.v(TAG, "update session");
                    // 更新当前的session
                    updateSession(session);

                    // 解除上报数据的锁，如果有的话
                    synchronized (mReportLock) {
                        mReportLock.notifyAll();
                    }
                    return;
                }

                // 其他无法识别的session，统统都关闭
                session.close();
                MiLinkLog.e(TAG, "handleMessage OPEN_SESSION_SUCCESS is unknown session No:" + session.getSessionNO());
            }
            break;
            case SessionRunError: {
                MiLinkLog.w(TAG, "SessionConnectEvent SessionRunError");
                if (isAbandonSession(session)) {
                    return;
                }

                MiLinkLog.e(TAG,
                        String.format("handleMessage SESSION_ERROR reason = %d, No:%d", errCode, session.getSessionNO()));

                setState(NO_SESSION_STATE);
                if (errCode == Const.InternalErrorCode.MNS_LOAD_LIBS_FAILED) {
                    return;
                }

                if (NetworkDash.isAvailable()) {
                    MiLinkLog.e(TAG, " SESSION_ERROR mSessionReconnectTimes=" + mSessionReconnectTryTimes
                            + ", mOpenSessionTryTimes=" + mOpenSessionTryTimes);
                    // 单个session的重试次数
                    if (mSessionReconnectTryTimes < SESSION_RECONNECT_TIMES) {
                        Session newSession = new Session();
                        newSession.mFlagForSessionManager = FLAG_TRTING_SESSION;
                        mSessionList.add(newSession);
                        setState(TRING_SESSION_STATE);
                        newSession.openSession(session.getServerProfileForStatistic());
                        mSessionReconnectTryTimes++;
                    } else {
                        mInternalAutoOpenRunnable.run();
                    }
                    HeartBeatManager.getInstance().reciveConnectRunError(errCode);
                } else {
                    MiLinkLog.e(TAG, "on seesion error network isAvailable = false");
                }
            }
            break;
            case AssistSessionConnectSuccess: {
                MiLinkLog.w(TAG, "SessionConnectEvent AssistSessionConnectSuccess");
                session.mFlagForSessionManager = FLAG_MASTER_SESSION;
                MiLinkLog.v(TAG, "updateSession in no session or tring session");
                if (mAssistSession != null && mAssistSession != session) {
                    mAssistSession.close();
                }
                mAssistSession = session;
                setAssistSessionState(SINGLE_SESSION_STATE);
            }
            break;
            case AssistSessionConnectFailed: {
                MiLinkLog.w(TAG, "SessionConnectEvent AssistSessionConnectFailed");
                session.mFlagForSessionManager = FLAG_ABANDON_SESSION;
                setAssistSessionState(NO_SESSION_STATE);
            }
            break;
            case AssistSessionRunError: {
                MiLinkLog.w(TAG, "SessionConnectEvent AssistSessionRunError");
                session.mFlagForSessionManager = FLAG_ABANDON_SESSION;
                setAssistSessionState(NO_SESSION_STATE);
            }
            break;
            default:
                break;
        }
    }

    private void processEvent(SessionLoginEvent event) {
        Session session = event.mSession;
        if (isAbandonSession(session)) {
            return;
        }
        switch (event.mEventType) {
            case LoginFailed: {
                MiLinkLog.w(TAG, "SessionLoginEvent LoginFailed");
                MiAccountManager.getInstance().setIsLogining(false);
                mLoginState = NOLOGIN_SESSION_STATE;
                EventBus.getDefault().post(new SessionManagerStateChangeEvent(
                        SessionManagerStateChangeEvent.EventType.LoginStateChange, Const.NONE, Const.LoginState.NotLogin));
                mHandler.removeMessages(MSG_TYPE_LOGIN_RETRY);
                mHandler.sendEmptyMessageDelayed(MSG_TYPE_LOGIN_RETRY, 10 * 1000);
            }
            break;
            case LoginSuccess: {
                MiLinkLog.w(TAG, "SessionLoginEvent LoginSuccess");
                MiAccountManager.getInstance().setIsLogining(false);
                mLoginState = LOGINED_SESSION_STATE;
                sendCacheRequest();
                MiLinkLog.v(TAG, "onLoginResult loginState=" + Const.LoginState.Logined);
                EventBus.getDefault().post(new SessionManagerStateChangeEvent(
                        SessionManagerStateChangeEvent.EventType.LoginStateChange, Const.NONE, Const.LoginState.Logined));
            }
            break;
            case LogoffCmdReturn: {
                MiLinkLog.w(TAG, "SessionLoginEvent LogoffCmdReturn");
                mHandler.removeCallbacks(mLogoffRunnable);
                mLogoffRunnable.run();
            }
            break;
            default:
                break;
        }
    }

    private void processEvent(SessionOtherEvent event) {
        Session session = event.mSession;
        if (isAbandonSession(session)) {
            return;
        }
        switch (event.mEventType) {
            case RecvInvalidPacket: {
                MiLinkLog.w(TAG, "SessionOtherEvent RecvInvalidPacket");
                EventBus.getDefault().post(
                        new SessionManagerNotificationEvent(SessionManagerNotificationEvent.EventType.RecvInvalidPacket));
            }
            break;
            case RequestMapIsEmpty: {
                MiLinkLog.w(TAG, "SessionOtherEvent RequestMapIsEmpty");
                // 定时器已经暂停
                if (!mCheckTimeOutTimerOpen) {
                    return;
                }
                mTryStopTimerRunnable.run();
            }
            break;
            case RequestMapIsNotEmpty: {
                MiLinkLog.w(TAG, "SessionOtherEvent RequestMapIsNotEmpty");
                if (!mCheckTimeOutTimerOpen) {
                    MiLinkLog.v(TAG, "mCheckTimeOutTimerOpen=false,startTimer");
                    startTimer();
                }
            }
            break;
            case StatisticsTimeoutPacket:
                MiLinkLog.w(TAG, "SessionOtherEvent StatisticsTimeoutPacket");
                session.postStatisticsTimeoutPacketAction();
            default:
                break;
        }
    }

    long mLastUploadTime = 0;

    @SuppressLint("UseSparseArrays")
    private void processEvent(ServerNotificationEvent event) {
        switch (event.mEventType) {
            case ServerLineBroken: {
                MiLinkLog.e(TAG, "ServerNotificationEvent ServerLineBroken");
                setState(NO_SESSION_STATE);
                if (NetworkDash.isAvailable()) {
                    mServerManager = MiLinkBackupServerManager.getInstance();
                    internalOpen();
                } else {
                    MiLinkLog.e(TAG, "on server line broken network isAvailable = false");
                }
            }
            break;
            case B2tokenExpired: {
                MiLinkLog.e(TAG, "ServerNotificationEvent B2tokenExpired");
                MiAccountManager.getInstance().logoffMiLink();
                login("B2_TOKEN_EXPIRED");
            }
            break;
            case ChannelPubKeyUpdate: {
                MiLinkLog.e(TAG, "ServerNotificationEvent ChannelPubKeyUpdate");
                MnsCmdChannelNewPubKeyRsp channelNewPubkey = (MnsCmdChannelNewPubKeyRsp) event.mObject;
                Map<Integer, String> channelPubKeyMap = new HashMap<Integer, String>();
                if (channelNewPubkey != null) {

                    List<PublicKeyInfo> pubInfoList = channelNewPubkey.getPubInfoList();
                    for (PublicKeyInfo pubKeyInfo : pubInfoList) {

                        int keyId = pubKeyInfo.getKeyId();
                        byte[] publicKeyByte = pubKeyInfo.getPublicKey().toByteArray();
                        try {
                            channelPubKeyMap.put(keyId, new String(publicKeyByte, "UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                        }
                    }
                    AnonymousAccount.getInstance().setChannelPubKey(channelPubKeyMap);
                    MiLinkLog.e(TAG, " onUpdateChannelPubKey update  " + channelPubKeyMap.size() + " pubkey ");
                }

                Session session = getSession();
                if (session == null || !session.isAvailable()) {
                    MiLinkLog.v(TAG, "login session is not available.");
                    break;
                }
                session.fastLogin();
            }
            break;

            case ServiceTokenExpired: {
                MiLinkLog.e(TAG, "ServerNotificationEvent ServiceTokenExpired");
                mLogoffRunnable.run();
                EventBus.getDefault().post(
                        new SessionManagerNotificationEvent(SessionManagerNotificationEvent.EventType.ServiceTokenExpired));
            }
            break;
            case ShouldUpdate: {
                MiLinkLog.e(TAG, "ServerNotificationEvent ShouldUpdate");
                EventBus.getDefault()
                        .post(new SessionManagerNotificationEvent(SessionManagerNotificationEvent.EventType.ShouldUpdate));
            }
            break;
            case KickByServer: {
                MiLinkLog.e(TAG, "ServerNotificationEvent KickByServer");
                mHandler.postDelayed(mLogoffRunnable, 5000);
                EventBus.getDefault().post(new SessionManagerNotificationEvent(
                        SessionManagerNotificationEvent.EventType.KickByServer, event.mObject));
            }
            break;
            case requireUploadLog: {
                try {
                    UploadLogManager.uploadMilinkLog((MilinkLogReq) event.mObject,
                            MiAccountManager.getInstance().getCurrentAccount(), false);
                } catch (Exception e) {
                }
            }
            break;
            case requireChannelLogLevel: {
                PushLogLevel logLevelReq = (PushLogLevel) event.mObject;
                MiLinkLog.d(TAG, "requireChannelLogLevel.");
                if (logLevelReq.hasLoglevel()) {

                    DataLoglevel.Builder dataLogLevel = DataLoglevel.newBuilder();
                    dataLogLevel.setLoglevel(logLevelReq.getLoglevel());
                    dataLogLevel.setTimeLong(logLevelReq.getTimeLong());

                    PacketData packet = new PacketData();
                    packet.setCommand(Const.DATA_LOGLEVEL_CMD);
                    packet.setData(dataLogLevel.build().toByteArray());
                    MnsPacketDispatcher.getInstance().dispatchPacket(packet);
                    MiLinkLog.e(TAG, "notify app to change log level.level=" + logLevelReq.getLoglevel() + ", time="
                            + logLevelReq.getTimeLong());
                    // 修改milink的日志级别，同时通知APP层
                }

            }
            break;
            default:
                break;
        }
    }

    private void processEvent(ClientActionEvent event) {
        switch (event.mEventType) {
            case ClientNotSameUserLogin:
                MiLinkLog.e(TAG, "ClientActionEvent ClientNotSameUserLogin");
                internalClose();
                break;
            case ClientRequestCheckConnection:
                MiLinkLog.w(TAG, "ClientActionEvent ClientRequestCheckConnection");
                tryConnectIfNeed();
                // 简单通知用户已经是登陆成功了
                if (mState == SINGLE_SESSION_STATE) {
                    EventBus.getDefault()
                            .post(new SessionManagerStateChangeEvent(
                                    SessionManagerStateChangeEvent.EventType.SessionStateChange, Const.NONE,
                                    Const.SessionState.Connected));
                }
                if (mLoginState == LOGINED_SESSION_STATE) {
                    EventBus.getDefault()
                            .post(new SessionManagerStateChangeEvent(
                                    SessionManagerStateChangeEvent.EventType.LoginStateChange, Const.NONE,
                                    Const.LoginState.Logined));
                }
                break;
            case ClientRequestLogin:
                MiLinkLog.w(TAG, "ClientActionEvent ClientRequestLogin");
                login("UserAction");
                break;
            case ClientRequestLogoff:
                MiLinkLog.w(TAG, "ClientActionEvent ClientRequestLogoff");
                logoff();
                break;
            case ClientForceOpen:
                MiLinkLog.w(TAG, "ClientActionEvent ClientForceOpen");
                setState(NO_SESSION_STATE);
                resetAllTryTimes();
                acquireWakeLock();
                login("ClientForceOpen");
                break;
            case ClientSuspectBadConnection:
                // 如果是已连接状态
                MiLinkLog.w(TAG, "ClientActionEvent ClientSuspectBadConnection");
                if (mState == SINGLE_SESSION_STATE) {
                    if (System.currentTimeMillis() - mConnectionBuildTimestamp > 5 * 60 * 1000) {
                        setState(NO_SESSION_STATE);
                        resetAllTryTimes();
                        acquireWakeLock();
                        login("ClientSuspectBadConnection");
                    }
                }
                break;
            default:
                break;
        }
    }

    private void processEvent(SystemNotificationEvent event) {
        switch (event.mEventType) {
            case AlarmArrived: {
                closeAllBandonSession();// 再补一下，防止未close造成的内存过大
                // mipush 不上报数据
                InternalDataMonitor.getInstance().onAlarmArrive();
                if (Global.getClientAppInfo().isMediumConnection()
                        && (System.currentTimeMillis() - mLastUserSendDataTime > MEDIUM_CONNECTION_CLOSE_INTERNAL)) {
                    MiLinkLog.w(TAG, "medium connection mode,user not send any packet in 10 min,close connection");
                    internalClose();
                    return;
                }
                if (!MiAccountManager.getInstance().appHasLogined()) {
                    MiLinkLog.w(TAG, "onAlarmArrived, app not login");
                    return;
                }
                MiLinkLog.w(TAG, "SystemNotificationEvent onAlarmArrived");
                resetAllTryTimes();
                acquireWakeLock();

                MiLinkLog.v(TAG, "session manager state: " + mState);

                Session session = getSession();

                // TODO::::去掉 判定死链接问题-死链接通过使用动态心跳判定
                // if (session != null && session.isAvailable()) {
                // if (session.isDeadConnection(60 * 1000, 2 *
                // ConfigManager.getInstance()
                // .getHeartBeatInterval() )) {
                // MiLinkLog.w(TAG,
                // "isDeadConnection=true!!! force disconnect");
                // setState(NO_SESSION_STATE);
                // }
                // }
                // if (session != null && session.isAvailable()) {
                // if (session.isDeadConnection(60 * 1000, 2 *
                // HeartBeatManager.getInstance()
                // .getHeartBeatInterval() )) {
                // MiLinkLog.w(TAG,
                // "isDeadConnection=true!!! force disconnect");
                // setState(NO_SESSION_STATE);
                // }
                // }

                // 心跳，但session处于NO_SESSION_STATE状态下，自动open一次
                if (tryConnectIfNeed()) {
                    return;
                }

                MiLinkLog.v(TAG, "send heart beat to session");

                if (session == null || !session.isAvailable()) {
                    MiLinkLog.v(TAG, "session is not available.");
                    return;
                }

                session.heartBeat(false);
            }
            break;
            case ScreenOn: {
                MiLinkLog.v(TAG, "SystemNotificationEvent screen_on");
                if (!MiAccountManager.getInstance().appHasLogined()) {
                    MiLinkLog.w(TAG, "screen_on, app not login");
                    return;
                }
                if (!Global.getClientAppInfo().isMediumConnection()) {
                    tryConnectIfNeed();
                }
            }
            break;
            case NetWorkChange: {
                MiLinkLog.v(TAG, "SystemNotificationEvent NetWorkChange");
                acquireWakeLock();
                tryConnectIfNeed();
            }
            break;
            case ServiceCreated: {
                MiLinkLog.v(TAG, "SystemNotificationEvent ServiceCreated");
                if (mAppInited) {
                    acquireWakeLock();
                    tryConnectIfNeed();
                } else {
                    EventBus.getDefault().post(
                            new SessionManagerNotificationEvent(SessionManagerNotificationEvent.EventType.GetServiceToken));
                }
            }
            break;
            default:
                break;
        }

    }

    private boolean isAbandonSession(Session session) {
        // 先检查是否是废弃的session
        if (session == null || session.mFlagForSessionManager == FLAG_ABANDON_SESSION) {
            MiLinkLog.w(TAG, String.format("Session No:%d is AbandonSession return ", session.getSessionNO()));
            if (session.close()) {
                mSessionList.remove(session);
            }
            return true;
        }
        return false;
    }

    /**
     * 获取当前session所处的状态
     *
     * @return session的状态类型
     * <ul>
     * <li>{@link SessionConst#NO_SESSION_STATE}
     * <li>{@link SessionConst#TRING_SESSION_STATE}
     * <li>{@link SessionConst#TEMP_SESSION_STATE}
     * <li>{@link SessionConst#SINGLE_SESSION_STATE}
     * <li>{@link SessionConst#DUAL_SESSION_STATE}
     * </ul>
     */
    public int getSessionState() {
        return mState;
    }

    private Runnable mInternalAutoOpenRunnable = new Runnable() {
        @Override
        public void run() {
            MiLinkLog.v(TAG, "internalAutoOpen mOpenSessionTryTimes=" + mOpenSessionTryTimes + ",mState=" + mState);
            if (mOpenSessionTryTimes < OPEN_SESSION_TRY_TIMES && mState == NO_SESSION_STATE) {
                mOpenSessionTryTimes++;
                internalOpen();
            }
        }
    };

    private void internalManualOpen() {
        MiLinkLog.v(TAG, "internalManualOpen,mState=" + mState);
        resetAllTryTimes();

        if (mEnableConnectionManualMode == true) {
            mServerManager = ManualServerManager.getInstance();
        } else {
            mServerManager = MiLinkServerManager.getInstance();
        }

        internalOpen();
    }

    /**
     * 跑马唯一入口
     */
    private void internalOpen() {
        if (!NetworkDash.isAvailable()) {
            MiLinkLog.i(TAG, "can not open session, network is not available.");
            return;
        }
        if (!MiAccountManager.getInstance().appHasLogined()) {
            MiLinkLog.i(TAG, "app not login internalOpen cancel");
            return;
        }
        if (mHandler == null) {
            MiLinkLog.i(TAG, "can not open session, mHandler == null.");
            return;
        }

        MiLinkLog.i(TAG, "open session, internalOpen with mState = " + mState);
        if (mState != NO_SESSION_STATE) {
            MiLinkLog.i(TAG, "mState is not No_Sesssion state,cancel paoma");
            return;
        }
        mSessionAddress2ErrorCodeMap.clear();
        // mOpenSessionAddressSet.clear();
        mOpenStartTime = System.currentTimeMillis();
        ServerProfile[] serverProfileList = mServerManager.reset(false);
        // 恢复到最优ip列表
        mServerManager = MiLinkServerManager.getInstance();
        if (serverProfileList == null || serverProfileList.length == 0) {
            MiLinkLog.e(TAG, "serverProfileList is null ,internalOpne cancel");
            return;
        }
        MiLinkLog.d(TAG, "internalOpen 4");
        setState(TRING_SESSION_STATE);
        // 打开各个session
        for (int i = 0; i < serverProfileList.length; i++) {
            if (serverProfileList[i] == null) {
                continue;
            }
            Session session = new Session();
            session.mFlagForSessionManager = FLAG_TRTING_SESSION;
            mSessionList.add(session);
            session.openSession(serverProfileList[i]);
        }
        mNetworkDetailInfoOnOpen = Network.getCurrentNetworkDetailInfo();
    }

    /**
     * 打开辅助通道
     */
    private void internalAssistSessionOpen() {
        if (!NetworkDash.isAvailable()) {
            MiLinkLog.i(TAG, "can not open session, network is not available.");
            return;
        }
        if (!MiAccountManager.getInstance().appHasLogined()) {
            MiLinkLog.i(TAG, "app not login internalOpen cancel");
            return;
        }
        if (mHandler == null) {
            MiLinkLog.i(TAG, "can not open session, mHandler == null.");
            return;
        }

        MiLinkLog.i(TAG, "open assistsession, internalAssistSessionOpen with mState = " + mState);
        if (mAssistSessionState != NO_SESSION_STATE) {
            MiLinkLog.i(TAG, "mAssistSessionState is not No_Sesssion state,cancel link");
            return;
        }
        setAssistSessionState(TRING_SESSION_STATE);
        mAssistSession = new Session(Session.SESSION_TYPE_ASSIST);
        mAssistSession.mFlagForSessionManager = FLAG_TRTING_SESSION;
        ServerProfile serverProfileForAssistSession = mMasterSession.getServerProfileForStatistic();
        if (serverProfileForAssistSession == null) {
            serverProfileForAssistSession = mServerManager.reset(true)[0];
        }
        mAssistSession.openSession(serverProfileForAssistSession);
    }

    /**
     * 当有session成功时，更新一下当前可用的session(比如tempSession，masterSession等)
     *
     * @param session 成功的session
     * @return 更新成功返回true，否则返回false
     */
    private boolean updateSession(Session session) {
        if (session == null) {
            return false;
        }

        MiLinkLog.v(TAG, "update session function.");

        session.mFlagForSessionManager = FLAG_MASTER_SESSION;
        // 第一个成功的，从尝试列表里清除掉；并且还要清除其相同类型的session
        mSessionList.remove(session);
        // 停掉可以停的session
        ServerProfile serverProfile = session.getServerProfile();
        if (serverProfile.getProtocol() == SessionConst.TCP_CONNECTION_TYPE) {
            // 如果是tcp，要停掉其他所有的session
            abandonAllSession();
        }

        if (mServerManager != null) {
            mServerManager.save(session.getServerProfile());
        }
        MiLinkLog.v(TAG, "updateSession in no session or tring session");
        if (mMasterSession != null && mMasterSession != session) {
            mMasterSession.close();
        }
        mMasterSession = session;
        setState(SINGLE_SESSION_STATE);
        MiLinkLog.v(TAG, "connected, start milink login");
        MiAccountManager.getInstance().setIsLogining(false);
        login("updateSession");
        // 通知成功 --- 仅这一次
        onOpenSessionResult(Const.InternalErrorCode.SUCCESS, mOpenStartTime);

        // 存起最优
        OpenSessionSucessReturnInfo info = session.getOpenSessionSucessReturnInfo();
        if (info != null) {
            String clientIp = info.getClientIp();
            String clientIsp = info.getClientIsp();
            ArrayList<ServerProfile> backupServerList = info.getBackupServerList();
            ArrayList<ServerProfile> optmumServerList = info.getOptmumServerList();
            MiLinkLog.w(TAG, String.format("clientip:%s clientIsp;%s", clientIp, clientIsp));
            if (!TextUtils.isEmpty(clientIp)) {
                Global.setClientIp(clientIp);
            }
            if (!TextUtils.isEmpty(clientIsp)) {
                Global.setClientIsp(clientIsp);
            }
            MiLinkIpInfoManager.getInstance().setOptmumServerList(Global.getClientIsp(), optmumServerList);
            MiLinkIpInfoManager.getInstance().setBackupServerList(backupServerList);
        } else {
            MiLinkLog.w(TAG, "info is null");
        }
        return true;
    }

    public boolean isTimerOpen() {
        return mCheckTimeOutTimerOpen;
    }

    private Runnable mTryStopTimerRunnable = new Runnable() {

        @Override
        public void run() {
            if (!mCheckTimeOutTimerOpen) {
                return;
            }
            boolean canStop = true;
            if (mMasterSession != null && mMasterSession.shouldCheckRequestsTimeout()) {
                canStop = false;
            }
            if (canStop && mAllowAssitSessionWork) {
                if (mAssistSession != null && mAssistSession.shouldCheckRequestsTimeout()) {
                    canStop = false;
                }
            }
            if (canStop) {
                for (Session s : mSessionList) {
                    int f = s.mFlagForSessionManager;
                    if ((f == FLAG_TRTING_SESSION || f == FLAG_MASTER_SESSION) && s.shouldCheckRequestsTimeout()) {
                        canStop = false;
                        break;
                    }
                }
            }
            if (canStop) {
                MiLinkLog.v(TAG, "all session request map is empty, stopTimer");
                stopTimer();
            }
        }
    };

    /**
     * 开启定时器。有两个作用:1.检查包的超时；2.关闭无用的session
     */
    private void startTimer() {
        mCheckTimeOutTimerOpen = true;
        mHandler.removeMessages(MSG_CHECK_SESSION_TIMER);
        mHandler.sendEmptyMessageDelayed(MSG_CHECK_SESSION_TIMER, CHECK_SESSION_INTERVAL);
    }

    /**
     * 关闭定时器
     */
    private void stopTimer() {
        mCheckTimeOutTimerOpen = false;
        mHandler.removeMessages(MSG_CHECK_SESSION_TIMER);
    }

    /**
     * 关闭所有的session
     */
    public boolean close() {
        return mHandler.post(new Runnable() {
            @Override
            public void run() {
                internalClose();
            }
        });
    }

    /**
     * 在sessionManager线程内关闭所有的session
     */
    private void internalClose() {
        MiLinkLog.w(TAG, "internalClose");
        setState(NO_SESSION_STATE);
        stopTimer();
    }

    /**
     * 获取当前可用的session
     *
     * @return 如果有可用的session，则返回;否则返回null
     */
    private Session getSession() {
        switch (mState) {
            case NO_SESSION_STATE: // 当前无session
            case TRING_SESSION_STATE: // 正在尝试session,但还没成功的
                return null;
            case SINGLE_SESSION_STATE: // 稳定状态，只有一个session
                return mMasterSession;
            default:
                // ERROR
                return null;
        }
    }

    /**
     * 下行push过多时的辅助通道
     */
    private Session getAssistSession() {
        switch (mAssistSessionState) {
            case NO_SESSION_STATE: // 当前无session
            case TRING_SESSION_STATE: // 正在尝试session,但还没成功的
                return null;
            case SINGLE_SESSION_STATE: // 稳定状态，只有一个session
                return mAssistSession;
            default:
                // ERROR
                return null;
        }
    }

    private long mConnectionBuildTimestamp = 0;

    private void setState(int newState) {
        switch (newState) {
            // 设置为no session状态，sessionmanger抛弃并关闭所维护的所有session
            case NO_SESSION_STATE:

                for (Session s : mSessionList) {
                    s.mFlagForSessionManager = FLAG_ABANDON_SESSION;
                }
                closeAllBandonSession();
                if (mMasterSession != null) {
                    mMasterSession.mFlagForSessionManager = FLAG_ABANDON_SESSION;
                    mMasterSession.close();
                    mMasterSession = null;
                }
                mLoginState = NOLOGIN_SESSION_STATE;
                MiAccountManager.getInstance().setIsLogining(false);
                break;
            case SINGLE_SESSION_STATE:
                closeAllBandonSession();
                mConnectionBuildTimestamp = System.currentTimeMillis();
            default:
                break;
        }
        MiLinkLog.i(TAG, "setState mState = " + mState + ",newState = " + newState);
        MiLinkLog.v(TAG, "mSessionList.size=" + mSessionList.size());
        int oldState = mState;
        mState = newState;
        if (mState != oldState) {
            EventBus.getDefault().post(new SessionManagerStateChangeEvent(
                    SessionManagerStateChangeEvent.EventType.SessionStateChange, oldState, mState));
        }
    }

    private void setAssistSessionState(int newState) {
        switch (newState) {
            // 设置为no session状态，sessionmanger抛弃并关闭所维护的所有session
            case NO_SESSION_STATE:
                if (mAssistSession != null) {
                    mAssistSession.mFlagForSessionManager = FLAG_ABANDON_SESSION;
                    mAssistSession.close();
                    mAssistSession = null;
                }
                break;
            default:
                break;
        }
        MiLinkLog.i(TAG,
                "setAssistSessionState mAssistSessionState = " + mAssistSessionState + ",newState = " + newState);
        mAssistSessionState = newState;
    }

    private void closeAllBandonSession() {
        List<Session> closeSuccessSession = new ArrayList<Session>();
        for (Session s : mSessionList) {
            if (s.mFlagForSessionManager == FLAG_ABANDON_SESSION && s.close()) {
                closeSuccessSession.add(s);
            }
        }
        for (Session s : closeSuccessSession) {
            mSessionList.remove(s);
        }
    }

    private Object mReportLock = new Object();

    /**
     * 建立session的最终结果
     *
     * @param errorCode 建立session的错误码
     * @return 是否成功通知到外面
     * @see Error2
     */
    public void onOpenSessionResult(final int errorCode, final long openStartTime) {
        MiLinkLog.w(TAG, "onOpenSessionResult, errorCode = " + errorCode);
        // 只有在session成功了，才启动定时器
        if (errorCode != Const.InternalErrorCode.SUCCESS) {
            stopTimer();
            if (isMultiUnavailable()) {
                MiLinkLog.w(TAG, "statistic milink.open, code=" + MILINK_OPEN_RET_CODE_MULTI_UNAVAILABLE);
                InternalDataMonitor.getInstance().trace("", 0, Const.MnsCmd.MNS_OPEN_CMD,
                        MILINK_OPEN_RET_CODE_MULTI_UNAVAILABLE, openStartTime, System.currentTimeMillis(), 0, 0, 0);
            } else {
                int errCodes[] = {ERRNO_PERMISSION_DENIED, ERRNO_CONNECT_TIME_OUT, ERRNO_NO_ROUTE, ERRNO_REFUSED,
                        ERRNO_NET_UNREACHABLE, Const.InternalErrorCode.MNS_LOAD_LIBS_FAILED};
                int retCodes[] = {MILINK_OPEN_RET_CODE_PERMISSION_DENIED, MILINK_OPEN_RET_CODE_ALL_TIME_OUT,
                        MILINK_OPEN_RET_CODE_NO_ROUTE, MILINK_OPEN_RET_CODE_REFUSED,
                        MILINK_OPEN_RET_CODE_NET_UNREACHABLE, MILINK_OPEN_RET_CODE_LOAD_SO_FAILED};
                // 是否都是同一种错误码
                boolean isSameErrCode = false;
                for (int i = 0; i < errCodes.length && i < retCodes.length; i++) {
                    if (isAllSessionErrorCode(errCodes[i])) {
                        MiLinkLog.w(TAG, "statistic milink.open, code=" + retCodes[i]);
                        InternalDataMonitor.getInstance().trace("", 0, Const.MnsCmd.MNS_OPEN_CMD, retCodes[i],
                                openStartTime, System.currentTimeMillis(), 0, 0, 0);
                        isSameErrCode = true;
                        // 都是同一种错误码就不上报了
                        return;
                    }
                }
                // 不是 检测外网
                if (!isSameErrCode) {
                    if (NetworkDash.isAvailable()) {

                        threadPool.execute(new Runnable() {

                            @Override
                            public void run() {
                                long now = System.currentTimeMillis();
                                MiLinkLog.w(TAG, "check isInternetAvailable begin ,id=" + now + ", mInfoOnOpen="
                                        + mNetworkDetailInfoOnOpen);
                                if (SessionConst.isInternetAvailable()) { // 可以访问外网
                                    NetworkDetailInfo info = Network.getCurrentNetworkDetailInfo();
                                    MiLinkLog.v(TAG, "NetworkDetailInfo current=" + info + ",id=" + now);
                                    int code = MILINK_OPEN_RET_CODE_NETWORK_CHANGE;
                                    if (info.equals(mNetworkDetailInfoOnOpen)) {
                                        MiLinkLog.v(TAG, "at most wait 15s，id=" + now);
                                        // 等15s钟,这一次跑马确实失败了。但是为了防止网络突然变好时，跑马也能成功了，外网也能访问了。系统网络类型也没变，但这种情况应该归于为网络变化引起的。
                                        synchronized (mReportLock) {
                                            try {
                                                mReportLock.wait(15 * 1000);
                                            } catch (InterruptedException e) {
                                            }
                                        }
                                        if (mMasterSession == null) {
                                            code = MILINK_OPEN_RET_CODE_FAIL;
                                        } else {
                                            code = MILINK_OPEN_RET_CODE_NETWORK_CHANGE;
                                        }
                                    } else {
                                        code = MILINK_OPEN_RET_CODE_NETWORK_CHANGE;
                                    }
                                    MiLinkLog.w(TAG, "statistic milink.open, code=" + code + ",id=" + now);
                                    InternalDataMonitor.getInstance().trace("", 0, Const.MnsCmd.MNS_OPEN_CMD, code,
                                            openStartTime, System.currentTimeMillis(), 0, 0, 0);
                                }
                                MiLinkLog.w(TAG, "check isInternetAvailable end, id=" + now);
                            }
                        });

                    } else {
                        MiLinkLog.w(TAG, "check isInternetAvailable, but network is unavailable");
                    }
                }
                // 网络可用，再次跑马
                if (NetworkDash.isAvailable()) {
                    mHandler.removeCallbacks(mInternalAutoOpenRunnable);
                    mHandler.postAtTime(mInternalAutoOpenRunnable, AUTO_INTERNAL_OPEN_DELAY);
                    MiLinkLog.v(TAG, "onOpenSessionResult reconnect times:" + mOpenSessionTryTimes);
                }
            }
        } else {
            // 上报成功
            if (mMasterSession != null) {
                MiLinkLog.w(TAG, String.format("mMasterSession = [Session No:%d] ", mMasterSession.getSessionNO()));
                InternalDataMonitor.getInstance().trace(mMasterSession.getServerProfile().getServerIP(),
                        mMasterSession.getServerProfile().getServerPort(), Const.MnsCmd.MNS_OPEN_CMD,
                        MILINK_OPEN_RET_CODE_OK, openStartTime, System.currentTimeMillis(), 0, 0, 0);
            }
        }
    }

    /**
     * 用户最后一次发包时间
     */
    long mLastUserSendDataTime = System.currentTimeMillis();

    public boolean sendData(PacketData data, int timeout, ResponseListener l) {
        if (TextUtils.isEmpty(data.getCommand())) {
            MiLinkLog.v(TAG, "send data ,cmd can not be null");
            return false;
        }
        if (!MiAccountManager.getInstance().appHasLogined()) {
            MiLinkLog.v(TAG, "send data ,appHasLogined=false,request get st");
            Message localMessage = mHandler.obtainMessage(MSG_TYPE_GET_SERVICE_TOKEN);
            mHandler.sendMessageAtFrontOfQueue(localMessage);
        }
        mLastUserSendDataTime = System.currentTimeMillis();
        data.setSeqNo(Global.getSequence());
        MiLinkLog.v(TAG, "send data cmd=" + data.getCommand() + ", seq=" + data.getSeqNo());
        Request request = new Request(data, l, MiAccountManager.getInstance().getBusinessEncByMode(),
                MiAccountManager.getInstance().getCurrentAccount());
        request.setTimeOut(timeout);
        Message localMessage = mHandler.obtainMessage(MSG_TYPE_SEND_MSG, request);
        mHandler.sendMessage(localMessage);
        return true;
    }

    /**
     * 跑马成功后，抛弃掉所有的session
     */
    private void abandonAllSession() {
        for (Iterator<Session> it = mSessionList.iterator(); it.hasNext(); ) {
            // 将这个session保存到废弃列表中，等它回来时删除
            Session s = it.next();
            if (s.mFlagForSessionManager == FLAG_TRTING_SESSION) {
                s.mFlagForSessionManager = FLAG_ABANDON_SESSION;
                if (s.getServerProfile() != null) {
                    MiLinkLog.w(TAG,
                            "abandon all session, ip=" + s.getServerProfile().getServerIP() + ", port="
                                    + s.getServerProfile().getServerPort() + ", protocol="
                                    + s.getServerProfile().getProtocol() + ",No=" + s.getSessionNO());
                } else {
                    MiLinkLog.w(TAG, "abandon all session, s.getServerProfile()=null, sessionNO=" + s.getSessionNO());
                }
            }
        }
    }

    /**
     * 获取下一个服务器配置列表
     *
     * @param session    失败的session
     * @param failReason 失败的原因
     *                   <ul>
     *                   <li>{@link SessionConst#CONN_FAILED}
     *                   <li>{@link SessionConst#HANDSHAKE_OTHERERROR_FAILED}
     *                   <li>{@link SessionConst#HANDSHAKE_PACKERROR_FAILED}
     *                   </ul>
     */
    private void getNextServerProfile(Session session, int failReason) {
        // 更新失败统计
        // updateOpenSessionStatistic(session, failReason);
        MiLinkLog.i(TAG, "getNextServerProfile " + String.format("[Session No:%d] ", session.getSessionNO()));
        ServerProfile[] newServerProfile = mServerManager.getNext(session.getServerProfile(), failReason);
        if (newServerProfile == null) {
            MiLinkLog.i(TAG, "newServerProfile == null");
            // 这个session都测试完成
            session.mFlagForSessionManager = FLAG_ABANDON_SESSION;
            if (session.close()) {
                mSessionList.remove(session);
            }

            if (!isHaveTryingSession()) {
                MiLinkLog.i(TAG, "already no trying session");
                if (mMasterSession == null) {
                    // 所有session都失败了
                    setState(NO_SESSION_STATE);
                    onOpenSessionResult(Const.InternalErrorCode.CONNECT_FAIL, mOpenStartTime);
                } else {
                    MiLinkLog.e(TAG, "this session is trying session but masterSession is not null");
                }
            }
        } else {
            // 返回的newServerProfile不为null，继续跑马
            for (int i = 0; i < newServerProfile.length; i++) {
                if (newServerProfile[i] == null) {
                    continue;
                }

                if (i == 0) {
                    session.mFlagForSessionManager = FLAG_TRTING_SESSION;
                    session.openSession(newServerProfile[i]);
                } else {
                    Session newSession = new Session();
                    newSession.mFlagForSessionManager = FLAG_TRTING_SESSION;
                    mSessionList.add(newSession);
                    newSession.openSession(newServerProfile[i]);
                }
            }

        }
    }

    /**
     * 判断是否为正在尝试的session
     *
     * @param session 需要判断的session
     * @return 如果是返回true，否则返回false
     */
    private boolean isHaveTryingSession() {
        // 先检查是否是正在尝试的session
        for (Iterator<Session> it = mSessionList.iterator(); it.hasNext(); ) {
            Session s = it.next();
            if (s.mFlagForSessionManager == FLAG_TRTING_SESSION) {
                return true;
            }
        }
        return false;
    }

    boolean mGlobalPushFlag = false;

    public void setGlobalPushFlag(boolean enable) {
        mGlobalPushFlag = enable;
    }

    public boolean getGlobalPushFlag() {
        return mGlobalPushFlag;
    }

    private class ScreenOnChangeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                EventBus.getDefault().post(new SystemNotificationEvent(SystemNotificationEvent.EventType.ScreenOn));
                MnsPacketDispatcher.getInstance().setDispatchPacketDelayTimeWhenScreenOn();
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                MiLinkLog.v(TAG, "ScreenOnChangeReceiver screen_off");
                MnsPacketDispatcher.getInstance().setDispatchPacketDelayTimeWhenScreenOff();
            }
        }

    }

    private class NetworkChangeReceiver extends BroadcastReceiver {


        private int currentType = -1; // TYPE_NONE

        private String apnName = ""; // 对于wifi网络，存放bssid

        private boolean isNetworkChanged(NetworkInfo networkInfo) {
            if (networkInfo == null) {
                if (currentType == -1 && TextUtils.isEmpty(apnName)) {
                    return false;
                } else {
                    return true;
                }
            }
            boolean isChanged = true;
            if (currentType == networkInfo.getType()) {
                if (currentType == ConnectivityManager.TYPE_MOBILE) {
                    if (apnName != null && apnName.equals(networkInfo.getSubtypeName())) {
                        isChanged = false;
                    }
                } else {
                    if (apnName != null && apnName.equals(WifiDash.getBSSID())) {
                        isChanged = false;
                    }
                }
            }
            return isChanged;
        }

        public void setCurrentNetworkInfo() {
            try {
                ConnectivityManager connectivityMgr = (ConnectivityManager) Global
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connectivityMgr.getActiveNetworkInfo();
                MiLinkLog.i(TAG, "NetworkChangeReceiver, setCurrentNetworkInfo=" + networkInfo);
                setCurrentNetworkInfo(networkInfo);
            } catch (Exception e) {
                setCurrentNetworkInfo(null);
                MiLinkLog.e(TAG, "Get networkInfo fail", e);
            }
        }

        private void setCurrentNetworkInfo(NetworkInfo networkInfo) {
            if (networkInfo != null) {
                currentType = networkInfo.getType();
                if (currentType == ConnectivityManager.TYPE_MOBILE) {
                    apnName = networkInfo.getSubtypeName();
                } else {
                    apnName = WifiDash.getBSSID();
                }
            } else {
                currentType = -1;
                apnName = "";
            }
        }

        Runnable runable = new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                try {
                    ConnectivityManager connectivityMgr = (ConnectivityManager) Global
                            .getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo networkInfo = connectivityMgr.getActiveNetworkInfo();
                    MiLinkLog.w(TAG, "NetworkChangeReceiver, networkInfo=" + networkInfo);

                    if (NetworkDash.isAvailable() && NetworkDash.isWifi()) {
                        MiLinkLog.i(TAG, "WIFI info : " + WifiDash.getWifiInfo());
                    }

                    boolean isNetworkChange = isNetworkChanged(networkInfo);
                    MiLinkLog.w(TAG, "isNetworkChange : " + isNetworkChange);
                    // 必然要都会更新当前网络信息。
                    setCurrentNetworkInfo(networkInfo);

                    // 如果网络是连接的
                    if (networkInfo != null && networkInfo.isAvailable()) {
                        // 尝试重新启动心跳
                        AlarmClockService.startIfNeed();
                        if (isNetworkChange) { // 网络变化了
                            MiLinkLog.i(TAG, "NetworkChangeReceiver, network change need forceOpen");
                            SessionConst.setNewApn(true);
                            String defaultHost = MiLinkIpInfoManager.getInstance().getDefaultHost();
                            if (MiAccountManager.getInstance().appHasLogined()) {
                                DomainManager.getInstance().startResolve(defaultHost);
                            }
                            Global.getMainHandler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    EventBus.getDefault().post(new SystemNotificationEvent(
                                            SystemNotificationEvent.EventType.NetWorkChange));
                                }
                            }, 2000);

                        } else { // 网络没有变化
                            MiLinkLog.i(TAG, "NetworkChangeReceiver, network not change, mState=" + mState);
                            if (mState == NO_SESSION_STATE) {
                                String defaultHost = MiLinkIpInfoManager.getInstance().getDefaultHost();
                                if (MiAccountManager.getInstance().appHasLogined()) {
                                    DomainManager.getInstance().startResolve(defaultHost);
                                }
                                Global.getMainHandler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        EventBus.getDefault().post(new SystemNotificationEvent(
                                                SystemNotificationEvent.EventType.NetWorkChange));
                                    }
                                }, 2000);
                            } else {
                                // 发ping包
                                Session session = getSession();
                                if (session != null && session.isAvailable()) {
                                    session.ping();
                                }
                            }
                        }
                    } else {
                        if (mState != NO_SESSION_STATE) {
                            close();
                        }
                        MiLinkLog.i(TAG, "network is disconnected()");
                    }
                } catch (Exception e) {
                    setCurrentNetworkInfo(null);
                    MiLinkLog.e(TAG, "Get networkInfo fail", e);
                }
            }
        };

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!MiAccountManager.getInstance().appHasLogined()) {
                MiLinkLog.v(TAG, "app not login, ignore network change broadcast");
                return;
            }
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                mHandler.post(runable);
            }
        }

    }

    // ////////////////////////wakelock代码段////////////////////////////////////////////////////

    /**
     * 在后台模式下，释放wakelock，省电
     */
    private void releaseWakeLock() {
        synchronized (mLock) {
            try {
                if (mWakeLock != null) {
                    MiLinkLog.w(TAG, "Wakelock RELEASED :)");
                    mWakeLock.release();
                    mWakeLock = null;
                }
            } catch (Exception e) {
                MiLinkLog.e(TAG, "releaseWakeLock exception", e);
                mWakeLock = null;
            }
        }
    }

    /**
     * 在后台模式下，获取wakelock，保障请求能够发送出去
     */
    private void acquireWakeLock() {
        acquireWakeLock(CHECK_WAKE_LOCK_TIMEOUT);
    }

    /**
     * 在后台模式下，获取wakelock，保障请求能够发送出去
     */
    private void acquireWakeLock(int wakeTime) {
        // if (mRunTimeState == RuntimeState.Foreground)
        // return;

        if (mHandler != null) {
            mHandler.removeMessages(MSG_TYPE_RELEASE_WAKE_LOCK);

            synchronized (mLock) {
                try {
                    Context context = Global.getApplicationContext();
                    if (context != null && mWakeLock == null) {
                        MiLinkLog.w(TAG, "Wakelock ACQUIRED :)");
                        PowerManager pm = (PowerManager) context.getApplicationContext()
                                .getSystemService(Context.POWER_SERVICE);
                        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "milink");
                        mWakeLock.acquire();
                    }
                } catch (Exception e) {
                    MiLinkLog.e(TAG, "acquireWakeLock exception", e);
                }
            }

            if (null != mHandler) {
                mHandler.sendEmptyMessageDelayed(MSG_TYPE_RELEASE_WAKE_LOCK, wakeTime);
            }
        }
    }

    /**
     * 登录milink,保证在SM所在线程被调用
     */
    public void login(String from) {
        MiLinkLog.v(TAG, "login from=" + from);
        mHandler.removeCallbacks(mLogoffRunnable);
        if (MiAccountManager.getInstance().isLogining()) {
            MiLinkLog.v(TAG, "milink is logining");
            return;
        }

        if (!MiAccountManager.getInstance().appHasLogined()) {
            MiLinkLog.v(TAG, "app not login, cancel milink login");
            Message localMessage = mHandler.obtainMessage(MSG_TYPE_GET_SERVICE_TOKEN);
            mHandler.sendMessageAtFrontOfQueue(localMessage);
            return;
        }

        if (!mAppInited) {
            MiLinkLog.v(TAG, "app not init");
            Message localMessage = mHandler.obtainMessage(MSG_TYPE_APP_NOT_INIT);
            mHandler.sendMessageAtFrontOfQueue(localMessage);
        }

        if (mState == NO_SESSION_STATE) {
            internalManualOpen();
            return;
        }
        if (mState == TRING_SESSION_STATE) {
            return;
        }
        MiLinkLog.v(TAG, "milink login, session manager state: " + mState);
        Session session = getSession();
        if (session == null || !session.isAvailable()) {
            MiLinkLog.v(TAG, "login session is not available.");
            return;
        }
        if (mLoginTryTimes < LOGIN_TRY_TIMES) {
            mLoginTryTimes++;
            MiLinkLog.v(TAG, "milink login start, mLoginTryTimes=" + mLoginTryTimes);
            session.fastLogin();
        } else {
            MiLinkLog.v(TAG, "milink login has exceeded max times");
        }
    }

    private Runnable mLogoffRunnable = new Runnable() {

        @Override
        public void run() {
            MiLinkLog.v(TAG, "milink mLogoffRunnable run");
            // 如果是标准登出且不支持匿名，则断开连接。如果是匿名模式登出，直接断开连接。
            if (!MiAccountManager.getInstance().isAllowAnonymousMode()
                    || MiAccountManager.getInstance().isAnonymousModeCurrent()) {
                internalClose();
            }
            MiAccountManager.getInstance().logoff();
            resetAllTryTimes();
            mSendQueue.clear();
        }
    };

    public void logoff() {
        MiLinkLog.v(TAG, "milink logoff");
        InternalDataMonitor.getInstance().doPostDataAtOnce();
        if (mMasterSession != null && mMasterSession.isAvailable()) {
            mMasterSession.logoff();
            mHandler.removeCallbacks(mLogoffRunnable);
            mHandler.postDelayed(mLogoffRunnable, 2000); // delay 2s
            // 这种写法不能保证logoff包成功发出，不过，无所谓，能发出最好，发不出也无所谓。
        } else {
            mLogoffRunnable.run();
        }
    }

    // 重置所有重试次数
    private void resetAllTryTimes() {
        mOpenSessionTryTimes = 0;
        mSessionReconnectTryTimes = 0;
        mLoginTryTimes = 0;
    }

    /**
     * 发送缓存请求
     *
     * @return 是否发送成功
     */
    private boolean sendCacheRequest() {
        Session session = getSession();
        if (session == null) {
            MiLinkLog.e(TAG, "sendCacheRequest session == null impossible!!!");
            return false;
        }

        MiLinkLog.i(TAG, "sendCacheRequest size = " + mSendQueue.size());
        for (Iterator<Request> it = mSendQueue.iterator(); it.hasNext(); ) {
            Request request = it.next();
            if (request == null) {
                it.remove();
                continue;
            }
            session.handleRequest(request);
            it.remove();
        }
        return true;
    }

    public boolean enableConnectionManualMode(boolean enable) {
        MiLinkLog.v(TAG, "enableConnectionManualMode, enable=" + enable);
        // 模式与当前模式相同，无需改变
        if (enable == mEnableConnectionManualMode) {
            return true;
        }
        // 若要变为手动模式
        if (enable) {
            mServerManager = ManualServerManager.getInstance();
        } else {
            // 若要变为自动模式
            mServerManager = MiLinkServerManager.getInstance();
        }
        mEnableConnectionManualMode = enable;
        close();
        EventBus.getDefault().post(new ClientActionEvent(ClientActionEvent.EventType.ClientForceOpen));
        return true;
    }

    public void suspectBadConnection() {
        EventBus.getDefault().post(new ClientActionEvent(ClientActionEvent.EventType.ClientSuspectBadConnection));
    }

    public void setIpAndPortInManualMode(String ip, int port) {
        MiLinkLog.v(TAG, "setIpAndPortInManualMode, ip=" + ip + ":" + port);
        ManualServerManager.getInstance().setIp(ip);
        ManualServerManager.getInstance().setPort(port);
        if (mEnableConnectionManualMode) {
            if (mMasterSession != null) {
                ServerProfile sp = mMasterSession.getServerProfile();
                // 若手动模式链接存活，且ip和port都与要设置的一致，则不需要改变
                if (sp.getServerIP() == ip && sp.getServerPort() == port) {
                    return;
                }
            }
            close();
            EventBus.getDefault().post(new ClientActionEvent(ClientActionEvent.EventType.ClientForceOpen));
        }
    }

    /**
     * 保证在SM线程运行
     *
     * @return
     */
    public boolean tryConnectIfNeed() {
        if (mState == NO_SESSION_STATE) {
            internalOpen();
            return true;
        }
        if (mState == TRING_SESSION_STATE) {
            return false;
        }
        if (mLoginState == NOLOGIN_SESSION_STATE) {
            mLoginTryTimes = 0;
            login("tryConnectIfNeed");
            return true;
        }
        return false;
    }

    public boolean isMiLinkLogined() {
        return mLoginState == LOGINED_SESSION_STATE;
    }

}

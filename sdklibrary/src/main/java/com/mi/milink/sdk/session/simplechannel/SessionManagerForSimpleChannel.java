
package com.mi.milink.sdk.session.simplechannel;

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

import com.mi.milink.sdk.account.manager.MiChannelAccountManager;
import com.mi.milink.sdk.aidl.PacketData;
import com.mi.milink.sdk.base.CustomHandlerThread;
import com.mi.milink.sdk.base.Global;
import com.mi.milink.sdk.base.os.Device.Network;
import com.mi.milink.sdk.base.os.Device.Network.NetworkDetailInfo;
import com.mi.milink.sdk.base.os.info.NetworkDash;
import com.mi.milink.sdk.base.os.info.WifiDash;
import com.mi.milink.sdk.config.MiLinkIpInfoManager;
import com.mi.milink.sdk.config.MiLinkIpInfoManagerForSimpleChannel;
import com.mi.milink.sdk.connection.DomainManager;
import com.mi.milink.sdk.data.Const;
import com.mi.milink.sdk.debug.MiLinkLog;
import com.mi.milink.sdk.debug.InternalDataMonitor;
import com.mi.milink.sdk.debug.TrafficMonitor;
import com.mi.milink.sdk.event.MiLinkEventForSimpleChannel.ClientActionEvent;
import com.mi.milink.sdk.event.MiLinkEventForSimpleChannel.ServerNotificationEvent;
import com.mi.milink.sdk.event.MiLinkEventForSimpleChannel.SessionConnectEvent;
import com.mi.milink.sdk.event.MiLinkEventForSimpleChannel.SessionLoginEvent;
import com.mi.milink.sdk.event.MiLinkEventForSimpleChannel.SessionManagerNotificationEvent;
import com.mi.milink.sdk.event.MiLinkEventForSimpleChannel.SessionManagerStateChangeEvent;
import com.mi.milink.sdk.event.MiLinkEventForSimpleChannel.SessionOtherEvent;
import com.mi.milink.sdk.event.MiLinkEventForSimpleChannel.SystemNotificationEvent;
import com.mi.milink.sdk.proto.SystemPacketProto.MnsCmdChannelNewPubKeyRsp;
import com.mi.milink.sdk.proto.SystemPacketProto.PublicKeyInfo;
import com.mi.milink.sdk.session.common.IServerManager;
import com.mi.milink.sdk.session.common.OpenSessionSucessReturnInfo;
import com.mi.milink.sdk.session.common.Request;
import com.mi.milink.sdk.session.common.ResponseListener;
import com.mi.milink.sdk.session.common.ServerProfile;
import com.mi.milink.sdk.session.common.SessionConst;
import com.mi.milink.sdk.session.persistent.Session;

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
public class SessionManagerForSimpleChannel extends CustomHandlerThread {
	private static final String TAG = "SessionManagerForSimpleChannel";

	// private static final int MEDIUM_CONNECTION_CLOSE_INTERNAL = 10 * 60 *
	// 1000;

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

	// private static final int MSG_TYPE_SPEED_TEST = 21;

	private static final int MSG_TYPE_RELEASE_WAKE_LOCK = 22;

	// private static final int MSG_TYPE_GET_SERVICE_TOKEN = 23;

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

	// private static SessionManagerForSimpleChannel sInstance;

	private final ConcurrentLinkedQueue<Request> mSendQueue = new ConcurrentLinkedQueue<Request>();

	private static final int FLAG_TRTING_SESSION = 1;

	private static final int FLAG_ABANDON_SESSION = 3;

	private static final int FLAG_MASTER_SESSION = 4;

	private final List<SessionForSimpleChannel> mSessionList = new ArrayList<SessionForSimpleChannel>(); // 所有由SessionManager维护的Session的列表

	private final HashMap<String, Integer> mSessionAddress2ErrorCodeMap = new HashMap<String, Integer>(); // 地址与错误码

	private SessionForSimpleChannel mMasterSession; // 目前主要一个主session

	private IServerManager mServerManager = null;
	private IServerManager mServerManagerNormal = null;
	private IServerManager mServerManagerBackup = null;

	private WakeLock mWakeLock = null; // 后台时唤醒

	private NetworkChangeReceiver mNetworkReveiver = null;// 网络变化

	private NetworkDetailInfo mNetworkDetailInfoOnOpen;

	private Object mLock = null;

	private int mState = NO_SESSION_STATE;

	private int mLoginState = NOLOGIN_SESSION_STATE;

	/**
	 * 当前链接模式，手动或自动。默认为自动跑马。
	 */
	private boolean mEnableConnectionManualMode = false;

	private int mOpenSessionTryTimes = 0; // 跑马重试次数

	private int mSessionReconnectTryTimes = 0; // 连接次数

	private int mLoginTryTimes = 0; // login重试次数

	private boolean mAppInited = false;

	private long mOpenStartTime = 0;

	private MiChannelAccountManager accountManager;

	private MiLinkIpInfoManagerForSimpleChannel ipInfoManage;

	private ThreadPoolExecutor threadPool = new ThreadPoolExecutor(1, 1, 1, TimeUnit.SECONDS,
			new ArrayBlockingQueue<Runnable>(3), new ThreadPoolExecutor.DiscardPolicy());

	private EventBus channelEventbus;

	public EventBus getChannelEventBus() {
		return channelEventbus;
	}

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
		resetAllTryTimes();
	}

	public SessionManagerForSimpleChannel(EventBus channelEventbus, MiChannelAccountManager accountManager) {
		super(TAG);
		MiLinkLog.w(TAG, "SessionManagerForSimpleChannel created, milinkversion=" + Global.getMiLinkVersion() + "_"
				+ Global.getMiLinkSubVersion());// 这行日志还隐含一个意思就是初始化MiLinkLog
		TrafficMonitor.getInstance().start();
		this.channelEventbus = channelEventbus;
		this.accountManager = accountManager;
		this.ipInfoManage = new MiLinkIpInfoManagerForSimpleChannel();
		mServerManagerNormal = new MiLinkServerManagerForSimpleChannel(ipInfoManage);
		mServerManagerBackup = new MiLinkBackupServerManagerForSimpleChannel(ipInfoManage);
		mServerManager = mServerManagerNormal;
		setState(NO_SESSION_STATE);
		mLock = new Object();
		// 设置心跳
		mNetworkReveiver = new NetworkChangeReceiver();
		mNetworkReveiver.setCurrentNetworkInfo();
		Global.registerReceiver(mNetworkReveiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		if (mAppInited) {
			String defaultHost = ipInfoManage.getDefaultHost();
			DomainManager.getInstance().startResolve(defaultHost);
		}
		MiLinkLog.w(TAG, "SessionManager created finish");
	}

	public void addPacketInSendQueue(Request request) {
		mSendQueue.add(request);
		MiLinkLog.d(TAG, "add packet in send queue");
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
			SessionForSimpleChannel session = getSession();
			if (session != null) {
				session.checkRequestsTimeout();
			}
			// 所有的tryingsession都应该被检查，防止一直处于 state=1的状态
			for (SessionForSimpleChannel s : mSessionList) {
				if (s.mFlagForSessionManager == FLAG_TRTING_SESSION) {
					s.checkRequestsTimeout();
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
		case MSG_TYPE_APP_NOT_INIT: {
			MiLinkLog.v(TAG, "MSG_TYPE_APP_NOT_INIT,app not init, call app init by onEventGetServiceToken");
			channelEventbus.post(
					new SessionManagerNotificationEvent(SessionManagerNotificationEvent.EventType.GetServiceToken));
		}
			break;
		case MSG_TYPE_SEND_MSG: {
			SessionForSimpleChannel session = getSession();
			MiLinkLog.v(TAG, "send data, session manager state: " + mState);

			// 判定死链接问题
			if (session != null && session.isDeadConnection(60 * 100, 3 * 60 * 1000)) {
				MiLinkLog.w(TAG, "session isDeadConnection=true");
				setState(NO_SESSION_STATE);
				session = null;
			}
			Request request = (Request) msg.obj;
			if (session == null || !session.isAvailable() || mLoginState != LOGINED_SESSION_STATE) {
				// cache request
				MiLinkLog.v(TAG, "push request in cache, seq=" + request.getSeqNo());
				mSendQueue.add(request);
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

	private void processEvent(SessionConnectEvent event) {
		SessionForSimpleChannel session = event.mSession;
		int errCode = event.mRetCode;
		switch (event.mEventType) {
		case SessionBuildFailed: {
			MiLinkLog.w(TAG, "SessionConnectEvent SessionBuildFailed");
			synchronized (mReportLock) {
				mReportLock.notifyAll();
			}
			// 错误码统计的唯一入口
			String address = String.format("%s:%s", session.getServerProfileForStatistic().getServerIP(),
					session.getServerProfileForStatistic().getServerPort());
			mSessionAddress2ErrorCodeMap.put(address, errCode);
			// 先检查是否是废弃的session
			if (isAbandonSession(session)) {
				return;
			}

			MiLinkLog.w(TAG, "MSG_TYPE_OPEN_SESSION_FAIL errCode:" + errCode);
			if (session.mFlagForSessionManager == FLAG_MASTER_SESSION) {
				MiLinkLog.w(TAG,
						String.format("handleMessage MSG_TYPE_OPEN_SESSION_FAIL is mMasterSession No:%d, mState = %d",
								session.getSessionNO(), mState));
				setState(NO_SESSION_STATE);
				if (NetworkDash.isAvailable()) {
					mInternalAutoOpenRunnable.run();
				}
				return;
			}
			if (session.mFlagForSessionManager == FLAG_TRTING_SESSION) {
				MiLinkLog.w(TAG,
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
					SessionForSimpleChannel newSession = new SessionForSimpleChannel(this, accountManager);
					newSession.mFlagForSessionManager = FLAG_TRTING_SESSION;
					mSessionList.add(newSession);
					setState(TRING_SESSION_STATE);
					newSession.openSession(session.getServerProfileForStatistic());
					mSessionReconnectTryTimes++;
				} else {
					mInternalAutoOpenRunnable.run();
				}
			} else {
				MiLinkLog.e(TAG, "on seesion error network isAvailable = false");
			}
		}
			break;
		default:
			break;
		}
	}

	private void processEvent(SessionLoginEvent event) {
		SessionForSimpleChannel session = event.mSession;
		if (isAbandonSession(session)) {
			return;
		}
		switch (event.mEventType) {
		case LoginFailed: {
			MiLinkLog.w(TAG, "SessionLoginEvent LoginFailed");
			accountManager.setIsLogining(false);
			mLoginState = NOLOGIN_SESSION_STATE;
			channelEventbus.post(new SessionManagerStateChangeEvent(
					SessionManagerStateChangeEvent.EventType.LoginStateChange, Const.NONE, Const.LoginState.NotLogin));
			mHandler.removeMessages(MSG_TYPE_LOGIN_RETRY);
			mHandler.sendEmptyMessageDelayed(MSG_TYPE_LOGIN_RETRY, 10 * 1000);
		}
			break;
		case LoginSuccess: {
			MiLinkLog.w(TAG, "SessionLoginEvent LoginSuccess");
			accountManager.setIsLogining(false);
			mLoginState = LOGINED_SESSION_STATE;
			sendCacheRequest();
			MiLinkLog.v(TAG, "onLoginResult loginState=" + Const.LoginState.Logined);
			channelEventbus.post(new SessionManagerStateChangeEvent(
					SessionManagerStateChangeEvent.EventType.LoginStateChange, Const.NONE, Const.LoginState.Logined));

			addClearConnRunnalbe();

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
		SessionForSimpleChannel session = event.mSession;
		if (isAbandonSession(session)) {
			return;
		}
		switch (event.mEventType) {
		case RecvInvalidPacket: {
			MiLinkLog.w(TAG, "SessionOtherEvent RecvInvalidPacket");
			channelEventbus.post(
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
		case StatisticsTimeoutPacket: {
			MiLinkLog.w(TAG, "SessionOtherEvent StatisticsTimeoutPacket");
			session.postStatisticsTimeoutPacketAction();
		}
			break;
		case PackageNeedRetry: {
			MiLinkLog.w(TAG, "SessionOtherEvent PackageNeedRetry");
			Request request = (Request) event.obj;
			// 立即重发
			Message localMessage = mHandler.obtainMessage(MSG_TYPE_SEND_MSG, request);
			// 是不是可以延迟重发
			mHandler.sendMessageDelayed(localMessage, 2 * 1000);
		}
			break;
		default:
			break;
		}
	}

	private void addChannelPubKeyQueue(Request mRequest) {

		String cmd = mRequest.getData().getCommand();
		if (!Const.MnsCmd.MNS_CHANNEL_FAST_LOGIN.equals(cmd)) {
			addPacketInSendQueue(mRequest);
		}
	}

	@SuppressLint("UseSparseArrays")
	private void processEvent(ServerNotificationEvent event) {
		switch (event.mEventType) {
		case B2tokenExpired: {
			MiLinkLog.w(TAG, "ServerNotificationEvent B2tokenExpired");
			accountManager.logoffMiLink();
			login("B2_TOKEN_EXPIRED");
		}
			break;
		case ChannelPubKeyUpdate: {
			MiLinkLog.e(TAG, "ServerNotificationEvent ChannelPubKeyUpdate");

			UpdateChannelPubKeyValue updateChannelPubKeyValue = (UpdateChannelPubKeyValue) event.mObject;
			MnsCmdChannelNewPubKeyRsp channelNewPubkey = updateChannelPubKeyValue.getChannelNewPubkey();

			addChannelPubKeyQueue(updateChannelPubKeyValue.getmRequeset());
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
				accountManager.getCurrentAccount().setChannelPubKey(channelPubKeyMap);
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
		case ChannelDelPubKey: {
			Request mRequest = (Request) event.mObject;
			addChannelPubKeyQueue(mRequest);
			accountManager.getCurrentAccount().DelChannelPubKey();
			Session session = getSession();
			if (session == null || !session.isAvailable()) {
				MiLinkLog.v(TAG, "login session is not available.");
				break;
			}
			session.fastLogin();
		}
			break;
		case ServerLineBroken: {
			MiLinkLog.e(TAG, "ServerNotificationEvent ServerLineBroken");
			setState(NO_SESSION_STATE);
			if (NetworkDash.isAvailable()) {
				mServerManager = mServerManagerBackup;
				internalOpen();
			} else {
				MiLinkLog.e(TAG, "on server line broken network isAvailable = false");
			}
		}
			break;
		default:
			break;
		}
	}

	private void processEvent(ClientActionEvent event) {
		switch (event.mEventType) {
		case ClientRequestCheckConnection:
			MiLinkLog.w(TAG, "ClientActionEvent ClientRequestCheckConnection");
			tryConnectIfNeed();
			// 简单通知用户已经是登陆成功了
			if (mState == SINGLE_SESSION_STATE) {
				channelEventbus.post(
						new SessionManagerStateChangeEvent(SessionManagerStateChangeEvent.EventType.SessionStateChange,
								Const.NONE, Const.SessionState.Connected));
			}
			if (mLoginState == LOGINED_SESSION_STATE) {
				channelEventbus.post(
						new SessionManagerStateChangeEvent(SessionManagerStateChangeEvent.EventType.LoginStateChange,
								Const.NONE, Const.LoginState.Logined));
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
		default:
			break;
		}
	}

	private void processEvent(SystemNotificationEvent event) {
		switch (event.mEventType) {

		case ScreenOn: {
			MiLinkLog.v(TAG, "SystemNotificationEvent screen_on");
			tryConnectIfNeed();
		}
			break;
		case NetWorkChange: {
			MiLinkLog.v(TAG, "SystemNotificationEvent NetWorkChange");
			acquireWakeLock();
			tryConnectIfNeed();
		}
			break;
		default:
			break;
		}

	}

	private boolean isAbandonSession(SessionForSimpleChannel session) {
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
	 *         <ul>
	 *         <li>{@link SessionConst#NO_SESSION_STATE}
	 *         <li>{@link SessionConst#TRING_SESSION_STATE}
	 *         <li>{@link SessionConst#TEMP_SESSION_STATE}
	 *         <li>{@link SessionConst#SINGLE_SESSION_STATE}
	 *         <li>{@link SessionConst#DUAL_SESSION_STATE}
	 *         </ul>
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
		// if (!MiChannelAccountManager.getInstance().appHasLogined()) {
		// MiLinkLog.i(TAG, "app not login internalOpen cancel");
		// return;
		// }
		if (mHandler == null) {
			MiLinkLog.i(TAG, "can not open session, mHandler == null.");
			return;
		}

		MiLinkLog.i(TAG, "open session, internalOpen with mState = " + mState);
		if (mState != NO_SESSION_STATE) {
			MiLinkLog.i(TAG, "mState is not No_Sesssion state,cancel paoma");
			return;
		}
		MiLinkLog.d(TAG, "internalOpen first");
		mSessionAddress2ErrorCodeMap.clear();
		mOpenStartTime = System.currentTimeMillis();
		mNetworkDetailInfoOnOpen = Network.getCurrentNetworkDetailInfo();
		MiLinkLog.d(TAG, "internalOpen first -0");
		ServerProfile[] serverProfileList = mServerManager.reset(false);
		// 恢复到最优ip列表 
		mServerManager = mServerManagerNormal;
		// error
		MiLinkLog.d(TAG, "internalOpen first -1");
		if (serverProfileList == null || serverProfileList.length == 0) {
			MiLinkLog.e(TAG, "serverProfileList is null ,internalOpne cancel");
			return;
		}
		MiLinkLog.d(TAG, "internalOpen second");
		setState(TRING_SESSION_STATE);
		// 打开各个session
		for (int i = 0; i < serverProfileList.length; i++) {
			if (serverProfileList[i] == null) {
				continue;
			}
			SessionForSimpleChannel session = new SessionForSimpleChannel(this, accountManager);
			session.mFlagForSessionManager = FLAG_TRTING_SESSION;
			mSessionList.add(session);
			session.openSession(serverProfileList[i]);
			MiLinkLog.d(TAG, "internalOpen thrid +" + i);
		}

	}

	/**
	 * 当有session成功时，更新一下当前可用的session(比如tempSession，masterSession等)
	 *
	 * @param session
	 *            成功的session
	 * @return 更新成功返回true，否则返回false
	 */
	private boolean updateSession(SessionForSimpleChannel session) {
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
		accountManager.setIsLogining(false);
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
			MiLinkLog.v(TAG, String.format("clientip:%s clientIsp;%s", clientIp, clientIsp));
			if (!TextUtils.isEmpty(clientIp)) {
				Global.setClientIp(clientIp);
			}
			if (!TextUtils.isEmpty(clientIsp)) {
				Global.setClientIsp(clientIsp);
			}
			if(ipInfoManage!=null){
				if (optmumServerList != null) {
					ipInfoManage.setOptmumServerList(Global.getClientIsp(), optmumServerList);
				}
				if (backupServerList != null) {
					ipInfoManage.setBackupServerList(backupServerList);
				}
			}
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
			if (canStop) {
				for (SessionForSimpleChannel s : mSessionList) {
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
	private SessionForSimpleChannel getSession() {
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

	private void setState(int newState) {
		switch (newState) {
		// 设置为no session状态，sessionmanger抛弃并关闭所维护的所有session
		case NO_SESSION_STATE:

			List<SessionForSimpleChannel> closeSuccessSession = new ArrayList<SessionForSimpleChannel>();
			for (SessionForSimpleChannel s : mSessionList) {
				s.mFlagForSessionManager = FLAG_ABANDON_SESSION;
				if (s.close()) {
					closeSuccessSession.add(s);
				}
			}
			for (SessionForSimpleChannel s : closeSuccessSession) {
				mSessionList.remove(s);
			}
			if (mMasterSession != null) {
				mMasterSession.mFlagForSessionManager = FLAG_ABANDON_SESSION;
				mMasterSession.close();
				mMasterSession = null;
			}
			mLoginState = NOLOGIN_SESSION_STATE;
			accountManager.setIsLogining(false);
			break;

		default:
			break;
		}
		MiLinkLog.i(TAG, "setState mState = " + mState + ",newState = " + newState);
		MiLinkLog.v(TAG, "mSessionList.size=" + mSessionList.size());
		int oldState = mState;
		mState = newState;
		if (mState != oldState) {
			channelEventbus.post(new SessionManagerStateChangeEvent(
					SessionManagerStateChangeEvent.EventType.SessionStateChange, oldState, mState));
		}
	}

	private Object mReportLock = new Object();

	/**
	 * 建立session的最终结果
	 *
	 * @param errorCode
	 *            建立session的错误码
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
				int errCodes[] = { ERRNO_PERMISSION_DENIED, ERRNO_CONNECT_TIME_OUT, ERRNO_NO_ROUTE, ERRNO_REFUSED,
						ERRNO_NET_UNREACHABLE, Const.InternalErrorCode.MNS_LOAD_LIBS_FAILED };
				int retCodes[] = { MILINK_OPEN_RET_CODE_PERMISSION_DENIED, MILINK_OPEN_RET_CODE_ALL_TIME_OUT,
						MILINK_OPEN_RET_CODE_NO_ROUTE, MILINK_OPEN_RET_CODE_REFUSED,
						MILINK_OPEN_RET_CODE_NET_UNREACHABLE, MILINK_OPEN_RET_CODE_LOAD_SO_FAILED };
				// 是否都是同一种错误码
				boolean isSameErrCode = false;
				for (int i = 0; i < errCodes.length && i < retCodes.length; i++) {
					if (isAllSessionErrorCode(errCodes[i])) {
						MiLinkLog.w(TAG, "statistic milink.open, code=" + retCodes[i]);
						InternalDataMonitor.getInstance().trace("", 0, Const.MnsCmd.MNS_OPEN_CMD, retCodes[i],
								openStartTime, System.currentTimeMillis(), 0, 0, 0);
						isSameErrCode = true;
						break;
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

	private void addClearConnRunnalbe() {
		mHandler.removeCallbacks(mClearConnRunnable);
		mHandler.postDelayed(mClearConnRunnable, accountManager.getKeepAliveTime());
		MiLinkLog.d(TAG, "add clearrunnable .");
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
		mLastUserSendDataTime = System.currentTimeMillis();
		data.setSeqNo(Global.getSequence());
		MiLinkLog.v(TAG, "send data cmd=" + data.getCommand() + ", seq=" + data.getSeqNo());
		Request request = new Request(data, l, accountManager.getBusinessEncByMode(),
				accountManager.getCurrentAccount());
		request.setTimeOut(timeout);
		Message localMessage = mHandler.obtainMessage(MSG_TYPE_SEND_MSG, request);
		mHandler.sendMessage(localMessage);

		addClearConnRunnalbe();

		return true;
	}

	/**
	 * 跑马成功后，抛弃掉所有的session
	 */
	private void abandonAllSession() {
		for (Iterator<SessionForSimpleChannel> it = mSessionList.iterator(); it.hasNext();) {
			// 将这个session保存到废弃列表中，等它回来时删除
			SessionForSimpleChannel s = it.next();
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
	 * @param session
	 *            失败的session
	 * @param failReason
	 *            失败的原因
	 *            <ul>
	 *            <li>{@link SessionConst#CONN_FAILED}
	 *            <li>{@link SessionConst#HANDSHAKE_OTHERERROR_FAILED}
	 *            <li>{@link SessionConst#HANDSHAKE_PACKERROR_FAILED}
	 *            </ul>
	 */
	private void getNextServerProfile(SessionForSimpleChannel session, int failReason) {
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
					SessionForSimpleChannel newSession = new SessionForSimpleChannel(this, accountManager);
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
	 * @param session
	 *            需要判断的session
	 * @return 如果是返回true，否则返回false
	 */
	private boolean isHaveTryingSession() {
		// 先检查是否是正在尝试的session
		for (Iterator<SessionForSimpleChannel> it = mSessionList.iterator(); it.hasNext();) {
			SessionForSimpleChannel s = it.next();
			if (s.mFlagForSessionManager == FLAG_TRTING_SESSION) {
				return true;
			}
		}
		return false;
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

		@Override
		public void onReceive(Context context, Intent intent) {
			// if (!MiChannelAccountManager.getInstance().appHasLogined()) {
			// MiLinkLog.v(TAG, "app not login, ignore network change
			// broadcast");
			// return;
			// }
			if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
				try {
					ConnectivityManager connectivityMgr = (ConnectivityManager) Global
							.getSystemService(Context.CONNECTIVITY_SERVICE);
					NetworkInfo networkInfo = connectivityMgr.getActiveNetworkInfo();
					MiLinkLog.i(TAG, "NetworkChangeReceiver, networkInfo=" + networkInfo);

					if (NetworkDash.isAvailable() && NetworkDash.isWifi()) {
						MiLinkLog.i(TAG, "WIFI info : " + WifiDash.getWifiInfo());
					}

					boolean isNetworkChange = isNetworkChanged(networkInfo);
					MiLinkLog.i(TAG, "isNetworkChange : " + isNetworkChange);
					// 必然要都会更新当前网络信息。
					setCurrentNetworkInfo(networkInfo);

					// 如果网络是连接的
					if (networkInfo != null && networkInfo.isAvailable()) {
						if (isNetworkChange) { // 网络变化了
							MiLinkLog.i(TAG, "NetworkChangeReceiver, network change need forceOpen");
							SessionConst.setNewApn(true);
							if (mAppInited) {
								String defaultHost = MiLinkIpInfoManager.getInstance().getDefaultHost();
								DomainManager.getInstance().startResolve(defaultHost);
							}
							Global.getMainHandler().postDelayed(new Runnable() {
								@Override
								public void run() {
									channelEventbus.post(new SystemNotificationEvent(
											SystemNotificationEvent.EventType.NetWorkChange));
								}
							}, 2000);

						} else { // 网络没有变化
							MiLinkLog.i(TAG, "NetworkChangeReceiver, network not change, mState=" + mState);
							if (mState == NO_SESSION_STATE) {
								if (mAppInited) {
									String defaultHost = MiLinkIpInfoManager.getInstance().getDefaultHost();
									DomainManager.getInstance().startResolve(defaultHost);
								}
								Global.getMainHandler().postDelayed(new Runnable() {
									@Override
									public void run() {
										channelEventbus.post(new SystemNotificationEvent(
												SystemNotificationEvent.EventType.NetWorkChange));
									}
								}, 2000);
							} else {
								// 发ping包
								SessionForSimpleChannel session = getSession();
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
		if (accountManager.isLogining()) {
			MiLinkLog.v(TAG, "milink is logining");
			return;
		}

		if (!mAppInited) {
			MiLinkLog.v(TAG, "app not init");
			Message localMessage = mHandler.obtainMessage(MSG_TYPE_APP_NOT_INIT);
			mHandler.sendMessageAtFrontOfQueue(localMessage);
			return;
		}

		if (mState == NO_SESSION_STATE) {
			internalManualOpen();
			return;
		}
		if (mState == TRING_SESSION_STATE) {
			return;
		}
		MiLinkLog.v(TAG, "milink login, session manager state: " + mState);
		SessionForSimpleChannel session = getSession();
		if (session == null || !session.isAvailable()) {
			MiLinkLog.v(TAG, "login session is not available.");
			return;
		}

		// if(
		// !TextUtils.isEmpty(MiChannelAccountManager.getInstance().getCurrentAccount().getB2Security())
		// &&
		// !TextUtils.isEmpty(MiChannelAccountManager.getInstance().getCurrentAccount().getB2Token())
		// ){
		// //b2，和b2token都有值，就不要再执行登陆操作了。
		// MiLinkLog.v(TAG, "b2token|b2key is not empty.no send fastlogin");
		// channelEventbus.post(
		// new SessionLoginEvent(SessionLoginEvent.EventType.LoginSuccess,
		// session.this, 0));
		// return ;
		// }

		if (mLoginTryTimes < LOGIN_TRY_TIMES) {
			mLoginTryTimes++;
			MiLinkLog.v(TAG, "milink login start, mLoginTryTimes=" + mLoginTryTimes);
			session.fastLogin();
		} else {
			MiLinkLog.v(TAG, "milink login has exceeded max times");
		}
	}

	private Runnable mClearConnRunnable = new Runnable() {

		@Override
		public void run() {
			MiLinkLog.v(TAG, "milink clearConn run");

			// 断开连接
			accountManager.logoff();
			close();

			resetAllTryTimes();
			mSendQueue.clear();
		}
	};

	private Runnable mLogoffRunnable = new Runnable() {

		@Override
		public void run() {
			MiLinkLog.v(TAG, "milink mLogoffRunnable run");
			internalClose();
			accountManager.logoff();
			resetAllTryTimes();
			mSendQueue.clear();
		}
	};

	public void logoff() {
		MiLinkLog.v(TAG, "milink logoff");
		InternalDataMonitor.getInstance().doPostDataAtOnce();
		mLogoffRunnable.run();
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
		SessionForSimpleChannel session = getSession();
		if (session == null) {
			MiLinkLog.e(TAG, "sendCacheRequest session == null impossible!!!");
			return false;
		}

		MiLinkLog.i(TAG, "sendCacheRequest size = " + mSendQueue.size());
		for (Iterator<Request> it = mSendQueue.iterator(); it.hasNext();) {
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

	/**
	 * 保证在SM线程运行
	 * 
	 * @return
	 */
	public boolean tryConnectIfNeed() {
		if (mState == NO_SESSION_STATE) {
			internalOpen();
			return false;
		}
		if (mState == TRING_SESSION_STATE) {
			return false;
		}
		if (mLoginState == NOLOGIN_SESSION_STATE) {
			mLoginTryTimes = 0;
			login("tryConnectIfNeed");
			return false;
		}
		return true;
	}

	public boolean isMilinkLogined() {
		return mLoginState == LOGINED_SESSION_STATE;
	}
}

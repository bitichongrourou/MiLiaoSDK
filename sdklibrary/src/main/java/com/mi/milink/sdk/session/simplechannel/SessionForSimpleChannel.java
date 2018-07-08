
package com.mi.milink.sdk.session.simplechannel;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mi.milink.sdk.account.IAccount;
import com.mi.milink.sdk.account.manager.MiAccountManager;
import com.mi.milink.sdk.account.manager.MiChannelAccountManager;
import com.mi.milink.sdk.aidl.PacketData;
import com.mi.milink.sdk.base.Global;
import com.mi.milink.sdk.base.os.Device;
import com.mi.milink.sdk.base.os.info.DeviceDash;
import com.mi.milink.sdk.config.ConfigManager;
import com.mi.milink.sdk.config.MiLinkIpInfoManager;
import com.mi.milink.sdk.connection.DomainManager;
import com.mi.milink.sdk.connection.IConnectionCallback;
import com.mi.milink.sdk.connection.TcpConnection;
import com.mi.milink.sdk.data.Const;
import com.mi.milink.sdk.debug.BaseDataMonitor;
import com.mi.milink.sdk.debug.InternalDataMonitor;
import com.mi.milink.sdk.debug.MiLinkLog;
import com.mi.milink.sdk.debug.TrafficMonitor;
import com.mi.milink.sdk.event.MiLinkEventForSimpleChannel;
import com.mi.milink.sdk.event.MiLinkEventForSimpleChannel.ServerNotificationEvent;
import com.mi.milink.sdk.event.MiLinkEventForSimpleChannel.SessionConnectEvent;
import com.mi.milink.sdk.event.MiLinkEventForSimpleChannel.SessionLoginEvent;
import com.mi.milink.sdk.event.MiLinkEventForSimpleChannel.SessionOtherEvent;
import com.mi.milink.sdk.proto.DataExtraProto.DataAnonymousWid;
import com.mi.milink.sdk.proto.SystemPacketProto.MnsCmdChannelReq;
import com.mi.milink.sdk.proto.SystemPacketProto.MnsCmdChannelRsp;
import com.mi.milink.sdk.proto.SystemPacketProto.MnsCmdLoginOff;
import com.mi.milink.sdk.session.common.InvalidPacketExecption;
import com.mi.milink.sdk.session.common.MsgProcessor;
import com.mi.milink.sdk.session.common.OpenSessionSucessReturnInfo;
import com.mi.milink.sdk.session.common.ReceiveBuffer;
import com.mi.milink.sdk.session.common.ReceiveBuffer.ReceiveBufferSink;
import com.mi.milink.sdk.session.common.Request;
import com.mi.milink.sdk.session.common.ResponseListener;
import com.mi.milink.sdk.session.common.ServerProfile;
import com.mi.milink.sdk.session.common.SessionConst;
import com.mi.milink.sdk.session.common.StreamUtil;
import com.mi.milink.sdk.session.persistent.MnsPacketDispatcher;

import android.annotation.SuppressLint;
import android.text.TextUtils;

/**
 * session封装了连接，收发消息等
 */
public class SessionForSimpleChannel extends com.mi.milink.sdk.session.persistent.Session
		implements IConnectionCallback, MsgProcessor {
	private static final String TAG = "SessionForSimpleChannel";

	// private IConnection mConn;
	//
	// private ReceiveBuffer mRecBuffer;
	//
	// private int mSessionNO = 0;
	//
	// private long mConnectStartTime = 0;
	//
	// private long mOpenSessionDoneTime = 0;
	//
	// protected long mLastReceivedPacketTime = 0; // 记录上次收到包的时间
	//
	// private long mDnsWaitTime = 0;
	//
	// private ConcurrentHashMap<Integer, Request> mRequestMap = new
	// ConcurrentHashMap<Integer, Request>();
	//
	// private ConcurrentHashMap<Integer, Request>
	// mPendingStatisticTimeoutedRequestMap = new ConcurrentHashMap<Integer,
	// Request>();
	//
	// private long mLastSendFastCheckPingTime = 0; // 记录上次发送check ping的时间
	//
	// private long mLastReceivedFastPingTime = 0; // 记录上次收到包的时间
	//
	// private String mLogTag = TAG;
	//
	// private int mContinuousRecv110Count = 0;
	//
	// public int mFlagForSessionManager;
	//
	// private boolean mCanClose = true; // 如果正在连接时，调用mConn.close()，native会出现崩溃
	//
	// private RecvDataProcessUtil mRecvDataProcessUtil = new
	// RecvDataProcessUtil(this);

	private SessionManagerForSimpleChannel sessionManager;
	private MiChannelAccountManager accountManager;
	@SuppressLint("DefaultLocale")
	private ReceiveBufferSink mRecBufSink = new ReceiveBufferSink() {

		@Override
		public boolean onRecvDownStream(int sessionNO, byte[] pcBuf) {
			if (pcBuf == null) {
				return false;
			}
			PacketData recvData = null;
			try {
				recvData = StreamUtil.getDownPacket(String.format("[No:%d]", sessionNO), pcBuf,
						new StreamUtil.GetAccountAdapter() {

							@Override
							public IAccount getAccount(int seq) {
								return accountManager.getCurrentAccount();
							}
						});
			} catch (IOException e) {
				MiLinkLog.e(mLogTag, "decode downstream failed", e);
			}
			if (recvData == null) {
				return false;
			}
			long curTime = System.currentTimeMillis();
			mLastReceivedPacketTime = curTime;
			mNeedClientInfo = !recvData.hasClientInfo();

			MiLinkLog.v(mLogTag, " onRecvDownStream cmd=" + recvData.getCommand() + " mnscode=" + recvData.getMnsCode()
					+ " seq=" + recvData.getSeqNo() + " and set mNeedClientInfo=" + mNeedClientInfo);
			TrafficMonitor.getInstance().traffic(recvData.getCommand(), pcBuf.length);

			// 对于fastping周期回来的包，走正常的流程，不走push流程。
			Request temp = mPendingStatisticTimeoutedRequestMap.remove(recvData.getSeqNo());

			Request request = mRequestMap.get(recvData.getSeqNo());
			// request==null , 有可能是push或者是已经从 mRequestMap 移除的请求有回包了
			if (request != null) {
				mRequestMap.remove(recvData.getSeqNo());
				if (isFastCheckPing(request)) {
					mLastReceivedFastPingTime = System.currentTimeMillis();
					// 收到ping，则可以立马决定超时包的上报逻辑。
					Global.getMainHandler().removeCallbacks(mHandlePendingStatisticTimeoutedRunnable);
					Global.getMainHandler().postAtFrontOfQueue(mHandlePendingStatisticTimeoutedRunnable);
				}
			} else {
				if (temp != null) {
					request = temp;
				}
			}
			mRecvDataProcessUtil.selectHandleUtil(recvData, request).handle();
			if (mRequestMap.isEmpty()) {
				// 定时器优化
				sessionManager.getChannelEventBus().post(new SessionOtherEvent(
						SessionOtherEvent.EventType.RequestMapIsEmpty, SessionForSimpleChannel.this));
			}
			return true;
		}

		@Override
		public boolean onAddTimeout(int sessionNO, int seqNo) {
			return false;
		}

	};

	private Runnable mHandlePendingStatisticTimeoutedRunnable = new Runnable() {

		@Override
		public void run() {
			sessionManager.getChannelEventBus().post(new SessionOtherEvent(
					SessionOtherEvent.EventType.StatisticsTimeoutPacket, SessionForSimpleChannel.this));
		}
	};

	public SessionManagerForSimpleChannel getSessionManagerForSimpleChannel() {
		return sessionManager;
	}

	public SessionForSimpleChannel(SessionManagerForSimpleChannel sessionManager,
			MiChannelAccountManager accountManager) {
		mSessionNO = SessionConst.generateSessionNO();
		mLogTag = String.format("[No:%d]", mSessionNO) + TAG;
		mConn = null;
		mServerProfile = null;
		mRecBuffer = new ReceiveBuffer(mRecBufSink, mSessionNO, true);
		mCurState = NO_CONNECT_STATE;
		this.sessionManager = sessionManager;
		this.accountManager = accountManager;
	}

	@Override
	public int getSessionNO() {
		return mSessionNO;
	}

	@Override
	public long getOpenSessionTimecost() {
		return mOpenSessionDoneTime - mConnectStartTime;
	}

	@Override
	public long getDnsWaitTime() {
		return mDnsWaitTime;
	}

	@Override
	public boolean openSession(ServerProfile serverprofile) {
		resetContinuousRecv110Count();
		mIsHandshakeRequestFailed = false;
		mNeedClientInfo = true;
		if (serverprofile == null || serverprofile.getProtocol() == SessionConst.NONE_CONNECTION_TYPE) {
			MiLinkLog.v(mLogTag, "openSession fail, serverprofile=" + serverprofile);
			onOpenSessionBuildConnectFail(Const.InternalErrorCode.IP_ADDRESS_NULL);
			return false;
		}

		mCurState = CONNECTING_STATE;

		boolean started = false;

		mLastSendFastCheckPingTime = 0;
		// 如果有serverprofile，比较一下protocol，是否需要重建conn
		if (mServerProfile == null || mServerProfile.getProtocol() != serverprofile.getProtocol()) {
			MiLinkLog.v(mLogTag, "openSession if");
			if (mConn != null) {
				mConn.stop();
			}

			if (serverprofile.getProtocol() == SessionConst.TCP_CONNECTION_TYPE) {
				mConn = new TcpConnection(mSessionNO, this);
			}
			mServerProfile = serverprofile;
			try {
				started = mConn.start();
			} catch (Exception e) {
				MiLinkLog.e(mLogTag, "connection start failed", e);
			}

			if (!started) {
				// 这些标记状态需要修改
				onOpenSessionBuildConnectFail(Const.InternalErrorCode.MNS_LOAD_LIBS_FAILED);
				return false;
			}
		} else {
			MiLinkLog.v(mLogTag, "openSession else");
			if (mConn == null) {
				if (serverprofile.getProtocol() == SessionConst.TCP_CONNECTION_TYPE) {
					mConn = new TcpConnection(mSessionNO, this);
				}
			}
			if (!mConn.isRunning()) {
				mServerProfile = serverprofile;
				try {
					started = mConn.start();
				} catch (Exception e) {
					MiLinkLog.e(mLogTag, "connection start failed", e);
				}

				if (!started) {
					onOpenSessionBuildConnectFail(Const.InternalErrorCode.MNS_LOAD_LIBS_FAILED);
					return false;
				}
			}
		}

		mServerProfile = serverprofile;

		// 连接
		postMessage(MSG_CONNECT, null, 0);
		return true;

	}

	/**
	 * 获取本会话的服务器配置信息
	 *
	 * @return
	 */
	@Override
	public ServerProfile getServerProfile() {
		return mServerProfile;
	}

	@Override
	public ServerProfile getServerProfileForStatistic() {
		return mServerProfileForStatistic;
	}

	/**
	 * 获取本会话是否可用
	 *
	 * @return 可用返回true;否则返回false
	 */
	@Override
	public boolean isAvailable() {
		switch (mCurState) {
		case HANDSHAKE_INITED_STATE:
			return true;
		case HANDSHAKE_INITING_STATE:
		case CONNECTED_STATE:
		case CONNECTING_STATE:
		case NO_CONNECT_STATE:
		default:
			return false;
		}
	}

	/**
	 * 获取本会话是否可用
	 *
	 * @return 可用返回true;否则返回false
	 */
	@Override
	public boolean isConnected() {
		switch (mCurState) {
		case HANDSHAKE_INITED_STATE:
		case HANDSHAKE_INITING_STATE:
		case CONNECTED_STATE:
			return true;
		case CONNECTING_STATE:
		case NO_CONNECT_STATE:
		default:
			return false;
		}
	}

	/**
	 * 发送请求 -- 异步
	 *
	 * @param request
	 *            需要发送的请求
	 * @return 成功丢给网络线程处理返回true，否则返回false
	 */
	@Override
	public boolean handleRequest(Request request) {
		if (request == null) {
			MiLinkLog.e(mLogTag, "handleRequest request == null");
			return false;
		}

		MiLinkLog.v(mLogTag, "handleRequest" + " seq=" + request.getSeqNo() + " mNeedClientInfo=" + mNeedClientInfo
				+ " " + mServerProfile);
		request.setHandleSessionNO(mSessionNO);
		boolean ret = postMessage(MSG_HANDLE_REQUEST, request, 0);

		if (mConn != null) {
			mConn.wakeUp();
		}

		// 通知我有包了
		if (!sessionManager.isTimerOpen()) {
			sessionManager.getChannelEventBus()
					.post(new SessionOtherEvent(SessionOtherEvent.EventType.RequestMapIsNotEmpty, this));
		}
		return ret;
	}

	@Override
	public void fastLogin() {

		Request request = null;
		switch (accountManager.getCurrentAccountType()) {
		case MiAccountManager.ACCOUNT_TYPE_CHANNEL: {
			MnsCmdChannelReq.Builder builder = MnsCmdChannelReq.newBuilder();

			builder.setPrivacyKey(accountManager.getCurrentAccount().getPrivacyKey());
			builder.setDeviceinfo(DeviceDash.getInstance().getDeviceSimplifiedInfo());
			PacketData data = new PacketData();
			data.setSeqNo(Global.getSequence());
			data.setData(builder.build().toByteArray());
			builder.getDeviceinfo();
			MiLinkLog.d(mLogTag,
					"channel info privacyKey:" + builder.getPrivacyKey() + ",device info:" + builder.getDeviceinfo());

			data.setCommand(Const.MnsCmd.MNS_CHANNEL_FAST_LOGIN);
			request = new Request(data, mChannelFastLoginRspListener, StreamUtil.MNS_ENCODE_CHANNEL_FAST_LOGIN,
					accountManager.getCurrentAccount());
			MiLinkLog.v(mLogTag, "start channel fastlogin, seq=" + request.getSeqNo());
		}
		default:
			break;
		}
		request.setInternal(true);
		handleRequest(request);
		accountManager.setIsLogining(true);

	}

	private ResponseListener mChannelFastLoginRspListener = new ResponseListener() {

		@Override
		public void onDataSendSuccess(int errCode, PacketData data) {
			// 这里应该加个判断，如果是已经不是通道模式了，则不做任何响应
			if (!accountManager.isChannelModCurrent()) {
				MiLinkLog.i(mLogTag, "current is not channel mode " + accountManager.getCurrentAccountType());
				return;
			}
			MiLinkLog.v(mLogTag, "channel fastlogin response mns code: " + data.getMnsCode());
			if (data.getMnsCode() == Const.MiLinkCode.MI_LINK_CODE_OK) {
				MnsCmdChannelRsp response = null;
				try {
					response = MnsCmdChannelRsp.parseFrom(data.getData());
				} catch (InvalidProtocolBufferException e) {
				}
				if (response == null) {
					MiLinkLog.w(mLogTag, "chanel fastlogin response = null");
					sessionManager.getChannelEventBus()
							.post(new SessionLoginEvent(SessionLoginEvent.EventType.LoginFailed,
									SessionForSimpleChannel.this, Const.InternalErrorCode.MNS_PACKAGE_INVALID));
					return;
				}
				if (response.getB2() != null && response.getGTKEYB2() != null) {
					accountManager.getCurrentAccount().loginMiLink(response.getB2().toByteArray(),
							response.getGTKEYB2().toByteArray());
				} else {
					MiLinkLog.w(mLogTag, "channel fastlogin response.getB2() = null or response.getGTKEYB2() = null");
				}
				String userId = String.valueOf(response.getWid());
				MiLinkLog.w(TAG, "userId=" + userId + ",accountManager.getCurrentAccount():"
						+ accountManager.getCurrentAccount());
				accountManager.getCurrentAccount().setUserId(userId);

				try {
					DataAnonymousWid.Builder dataExtra = DataAnonymousWid.newBuilder();
					dataExtra.setWid(Long.valueOf(userId));
					PacketData packet = new PacketData();
					packet.setCommand(Const.DATA_CHANNEL_ANONYMOUSWID_EXTRA_CMD);
					packet.setData(dataExtra.build().toByteArray());
					MnsPacketDispatcher.getInstance().dispatchPacket(packet);
					MiLinkLog.d(TAG, " dispwid to app success wid = " + userId);
				} catch (Exception e) {
				}
				MiLinkLog.w(TAG, "wid=" + userId);

				sessionManager.getChannelEventBus().post(new SessionLoginEvent(SessionLoginEvent.EventType.LoginSuccess,
						SessionForSimpleChannel.this, 0));
			} else {
				sessionManager.getChannelEventBus().post(new SessionLoginEvent(SessionLoginEvent.EventType.LoginFailed,
						SessionForSimpleChannel.this, data.getMnsCode()));
			}
		}

		@Override
		public void onDataSendFailed(int errCode, String errMsg) {
			if (!accountManager.isChannelModCurrent()) {
				MiLinkLog.i(mLogTag, "failed current is not channel mode " + accountManager.getCurrentAccountType());
				return;
			}
			MiLinkLog.i(mLogTag, "channel fastlogin onDataSendFailed errCode= " + errCode + ", errMsg=" + errMsg);
			sessionManager.getChannelEventBus().post(new SessionLoginEvent(SessionLoginEvent.EventType.LoginFailed,
					SessionForSimpleChannel.this, errCode));
		}
	};

	/**
	 * 发送线程消息
	 *
	 * @param uMsg
	 *            消息ID
	 * @param lParam
	 *            对象类型参数
	 * @param wParam
	 *            基本类型参数
	 * @return 发送消息成功返回true;否则返回false
	 */
	private boolean postMessage(int uMsg, Object lParam, int wParam) {
		if (mConn == null) {
			MiLinkLog.e(mLogTag, "postMessage " + uMsg + " mConn == null!!!!");
			return false;
		}

		boolean ret = false;
		try {
			ret = mConn.postMessage(uMsg, lParam, wParam, this);
		} catch (NullPointerException e) {
			return ret;
		}

		if (ret == false) {
			MiLinkLog.e(mLogTag, "mMessage must be full ! uMsg = " + uMsg);
			return ret;
		}

		return ret;
	}

	/**
	 * 关闭网络会话
	 */
	@Override
	public boolean close() {
		if (!mCanClose) {
			MiLinkLog.i(mLogTag, "connecting! can not close");
			return false;
		}
		if (mConn != null) {
			MiLinkLog.i(mLogTag, "stop begin");
			mConn.stop();
			mConn = null;
			mServerProfile = null;
			mCurState = NO_CONNECT_STATE;
			MiLinkLog.i(mLogTag, "stop over");
		}
		return true;
	}

	/**
	 * 断开连接, 不会触发SessionManager.getInstance().onSessionError()
	 */
	@Override
	public void disConnect() {
		disConnect(-1);
	}

	/**
	 * 断开连接, 如果errorCallBackErrorCode大于0，
	 * 断开连接之后，会触发SessionManager.getInstance().onSessionError()
	 * 
	 * @param errorCallBackErrorCode
	 */
	@Override
	public void disConnect(int errorCallBackErrorCode) {
		MiLinkLog.i(mLogTag, "disConnect, errorCallBackErrorCode=" + errorCallBackErrorCode);
		postMessage(MSG_DISCONNECT, null, errorCallBackErrorCode);
	}

	@Override
	public void onMsgProc(int uMsg, Object lParam, int wParam) {
		MiLinkLog.v(mLogTag, "onMsgProc, uMsg=" + uMsg + ", wParam=" + wParam);
		// 消息回调的通知
		switch (uMsg) {
		case MSG_CONNECT: // 连接
		{
			if (mServerProfile == null) {
				// 参数检查
				MiLinkLog.e(mLogTag, "OnMsgProc mServerProfile == null!!!");
				onOpenSessionBuildConnectFail(SessionConst.CONN_FAILED);
				return;
			}
			mServerProfileForStatistic = mServerProfile;

			// 域名解析
			String ip = null;

			long ndsStartTime = System.currentTimeMillis();
			String defaultHost = MiLinkIpInfoManager.getInstance().getDefaultHost();
			if (mServerProfile.getServerIP().equals(defaultHost)) {
				ip = DomainManager.getInstance().getDomainIP(mServerProfile.getServerIP());
				if (ip == null) {
					onOpenSessionBuildConnectFail(SessionConst.CONN_FAILED);
					return;
				}
				mServerProfile.setServerIP(ip);
			} else {
				ip = mServerProfile.getServerIP();
			}
			MiLinkLog.i(mLogTag, "connect to " + mServerProfile);

			mConnectStartTime = System.currentTimeMillis();

			mDnsWaitTime = mConnectStartTime - ndsStartTime;

			if (mConn != null) {
				mCanClose = false;
				mConn.connect(ip, mServerProfile.getServerPort(), mServerProfile.getProxyIP(),
						mServerProfile.getPorxyPort(), ConfigManager.getInstance().getConnetionTimeout(), 0);
			}

		}
			break;
		case MSG_HANDLE_REQUEST: // 发送请求
		{
			Request request = (Request) lParam;
			if (request == null) {
				return;
			}
			if (!request.isValidNow()) {
				MiLinkLog.e(mLogTag,
						String.format("seq=%d,cmd=%s is invalid", request.getSeqNo(), request.getData().getCommand()));
				mRequestMap.remove(request.getSeqNo());
				request.onDataSendFailed(Const.InternalErrorCode.MNS_PACKAGE_INVALID,
						"package is already over the valid time");
				return;
			}
			request.setSentTime(System.currentTimeMillis());
			PacketData data = request.getData();
			String cmd = data.getCommand();
			if (Const.MnsCmd.MNS_FAST_LOGIN.equals(cmd) || Const.MnsCmd.MNS_ANONYMOUS_FAST_LOGIN.equals(cmd)) {
				mNeedClientInfo = true;
				MiLinkLog.v(mLogTag, "set mNeedClientInfo=true when send login or fastlogin");
			}
			data.setNeedClientInfo(mNeedClientInfo);

			byte[] packet = request.toBytes();
			if (data.needResponse()) {
				mRequestMap.put(request.getSeqNo(), request);
			}
			if (isFastCheckPing(request)) {
				mLastSendFastCheckPingTime = System.currentTimeMillis();
				// 既然发送了fastping，肯定要一个fastping超时周期后处理一下超时包,为了确保跑满一个超时周期，增加200ms。
				Global.getMainHandler().postDelayed(mHandlePendingStatisticTimeoutedRunnable,
						FAST_CHECK_PING_TIME_OUT + 200);
			}
			if (packet != null) {
				MiLinkLog.v(mLogTag, "connection send data, seq=" + request.getSeqNo());
				if (mConn.sendData(packet, request.getSeqNo(), request.getTimeOut())) {
					TrafficMonitor.getInstance().traffic(cmd, packet.length);
				}
			} else {
				mRequestMap.remove(request.getSeqNo());
				request.onDataSendFailed(Const.InternalErrorCode.ENCRYPT_FAILED, "data encryption failed");
				MiLinkLog.w(mLogTag, "connection send data, but data = null");
			}
		}
			break;
		case MSG_CHECK_TIMEOUT: // 检查超时
		{
			checkIsReadTimeOut();
		}
			break;
		case MSG_DISCONNECT: // 断开连接
		{
			if (mConn != null) {
				mConn.disconnect();
			}
			mCurState = NO_CONNECT_STATE;
			if (wParam > 0) {
				onSessionError(wParam);
			}
		}
			break;
		case MSG_POST_STATISTICS_TIMEOUT_PACKET: {
			handlePendingStatisticTimeoutedRequestMap();
		}
			break;
		default: // 无法识别的消息
			MiLinkLog.e(mLogTag, "OnMsgProc unknow uMsgID = " + uMsg);
			break;
		}

	}

	private ResponseListener mLogoffRspListener = new ResponseListener() {

		@Override
		public void onDataSendSuccess(int errCode, PacketData data) {
			sessionManager.getChannelEventBus().post(new SessionLoginEvent(SessionLoginEvent.EventType.LogoffCmdReturn,
					SessionForSimpleChannel.this, 0));
		}

		@Override
		public void onDataSendFailed(int errCode, String errMsg) {
			sessionManager.getChannelEventBus().post(new SessionLoginEvent(SessionLoginEvent.EventType.LogoffCmdReturn,
					SessionForSimpleChannel.this, 0));
		}

	};

	@Override
	public void logoff() {
		MnsCmdLoginOff.Builder builder = MnsCmdLoginOff.newBuilder();
		if (!TextUtils.isEmpty(ConfigManager.getInstance().getSuid())) {
			builder.setSUID(ConfigManager.getInstance().getSuid());
		}
		MnsCmdLoginOff logoffPacket = builder.build();
		PacketData data = new PacketData();
		data.setNeedResponse(false);
		data.setCommand(Const.MnsCmd.MNS_LOGOFF);
		data.setSeqNo(Global.getSequence());
		data.setData(logoffPacket.toByteArray());
		Request request = new Request(data, mLogoffRspListener, StreamUtil.MNS_ENCODE_ANONYMOUS_B2_TOKEN,
				accountManager.getCurrentAccount());
		request.setInternal(true);
		MiLinkLog.v(mLogTag, "start logoff, seq=" + request.getSeqNo());
		handleRequest(request);
	}

	// ///////////////IConnectionCallback回调函数///////////////////
	@Override
	public boolean onStart() {
		return false;
	}

	@Override
	public boolean onConnect(boolean isSuccess, int errorCode) {
		mCanClose = true;
		MiLinkLog.d(mLogTag, "isSuccess=" + isSuccess);
		if (isSuccess) {
			onOpenSessionBuildConnectSuccess();
		} else {
			onOpenSessionBuildConnectFail(errorCode);
		}
		return true;
	}

	private boolean mIsHandshakeRequestFailed = false;

	// 肯定会调用一次
	@Override
	public boolean onDisconnect() {
		// 断开连接的通知
		MiLinkLog.i(mLogTag, "OnDisconnect");
		mRecBuffer.reset();
		// 检查requestMap
		Set<Integer> keySet = mRequestMap.keySet();

		// 是外部的包
		for (Iterator<Integer> it = keySet.iterator(); it.hasNext();) {
			Integer seqNo = it.next();
			Request request = mRequestMap.get(seqNo);
			if (request != null) {
				MiLinkLog.e(mLogTag, "Const.InternalErrorCode.CONNECT_FAIL, seq=" + request.getSeqNo() + ",cmd="
						+ request.getData().getCommand());
				// 这里面的包都是要走失败的。
				if (request.canRetry()) {
					// 走一次重试
					request.setHasRetry();
					// 丢给SM重试
					MiLinkLog.e(mLogTag, "seq=" + request.getSeqNo() + ",cmd=" + request.getData().getCommand()
							+ " will be retry send from onDisconnect.");
					MiLinkEventForSimpleChannel.SessionOtherEvent event = new SessionOtherEvent(
							MiLinkEventForSimpleChannel.SessionOtherEvent.EventType.PackageNeedRetry, this);
					event.obj = request;
					sessionManager.getChannelEventBus().post(event);
				} else {
					request.onDataSendFailed(Const.InternalErrorCode.CONNECT_FAIL, "native network broken");
				}
			}
		}
		mRequestMap.clear();
		handlePendingStatisticTimeoutedRequestMap();
		mPendingStatisticTimeoutedRequestMap.clear();
		return true;
	}

	// onError肯定会调用disconnect，而且是先disconnect再调用Error
	@Override
	public boolean onError(int socketStatus) {
		if (mIsHandshakeRequestFailed) {
			// handshake的失败回调已经通知了SM了
			MiLinkLog.e(mLogTag, "onError but handshake failed has already notice SM, socketStatus:" + socketStatus
					+ ", mCurState=" + mCurState);
			mIsHandshakeRequestFailed = false;
			return true;
		}
		// 连接出错的通知
		MiLinkLog.e(mLogTag, "onError socketStatus " + socketStatus + ", mCurState=" + mCurState);
		switch (mCurState) {
		case NO_CONNECT_STATE:
		case CONNECTING_STATE:
		case CONNECTED_STATE:
			onOpenSessionBuildConnectFail(SessionConst.CONN_FAILED);
			break;
		case HANDSHAKE_INITING_STATE: {
			if (socketStatus == Const.InternalErrorCode.MNS_PACKAGE_ERROR) {
				onOpenSessionHandshakeFail(SessionConst.HANDSHAKE_PACKERROR_FAILED);
			} else {
				onOpenSessionHandshakeFail(SessionConst.HANDSHAKE_OTHERERROR_FAILED);
			}
		}
			break;
		case HANDSHAKE_INITED_STATE: {
			onSessionError(socketStatus);
		}
			break;
		default:
			MiLinkLog.e(mLogTag, "onError wrong state = " + mCurState);
			break;
		}
		return true;
	}

	@Override
	public boolean onTimeOut(int dwSeqNo, int nReason) {
		MiLinkLog.v(mLogTag, "send time out: seq=" + dwSeqNo);
		return false;
	}

	@Override
	public boolean onRecv(byte[] pcBuf) {
		MiLinkLog.v(mLogTag, "recv data:" + pcBuf.length);
		if (mRecBuffer != null) {
			try {
				mRecBuffer.append(pcBuf);
			} catch (InvalidPacketExecption e) {
				disConnect(Const.InternalErrorCode.READ_FAIL);
				if (e.errCode == InvalidPacketExecption.ERROR_CODE_NO_MNS_HEAD) { // 数据被劫持了
					sessionManager.getChannelEventBus()
							.post(new SessionOtherEvent(SessionOtherEvent.EventType.RecvInvalidPacket, this));
				}
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean onSendBegin(int dwSeqNo) {
		MiLinkLog.v(mLogTag, "send begin: seq=" + dwSeqNo);
		return false;
	}

	@Override
	public boolean onSendEnd(int dwSeqNo) {
		MiLinkLog.v(mLogTag, "send end: seq=" + dwSeqNo);
		return false;
	}

	/**
	 * 建立会话失败，通知SessionManager
	 *
	 * @param failReason
	 *            失败原因
	 *            <ul>
	 *            <li>{@link #CONN_FAILED}
	 *            <li>{@link #HANDSHAKE_OTHERERROR_FAILED}
	 *            <li>{@link #HANDSHAKE_PACKERROR_FAILED}
	 *            </ul>
	 */
	private void onOpenSessionBuildConnectFail(int failReason) {
		mOpenSessionDoneTime = System.currentTimeMillis();
		mCurState = NO_CONNECT_STATE;
		sessionManager.getChannelEventBus()
				.post(new SessionConnectEvent(SessionConnectEvent.EventType.SessionBuildFailed, this, failReason));
	}

	private void onOpenSessionBuildConnectSuccess() {
		mCurState = CONNECTED_STATE;
		handShake(accountManager.getCurrentAccount());
	}

	private void onOpenSessionHandshakeFail(int failReason) {
		mOpenSessionDoneTime = System.currentTimeMillis();
		mCurState = CONNECTED_STATE;
		sessionManager.getChannelEventBus()
				.post(new SessionConnectEvent(SessionConnectEvent.EventType.SessionBuildFailed, this, failReason));
	}

	private OpenSessionSucessReturnInfo mOpenSessionSucessReturnInfo;

	@Override
	public OpenSessionSucessReturnInfo getOpenSessionSucessReturnInfo() {
		return mOpenSessionSucessReturnInfo;
	}

	@Override
	protected void onOpenSessionHandshakeSuccess(OpenSessionSucessReturnInfo info) {
		mOpenSessionDoneTime = System.currentTimeMillis();
		mCurState = HANDSHAKE_INITED_STATE;
		mOpenSessionSucessReturnInfo = info;
		sessionManager.getChannelEventBus()
				.post(new SessionConnectEvent(SessionConnectEvent.EventType.SessionBuildSuccess, this, 0));
	}

	private void onSessionError(int errCode) {
		mCurState = NO_CONNECT_STATE;
		sessionManager.getChannelEventBus()
				.post(new SessionConnectEvent(SessionConnectEvent.EventType.SessionRunError, this, errCode));
	}

	/**
	 * 轮询检查请求队列里是否超时 -- 异步
	 *
	 * @return 成功丢给网络线程处理返回true，否则返回false
	 */
	@Override
	public boolean checkRequestsTimeout() {
		if (shouldCheckRequestsTimeout()) {
			return postMessage(MSG_CHECK_TIMEOUT, null, 0);
		}
		sessionManager.getChannelEventBus()
				.post(new SessionOtherEvent(SessionOtherEvent.EventType.RequestMapIsEmpty, this));
		return false;
	}

	/**
	 * 是否需要检查超时
	 * 
	 * @return
	 */
	@Override
	public boolean shouldCheckRequestsTimeout() {
		if (!isConnected() || mRequestMap.isEmpty()) {
			return false;
		}
		return true;
	}

	private void fastCheckPing() {
		PacketData data = new PacketData();
		data.setCommand(Const.MnsCmd.MNS_PING_CMD);
		data.setSeqNo(Global.getSequence());
		Request request = new Request(data, null, StreamUtil.MNS_ENCODE_NONE, accountManager.getCurrentAccount());
		request.setInternal(true);
		request.setPing(true);
		request.setTimeOut(FAST_CHECK_PING_TIME_OUT);
		MiLinkLog.v(mLogTag, "start fast ping, seq=" + request.getSeqNo());
		handleRequest(request);
	}

	private boolean isFastCheckPing(Request request) {
		return request.isPingRequest() && request.getTimeOut() == FAST_CHECK_PING_TIME_OUT;
	}

	// 检查超时
	private void checkIsReadTimeOut() {
		boolean hasFastCheckPing = false;
		boolean isFastCheckPingTimeout = false;
		boolean hasLongTimeoutRequest = false;
		for (int seqNo : mRequestMap.keySet()) {
			Request request = mRequestMap.get(seqNo);
			if (request != null) {
				if (isFastCheckPing(request)) {
					hasFastCheckPing = true;
					if (request.isTimeout()) {
						// 已经发送了fastping 且已经超时
						isFastCheckPingTimeout = true;
					}
				}
				if (request.isTimeout()) {
					MiLinkLog.e(mLogTag, "Const.InternalErrorCode.CONNECT_FAIL, seq=" + request.getSeqNo() + ",cmd="
							+ request.getData().getCommand());
					if (request.getTimeOut() >= 10 * 1000) {// 有长超时包超时直接重连
						hasLongTimeoutRequest = true;
					}
					// 这里面的包都是要走失败的。
					if (request.canRetry()) {
						// 走一次重试
						request.setHasRetry();
						// 丢给SM重试
						MiLinkLog.e(mLogTag, "seq=" + request.getSeqNo() + ",cmd=" + request.getData().getCommand()
								+ " will be retry send from timeout check.");
						MiLinkEventForSimpleChannel.SessionOtherEvent event = new SessionOtherEvent(
								MiLinkEventForSimpleChannel.SessionOtherEvent.EventType.PackageNeedRetry, this);
						event.obj = request;
						sessionManager.getChannelEventBus().post(event);
					} else {
						mRequestMap.remove(seqNo);
						// 读超时
						MiLinkLog.e(mLogTag, "Request read time out, seq=" + request.getSeqNo() + ",cmd="
								+ request.getData().getCommand());
						// 通知上层读失败
						request.onDataSendFailed(Const.InternalErrorCode.READ_TIME_OUT, "request time out");
						mPendingStatisticTimeoutedRequestMap.put(request.getSeqNo(), request);
						request.onDataSendFailed(Const.InternalErrorCode.CONNECT_FAIL, "native network broken");
					}
				}
			}
		}

		if (hasLongTimeoutRequest) {
			MiLinkLog.e(mLogTag, Device.Network.getCurrentNetworkDetailInfo().toString());
		}
		// 重连唯一条件，发送了fastping且fastping也超时了，才会触发断线重连逻辑。
		if (isFastCheckPingTimeout || hasLongTimeoutRequest) {
			MiLinkLog.e(mLogTag, "checkIsReadTimeOut, fast ping timeout, reconnect");
			disConnect(Const.InternalErrorCode.READ_TIME_OUT);
		} else {
			if (!hasFastCheckPing && hasLongTimeoutRequest) { // 还没有发过 check
																// ping
				fastCheckPing();
			}
		}
	}

	// 判断是不是刚刚发送fast ping(判断算法上次发送时间和当前时间相差不超过2个fast ping timeout)
	private boolean isJustSentFastCheckPing() {
		return (System.currentTimeMillis() - mLastSendFastCheckPingTime <= 2 * FAST_CHECK_PING_TIME_OUT);
	}

	@Override
	public void postStatisticsTimeoutPacketAction() {
		postMessage(MSG_POST_STATISTICS_TIMEOUT_PACKET, null, 0);
	}

	// 错误上报逻辑
	private void handlePendingStatisticTimeoutedRequestMap() {
		final long now = System.currentTimeMillis();

		Iterator<Map.Entry<Integer, Request>> iterator = mPendingStatisticTimeoutedRequestMap.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Integer, Request> mapEntry = iterator.next();
			Request request = mapEntry.getValue();
			MiLinkLog.v(mLogTag, "handleTimeoutedRequest seq=" + request.getSeqNo());
			if (isJustSentFastCheckPing()) { // 刚刚发送了 check ping
				// 就算是刚刚发送了 fastping 也要等待一个fastping超时周期后 才能下定结论。
				if (mLastSendFastCheckPingTime > mLastReceivedFastPingTime /* ping没回来 */) {
					if (request.getSentTime() < mLastSendFastCheckPingTime) {
						MiLinkLog.e(mLogTag, "seq=" + request.getSeqNo() + " timeouted, ping also timeout,Reported 3");
						String cmd = request.getData() == null ? Const.MnsCmd.MNS_PING_CMD
								: request.getData().getCommand();
						InternalDataMonitor.getInstance().trace(
								(mServerProfileForStatistic != null ? mServerProfileForStatistic.getServerIP() : ""),
								(mServerProfileForStatistic != null ? mServerProfileForStatistic.getServerPort() : 0),
								cmd, BaseDataMonitor.RET_CODE_PING_TIME_OUT, request.getSentTime(), now,
								request.getSize(), 0, request.getSeqNo());
						iterator.remove();
					}
				} else {// fast ping 没有超时
					MiLinkLog.e(mLogTag, "seq=" + request.getSeqNo() + " timeouted, ping not timeout,Reported 1");
					String cmd = request.getData() == null ? Const.MnsCmd.MNS_PING_CMD : request.getData().getCommand();
					InternalDataMonitor.getInstance().trace(
							(mServerProfileForStatistic != null ? mServerProfileForStatistic.getServerIP() : ""),
							(mServerProfileForStatistic != null ? mServerProfileForStatistic.getServerPort() : 0), cmd,
							BaseDataMonitor.RET_CODE_TIME_OUT, request.getSentTime(), now, request.getSize(), 0,
							request.getSeqNo());
					iterator.remove();
				}
			} else {
				break;
			}
		}

	}

	/**
	 * 带上clientinfo重新发送此包
	 * 
	 * @param request
	 */
	@Override
	public void onAccNeedRetryWithClientInfo(Request request) {
		MiLinkLog.w(TAG, "onAccNeedRetryWithClientInfo");
		mNeedClientInfo = true;
		// 这个重试次数是与mnscode 109 共享的
		if (request.getRetryCount() < SessionConst.ACC_NEED_CLIENT_RETRY_TIMES) {
			request.addRetryCount();
			handleRequest(request);
		} else {
			MiLinkLog.w(TAG, "try 118 too many times");
		}
	}

	@Override
	public void addContinuousRecv110Count() {
		mContinuousRecv110Count++;
	}

	@Override
	public void resetContinuousRecv110Count() {
		mContinuousRecv110Count = 0;
	}

	@Override
	public boolean checkExceedMaxContinuousRecv110Count() {
		MiLinkLog.v(mLogTag, "mContinuousRecv110Count = " + mContinuousRecv110Count);
		if (mContinuousRecv110Count >= 3) {
			sessionManager.getChannelEventBus()
					.post(new ServerNotificationEvent(ServerNotificationEvent.EventType.ServerLineBroken));
			return false;
		}
		return true;
	}

	private String mClientIp = "";

	private String mClientIsp = "";

	@Override
	public String getClientIp() {
		return mClientIp;
	}

	@Override
	public String getClientIsp() {
		return mClientIsp;
	}

	public boolean isDeadConnection(long limit1, long limit2) {
		int i = 0;
		long now = System.currentTimeMillis();
		// for (int key : mRequestMap.keySet()) {
		// // 只抽3个包检查一下
		// if (i++ > 3) {
		// break;
		// }
		// Request req = mRequestMap.get(key);
		// if (req == null) {
		// break;
		// }
		// if (now - req.getSentTime() > limit1) {
		// return true;
		// }
		// }
		if (now - mLastReceivedPacketTime > limit2) {
			return true;
		}
		return false;
	}
}

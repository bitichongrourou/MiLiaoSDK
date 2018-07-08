
package com.mi.milink.sdk.session.persistent;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.json.JSONObject;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.mi.milink.sdk.account.AnonymousAccount;
import com.mi.milink.sdk.account.IAccount;
import com.mi.milink.sdk.account.MiAccount;
import com.mi.milink.sdk.account.manager.MiAccountManager;
import com.mi.milink.sdk.aidl.PacketData;
import com.mi.milink.sdk.base.Global;
import com.mi.milink.sdk.base.data.Convert;
import com.mi.milink.sdk.base.os.Device;
import com.mi.milink.sdk.base.os.info.DeviceDash;
import com.mi.milink.sdk.config.ConfigManager;
import com.mi.milink.sdk.config.HeartBeatManager;
import com.mi.milink.sdk.config.MiLinkIpInfoManager;
import com.mi.milink.sdk.connection.DomainManager;
import com.mi.milink.sdk.connection.IConnection;
import com.mi.milink.sdk.connection.IConnectionCallback;
import com.mi.milink.sdk.connection.TcpConnection;
import com.mi.milink.sdk.data.ClientAppInfo;
import com.mi.milink.sdk.data.Const;
import com.mi.milink.sdk.debug.BaseDataMonitor;
import com.mi.milink.sdk.debug.InternalDataMonitor;
import com.mi.milink.sdk.debug.MiLinkLog;
import com.mi.milink.sdk.debug.TrafficMonitor;
import com.mi.milink.sdk.event.MiLinkEvent.ServerNotificationEvent;
import com.mi.milink.sdk.event.MiLinkEvent.SessionConnectEvent;
import com.mi.milink.sdk.event.MiLinkEvent.SessionLoginEvent;
import com.mi.milink.sdk.event.MiLinkEvent.SessionOtherEvent;
import com.mi.milink.sdk.proto.DataExtraProto.DataAnonymousWid;
import com.mi.milink.sdk.proto.DataExtraProto.DataClientIp;
import com.mi.milink.sdk.proto.DataExtraProto.DataExtra;
import com.mi.milink.sdk.proto.SystemPacketProto.MnsCmdAnonymousReq;
import com.mi.milink.sdk.proto.SystemPacketProto.MnsCmdAnonymousRsp;
import com.mi.milink.sdk.proto.SystemPacketProto.MnsCmdChannelRsp;
import com.mi.milink.sdk.proto.SystemPacketProto.MnsCmdFastloginReq;
import com.mi.milink.sdk.proto.SystemPacketProto.MnsCmdFastloginRsp;
import com.mi.milink.sdk.proto.SystemPacketProto.MnsCmdHandShakeReq;
import com.mi.milink.sdk.proto.SystemPacketProto.MnsCmdHandShakeRsp;
import com.mi.milink.sdk.proto.SystemPacketProto.MnsCmdHeartBeat;
import com.mi.milink.sdk.proto.SystemPacketProto.MnsCmdHeartBeatRsp;
import com.mi.milink.sdk.proto.SystemPacketProto.MnsCmdLoginOff;
import com.mi.milink.sdk.proto.SystemPacketProto.MnsIpInfo;
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

import android.text.TextUtils;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

/**
 * session封装了连接，收发消息等
 */
public class Session implements IConnectionCallback, MsgProcessor {
	private static final String TAG = "Session";

	// Connection thread消息类型
	protected static final int MSG_CONNECT = 1;

	protected static final int MSG_HANDLE_REQUEST = 2;

	protected static final int MSG_CHECK_TIMEOUT = 3;

	protected static final int MSG_DISCONNECT = 4;

	protected static final int MSG_POST_STATISTICS_TIMEOUT_PACKET = 5;

	// Session的状态
	protected int mCurState = NO_CONNECT_STATE;

	protected static final int NO_CONNECT_STATE = 0;

	protected static final int CONNECTING_STATE = 1;

	protected static final int CONNECTED_STATE = 2;

	protected static final int HANDSHAKE_INITING_STATE = 3;

	protected static final int HANDSHAKE_INITED_STATE = 4;

	protected static final int FAST_CHECK_PING_TIME_OUT = 10 * 1000;

	public static final int SESSION_TYPE_DEFAULT = 0;

	public static final int SESSION_TYPE_ASSIST = 1;

	protected int mSessionType = SESSION_TYPE_ASSIST;

	protected boolean mNeedClientInfo = true;

	protected IConnection mConn;

	protected ServerProfile mServerProfile;

	protected ServerProfile mServerProfileForStatistic;

	protected ReceiveBuffer mRecBuffer;

	protected int mSessionNO = 0;

	protected long mConnectStartTime = 0;

	protected long mOpenSessionDoneTime = 0;

	protected long mDnsWaitTime = 0;

	protected ConcurrentHashMap<Integer, Request> mRequestMap = new ConcurrentHashMap<Integer, Request>();

	protected ConcurrentHashMap<Integer, Request> mPendingStatisticTimeoutedRequestMap = new ConcurrentHashMap<Integer, Request>();

	protected long mLastSendFastCheckPingTime = 0; // 记录上次发送check ping的时间

	protected long mLastReceivedFastPingTime = 0; // 记录上次收到check ping的时间

	protected long mLastReceivedPacketTime = 0; // 记录上次收到包的时间

	protected String mLogTag = TAG;

	protected RecvDataProcessUtil mRecvDataProcessUtil = new RecvDataProcessUtil(this);

	protected int mContinuousRecv110Count = 0;

	public int mFlagForSessionManager;

	protected boolean mCanClose = true; // 如果正在连接时，调用mConn.close()，native会出现崩溃

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
								Request request = mRequestMap.get(seq);
								if (request == null) {
									return MiAccountManager.getInstance().getCurrentAccount();
								} else {
									return request.getOwnerAccount();
								}
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

			Log.w("wangchuntao","onRecvDownStream  cmd:"+recvData.getCommand() +"  mnscode:"+recvData.getMnsCode());
			MiLinkLog.w(mLogTag, " onRecvDownStream cmd=" + recvData.getCommand() + " mnscode=" + recvData.getMnsCode()
					+ " seq=" + recvData.getSeqNo() + " and set mNeedClientInfo=" + mNeedClientInfo);
			TrafficMonitor.getInstance().traffic(recvData.getCommand(), pcBuf.length);

			// 对于fastping周期回来的包，走正常的流程，不走push流程。
			Request temp = mPendingStatisticTimeoutedRequestMap.remove(recvData.getSeqNo());

			Request request = mRequestMap.get(recvData.getSeqNo());
			if (request != null) {
				mRequestMap.remove(recvData.getSeqNo());
				if (isFastCheckPing(request)) {
					mLastReceivedFastPingTime = curTime;
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
				EventBus.getDefault()
						.post(new SessionOtherEvent(SessionOtherEvent.EventType.RequestMapIsEmpty, Session.this));
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
			EventBus.getDefault()
					.post(new SessionOtherEvent(SessionOtherEvent.EventType.StatisticsTimeoutPacket, Session.this));
		}
	};

	public Session() {
		this(SESSION_TYPE_DEFAULT);
	}

	public Session(int type) {
		this.mSessionType = type;
		mSessionNO = SessionConst.generateSessionNO();
		if (mSessionType == SESSION_TYPE_ASSIST) {
			mLogTag = String.format("[as_No:%d]", mSessionNO) + TAG; // assist
																		// session
		} else {
			mLogTag = String.format("[No:%d]", mSessionNO) + TAG;
		}
		mConn = null;
		mServerProfile = null;
		mRecBuffer = new ReceiveBuffer(mRecBufSink, mSessionNO, mSessionType == SESSION_TYPE_ASSIST);
		mCurState = NO_CONNECT_STATE;
	}

	public int getSessionNO() {
		return mSessionNO;
	}

	public long getOpenSessionTimecost() {
		return mOpenSessionDoneTime - mConnectStartTime;
	}

	public long getDnsWaitTime() {
		return mDnsWaitTime;
	}

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
	public ServerProfile getServerProfile() {
		return mServerProfile;
	}

	public ServerProfile getServerProfileForStatistic() {
		return mServerProfileForStatistic;
	}

	/**
	 * 获取本会话是否可用
	 *
	 * @return 可用返回true;否则返回false
	 */
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
	protected boolean handleRequest(Request request) {
		if (request == null) {
			MiLinkLog.e(mLogTag, "handleRequest request == null");
			return false;
		}

		HeartBeatManager.getInstance().setLastPacketSendTime(request.getData().getSeqNo(),
				request.getData().getCommand());

		StringBuilder sb = new StringBuilder();
		sb.append("handleRequest seq=").append(request.getSeqNo()).append(" cmd=")
				.append(request.getData().getCommand()).append(" mServerProfile=").append(mServerProfile.toString())
				.append(" mNeedClientInfo=").append(mNeedClientInfo);
		MiLinkLog.w(mLogTag, sb.toString());
		request.setHandleSessionNO(mSessionNO);
		boolean ret = postMessage(MSG_HANDLE_REQUEST, request, 0);

		if (mConn != null) {
			mConn.wakeUp();
		}

		// 通知我有包了
		if (!SessionManager.getInstance().isTimerOpen()) {
			EventBus.getDefault().post(new SessionOtherEvent(SessionOtherEvent.EventType.RequestMapIsNotEmpty, this));
		}
		return ret;
	}

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
	public boolean close() {
		if (!mCanClose) {
			MiLinkLog.i(mLogTag, "connecting! can not close");
			return false;
		}
		if (mConn != null) {
			MiLinkLog.w(mLogTag, "stop begin");
			mConn.stop();
			mConn = null;
			mServerProfile = null;
			mCurState = NO_CONNECT_STATE;
			MiLinkLog.w(mLogTag, "stop over");
		}
		return true;
	}

	/**
	 * 断开连接, 不会触发SessionManager.getInstance().onSessionError()
	 */
	public void disConnect() {
		disConnect(-1);
	}

	/**
	 * 断开连接, 如果errorCallBackErrorCode大于0，
	 * 断开连接之后，会触发SessionManager.getInstance().onSessionError()
	 * 
	 * @param errorCallBackErrorCode
	 */
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
			MiLinkLog.w(mLogTag, "connect to " + mServerProfile);

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
				if (mConn.sendData(packet, request.getSeqNo(), request.getTimeOut())) {
					TrafficMonitor.getInstance().traffic(cmd, packet.length);
				}
			} else {
				// 2016.12.23 修复一个上报为1的bug
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

	protected ResponseListener mHandShakeRspListener = new ResponseListener() {

		@Override
		public void onDataSendSuccess(int errCode, PacketData data) {
			MiLinkLog.v(mLogTag, "hand shake success session " + mServerProfile + ", seq=" + data.getSeqNo());
			MnsCmdHandShakeRsp response = null;
			try {
				response = MnsCmdHandShakeRsp.parseFrom(data.getData());
			} catch (InvalidProtocolBufferException e) {
			}
			if (response == null) {
				MiLinkLog.e(mLogTag, "handshake response is null");
				onOpenSessionHandshakeSuccess(null);
				return;
			}

			String clientInfo = response.getClientinfo();
			MiLinkLog.v(mLogTag, "clientInfo:" + clientInfo);
			mClientIp = "";
			mClientIsp = "";
			if (!TextUtils.isEmpty(clientInfo)) {
				String[] ary = clientInfo.split("#");
				if (ary != null) {
					mClientIp = ary[0];
					if (ary.length > 1) {
						mClientIsp = ary[1];
					}
				}
			}

			if (!TextUtils.isEmpty(mClientIp)) {
				DataClientIp.Builder dataExtra = DataClientIp.newBuilder();
				dataExtra.setClientIp(mClientIp);
				dataExtra.setClientIpIsp(mClientIsp);

				PacketData packet = new PacketData();
				packet.setCommand(Const.DATA_CLIENTIP_EXTRA_CMD);
				packet.setData(dataExtra.build().toByteArray());
				MiLinkLog.d(TAG, " disp clientIP");
				MnsPacketDispatcher.getInstance().dispatchPacket(packet);
			}

			ArrayList<ServerProfile> optmumServerList = null;
			ArrayList<ServerProfile> backupServerList = null;
			if (response.getRedirectList() != null) {
				optmumServerList = new ArrayList<ServerProfile>();
				backupServerList = new ArrayList<ServerProfile>();
				for (int i = 0; i < response.getRedirectCount(); i++) {
					MnsIpInfo info = response.getRedirect(i);
					if (info.getIp() != 0) {
						optmumServerList.add(new ServerProfile(Convert.intToIPv4(info.getIp()), 0,
								SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP));
					}
					if (i == 0) { // 第一个元素的remark字段表示保底IP
						String remark = info.getRemark();
						if (!TextUtils.isEmpty(remark)) {
							String[] ips = remark.split("#");
							if (ips != null) {
								for (String ip : ips) {
									if (!TextUtils.isEmpty(ip)) {
										backupServerList.add(new ServerProfile(ip, 0, SessionConst.TCP_CONNECTION_TYPE,
												SessionConst.BACKUP_IP));
									}
								}
							}
						}
					}
					MiLinkLog.w(mLogTag, "milink server ip:" + Convert.intToIPv4(info.getIp()) + " port:"
							+ info.getPort() + " remark:" + info.getRemark());
				}

			}
			onOpenSessionHandshakeSuccess(
					new OpenSessionSucessReturnInfo(mClientIp, mClientIsp, optmumServerList, backupServerList));

			// handshake时，自己上报自己的
		}

		@Override
		public void onDataSendFailed(int errCode, String errMsg) {
			MiLinkLog.i(mLogTag, "hand shake error session, errCode=" + errCode + ", errMsg=" + errMsg);
			if (errCode == Const.InternalErrorCode.CONNECT_FAIL || errCode == Const.InternalErrorCode.READ_TIME_OUT
					|| errCode == Const.InternalErrorCode.IP_ADDRESS_NULL) {
				mIsHandshakeRequestFailed = true;
				onOpenSessionHandshakeFail(errCode);
			} else {
				// 只要handshake从服务器端回来就算成功
				onOpenSessionHandshakeSuccess(null);
			}
		}
	};

	protected void handShake(IAccount account) {
		mCurState = HANDSHAKE_INITING_STATE;
		MnsCmdHandShakeReq.Builder builder = MnsCmdHandShakeReq.newBuilder();
		builder.setType(1);
		MnsCmdHandShakeReq hsPacket = builder.build();
		PacketData data = new PacketData();
		data.setCommand(Const.MnsCmd.MNS_HAND_SHAKE);
		data.setSeqNo(Global.getSequence());
		data.setData(hsPacket.toByteArray());
		Request request = new Request(data, mHandShakeRspListener, StreamUtil.MNS_ENCODE_B2_TOKEN_FOR_HS, account);
		request.setInternal(true);
		MiLinkLog.v(mLogTag, "start hand shake, seq=" + request.getSeqNo());
		handleRequest(request);
	}

	// 如果是ping only不回调，仅用于测试链路
	public void ping() {
		PacketData data = new PacketData();
		data.setCommand(Const.MnsCmd.MNS_PING_CMD);
		data.setSeqNo(Global.getSequence());
		Request request = new Request(data, null, StreamUtil.MNS_ENCODE_NONE,
				MiAccountManager.getInstance().getCurrentAccount());
		request.setInternal(true);
		request.setPing(true);
		MiLinkLog.v(mLogTag, "start ping, seq=" + request.getSeqNo());
		handleRequest(request);
	}

	private void fastCheckPing() {
		PacketData data = new PacketData();
		data.setCommand(Const.MnsCmd.MNS_PING_CMD);
		data.setSeqNo(Global.getSequence());
		Request request = new Request(data, null, StreamUtil.MNS_ENCODE_NONE,
				MiAccountManager.getInstance().getCurrentAccount());
		request.setInternal(true);
		request.setPing(true);
		request.setTimeOut(FAST_CHECK_PING_TIME_OUT);
		MiLinkLog.v(mLogTag, "start fast ping, seq=" + request.getSeqNo());
		handleRequest(request);
	}

	private ResponseListener mAnonymousFastLoginRspListener = new ResponseListener() {

		@Override
		public void onDataSendSuccess(int errCode, PacketData data) {
			// 这里应该加个判断，如果是已经不是匿名模式了，则不做任何响应
			if (!MiAccountManager.getInstance().isAnonymousModeCurrent()) {
				MiLinkLog.i(mLogTag, "current is not anonymous mode");
				return;
			}
			MiLinkLog.v(mLogTag, "anonymous fastlogin response mns code: " + data.getMnsCode());
			if (data.getMnsCode() == Const.MiLinkCode.MI_LINK_CODE_OK) {
				MnsCmdAnonymousRsp response = null;
				try {
					response = MnsCmdAnonymousRsp.parseFrom(data.getData());
				} catch (InvalidProtocolBufferException e) {
				}
				if (response == null) {
					MiLinkLog.w(mLogTag, "anonymous fastlogin response = null");
					EventBus.getDefault().post(new SessionLoginEvent(SessionLoginEvent.EventType.LoginFailed,
							Session.this, Const.InternalErrorCode.MNS_PACKAGE_INVALID));
					return;
				}
				if (response.getB2() != null && response.getGTKEYB2() != null) {
					AnonymousAccount.getInstance().loginMiLink(response.getB2().toByteArray(),
							response.getGTKEYB2().toByteArray());
				} else {
					MiLinkLog.w(mLogTag, "anonymous fastlogin response.getB2() = null or response.getGTKEYB2() = null");
				}
				MiAccountManager.getInstance().setPassportInit(false);
				ConfigManager.getInstance().updateSuidAnonymous(response.getSUID());
				String userId = String.valueOf(response.getWid());
				MiLinkLog.w(TAG, "userId=" + userId);
				AnonymousAccount.getInstance().setUserId(userId);

				try {
					DataAnonymousWid.Builder dataExtra = DataAnonymousWid.newBuilder();
					dataExtra.setWid(Long.valueOf(userId));
					PacketData packet = new PacketData();
					packet.setCommand(Const.DATA_ANONYMOUSWID_EXTRA_CMD);
					packet.setData(dataExtra.build().toByteArray());
					MnsPacketDispatcher.getInstance().dispatchPacket(packet);
					MiLinkLog.d(TAG, " dispwid to app success wid = " + userId);
				} catch (Exception e) {
				}

				EventBus.getDefault()
						.post(new SessionLoginEvent(SessionLoginEvent.EventType.LoginSuccess, Session.this, 0));
				heartBeat(true);

			} else {
				EventBus.getDefault().post(new SessionLoginEvent(SessionLoginEvent.EventType.LoginFailed, Session.this,
						data.getMnsCode()));
			}
		}

		@Override
		public void onDataSendFailed(int errCode, String errMsg) {
			if (!MiAccountManager.getInstance().isAnonymousModeCurrent()) {
				MiLinkLog.i(mLogTag, "current is not anonymous mode");
				return;
			}
			MiLinkLog.i(mLogTag, "fastlogin onDataSendFailed errCode= " + errCode + ", errMsg=" + errMsg);
			EventBus.getDefault()
					.post(new SessionLoginEvent(SessionLoginEvent.EventType.LoginFailed, Session.this, errCode));
		}
	};

	private ResponseListener mFastLoginRspListener = new ResponseListener() {

		@Override
		public void onDataSendSuccess(int errCode, PacketData data) {
			if (MiAccountManager.getInstance().isAnonymousModeCurrent()) {
				MiLinkLog.i(mLogTag, "current is not stardard mode");
				return;
			}
			MiLinkLog.v(mLogTag, "fastlogin response mns code: " + data.getMnsCode());
			if (data.getMnsCode() == Const.MiLinkCode.MI_LINK_CODE_OK) {
				MnsCmdFastloginRsp response = null;
				try {
					response = MnsCmdFastloginRsp.parseFrom(data.getData());
				} catch (InvalidProtocolBufferException e) {
				}
				if (response == null) {
					MiLinkLog.w(mLogTag, "fastlogin response = null");
					EventBus.getDefault().post(new SessionLoginEvent(SessionLoginEvent.EventType.LoginFailed,
							Session.this, Const.InternalErrorCode.MNS_PACKAGE_INVALID));
					return;
				}
				if (response.getB2() != null && response.getGTKEYB2() != null) {
					MiAccount.getInstance().loginMiLink(response.getB2().toByteArray(),
							response.getGTKEYB2().toByteArray());
				} else {
					MiLinkLog.w(mLogTag, "fastlogin response.getB2() = null or response.getGTKEYB2() = null");
				}
				MiAccountManager.getInstance().setPassportInit(false);
				ConfigManager.getInstance().updateSuid(response.getSUID());
				EventBus.getDefault()
						.post(new SessionLoginEvent(SessionLoginEvent.EventType.LoginSuccess, Session.this, 0));
				heartBeat(true);// login成功，发心跳
			} else {
				EventBus.getDefault().post(new SessionLoginEvent(SessionLoginEvent.EventType.LoginFailed, Session.this,
						data.getMnsCode()));
			}
		}

		@Override
		public void onDataSendFailed(int errCode, String errMsg) {
			if (MiAccountManager.getInstance().isAnonymousModeCurrent()) {
				MiLinkLog.i(mLogTag, "current is not stardard mode");
				return;
			}
			MiLinkLog.i(mLogTag, "fastlogin onDataSendFailed errCode= " + errCode + ", errMsg=" + errMsg);
			EventBus.getDefault()
					.post(new SessionLoginEvent(SessionLoginEvent.EventType.LoginFailed, Session.this, errCode));
		}
	};

	private ResponseListener mChannelFastLoginRspListener = new ResponseListener() {

		@Override
		public void onDataSendSuccess(int errCode, PacketData data) {
			// 这里应该加个判断，如果是已经不是通道模式了，则不做任何响应
			if (!MiAccountManager.getInstance().isChannelModCurrent()) {
				MiLinkLog.i(mLogTag,
						"current is not channel mode " + MiAccountManager.getInstance().getCurrentAccountType());
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
					EventBus.getDefault().post(new SessionLoginEvent(SessionLoginEvent.EventType.LoginFailed,
							Session.this, Const.InternalErrorCode.MNS_PACKAGE_INVALID));
					return;
				}
				if (response.getB2() != null && response.getGTKEYB2() != null) {
					AnonymousAccount.getInstance().loginMiLink(response.getB2().toByteArray(),
							response.getGTKEYB2().toByteArray());
				} else {
					MiLinkLog.w(mLogTag, "channel fastlogin response.getB2() = null or response.getGTKEYB2() = null");
				}
				MiAccountManager.getInstance().setPassportInit(false);
				String userId = String.valueOf(response.getWid());
				MiLinkLog.v(TAG, "wid=" + userId);
				AnonymousAccount.getInstance().setUserId(userId);
				EventBus.getDefault()
						.post(new SessionLoginEvent(SessionLoginEvent.EventType.LoginSuccess, Session.this, 0));
			} else {
				EventBus.getDefault().post(new SessionLoginEvent(SessionLoginEvent.EventType.LoginFailed, Session.this,
						data.getMnsCode()));
			}
		}

		@Override
		public void onDataSendFailed(int errCode, String errMsg) {
			if (!MiAccountManager.getInstance().isChannelModCurrent()) {
				MiLinkLog.i(mLogTag,
						"failed current is not channel mode " + MiAccountManager.getInstance().getCurrentAccountType());
				return;
			}
			MiLinkLog.i(mLogTag, "channel fastlogin onDataSendFailed errCode= " + errCode + ", errMsg=" + errMsg);
			EventBus.getDefault()
					.post(new SessionLoginEvent(SessionLoginEvent.EventType.LoginFailed, Session.this, errCode));
		}
	};

	public void fastLogin() {
		Request request = null;
		switch (MiAccountManager.getInstance().getCurrentAccountType()) {
		// case MiAccountManager.ACCOUNT_TYPE_CHANNEL: {
		// MnsCmdChannelReq.Builder builder = MnsCmdChannelReq.newBuilder();
		//
		// builder.setPrivacyKey(ChannelAccount.getInstance().getPrivacyKey());
		// builder.setDeviceinfo(DeviceDash.getInstance().getDeviceSimplifiedInfo());
		// PacketData data = new PacketData();
		// data.setSeqNo(Global.getSequence());
		// data.setData(builder.build().toByteArray());
		// builder.getDeviceinfo();
		// MiLinkLog.d(mLogTag,
		// "channel info privacyKey:"+builder.getPrivacyKey()+",device
		// info:"+builder.getDeviceinfo());
		//
		// data.setCommand(Const.MnsCmd.MNS_CHANNEL_FAST_LOGIN);
		// request = new Request(data, mChannelFastLoginRspListener,
		// StreamUtil.MNS_ENCODE_CHANNEL_FAST_LOGIN,
		// ChannelAccount.getInstance());
		// MiLinkLog.v(mLogTag, "start channel fastlogin, seq=" +
		// request.getSeqNo());
		// }
		// break;
		case MiAccountManager.ACCOUNT_TYPE_ANONYMOUS: {
			MnsCmdAnonymousReq.Builder builder = MnsCmdAnonymousReq.newBuilder();
			String suid = ConfigManager.getInstance().getSuidAnonymous();
			if (!TextUtils.isEmpty(suid)) {
				builder.setSUID(suid);
				MiLinkLog.w(TAG, "start Anonymous fastlogin, suid=" + suid);
			} else {
				MiLinkLog.w(TAG, "start Anonymous fastlogin, suid is empty");
			}
			builder.setPrivacyKey(AnonymousAccount.getInstance().getPrivacyKey());
			builder.setDeviceinfo(DeviceDash.getInstance().getDeviceSimplifiedInfo());
			builder.setOnoff(true);
			PacketData data = new PacketData();
			data.setSeqNo(Global.getSequence());
			data.setData(builder.build().toByteArray());
			data.setCommand(Const.MnsCmd.MNS_ANONYMOUS_FAST_LOGIN);
			request = new Request(data, mAnonymousFastLoginRspListener, StreamUtil.MNS_ENCODE_ANONYMOUS_FAST_LOGIN,
					AnonymousAccount.getInstance());
			MiLinkLog.v(mLogTag, "start anonymous fastlogin, seq=" + request.getSeqNo());
		}
			break;
		case MiAccountManager.ACCOUNT_TYPE_STANDARD: {
			MnsCmdFastloginReq.Builder builder = MnsCmdFastloginReq.newBuilder();
			String suid = ConfigManager.getInstance().getSuid();
			if (!TextUtils.isEmpty(suid)) {
				builder.setSUID(suid);
				MiLinkLog.w(TAG, "start fastlogin, suid=" + suid);
			} else {
				MiLinkLog.w(TAG, "start fastlogin, suid is empty");
			}
			builder.setPassportlogin(MiAccountManager.getInstance().getPassportInit());
			builder.setOnoff(true);
			byte[] fastloginExtra = MiAccount.getInstance().getFastLoginExtra();
			if (fastloginExtra != null) {
				builder.setExtra(ByteString.copyFrom(fastloginExtra));
			}
			PacketData data = new PacketData();
			data.setSeqNo(Global.getSequence());
			data.setData(builder.build().toByteArray());
			data.setCommand(Const.MnsCmd.MNS_FAST_LOGIN);
			request = new Request(data, mFastLoginRspListener, StreamUtil.MNS_ENCODE_FAST_LOGIN,
					MiAccount.getInstance());
			MiLinkLog.v(mLogTag, "start fastlogin, seq=" + request.getSeqNo());
		}
			break;
		default:
			break;
		}
		request.setInternal(true);
		handleRequest(request);
		MiAccountManager.getInstance().setIsLogining(true);
	}

	static class B {
		public boolean b = false;
	}

	public void heartBeat(boolean firstHeart) {
		MnsCmdHeartBeat.Builder builder = MnsCmdHeartBeat.newBuilder()
				.setTimeStamp(ConfigManager.getInstance().getConfigTimeStamp());
		String suid = null;
		if (MiAccountManager.getInstance().isAnonymousModeCurrent()) {
			suid = ConfigManager.getInstance().getSuidAnonymous();
		} else {
			suid = ConfigManager.getInstance().getSuid();
		}
		if (!TextUtils.isEmpty(suid)) {
			builder.setSUID(suid);
			MiLinkLog.v(mLogTag, "start heartbeat, suid=" + suid);
		} else {
			MiLinkLog.v(mLogTag, "start heartbeat, suid is empty");
		}
		final B b = new B(); // 标志这次心跳是否是带regid的。
		if (true/*ClientAppInfo.isSupportMiPush() && !MiAccountManager.getInstance().hasUploadRegIdToServer()*/) {
			String regId = MiAccountManager.getInstance().getDeviceToken();
			Log.w("onTokenRefresh","token:"+regId);
			if (!TextUtils.isEmpty(regId)) {
				try {
					MiLinkLog.v(TAG, "heartbeat regid:" + regId);
					builder.setDevicetoken(ByteString.copyFrom(regId.getBytes("utf-8")));
					b.b = true;
				} catch (UnsupportedEncodingException e) {
				}
			}
		}
		PacketData data = new PacketData();
		data.setCommand(Const.MnsCmd.MNS_HEARTBEAT);
		data.setSeqNo(Global.getSequence());
		data.setData(builder.build().toByteArray());
		Request request = new Request(data, new ResponseListener() {

			@Override
			public void onDataSendSuccess(int errCode, PacketData data) {
				if (b.b) {
					MiLinkLog.v(TAG, "upload regid to server success");
					MiAccountManager.getInstance().setHasUploadRegIdToServer(true);
				}
				MiLinkLog.v(mLogTag, "heartbeat success");
				if (data != null && data.getData() != null) {
					try {
						MnsCmdHeartBeatRsp response = MnsCmdHeartBeatRsp.parseFrom(data.getData());
						if (response != null) {
							// 如果有更新，通知客户端
							if (ConfigManager.getInstance().updateConfig(response.getTimeStamp(),
									response.getJsonconfig())) {
								DataExtra.Builder dataExtra = DataExtra.newBuilder();
								float engineConfigRatio = ConfigManager.getInstance().getEngineConfigRatio();
								MiLinkLog.v(mLogTag, "engineConfigRatio=" + engineConfigRatio);
								dataExtra.setEngineratio(engineConfigRatio);
								JSONObject engineMatchJson = ConfigManager.getInstance().getEngineMatch();
								if (engineMatchJson != null) {
									MiLinkLog.v(mLogTag, "engineMatchJson=" + engineMatchJson);
									dataExtra.setEngineConfigJson(engineMatchJson.toString());
								}
								PacketData packet = new PacketData();
								packet.setCommand(Const.DATA_EXTRA_CMD);
								packet.setData(dataExtra.build().toByteArray());
								MnsPacketDispatcher.getInstance().dispatchPacket(packet);
							}
						}
					} catch (Exception e) {
					}
				}
			}

			@Override
			public void onDataSendFailed(int errCode, String errMsg) {
				MiLinkLog.v(mLogTag, "heartbeat failed");
			}
		}, MiAccountManager.getInstance().getBusinessEncByMode(), MiAccountManager.getInstance().getCurrentAccount());
		request.setInternal(true);
		if (firstHeart) {
			request.setAfterHandleCallBack(new Request.AfterHandleCallBack() {

				@Override
				public void onCallBack(String accIp, int accPort, String cmd, int retCode, long sentTime,
						long responseTime, int reqSize, int responseSize, int seqNo, String clientIp,
						String clientIsp) {
					// 第一个心跳，为了后台统计跑马成功的ip用。
					cmd = Const.MnsCmd.MNS_FIRST_HEARTBEAT;
					InternalDataMonitor.getInstance().trace(accIp, accPort, cmd, retCode, sentTime, responseTime,
							reqSize, responseSize, seqNo, clientIp, clientIsp);
				}
			});
			HeartBeatManager.getInstance().startHeartBeatProbeManager(data.getSeqNo());
		} else {
			HeartBeatManager.getInstance().sendHeartBeat(data.getSeqNo());
		}
		MiLinkLog.v(mLogTag, "start heartbeat, seq=" + request.getSeqNo());
		handleRequest(request);
	}

	public void pushAck(int seq) {
		seq *= -1;
		PacketData data = new PacketData();
		data.setCommand(Const.MnsCmd.MNS_PUSH__ACK_CMD);
		data.setSeqNo(seq);
		data.setData(new byte[] {});
		data.setNeedResponse(false);
		data.setNeedClientInfo(false);
		Request request = new Request(data, null, MiAccountManager.getInstance().getBusinessEncByMode(),
				MiAccountManager.getInstance().getCurrentAccount());
		request.setInternal(true);
		MiLinkLog.v(mLogTag, "start push ack, seq=" + seq);
		handleRequest(request);
	}

	private ResponseListener mLogoffRspListener = new ResponseListener() {

		@Override
		public void onDataSendSuccess(int errCode, PacketData data) {
			EventBus.getDefault()
					.post(new SessionLoginEvent(SessionLoginEvent.EventType.LogoffCmdReturn, Session.this, 0));
		}

		@Override
		public void onDataSendFailed(int errCode, String errMsg) {
			EventBus.getDefault()
					.post(new SessionLoginEvent(SessionLoginEvent.EventType.LogoffCmdReturn, Session.this, 0));
		}

	};

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
		Request request = new Request(data, mLogoffRspListener, MiAccountManager.getInstance().getBusinessEncByMode(),
				MiAccountManager.getInstance().getCurrentAccount());
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

		for (Iterator<Integer> it = keySet.iterator(); it.hasNext();) {
			Integer seqNo = it.next();
			Request request = mRequestMap.get(seqNo);
			if (request != null) {
				MiLinkLog.e(mLogTag, "Const.InternalErrorCode.CONNECT_FAIL, seq=" + request.getSeqNo() + ",cmd="
						+ request.getData().getCommand());
				request.onDataSendFailed(Const.InternalErrorCode.CONNECT_FAIL, "native network broken");
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
		MiLinkLog.w(mLogTag, "recv data:" + pcBuf.length);
		if (mRecBuffer != null) {
			try {
				mRecBuffer.append(pcBuf);
			} catch (InvalidPacketExecption e) {
				disConnect(Const.InternalErrorCode.READ_FAIL);
				if (e.errCode == InvalidPacketExecption.ERROR_CODE_NO_MNS_HEAD) { // 数据被劫持了
					if (mSessionType == SESSION_TYPE_DEFAULT) {
						EventBus.getDefault()
								.post(new SessionOtherEvent(SessionOtherEvent.EventType.RecvInvalidPacket, this));
					}
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
		MiLinkLog.w(mLogTag, "send end: seq=" + dwSeqNo);
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

		if (mSessionType == SESSION_TYPE_ASSIST) {
			EventBus.getDefault().post(new SessionConnectEvent(SessionConnectEvent.EventType.AssistSessionConnectFailed,
					this, failReason));
		} else {
			EventBus.getDefault()
					.post(new SessionConnectEvent(SessionConnectEvent.EventType.SessionBuildFailed, this, failReason));
		}
	}

	private void onOpenSessionBuildConnectSuccess() {
		mCurState = CONNECTED_STATE;
		if (mSessionType == SESSION_TYPE_ASSIST) {
			EventBus.getDefault()
					.post(new SessionConnectEvent(SessionConnectEvent.EventType.AssistSessionConnectSuccess, this, 0));
		} else {
			handShake(MiAccountManager.getInstance().getCurrentAccount());
		}
	}

	private void onOpenSessionHandshakeFail(int failReason) {
		mOpenSessionDoneTime = System.currentTimeMillis();
		mCurState = CONNECTED_STATE;
		EventBus.getDefault()
				.post(new SessionConnectEvent(SessionConnectEvent.EventType.SessionBuildFailed, this, failReason));
	}

	private OpenSessionSucessReturnInfo mOpenSessionSucessReturnInfo;

	public OpenSessionSucessReturnInfo getOpenSessionSucessReturnInfo() {
		return mOpenSessionSucessReturnInfo;
	}

	protected void onOpenSessionHandshakeSuccess(OpenSessionSucessReturnInfo info) {
		mOpenSessionDoneTime = System.currentTimeMillis();
		mCurState = HANDSHAKE_INITED_STATE;
		mOpenSessionSucessReturnInfo = info;
		EventBus.getDefault().post(new SessionConnectEvent(SessionConnectEvent.EventType.SessionBuildSuccess, this, 0));
	}

	private void onSessionError(int errCode) {
		mCurState = NO_CONNECT_STATE;
		if (mSessionType == SESSION_TYPE_ASSIST) {
			EventBus.getDefault()
					.post(new SessionConnectEvent(SessionConnectEvent.EventType.AssistSessionRunError, this, errCode));
		} else {
			EventBus.getDefault()
					.post(new SessionConnectEvent(SessionConnectEvent.EventType.SessionRunError, this, errCode));
		}
	}

	/**
	 * 轮询检查请求队列里是否超时 -- 异步
	 *
	 * @return 成功丢给网络线程处理返回true，否则返回false
	 */
	public boolean checkRequestsTimeout() {
		if (shouldCheckRequestsTimeout()) {
			return postMessage(MSG_CHECK_TIMEOUT, null, 0);
		}
		EventBus.getDefault().post(new SessionOtherEvent(SessionOtherEvent.EventType.RequestMapIsEmpty, this));
		return false;
	}

	/**
	 * 是否需要检查超时
	 * 
	 * @return
	 */
	public boolean shouldCheckRequestsTimeout() {
		if (!isConnected() || mRequestMap.isEmpty()) {
			return false;
		}
		return true;
	}

	private boolean isFastCheckPing(Request request) {
		return request.isPingRequest() && request.getTimeOut() == FAST_CHECK_PING_TIME_OUT;
	}

	private void checkIsReadTimeOut() {
		boolean hasFastCheckPing = false;
		boolean isFastCheckPingTimeout = false;
		ConcurrentLinkedQueue<Request> timeoutRequest = new ConcurrentLinkedQueue<Request>();
		boolean isHeartTimeout = false;
		int timeoutNum = 0;
		for (int seqNo : mRequestMap.keySet()) {
			Request request = mRequestMap.get(seqNo);
			if (request != null) {
				if (isFastCheckPing(request)) {
					hasFastCheckPing = true;
					if (request.isTimeout()) {
						// 已经发送了fastping 且已经超时 <条件需要判断 ,在ping包发送开始 到 ping超时期间
						// 没有收到下行。>
						if (mLastReceivedPacketTime < request.getCreatedTime()) {
							isFastCheckPingTimeout = true;
							MiLinkLog.e(mLogTag, "mLastReceivedPacketTime = " + mLastReceivedPacketTime
									+ ",createTime = " + request.getCreatedTime() + ", fastping is timeout");

						} else {
							MiLinkLog.e(mLogTag,
									"mLastReceivedPacketTime = " + mLastReceivedPacketTime + ",createTime = "
											+ request.getCreatedTime() + ", fastping timeout,but can recv msg");
						}
					}
				}
				if (request.isTimeout()) {
					timeoutNum++;
					mRequestMap.remove(seqNo);
					timeoutRequest.add(request);

					if (Const.MnsCmd.MNS_HEARTBEAT == request.getData().getCommand()) {
						HeartBeatManager.getInstance().reciveTimeoutHeartBeat(request.getData().getSeqNo());
					}
				}
			}
		}
		boolean hasLongTimeoutRequest = false;
		boolean hasLongLongTimeoutRequest = false;
		long now = System.currentTimeMillis();
		// 通知上层读失败
		for (Request request : timeoutRequest) {
			// 读超时
			MiLinkLog.e(mLogTag,
					"Request read time out, seq=" + request.getSeqNo() + ",cmd=" + request.getData().getCommand());
			if (request.getTimeOut() >= 10 * 1000) {// 有长超时包超时，短包超时不发ping
				hasLongTimeoutRequest = true;
			}
			// 有一个包超时40s直接重连
			if (now - request.getSentTime() > 40 * 1000) {
				hasLongLongTimeoutRequest = true;
				MiLinkLog.e(mLogTag, "hasLongLongTimeoutRequest=true");
			}
			request.onDataSendFailed(Const.InternalErrorCode.READ_TIME_OUT, "request time out");
			mPendingStatisticTimeoutedRequestMap.put(request.getSeqNo(), request);
		}
		timeoutRequest.clear();
		if (hasLongTimeoutRequest) {
			MiLinkLog.e(mLogTag, Device.Network.getCurrentNetworkDetailInfo().toString());
		}
		// 重连唯一条件，发送了fastping且fastping也超时了，才会触发断线重连逻辑。另外有两个包超时了很长时间。
		// 还有已经是坏链接了，就发一个包必超时，上一次收包很晚了
		boolean isBadConnect = System.currentTimeMillis() - mLastReceivedPacketTime > 5 * 60 * 1000;
		if (isFastCheckPingTimeout || hasLongLongTimeoutRequest || isBadConnect || timeoutNum > 2) {
			MiLinkLog.e(mLogTag,
					"checkIsReadTimeOut,isFastCheckPingTimeout=" + isFastCheckPingTimeout
							+ " hasLongLongTimeoutRequest=" + hasLongLongTimeoutRequest
							+ " isFastCheckPingTimeout || hasLongLongTimeoutRequest || isBadConnect=" + isBadConnect
							+ " timeoutNum=" + timeoutNum);
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

	public void postStatisticsTimeoutPacketAction() {
		postMessage(MSG_POST_STATISTICS_TIMEOUT_PACKET, null, 0);
	}

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

	public void addContinuousRecv110Count() {
		mContinuousRecv110Count++;
	}

	public void resetContinuousRecv110Count() {
		mContinuousRecv110Count = 0;
	}

	public boolean checkExceedMaxContinuousRecv110Count() {
		MiLinkLog.v(mLogTag, "mContinuousRecv110Count = " + mContinuousRecv110Count);
		if (mContinuousRecv110Count >= 3) {
			if (mSessionType == SESSION_TYPE_DEFAULT) {
				EventBus.getDefault()
						.post(new ServerNotificationEvent(ServerNotificationEvent.EventType.ServerLineBroken));
			}
			return false;
		}
		return true;
	}

	private String mClientIp = "";

	private String mClientIsp = "";

	public String getClientIp() {
		return mClientIp;
	}

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

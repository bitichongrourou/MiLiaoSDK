
package com.mi.milink.sdk.session.persistent;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.greenrobot.eventbus.EventBus;

import android.text.TextUtils;

import com.mi.milink.sdk.account.IAccount;

import com.mi.milink.sdk.aidl.PacketData;
import com.mi.milink.sdk.base.Global;
import com.mi.milink.sdk.base.os.info.NetworkDash;
import com.mi.milink.sdk.config.ConfigManager;
import com.mi.milink.sdk.connection.DomainManager;
import com.mi.milink.sdk.connection.IConnection;
import com.mi.milink.sdk.connection.IConnectionCallback;
import com.mi.milink.sdk.connection.TcpConnection;
import com.mi.milink.sdk.data.Const;
import com.mi.milink.sdk.debug.MiLinkLog;
import com.mi.milink.sdk.debug.TrafficMonitor;
import com.mi.milink.sdk.proto.PushPacketProto.MilinkLogReq;
import com.mi.milink.sdk.session.common.InvalidPacketExecption;
import com.mi.milink.sdk.session.common.MsgProcessor;
import com.mi.milink.sdk.session.common.ReceiveBuffer;
import com.mi.milink.sdk.session.common.Request;
import com.mi.milink.sdk.session.common.ResponseListener;
import com.mi.milink.sdk.session.common.ServerProfile;
import com.mi.milink.sdk.session.common.SessionConst;
import com.mi.milink.sdk.session.common.StreamUtil;
import com.mi.milink.sdk.session.common.ReceiveBuffer.ReceiveBufferSink;
import com.mi.milink.sdk.util.CommonUtils;

/**
 * session封装了连接，收发消息等
 */
public class SessionForUploadLog implements IConnectionCallback, MsgProcessor {
	private static final String TAG = "SessionForUploadLog";

	// Connection thread消息类型
	private static final int MSG_CONNECT = 1;

	private static final int MSG_HANDLE_REQUEST = 2;

	private static final int MSG_DISCONNECT = 4;

	// Session的状态
	private int mCurState = NO_CONNECT_STATE;

	private static final int NO_CONNECT_STATE = 0;

	private static final int CONNECTING_STATE = 1;

	private static final int CONNECTED_STATE = 2;

	private IConnection mConn;

	private ServerProfile mServerProfile;

	public int mFlagForSessionManager;

	private boolean mCanClose = true; // 如果正在连接时，调用mConn.close()，native会出现崩溃

	private MilinkLogReq mLogReq = null;

	private IAccount mIAccount;

	UploadLogListener mUploadLogListener;

	protected ReceiveBuffer mRecBuffer;

	Request request;

	public interface UploadLogListener {
		void success();

		void failed();
	}

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
								return mIAccount;
							}
						});
			} catch (IOException e) {
				MiLinkLog.e(TAG, "decode downstream failed", e);
			}
			if (recvData == null) {
				return false;
			}
			long curTime = System.currentTimeMillis();
			if (request != null && recvData != null) {
				request.onDataSendSuccess(0, recvData);
//				if (recvData.getMnsCode() == 0) {
//					request.getListener().onDataSendSuccess(0, recvData);
//				} else {
//					request.getListener().onDataSendFailed(recvData.getMnsCode(),
//							"upload log failed,mnscode=" + recvData.getMnsCode());
//				}
				request.addRetryCount();
			}
			return true;
		}

		@Override
		public boolean onAddTimeout(int sessionNO, int seqNo) {
			return false;
		}

	};

	public SessionForUploadLog(MilinkLogReq logReq, IAccount iaccount, UploadLogListener uploadLogListener) {
		mConn = null;
		mServerProfile = null;
		mCurState = NO_CONNECT_STATE;

		this.mLogReq = logReq;
		if (mLogReq == null) {
			mLogReq = MilinkLogReq.newBuilder().setIp("58.83.160.103:80").setTime(-1).build();
		}
		this.mIAccount = iaccount;
		this.mUploadLogListener = uploadLogListener;
		mRecBuffer = new ReceiveBuffer(mRecBufSink, 0, true);
	}

	public boolean openSession() {
		if (mLogReq == null) {
			MiLinkLog.v(TAG, "mLogReq is null");
			return false;
		}
		String ipAndPortStr = mLogReq.getIp();
		MiLinkLog.v(TAG, "ipAndPortStr" + ipAndPortStr);
		String ipAndPort[] = null;
		if (!TextUtils.isEmpty(ipAndPortStr)) {
			ipAndPort = ipAndPortStr.split(":");
		}
		if (ipAndPort == null || ipAndPort.length != 2) {
			return false;
		}
		ServerProfile serverprofile = null;
		try {
			serverprofile = new ServerProfile(ipAndPort[0], Integer.parseInt(ipAndPort[1]),
					SessionConst.TCP_CONNECTION_TYPE, SessionConst.DOMAIN_IP);
		} catch (Exception e) {
			serverprofile = null;
		}
		if (serverprofile == null || serverprofile.getProtocol() == SessionConst.NONE_CONNECTION_TYPE) {
			MiLinkLog.v(TAG, "openSession fail, serverprofile=" + serverprofile);
			onOpenSessionBuildConnectFail(Const.InternalErrorCode.IP_ADDRESS_NULL);
			return false;
		}

		mCurState = CONNECTING_STATE;

		boolean started = false;

		// 如果有serverprofile，比较一下protocol，是否需要重建conn
		if (mServerProfile == null || mServerProfile.getProtocol() != serverprofile.getProtocol()) {
			MiLinkLog.v(TAG, "openSession if");
			if (mConn != null) {
				mConn.stop();
			}

			if (serverprofile.getProtocol() == SessionConst.TCP_CONNECTION_TYPE) {
				mConn = new TcpConnection(0, this);
			}
			mServerProfile = serverprofile;
			try {
				started = mConn.start();
			} catch (Exception e) {
				MiLinkLog.e(TAG, "connection start failed", e);
			}

			if (!started) {
				// 这些标记状态需要修改
				onOpenSessionBuildConnectFail(Const.InternalErrorCode.MNS_LOAD_LIBS_FAILED);
				return false;
			}
		} else {
			MiLinkLog.v(TAG, "openSession else");
			if (mConn == null) {
				if (serverprofile.getProtocol() == SessionConst.TCP_CONNECTION_TYPE) {
					mConn = new TcpConnection(0, this);
				}
			}
			if (!mConn.isRunning()) {
				mServerProfile = serverprofile;
				try {
					started = mConn.start();
				} catch (Exception e) {
					MiLinkLog.e(TAG, "connection start failed", e);
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

	/**
	 * 获取本会话是否可用
	 *
	 * @return 可用返回true;否则返回false
	 */
	public boolean isConnected() {
		switch (mCurState) {
		case CONNECTED_STATE:
			return true;
		case CONNECTING_STATE:
		case NO_CONNECT_STATE:
		default:
			return false;
		}
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
			MiLinkLog.e(TAG, "postMessage " + uMsg + " mConn == null!!!!");
			return false;
		}

		boolean ret = false;
		try {
			ret = mConn.postMessage(uMsg, lParam, wParam, this);
		} catch (NullPointerException e) {
			return ret;
		}

		if (ret == false) {
			MiLinkLog.e(TAG, "mMessage must be full ! uMsg = " + uMsg);
			return ret;
		}

		return ret;
	}

	/**
	 * 关闭网络会话
	 */
	public boolean close() {
		if (!mCanClose) {
			MiLinkLog.i(TAG, "connecting! can not close");
			return false;
		}
		if (mConn != null) {
			MiLinkLog.w(TAG, "stop begin");
			mConn.stop();
			mConn = null;
			mServerProfile = null;
			mCurState = NO_CONNECT_STATE;
			MiLinkLog.w(TAG, "stop over");
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
		MiLinkLog.i(TAG, "disConnect, errorCallBackErrorCode=" + errorCallBackErrorCode);
		postMessage(MSG_DISCONNECT, null, errorCallBackErrorCode);
	}

	@Override
	public void onMsgProc(int uMsg, Object lParam, int wParam) {
		MiLinkLog.v(TAG, "onMsgProc, uMsg=" + uMsg + ", wParam=" + wParam);
		// 消息回调的通知
		switch (uMsg) {
		case MSG_CONNECT: // 连接
		{
			// 域名解析
			String ip = null;

			if (!CommonUtils.isLegalIp(mServerProfile.getServerIP())) {
				ip = DomainManager.getInstance().getDomainIP(mServerProfile.getServerIP());
				if (ip == null) {
					onOpenSessionBuildConnectFail(SessionConst.CONN_FAILED);
					return;
				}
				mServerProfile.setServerIP(ip);
			} else {
				ip = mServerProfile.getServerIP();
			}
			MiLinkLog.i(TAG, "connect to " + mServerProfile);

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
				MiLinkLog.e(TAG,
						String.format("seq=%d,cmd=%s is invalid", request.getSeqNo(), request.getData().getCommand()));
				request.onDataSendFailed(Const.InternalErrorCode.MNS_PACKAGE_INVALID,
						"package is already over the valid time");
				return;
			}
			request.setSentTime(System.currentTimeMillis());
			PacketData data = request.getData();
			String cmd = data.getCommand();

			byte[] packet = request.toBytes();
			if (packet != null) {
				MiLinkLog.v(TAG, "connection send data, seq=" + request.getSeqNo());
				if (mConn.sendData(packet, request.getSeqNo(), request.getTimeOut())) {
					TrafficMonitor.getInstance().traffic(cmd, packet.length);
				}
			} else {
				request.onDataSendFailed(Const.InternalErrorCode.ENCRYPT_FAILED, "data encryption failed");
				MiLinkLog.w(TAG, "connection send data, but data = null");
				if (mUploadLogListener != null) {
					mUploadLogListener.failed();
				}
			}
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
		default: // 无法识别的消息
			MiLinkLog.e(TAG, "OnMsgProc unknow uMsgID = " + uMsg);
			break;
		}

	}

	public void uploadLog() {
		PacketData data = new PacketData();
		data.setSeqNo(Global.getSequence());
		data.setHasClientInfo(false);
		data.setCommand(Const.MnsCmd.MNS_MILINK_UPLOADLOG);
		File[] logs = UploadLogUtils.getLogFile(mLogReq.getTime());
		File tempFile = UploadLogUtils.createTemp(logs);
		byte[] logBytes = UploadLogUtils.getFileBytes(tempFile);
		if (logBytes == null) {
			close();
			if (mUploadLogListener != null) {
				mUploadLogListener.failed();
			}
			return;
		}
		data.setData(logBytes);
		// data.setNeedResponse(false);
		request = new Request(data, new ResponseListener() {

			@Override
			public void onDataSendSuccess(int errCode, PacketData data) {
				if (mUploadLogListener != null) {
					mUploadLogListener.success();
				}
			}

			@Override
			public void onDataSendFailed(int errCode, String errMsg) {
				if (mUploadLogListener != null) {
					mUploadLogListener.failed();
				}
			}
		}, StreamUtil.MNS_ENCODE_B2_TOKEN_FOR_HS, mIAccount);
		boolean ret = postMessage(MSG_HANDLE_REQUEST, request, 0);
		if (mConn != null) {
			mConn.wakeUp();
		}
		tempFile.delete();
		System.gc();
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

	// 肯定会调用一次
	@Override
	public boolean onDisconnect() {
		// 断开连接的通知
		MiLinkLog.i(TAG, "OnDisconnect");
		mRecBuffer.reset();
		return true;
	}

	// onError肯定会调用disconnect，而且是先disconnect再调用Error
	@Override
	public boolean onError(int socketStatus) {
		// 连接出错的通知
		MiLinkLog.e(TAG, "onError socketStatus " + socketStatus + ", mCurState=" + mCurState);
		switch (mCurState) {
		case NO_CONNECT_STATE:
		case CONNECTING_STATE:
		case CONNECTED_STATE:
			onOpenSessionBuildConnectFail(SessionConst.CONN_FAILED);
			break;
		default:
			MiLinkLog.e(TAG, "onError wrong state = " + mCurState);
			break;
		}
		return true;
	}

	@Override
	public boolean onTimeOut(int dwSeqNo, int nReason) {
		MiLinkLog.v(TAG, "send time out: seq=" + dwSeqNo);
		return false;
	}

	@Override
	public boolean onRecv(byte[] pcBuf) {
		MiLinkLog.w(TAG, "recv data:" + pcBuf.length);
		if (mRecBuffer != null) {
			try {
				mRecBuffer.append(pcBuf);
			} catch (InvalidPacketExecption e) {
				disConnect(Const.InternalErrorCode.READ_FAIL);
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean onSendBegin(int dwSeqNo) {
		MiLinkLog.v(TAG, "send begin: seq=" + dwSeqNo);
		return false;
	}

	@Override
	public boolean onSendEnd(int dwSeqNo) {
		MiLinkLog.v(TAG, "send end: seq=" + dwSeqNo);
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
		mCurState = NO_CONNECT_STATE;
	}

	private void onOpenSessionBuildConnectSuccess() {
		mCurState = CONNECTED_STATE;
		uploadLog();
	}

	private void onSessionError(int errCode) {
		mCurState = NO_CONNECT_STATE;
	}

	static class UploadLogUtils {
		public final static int ALL = -1;

		public final static int TODAY = 0;

		public final static int YESTODAY = 1;

		public final static int BEFORE_YEST = 2;

		private static void getFileRecursion(File root, List<File> temp) {
			if (!root.isDirectory()) {
				if (root.getName().endsWith(".log")) {
					temp.add(root);
				}
				return;
			} else {
				for (File f : root.listFiles()) {
					getFileRecursion(f, temp);
				}
			}
		}

		public static File[] getLogFile(int flag) {

			String dir = Global.getClientAppInfo().getLogPath();
			MiLinkLog.v(TAG, "dir:" + dir);
			File root = new File(dir);
			if (root.exists() && root.isDirectory()) {
				long time = System.currentTimeMillis();
				switch (flag) {
				case TODAY:
					break;
				case YESTODAY:
					time -= 24 * 60 * 60 * 1000;
					break;
				case BEFORE_YEST:
					time -= 24 * 60 * 60 * 1000 * 2;
					break;
				case ALL:

				default:
					List<File> temp = new LinkedList<File>();
					getFileRecursion(root, temp);
					File[] files = new File[temp.size()];
					int i = 0;
					for (File f : temp) {
						files[i++] = f;
					}
					return files;
				}
				SimpleDateFormat df = CommonUtils.createDataFormat("yyyy-MM-dd");
				String childName = df.format(new Date(time));
				File child = new File(root, childName);
				if (child.exists() && child.isDirectory()) {
					File[] mlogs = child.listFiles(new FilenameFilter() {

						@Override
						public boolean accept(File dir, String name) {
							if (name.endsWith(".log")) {
								return true;
							}
							return false;
						}
					});
					return mlogs;
				}
			}
			return null;
		}

		public static File createTemp(File[] _files) {
			File tempFile = null;
			try {
				int BUFFER = 1024;
				// 文件目录
				BufferedInputStream origin = null;
				tempFile = new File(Global.getClientAppInfo().getLogPath(), "temp.zip");
				// 输出文件流
				FileOutputStream dest = new FileOutputStream(tempFile);
				// ByteArrayOutputStream dest = new ByteArrayOutputStream(1024);
				// 输出压缩流 包裹缓存流
				ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));

				byte data[] = new byte[BUFFER];

				int fileTotalSize = 0;

				// 这里加个限制，不能加载太多进内存.代表最多load文件大小
				int limit = 1024 * 1024 * 10;
				if (NetworkDash.isWifi()) {
					limit = 1024 * 1024 * 80;
				}

				for (int i = _files.length - 1; i >= 0 && fileTotalSize < limit; i--) {
					// 输入流
					FileInputStream fi = new FileInputStream(_files[i]);
					// 输入缓存流
					origin = new BufferedInputStream(fi, BUFFER);
					String absolutePath = _files[i].getAbsolutePath();
					int temp = 0, subBegin = 0;
					for (int j = absolutePath.length() - 1; j >= 0; j--) {
						if (absolutePath.charAt(j) == '/') {
							temp++;
							// 往上两级目录，这里会不会以后有变
							if (temp == 3) {
								subBegin = j;
								break;
							}
						}
					}
					String zipName = absolutePath.substring(subBegin + 1, absolutePath.length());
					System.out.println(zipName);
					// 单个压缩实体
					ZipEntry entry = new ZipEntry(zipName);// .substring(_files[i].lastIndexOf("/")
															// + 1));

					// 到输出
					out.putNextEntry(entry);

					int sigleFileSize = 0;
					int count;
					while ((count = origin.read(data, 0, BUFFER)) != -1) {
						out.write(data, 0, count);
						fileTotalSize += count;
						sigleFileSize += count;
					}
					System.out.println("sigleFileSize:" + sigleFileSize);
					origin.close();
				}
				System.out.println("fileTotalSize:" + fileTotalSize);
				out.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.gc();
			return tempFile;
		}

		static byte[] getFileBytes(File tempFile) {
			byte[] content = null;
			BufferedInputStream in = null;
			int len = 0;
			try {
				in = new BufferedInputStream(new FileInputStream(tempFile));
				ByteArrayOutputStream out = new ByteArrayOutputStream(1024 * 500);

				byte[] temp = new byte[1024 * 10];
				int size = 0;
				while ((size = in.read(temp)) != -1) {
					out.write(temp, 0, size);
					len += size;
				}
				in.close();
				content = out.toByteArray();
			} catch (Exception e) {
				e.printStackTrace();
			}
			MiLinkLog.d(TAG, "compress file len=" + len);
			return content;
		}
	}
}

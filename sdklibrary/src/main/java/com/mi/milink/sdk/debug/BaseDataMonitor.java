
package com.mi.milink.sdk.debug;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import android.os.Message;

import com.mi.milink.sdk.account.manager.MiAccountManager;
import com.mi.milink.sdk.base.CustomHandlerThread;
import com.mi.milink.sdk.base.Global;
import com.mi.milink.sdk.base.os.info.NetworkDash;
import com.mi.milink.sdk.config.ConfigManager;
import com.mi.milink.sdk.data.ClientAppInfo;
import com.mi.milink.sdk.data.Const;

/**
 * 数据监控抽象类
 * 
 * @author MK
 */
public abstract class BaseDataMonitor extends CustomHandlerThread {

	private static final String TAG = "BaseDataMonitor";

	public static final int RET_CODE_OK = 0;

	public static final int RET_CODE_TIME_OUT = 1; // 超时

	public static final int RET_CODE_TOKEN_EXPIRED = 2;

	public static final int RET_CODE_PING_TIME_OUT = 3;

	public static final int RET_CODE_ABANDONED_SESSION_NOT_READY = 4;

	public static final int MESSAGE_UPLOAD_DATA = 10;

	protected final ConcurrentHashMap<String, List<MonitorItem>> mMonitorItemMap = new ConcurrentHashMap<String, List<MonitorItem>>();

	protected long mUploadInterval = ConfigManager.getInstance().getUploadStasticInterval();

	protected boolean mSamplingStatisticsSwitch = false;

	protected boolean mLoopPost = true;

	protected boolean mSwitch = true;

	/** 统计上传的域名 **/
	protected String stasticServerHost;
	/** 统计上传的保底IP **/
	protected String stasticServerAddIp;
	protected String stasticServerAdd;

	private long mLastUploadTs;

	protected BaseDataMonitor(String name) {
		super(name);
	}

	@Override
	protected void processMessage(Message msg) {
		switch (msg.what) {
		case MESSAGE_UPLOAD_DATA:
			mLastUploadTs = System.currentTimeMillis();
			doPostData();
			if (mLoopPost) {
				Message message = obtainMessage();
				message.what = MESSAGE_UPLOAD_DATA;
				sendMessageDelayed(message, mUploadInterval);
			}
			break;

		default:
			break;
		}
	}

	boolean mStarted = false;

	protected void startUpload(long delayTime) {
		mStarted = true;
		removeMessage(MESSAGE_UPLOAD_DATA);
		Message message = obtainMessage();
		message.what = MESSAGE_UPLOAD_DATA;
		sendMessageDelayed(message, delayTime);
	}

	protected abstract void doPostData();

	protected abstract String toJson(ConcurrentHashMap<String, List<MonitorItem>> map);

	public String getStasticServerAddr() {

		boolean isLive = ClientAppInfo.isLiveApp();
		if (isLive) {
			MiLinkLog.d(TAG, "static server addr = " + Const.STASTIC_ZHIBO_SERVER_ADDR);
			return Const.STASTIC_ZHIBO_SERVER_ADDR;
		}
		return Const.STASTIC_SERVER_ADDR;
	}

	public String getStaticServerAddIp() {

		boolean isLive = ClientAppInfo.isLiveApp();
		if (isLive) {
			MiLinkLog.d(TAG, "static server Ip = " + Const.STASTIC_ZHIBO_SERVER_ADDR_IP);
			return Const.STASTIC_ZHIBO_SERVER_ADDR_IP;
		}
		return Const.STASTIC_SERVER_ADDR_IP;
	}

	public String getStaticServerHost() {

		boolean isLive = ClientAppInfo.isLiveApp();
		if (isLive) {
			MiLinkLog.d(TAG, "static server host = " + Const.STASTIC_ZHIBO_SERVER_HOST);
			return Const.STASTIC_ZHIBO_SERVER_HOST;
		}

		return Const.STASTIC_SERVER_HOST;
	}

	public void addMonitorItem(MonitorItem item) {
		if (null != item && mSwitch) {
			String cmd = item.cmd;
			List<MonitorItem> itemList = null;
			if (mMonitorItemMap.containsKey(cmd)) {
				// 如果在这两条语句之间执行了mMonitorItemMap.clear。别的线程。会造成itemList为空。
				itemList = mMonitorItemMap.get(cmd);
				if (null == itemList) {
					return;
				}
			} else {
				itemList = new ArrayList<MonitorItem>();
				mMonitorItemMap.put(cmd, itemList);
			}
			if (itemList.size() < 100) {
				itemList.add(item);
				// 如果未启动打点 这里启动一下
				if (!mStarted) {
					startUpload(60 * 1000);
				}
			} else if (System.currentTimeMillis() - mLastUploadTs > 3 * 60 * 1000) {
				startUpload(0);
			}
		}
	}

	/**
	 * MiLink命令字默认的监控
	 * 
	 * @param accIp
	 * @param accPort
	 * @param cmd
	 * @param retCode
	 * @param requestTime
	 * @param responseTime
	 * @param reqSize
	 * @param rspSize
	 * @param seq
	 */
	public void trace(final String accIp, final int accPort, final String cmd, final int retCode,
			final long requestTime, final long responseTime, final int reqSize, final int rspSize, final int seq) {
		trace(Const.TRACE_AC_VALUE, accIp, accPort, cmd, retCode, requestTime, responseTime, reqSize, rspSize, seq,
				MiAccountManager.getInstance().getUserId());
	}

	public void trace(final String ac, final String accIp, final int accPort, final String cmd, final int retCode,
			final long requestTime, final long responseTime, final int reqSize, final int rspSize, final int seq,
			final String vuid) {
		if (mHandler == null) {
			return;
		}
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				traceToServer(ac, accIp, accPort, cmd, retCode, requestTime, responseTime, reqSize, rspSize, seq, vuid,
						Global.getClientIp(), Global.getClientIsp());
			}
		});
	}

	public void trace(final String accIp, final int accPort, final String cmd, final int retCode,
			final long requestTime, final long responseTime, final int reqSize, final int rspSize, final int seq,
			final String clientIp, final String clientIsp) {
		trace(Const.TRACE_AC_VALUE, accIp, accPort, cmd, retCode, requestTime, responseTime, reqSize, rspSize, seq,
				MiAccountManager.getInstance().getUserId(), clientIp, clientIsp);
	}

	public void trace(final String ac, final String accIp, final int accPort, final String cmd, final int retCode,
			final long requestTime, final long responseTime, final int reqSize, final int rspSize, final int seq,
			final String vuid, final String clientIp, final String clientIsp) {
		if (mHandler == null) {
			return;
		}
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				traceToServer(ac, accIp, accPort, cmd, retCode, requestTime, responseTime, reqSize, rspSize, seq, vuid,
						clientIp, clientIsp);
			}
		});
	}

	protected void traceToServer(final String ac, final String accIp, final int accPort, final String cmd,
			final int retCode, final long requestTime, final long responseTime, final int reqSize, final int rspSize,
			final int seq, final String vuid, final String clientIp, final String clientIsp) {
		MonitorItem item = new MonitorItem();
		item.cmd = cmd;
		item.waste = responseTime - requestTime;
		item.isSuccess = (retCode == 0);
		item.errorCode = retCode;
		item.accip = (accIp != null ? accIp.trim() : "");
		item.apn = NetworkDash.getApnName();
		item.apnType = String.valueOf(NetworkDash.getApnType());
		item.port = accPort;
		item.seq = seq;
		if (Const.MnsCmd.MNS_FIRST_HEARTBEAT.equals(item.cmd) || Const.MnsCmd.MNS_HAND_SHAKE.equals(item.cmd)) { // 只有handshake包和firstheartbeat才需要clientip
			item.clientIp = clientIp;
			item.clientIsp = clientIsp;
			addMonitorItem(item);
			return;
		}
		if (!mSamplingStatisticsSwitch) {
			addMonitorItem(item);
		} else {
			int r = random.nextInt(100);
			if (r < ConfigManager.getInstance().getSamplingStatisticsFactor()) {
				addMonitorItem(item);
			}
		}
	}

	private Random random = new Random();

	public static class MonitorItem implements Serializable {

		private static final long serialVersionUID = -5333015815983866181L;

		public String cmd;// 命令字

		public boolean isSuccess = false;

		public int errorCode = 0;

		public long waste = 0;// 耗时

		public String accip = "";

		public String apn = "";

		public long seq = 0;

		public int port = 0;

		public String apnType = "";

		public String clientIp = "";

		public String clientIsp = "";
	}
}

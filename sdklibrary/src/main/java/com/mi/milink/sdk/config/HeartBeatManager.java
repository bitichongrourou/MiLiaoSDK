package com.mi.milink.sdk.config;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.mi.milink.sdk.base.Global;
import com.mi.milink.sdk.base.os.timer.AlarmClockService;
import com.mi.milink.sdk.data.ClientAppInfo;
import com.mi.milink.sdk.data.Const;
import com.mi.milink.sdk.debug.MiLinkLog;

import android.annotation.SuppressLint;

/***
 * 
 * @author zhaolingzhi
 *
 */
@SuppressLint("NewApi")
public class HeartBeatManager {

	private static final String TAG = "HeartBeatManager";

	private static final int incIntervalTime = 25 * 1000;// 30s 增长

	private static int minHeartBeatInterval = 3*60 * 1000;// 最小值 2*60
	private static int maxHeartBeatInterval = 12 * 60 * 1000;// 最大值

	private int heartBeatInterval = minHeartBeatInterval; // 默认起始是3分钟
	private long lastPacketSendTime = 0;

	private static final int maxListElemet = 2;

	Queue<HeartBeatInfo> packetList = new ConcurrentLinkedQueue<HeartBeatManager.HeartBeatInfo>();

	private HeartBeatInfo lastHeartBeatInfo;// 上一个心跳数据
	private HeartBeatInfo currHeartBeatInfo;// 当前心跳数据

	private long lastReviveTimeoutHbTime = 0;// 上一次收到超时时间

	private HeartBeatModEnum modle;

	private boolean probeIsStop = false;

	boolean probeFailedPoint = false;// 是否需要继续探测上一个点 m=,true
										// 代表,还需要上个心跳时钟，false代表重新开始.

	private Map<String, Integer> apnsHeartBeatTimeMap = new ConcurrentHashMap<String, Integer>(5);
	private static final String heartBeatTimeFileName = "apnhearttime";

	private static HeartBeatManager sIntance = new HeartBeatManager();

	/***
	 * 打印变量
	 */
	private void printfManager(String info) {

		MiLinkLog.d(TAG,
				"HeartBeatManager info:" + info + ",Interval=" + heartBeatInterval + ",lHbI=" + lastHeartBeatInfo
						+ ",cHBI=" + currHeartBeatInfo + ",lPacketSt=" + lastPacketSendTime + ",model=" + modle
						+ ",packetSize = " + packetList.size());
		if (packetList.size() > 0) {
			for (HeartBeatInfo h : packetList) {

				MiLinkLog.d(TAG, "PacketListInfo = " + h);
			}

		}
	}

	public static HeartBeatManager getInstance() {
		return sIntance;
	}

	public long getLastPacketSendTime() {
		MiLinkLog.d(TAG, "lastPacketSendTime = " + lastPacketSendTime);
		return lastPacketSendTime;
	}

	public long getHeartBeatInterval() {
		MiLinkLog.d(TAG, "heartBeatInterval = " + heartBeatInterval);
		return heartBeatInterval;
	}

	@SuppressWarnings("unchecked")
	private void loadHeartBeatTimeMap() {
		try {
			Object obj = IIpInfoManager.loadObject(heartBeatTimeFileName);

			if (obj != null) {
				apnsHeartBeatTimeMap = (Map<String, Integer>) obj;
			}
		} catch (Throwable t) {
		}
	}

	public void saveConfig() {

		String apn = IIpInfoManager.getCurrentApn();
		if(apn == null)
		{
			MiLinkLog.i(TAG, "saveconfig apn = null , no save");
			return ;
		}
		apnsHeartBeatTimeMap.put(apn, heartBeatInterval);

		IIpInfoManager.saveObject(apnsHeartBeatTimeMap, heartBeatTimeFileName);
		MiLinkLog.i(TAG, "********* save config apn=" + apn + ",time = " + heartBeatInterval);
	}

	private void setHeartBeatInterval() {

		String apn = IIpInfoManager.getCurrentApn();
		MiLinkLog.d(TAG, "apn = " + apn);
		if (apnsHeartBeatTimeMap == null || apn == null) {
			heartBeatInterval = minHeartBeatInterval;
			return;
		}
		Integer hbt = apnsHeartBeatTimeMap.get(apn);
		if (hbt != null && hbt >= minHeartBeatInterval) {
			heartBeatInterval = hbt.intValue();
			MiLinkLog.i(TAG, "load config find apn = " + apn + ",heartBeatInterval = " + heartBeatInterval);
		}

		for (String apnName : apnsHeartBeatTimeMap.keySet()) {

			MiLinkLog.d(TAG, "apnName=" + apnName + ",hbt=" + apnsHeartBeatTimeMap.get(apnName));

		}
	}

	private void loadConfig() {

		loadHeartBeatTimeMap();
		if (apnsHeartBeatTimeMap == null)
			return;
		setHeartBeatInterval();
	}

	private HeartBeatManager() {
		 MiLinkLog.d(TAG, "HeartBeatManager start()");
		clearPacketList();
		// 从存储里面读取时间
		clearHeartBeatManagerInfo(true);
		registerAlarmClock();
		modle = HeartBeatModEnum.INC;
		loadConfig();
		probeIsStop = false;
		// 如果是直播项目，心跳周期弄短点
		if(Global.getClientAppInfo().getAppId()==ClientAppInfo.LIVE_APP_ID){
		    maxHeartBeatInterval = 4 * 60 * 1000;// 最大值
		}
		 MiLinkLog.d(TAG, "HeartBeatManager end()");
	}

	private void clearPacketList() {

		packetList.clear();

	}

	/***
	 * 清楚心跳管理 信息 重新建立连接的时候/网络切换的时候，需要执行
	 */
	private void clearHeartBeatManagerInfo(boolean clearlast) {

		if (clearlast == true) {
			lastHeartBeatInfo = null;
			currHeartBeatInfo = null;
		}

		lastReviveTimeoutHbTime = 0;
		MiLinkLog.i(TAG, "clearHeartBeatManagerInfo");
	}

	/***
	 * 开始心跳探测
	 */
	public void startHeartBeatProbeManager(int seq) {
		printfManager("startHeartBeatProbeManager-----start");
		probeIsStop = false;
		clearHeartBeatManagerInfo(true);
		MiLinkLog.d(TAG, "start heartBeatProbeManager send first beat..");
		// modle = HeartBeatModEnum.INC;//
		// 每次fastlogin开始的时候，不能设置INC,modle是有状态的一个值

		if (probeFailedPoint == false)
			setHeartBeatInterval();
		registerAlarmClock();
		MiLinkLog.d(TAG, "probeFailedPoint = " + probeFailedPoint);
		printfManager("startHeartBeatProbeManager-----end");
		sendHeartBeat(seq);
	}

	/***
	 * 设置业务包发送的最后时间
	 * 
	 * @param i
	 * 
	 * @param nomalPacket
	 */
	public void setLastPacketSendTime(int i) {

		lastPacketSendTime = System.currentTimeMillis();
		MiLinkLog.d(TAG, "set lastpacketSendTime time = " + lastPacketSendTime + ",seq = " + i);
	}

	public void setLastPacketSendTime(int seqNo, String command) {
		if (Const.MnsCmd.MNS_HEARTBEAT.equals(command))
			return;
		lastPacketSendTime = System.currentTimeMillis();
		MiLinkLog.d(TAG,
				"set lastpacketSendTime time = " + lastPacketSendTime + ",seq = " + seqNo + ",command = " + command);

	}

	public long getLastHeartBeatSendTime() {
		long lastHeartBeatSendTime = 0;
		if (currHeartBeatInfo != null)
			lastHeartBeatSendTime = currHeartBeatInfo.getSendTime();

		return lastHeartBeatSendTime;
	}

	/***
	 * 发送心跳
	 * 
	 * @param packetSeq
	 */
	public void sendHeartBeat(int packetSeq) {

		if (probeIsStop == true) {
			MiLinkLog.d(TAG, "probeIsStop is true do nothing sendHeartBeat");
			return;
		}

		printfManager("sendHeartBeat----start---");

		HeartBeatInfo hbi = new HeartBeatInfo();
		hbi.setSeq(packetSeq);
		hbi.setSendTime(System.currentTimeMillis());
		long lastHeartBeatSendTime = 0;
		if (currHeartBeatInfo != null)
			lastHeartBeatSendTime = currHeartBeatInfo.getSendTime();

		lastHeartBeatInfo = currHeartBeatInfo;

		MiLinkLog.d(TAG, "sendHeartBeat lastPacketSendTime =" + lastPacketSendTime + ",lastHeartBeatSendTime = "
				+ lastHeartBeatSendTime + ",less = " + (lastPacketSendTime - lastHeartBeatSendTime));

		currHeartBeatInfo = hbi;
		if (lastHeartBeatInfo == null) {
			MiLinkLog.i(TAG, "send Heart Beat first beat,no put in packetlist,currHeartBeatInfo = "
					+ currHeartBeatInfo.toString());
			printfManager("sendHeartBeat----end---");
			return;
		}

		if (lastHeartBeatSendTime > 0 && lastPacketSendTime > lastHeartBeatSendTime) {
			MiLinkLog.d(TAG, "lastpacketSendtime > lastHeartBeatSendTime," + lastPacketSendTime + ","
					+ lastHeartBeatSendTime + ",no put in pa" + "" + "cketlist");
			printfManager("sendHeartBeat----end---");
			return;
		}

		MiLinkLog.d(TAG, "sendHeartBeat seqNo=" + packetSeq + " find ok,put in packetlist");
		lastHeartBeatInfo.setOk(true);
		putHBInfoInList(lastHeartBeatInfo);
		startAanalysisHeartBeat();

		printfManager("sendHeartBeat----end---");

	}

	/****
	 * 收到了链接断开的数据 如果这个心跳范围内，没有数据包的收发，则当做失败。
	 * 
	 * @param errCode
	 */
	public void reciveConnectRunError(int errCode) {

		printfManager("reciveConnectRunError----start---");
		long currentTime = System.currentTimeMillis();
		MiLinkLog.d(TAG, "currentTime=" + currentTime);

		long timerStartTime = 0;
		if (currHeartBeatInfo != null)
			timerStartTime = currHeartBeatInfo.getSendTime();
		long less = currentTime - timerStartTime;

		long packetSendLess = lastPacketSendTime - timerStartTime;

		MiLinkLog.i(TAG, "reciveConnectRunError,currentTime - timerStartTime = " + less + ", " + minHeartBeatInterval
				+ ",timerStartTime = " + timerStartTime + ",packetSendLess=" + packetSendLess);

		//
		if (  ( packetSendLess < 0 && less >= (heartBeatInterval - 2 * incIntervalTime)   ) || errCode == 104   ) {
			// 这里是防止长时间，socket直接断开，但是又没有实际的值可以量化，只能设置大概值
			// socket直接断开，一般都会等心跳时钟到期才知道。
			reciveTimeoutHeartBeat(0);
		}
		printfManager("reciveConnectRunError----end---");
	}

	/***
	 * 心跳发送超时
	 * 
	 * @param packetSeq
	 */
	public void reciveTimeoutHeartBeat(int packetSeq) {

		printfManager("reciveTimeoutHeartBeat----start---");
		if (probeIsStop == true) {
			MiLinkLog.d(TAG, "probeIsStop is true do nothing reciveTimeoutHeartBeat");
			return;
		}

		long currentTime = System.currentTimeMillis();
		if ((currentTime - lastReviveTimeoutHbTime) < heartBeatInterval) {// 每个心跳周期内，只接收一个心跳超时的包

			MiLinkLog.e(TAG,
					"reciveTimeoutHeartBeat but do nothing, currentTime = " + currentTime
							+ ", lastReviveTimeoutHbTime = " + lastReviveTimeoutHbTime + ",less = "
							+ (currentTime - lastReviveTimeoutHbTime) + " < heartBeatInterval" + heartBeatInterval);

			return;
		}

		if (currHeartBeatInfo == null) {
			currHeartBeatInfo = new HeartBeatInfo();
		}

		currHeartBeatInfo.setOk(false);
		MiLinkLog.e(TAG,
				"recive TimeoutHeartBeat.packetSeq=" + packetSeq + ",currHeartBeatInfo = " + currHeartBeatInfo);
		putHBInfoInList(currHeartBeatInfo);
		startAanalysisHeartBeat();

		lastReviveTimeoutHbTime = currentTime;
		printfManager("reciveTimeoutHeartBeat----end---");
	}

	private void putHBInfoInList(HeartBeatInfo hbi) {

		if (hbi.getSeq() != 0)
			packetList.remove(hbi);
		
		try {
			packetList.offer(hbi);
			while (true) {
				int size = packetList.size();
				if (size <= maxListElemet) {
					// 队列长度超过N个，删除
					MiLinkLog.d(TAG, "packetList size = " + size);
					break;
				}
				MiLinkLog.d(TAG, "packetList poll element.size = " + size);
				packetList.poll();
			}

			if (packetList.size() > 0) {
				for (HeartBeatInfo h : packetList) {
					MiLinkLog.d(TAG, "PacketListInfo = " + h);
				}
			}
		} catch (Throwable t) {
			MiLinkLog.e(TAG, "putHBInfoInList error, err" + t.getMessage());
		}
	}

	private void startAanalysisHeartBeat() {

		// 因为失败之后会直接重连，清空了所有数据，所以如果有失败的，则就可以认为网络不可用了
		int size = packetList.size();
		MiLinkLog.d(TAG, "currunt modle = " + modle);
		if (size != maxListElemet) {

			// 如果全部都是false，就直接衰减
			if (size > 0) {

				// boolean tmpFlag = false;
				//
				// for (HeartBeatInfo hbi : packetList) {
				//
				// boolean isOk = hbi.isOk();
				// if (isOk == true) {
				// tmpFlag = true;
				// }
				// }

				ListPacketStatusEnum listStatus = getPacketStatus();
				//
				// MiLinkLog.i(TAG,
				// "list packet status is =" + listStatus + " modle = " + modle
				// + ",packet size = " + size);
				//
				// // if (tmpFlag == false) {
				if (listStatus == ListPacketStatusEnum.ALLFAILED) {
					probeFailedPoint = true;
					MiLinkLog.d(TAG, "probeFailedPoint = " + probeFailedPoint);
				}
				//
				// MiLinkLog.i(TAG, "packetList emel all is false,start dec
				// hbInterval");
				// modle = HeartBeatModEnum.DEC;
				// heartBeatInterval = heartBeatInterval - incIntervalTime;
				// if (heartBeatInterval < minHeartBeatInterval ||
				// heartBeatInterval == 0) {
				//
				// heartBeatInterval = minHeartBeatInterval;
				// }
				// MiLinkLog.i(TAG,
				// "--find all hb status error.heartBeatInterval=" +
				// heartBeatInterval + ",modle = " + modle);
				//
				// // 更新时钟
				// registerAlarmClock();
				// clearHeartBeatManagerInfo();
				// }

				MiLinkLog.d(TAG, "packetLise size = " + size + ",do nothing");
			}
			return;
		}

		// boolean hbCut = true;
		// for (HeartBeatInfo hbi : packetList) {// 全部成功，全部失败，一个成功，一个失败
		//
		// boolean isOk = hbi.isOk();
		// MiLinkLog.d(TAG, "packetList info:" + hbi.toString());
		// if (isOk == false) {
		// hbCut = false;
		// }
		// }

		ListPacketStatusEnum listStatus = getPacketStatus();
		MiLinkLog.i(TAG, "list packet status is =" + listStatus + " modle = " + modle);
		if (listStatus == ListPacketStatusEnum.ALLSUCCESS) {
			probeFailedPoint = false;// 不需要继续探测这个点
			// 增序列里面,都是成功,则增加心跳时间
			if (modle == HeartBeatModEnum.INC) {
				// 先保存
				saveConfig();
				if (heartBeatInterval >= maxHeartBeatInterval) {// 已经是最大值的位置探测
					probeIsStop = true;// 探测停止，就使用它了
					registerAlarmClock();
					clearHeartBeatManagerInfo(true);
					clearPacketList();
					modle = HeartBeatModEnum.INC;
					MiLinkLog.i(TAG,
							"probeIsStop max come heartBeatInterval register alarm time = " + heartBeatInterval);
					return;
				}

				heartBeatInterval = heartBeatInterval + incIntervalTime;
				if (heartBeatInterval >= maxHeartBeatInterval) {
					heartBeatInterval = maxHeartBeatInterval;
				}

				// lastHeartBeatInfo = currHeartBeatInfo;
				registerAlarmClock();
				clearHeartBeatManagerInfo(false);
				clearPacketList();
				MiLinkLog.d(TAG, "inc heartBeatInterval value = " + heartBeatInterval + ",lastHeartBeatInfo="
						+ lastHeartBeatInfo);
			}

			// 减序列里面，遇到成功，就停止继续探测，固定这个数据了。
			if (modle == HeartBeatModEnum.DEC) {

				saveConfig();
				probeIsStop = true;// 探测停止，就使用它了
				registerAlarmClock();
				clearHeartBeatManagerInfo(true);
				clearPacketList();
				modle = HeartBeatModEnum.INC;// 已经到顶了，需要向下递减了
				MiLinkLog.i(TAG, "probeIsStop  register alarm time = " + heartBeatInterval);
			}
		}

		if (listStatus == ListPacketStatusEnum.ALLFAILED) {

			modle = HeartBeatModEnum.DEC;
			heartBeatInterval = heartBeatInterval - incIntervalTime;
			if (heartBeatInterval < minHeartBeatInterval || heartBeatInterval == 0) {

				heartBeatInterval = minHeartBeatInterval;
			}
			MiLinkLog.i(TAG, "find all hb status error.heartBeatInterval=" + heartBeatInterval + " modle = " + modle);
			saveConfig();
			// 更新时钟
			registerAlarmClock();
			clearHeartBeatManagerInfo(false);
			clearPacketList();
			probeFailedPoint = false;// 不需要继续探测这个点
		}
		if (listStatus == ListPacketStatusEnum.BLENDSTATUS) {
			MiLinkLog.i(TAG, "list packet status is =" + listStatus + " modle = " + modle + " do nothing.");
			probeFailedPoint = true;// 还需要继续探测这个点
		}

		//
		// if (hbCut == true) {
		//
		// // 增序列里面,都是成功,则增加心跳时间
		// if (modle == HeartBeatModEnum.INC) {
		//
		// // 先保存
		// saveConfig();
		// if (heartBeatInterval >= maxHeartBeatInterval) {// 已经是最大值的位置探测
		// probeIsStop = true;// 探测停止，就使用它了
		// registerAlarmClock();
		// clearHeartBeatManagerInfo();
		// modle = HeartBeatModEnum.INC;
		// MiLinkLog.i(TAG,
		// "probeIsStop max come heartBeatInterval register alarm time = " +
		// heartBeatInterval);
		// }
		//
		// heartBeatInterval = heartBeatInterval + incIntervalTime;
		// if (heartBeatInterval >= maxHeartBeatInterval) {
		// heartBeatInterval = maxHeartBeatInterval;
		// }
		//
		// registerAlarmClock();
		// clearHeartBeatManagerInfo();
		// MiLinkLog.d(TAG, "inc heartBeatInterval value = " +
		// heartBeatInterval);
		// }
		//
		// // 减序列里面，遇到成功，就停止继续探测，固定这个数据了。
		// if (modle == HeartBeatModEnum.DEC) {
		//
		// saveConfig();
		// probeIsStop = true;// 探测停止，就使用它了
		// registerAlarmClock();
		// clearHeartBeatManagerInfo();
		// modle = HeartBeatModEnum.INC;// 已经到顶了，需要向下递减了
		// MiLinkLog.i(TAG, "probeIsStop register alarm time = " +
		// heartBeatInterval);
		// }
		// }
		//
		// if (hbCut == false) {
		//
		// modle = HeartBeatModEnum.DEC;
		// heartBeatInterval = heartBeatInterval - incIntervalTime;
		// if (heartBeatInterval < minHeartBeatInterval || heartBeatInterval ==
		// 0) {
		//
		// heartBeatInterval = minHeartBeatInterval;
		// }
		// MiLinkLog.i(TAG, "find all hb status error.heartBeatInterval=" +
		// heartBeatInterval + " modle = " + modle);
		//
		// // 更新时钟
		// registerAlarmClock();
		// clearHeartBeatManagerInfo();
		// }

	}

	private ListPacketStatusEnum getPacketStatus() {

		boolean findSucc = false;
		boolean findFailed = false;

		for (HeartBeatInfo hbi : packetList) {// 全部成功，全部失败，一个成功，一个失败
			boolean status = hbi.isOk();

			if (status == true) {
				findSucc = true;
			} else {
				findFailed = true;
			}
		}
		if (findSucc == true && findFailed == true) {

			return ListPacketStatusEnum.BLENDSTATUS;
		}
		if (findSucc == true) {
			return ListPacketStatusEnum.ALLSUCCESS;
		}
		if (findFailed == true) {
			return ListPacketStatusEnum.ALLFAILED;
		}
		return ListPacketStatusEnum.ALLSUCCESS;
	}

	public void registerAlarmClock() {

		AlarmClockService.stop();
		AlarmClockService.start(heartBeatInterval);
		MiLinkLog.i(TAG, "re registerALarmClock time=" + heartBeatInterval);
	}

	private class HeartBeatInfo {

		private int seq;

		private long sendTime;
		private boolean isOk;

		public boolean isOk() {
			return isOk;
		}

		public void setOk(boolean isOk) {
			this.isOk = isOk;
		}

		public long getSendTime() {
			return sendTime;
		}

		public int getSeq() {
			return seq;
		}

		public void setSeq(int seq) {
			this.seq = seq;
		}

		public void setSendTime(long sendTime) {
			this.sendTime = sendTime;
		}

		@Override
		public String toString() {
			return "[seq:" + seq + ",isOk:" + isOk + " sendTime:" + sendTime + "]";
		}

		@Override
        public boolean equals(Object other) { // 重写equals方法，后面最好重写hashCode方法

			if (this == other) // 先检查是否其自反性，后比较other是否为空。这样效率高
				return true;
			if (other == null)
				return false;
			if (!(other instanceof HeartBeatInfo))
				return false;

			final HeartBeatInfo cat = (HeartBeatInfo) other;

			if (getSeq() != cat.getSeq()) {
				return false;
			}

			return true;
		}
	}

	public enum HeartBeatModEnum {
		INC, DEC;
	}

	public enum ListPacketStatusEnum {

		ALLSUCCESS, ALLFAILED, BLENDSTATUS;
	}
}


package com.mi.milink.sdk.session.persistent;

import android.util.Log;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mi.milink.sdk.data.Const;
import com.mi.milink.sdk.debug.InternalDataMonitor;
import com.mi.milink.sdk.debug.MiLinkLog;
import com.mi.milink.sdk.event.MiLinkEvent.*;
import com.mi.milink.sdk.event.MiLinkEventForSimpleChannel;
import com.mi.milink.sdk.proto.PushPacketProto.KickMessage;
import com.mi.milink.sdk.proto.PushPacketProto.MilinkLogReq;
import com.mi.milink.sdk.proto.PushPacketProto.PushLogLevel;
import com.mi.milink.sdk.proto.PushPacketProto.SimplePushData;
import com.mi.milink.sdk.session.simplechannel.SessionForSimpleChannel;

import org.greenrobot.eventbus.EventBus;

public class MnsCodeCopeWaysWithPush extends IMnsCodeCopeWays {

	private static final String CLASSTAG = "MnsCodeCopeWaysWithPush";

	private String TAG;

	private long mBeginStatisticsTime = 0; // 开始统计的时间

	private int mRecvPushNumber = 0;// 一定时间间隔内收到的push包

	private long mLastRecvPushTimeStamp = 0;// 上一次收到push的时间戳

	private int mLargeIntervalNumber = 0; // 大间隔push的个数

	public static final long CHANNEL_BUSY_FLAG_INTERVAL = 3000;

	public static final long CHANNEL_BUSY_FLAG_NUMBER = 150;

	public static final long CHANNEL_BUSY_CANCEL_FLAG_NUMBER = 3;

	public MnsCodeCopeWaysWithPush(Session session) {
		super(session);
		TAG = String.format("[No:%d]%s", session.getSessionNO(), CLASSTAG);
	}

	@Override
	protected void onOk() {
		Log.w("wangchuntao","onRecvDownStream  onOk");
		// push以及超时response扔给dispatcher
		MiLinkLog.v(TAG, "recv data and to dispatcher");
		if (Const.MnsCmd.MNS_KICK_CMD.equals(mRecvData.getCommand())) {
			MiLinkLog.v(TAG, "get kick push");
			try {
				SimplePushData sp = SimplePushData.parseFrom(mRecvData.getData());
				KickMessage kickMsg = KickMessage.parseFrom(sp.getPushdata());
				EventBus.getDefault()
						.post(new ServerNotificationEvent(ServerNotificationEvent.EventType.KickByServer, kickMsg));
			} catch (InvalidProtocolBufferException e) {
				MiLinkLog.e(TAG, e.getMessage());
			}
		} else if (Const.MnsCmd.MNS_MILINK_PUSH_LOG.equals(mRecvData.getCommand())) {
			try {
				SimplePushData sp = SimplePushData.parseFrom(mRecvData.getData());
				MilinkLogReq logReq = MilinkLogReq.parseFrom(sp.getPushdata());

				int type = logReq.getType();
				if (Const.PushLogType.PUSH_UPLOADLOG == type) {
					EventBus.getDefault().post(
							new ServerNotificationEvent(ServerNotificationEvent.EventType.requireUploadLog, logReq));
					// 请求上传日志
					MnsPacketDispatcher.getInstance().dispatchPacket(mRecvData);
				} else if (Const.PushLogType.PUSH_LOGLEVEL == type) {
					PushLogLevel logLevelReq = logReq.getLogLevel();
					MiLinkLog.v(TAG, "recv push log level,loglevel=" + logLevelReq.getLoglevel() + ",time="
							+ logLevelReq.getTimeLong());
					EventBus.getDefault().post(new ServerNotificationEvent(
							ServerNotificationEvent.EventType.requireChannelLogLevel, logLevelReq));
				}

			} catch (InvalidProtocolBufferException e) {
				MiLinkLog.e(TAG, e.getMessage());
			}
		} else {
			// estimateChannelBusy();
			MnsPacketDispatcher.getInstance().dispatchPacket(mRecvData);
			int seq = mRecvData.getSeqNo();
			if (seq < 0) {
				// 发个不需要回复的ack包
				mSession.pushAck(seq);
				mRecvData.setSeqNo(0);
			}
		}
		mRetCode = mRecvData.getBusiCode();
	}

	/* 改为在收到recvbuffer时判断 */
	private void estimateChannelBusy() {
		long now = System.currentTimeMillis();
		if (now - mBeginStatisticsTime < CHANNEL_BUSY_FLAG_INTERVAL) {
			mRecvPushNumber++;
			if (mRecvPushNumber > CHANNEL_BUSY_FLAG_NUMBER) {
				// 请求进入通道忙状态
				EventBus.getDefault()
						.post(new ChannelStatusChangeEvent(ChannelStatusChangeEvent.EventType.channelBusy, null));
			}
		} else {
			if (mRecvPushNumber > CHANNEL_BUSY_FLAG_NUMBER) {
				// 已经是通道忙状态
			} else {
				mBeginStatisticsTime = now;
				mRecvPushNumber = 0;
			}
		}

		if (now - mLastRecvPushTimeStamp > CHANNEL_BUSY_FLAG_INTERVAL) {
			mLargeIntervalNumber++;
			if (mLargeIntervalNumber > CHANNEL_BUSY_CANCEL_FLAG_NUMBER) {
				// 可以变成空闲模式
				mBeginStatisticsTime = 0;
				mRecvPushNumber = 0;
				// 请求进入通道空闲状态
				EventBus.getDefault()
						.post(new ChannelStatusChangeEvent(ChannelStatusChangeEvent.EventType.channelIdle, null));
			}
		} else {
			// 无法改变现状
			mLargeIntervalNumber = 0;
		}
		mLastRecvPushTimeStamp = now;
	}

	@Override
	protected void onB2TokenExpired() {

		if (mSession instanceof SessionForSimpleChannel) {
			SessionForSimpleChannel s = (SessionForSimpleChannel) mSession;
			s.getSessionManagerForSimpleChannel().getChannelEventBus()
					.post(new MiLinkEventForSimpleChannel.ServerNotificationEvent(
							MiLinkEventForSimpleChannel.ServerNotificationEvent.EventType.B2tokenExpired));
		} else {

			EventBus.getDefault().post(new ServerNotificationEvent(ServerNotificationEvent.EventType.B2tokenExpired));
		}
	}

	@Override
	protected void onServerTokenExpired() {
		if (mSession instanceof SessionForSimpleChannel) {
			// EventBus.getDefault().post(new
			// MiLinkEventForSimpleChannel.ServerNotificationEvent(
			// MiLinkEventForSimpleChannel.ServerNotificationEvent.EventType.ServiceTokenExpired));
			return;
		} else {
			EventBus.getDefault()
					.post(new ServerNotificationEvent(ServerNotificationEvent.EventType.ServiceTokenExpired));
		}
	}

	// @Override
	// protected void onShouldCheckUpdate() {
	// if (mSession instanceof SessionForSimpleChannel) {
	// EventBus.getDefault().post(new
	// MiLinkEventForSimpleChannel.ServerNotificationEvent(
	// MiLinkEventForSimpleChannel.ServerNotificationEvent.EventType.ShouldUpdate));
	// } else {
	// EventBus.getDefault().post(new
	// ServerNotificationEvent(ServerNotificationEvent.EventType.ShouldUpdate));
	// }
	// }

	@Override
	protected void afterHandle() {
		if (mRecvData.isPushPacket()) {
			InternalDataMonitor.getInstance()
					.trace((mSession.getServerProfileForStatistic() != null
							? mSession.getServerProfileForStatistic().getServerIP() : ""),
							(mSession.getServerProfileForStatistic() != null
									? mSession.getServerProfileForStatistic().getServerPort() : 0),
							mRecvData.isPushPacket() ? Const.MnsCmd.MNS_PUSH_CMD + "." + mRecvData.getCommand()
									: mRecvData.getCommand(),
							mRetCode, System.currentTimeMillis(), System.currentTimeMillis(), 0,
							mRecvData.getResponseSize(), mRecvData.getSeqNo());
		}
	}

	@Override
	protected void onBusinessCmdTimeout() {

	}

	@Override
	protected void onInternalCmdTimeout() {

	}

	@Override
	protected void onAccNeedRetry() {

	}

	@Override
	protected void onKickedByServer() {

	}

	@Override
	protected void onUnknowMsnCode(int mnsCode) {

	}

	@Override
	protected void onUpdateChannelPubKey() {
		// TODO Auto-generated method stub

	}
}


package com.mi.milink.sdk.session.persistent;

import android.util.Log;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mi.milink.sdk.aidl.PacketData;
import com.mi.milink.sdk.data.Const;
import com.mi.milink.sdk.debug.MiLinkLog;
import com.mi.milink.sdk.event.MiLinkEvent.ServerNotificationEvent;
import com.mi.milink.sdk.event.MiLinkEventForSimpleChannel;
import com.mi.milink.sdk.proto.PushPacketProto.KickMessage;
import com.mi.milink.sdk.proto.SystemPacketProto.MnsCmdChannelNewPubKeyRsp;
import com.mi.milink.sdk.session.common.Request;
import com.mi.milink.sdk.session.simplechannel.SessionForSimpleChannel;
import com.mi.milink.sdk.session.simplechannel.UpdateChannelPubKeyValue;

import org.greenrobot.eventbus.EventBus;

public abstract class IMnsCodeCopeWays {
	public static String TAG = "IMnsCodeCopeWays";

	protected Request mRequeset;

	protected PacketData mRecvData;

	protected Session mSession;

	protected int mRetCode;

	public IMnsCodeCopeWays(Session session) {
		this.mSession = session;
	}

	public void setParam(PacketData recvData, Request request) {
		this.mRecvData = recvData;
		this.mRequeset = request;
	}

	protected abstract void onOk();

	protected void onB2TokenExpired() {
		if (mSession instanceof SessionForSimpleChannel) {
			SessionForSimpleChannel s = (SessionForSimpleChannel)mSession;
			s.getSessionManagerForSimpleChannel().getChannelEventBus().post(new MiLinkEventForSimpleChannel.ServerNotificationEvent(
					MiLinkEventForSimpleChannel.ServerNotificationEvent.EventType.B2tokenExpired));
		} else {
			EventBus.getDefault().post(new ServerNotificationEvent(ServerNotificationEvent.EventType.B2tokenExpired));
		}
	}

	protected void onUpdateChannelPubKey() {

		MiLinkLog.v(TAG, "onUpdateChannelPubKey handler");
		if (mRecvData != null && mRecvData.getData() != null) {
			try {
				MnsCmdChannelNewPubKeyRsp channelNewPubkey = MnsCmdChannelNewPubKeyRsp.parseFrom(mRecvData.getData());

				if (mSession instanceof SessionForSimpleChannel) {

					UpdateChannelPubKeyValue channelPubKeyValue = new UpdateChannelPubKeyValue();
					channelPubKeyValue.setChannelNewPubkey(channelNewPubkey);
					channelPubKeyValue.setmRequeset(mRequeset);
					SessionForSimpleChannel s = (SessionForSimpleChannel)mSession;
					s.getSessionManagerForSimpleChannel().getChannelEventBus()
							.post(new MiLinkEventForSimpleChannel.ServerNotificationEvent(
									MiLinkEventForSimpleChannel.ServerNotificationEvent.EventType.ChannelPubKeyUpdate,
									channelPubKeyValue));

				} else {
					EventBus.getDefault().post(new ServerNotificationEvent(
							ServerNotificationEvent.EventType.ChannelPubKeyUpdate, channelNewPubkey));
				}
			} catch (Exception e) {
			}
		}
	}

	protected void onDeleteChannelPubKey() {

		if (mSession instanceof SessionForSimpleChannel) {
			SessionForSimpleChannel s = (SessionForSimpleChannel)mSession;
			s.getSessionManagerForSimpleChannel().getChannelEventBus()
			.post(new MiLinkEventForSimpleChannel.ServerNotificationEvent(
					MiLinkEventForSimpleChannel.ServerNotificationEvent.EventType.ChannelDelPubKey, mRequeset));
		} else {
		}
	}

	protected abstract void onServerTokenExpired();

	protected abstract void afterHandle();

	protected void onTimeOut() {
		if (mRequeset != null) {
			if (mRequeset.isInternalRequest()) {
				onInternalCmdTimeout();
			} else {
				onBusinessCmdTimeout();
			}
		}
	}

	/**
	 * 内部命令字超时
	 */
	protected abstract void onInternalCmdTimeout();

	/**
	 * 业务命令字超时
	 */
	protected abstract void onBusinessCmdTimeout();

	protected abstract void onAccNeedRetry();

	protected abstract void onUnknowMsnCode(int mnsCode);

	protected void onKickedByServer() {
		try {
			KickMessage kickMsg = KickMessage.parseFrom(mRecvData.getData());
			EventBus.getDefault()
					.post(new ServerNotificationEvent(ServerNotificationEvent.EventType.KickByServer, kickMsg));
		} catch (InvalidProtocolBufferException e) {
			MiLinkLog.e(TAG, e);
			MiLinkLog.e(TAG, "kick but InvalidProtocolBufferException construct a message and post.");
			int time = (int) (System.currentTimeMillis() / 1000);
			KickMessage kickMsg = KickMessage.newBuilder().setDevice("unknowdevices").setTime(time).setType(10).build();
			EventBus.getDefault()
					.post(new ServerNotificationEvent(ServerNotificationEvent.EventType.KickByServer, kickMsg));
		}
	}

	protected void onServerSpecialLineBroken() {
		mRequeset.onDataSendFailed(mRecvData.getMnsCode(), "MI_LINK_CODE_SERVER_SPECIAL_LINE_BROKEN");
		mSession.checkExceedMaxContinuousRecv110Count();
	}

	protected void onServerSpecialLineBrokenUrgent() {
		MiLinkLog.e(TAG, "onServerSpecialLineBrokenUrgent");
		mRequeset.onDataSendFailed(mRecvData.getMnsCode(), "MI_LINK_CODE_SERVER_SPECIAL_LINE_BROKEN_URGENT");
		
		if (mSession instanceof SessionForSimpleChannel) {
			SessionForSimpleChannel s = (SessionForSimpleChannel)mSession;
			s.getSessionManagerForSimpleChannel().getChannelEventBus()
			.post(new MiLinkEventForSimpleChannel.ServerNotificationEvent(
					MiLinkEventForSimpleChannel.ServerNotificationEvent.EventType.ServerLineBroken, mRequeset));
		} else {
			EventBus.getDefault().post(new ServerNotificationEvent(ServerNotificationEvent.EventType.ServerLineBroken));	
		}
	}

	public void handleMnsCode() {
		Log.w("wangchuntao","handleMnsCode ");
		int mnsCode = Math.abs(mRecvData.getMnsCode());
		mRetCode = mnsCode;
		// 如果是handshake命令字，则直接忽略，不计数，它不会触发连接保底ip的逻辑，计数也没有意义，因opensession会重置。
		if (!Const.MnsCmd.MNS_HAND_SHAKE.equals(mRecvData.getCommand())) {
			if (mnsCode == Const.MiLinkCode.MI_LINK_CODE_SERVER_SPECIAL_LINE_BROKEN) {
				mSession.addContinuousRecv110Count();
			} else {
				mSession.resetContinuousRecv110Count();
			}
		}

		// 防止发送logoff命令字时的死循环
		if (Const.MnsCmd.MNS_LOGOFF.equals(mRecvData.getCommand()) && mnsCode != Const.MiLinkCode.MI_LINK_CODE_OK) {
			if (mRequeset != null) {
				mRequeset.onDataSendFailed(mRecvData.getMnsCode(), "MNS_LOGOFF failed, return");
			}
			return;
		}
		switch (mnsCode) {
		case Const.MiLinkCode.MI_LINK_CODE_OK:
			onOk();
			break;
		case Const.MiLinkCode.MI_LINK_CODE_B2_TOKEN_EXPIRED:
			onB2TokenExpired();
			break;
		case Const.MiLinkCode.MI_LINK_CODE_SERVICE_TOKEN_EXPIRED:
			onServerTokenExpired();
			break;
		// case Const.MiLinkCode.MI_LINK_CODE_SHOULD_CHECK_UPDATE:
		// onShouldCheckUpdate();
		// break;
		case Const.MiLinkCode.MI_LINK_CODE_TIMEOUT:
			onTimeOut();
			break;
		case Const.MiLinkCode.MI_LINK_CODE_KICKED_BY_SERVER:
			onKickedByServer();
			break;
		case Const.MiLinkCode.MI_LINK_CODE_ACC_NEED_RETRY:
			onAccNeedRetry();
			break;
		case Const.MiLinkCode.MI_LINK_CODE_SERVER_SPECIAL_LINE_BROKEN:
			onServerSpecialLineBroken();
			break;
		case Const.MiLinkCode.MI_LINK_CODE_SERVER_SPECIAL_LINE_BROKEN_URGENT:
			onServerSpecialLineBrokenUrgent();
			break;
		case Const.MiLinkCode.MI_LINK_CODE_SERVER_UPADTE_CHANNEL_PUB_KEY:
			onUpdateChannelPubKey();
			break;
		case Const.MiLinkCode.MI_LINK_CODE_SERVER_DELETE_CHANNEL_PUB_KEY:
			onDeleteChannelPubKey();
			break;
		default:
			// 不认识的包
			onUnknowMsnCode(mnsCode);
			break;
		}
		afterHandle();
		mRecvData = null;
		mRequeset = null;
	}

}

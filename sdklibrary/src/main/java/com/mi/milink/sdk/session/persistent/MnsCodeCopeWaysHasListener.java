
package com.mi.milink.sdk.session.persistent;

import com.mi.milink.sdk.debug.InternalDataMonitor;
import com.mi.milink.sdk.debug.MiLinkLog;
import com.mi.milink.sdk.event.MiLinkEvent.ServerNotificationEvent;
import com.mi.milink.sdk.session.simplechannel.SessionForSimpleChannel;

import android.text.TextUtils;
import org.greenrobot.eventbus.EventBus;
public class MnsCodeCopeWaysHasListener extends IMnsCodeCopeWays {

    private static final String CLASSTAG = "MnsCodeCopeWaysHasListener";

    private String TAG;

    public MnsCodeCopeWaysHasListener(Session session) {
        super(session);
        TAG = String.format("[No:%d]%s", session.getSessionNO(), CLASSTAG);
    }

    @Override
    protected void onOk() {
        mRequeset.onDataSendSuccess(0, mRecvData);
        mRetCode = mRecvData.getBusiCode();
    }

//    @Override
//    protected void onB2TokenExpired() {
        
//        mRequeset.getListener().onDataSendFailed(101, "b2 token expired");
//    }

    @Override
    protected void onServerTokenExpired() {
    	
    	if(mSession instanceof SessionForSimpleChannel) {
//    		EventBus.getDefault().post(
//                    new MiLinkEventForSimpleChannel.ServerNotificationEvent(MiLinkEventForSimpleChannel.ServerNotificationEvent.EventType.ServiceTokenExpired));
    		return ;
    	}else {
    		 EventBus.getDefault().post(
    	                new ServerNotificationEvent(ServerNotificationEvent.EventType.ServiceTokenExpired));
    	}
    	
       
        mRequeset.onDataSendFailed(100, "service token expired");
    }

//    @Override
//    protected void onShouldCheckUpdate() {
//    	if(mSession instanceof SessionForSimpleChannel) {
//    		EventBus.getDefault().post(
//                    new MiLinkEventForSimpleChannel.ServerNotificationEvent(MiLinkEventForSimpleChannel.ServerNotificationEvent.EventType.ShouldUpdate));
//    	}else {
//    		EventBus.getDefault().post(
//                    new ServerNotificationEvent(ServerNotificationEvent.EventType.ShouldUpdate));
//    	}
//        
//        mRequeset.getListener().onDataSendSuccess(103, mRecvData);
//    }
    

    @Override
    protected void afterHandle() {
        String accIp = mSession.getServerProfileForStatistic() != null ? mSession
                .getServerProfileForStatistic().getServerIP() : "";
        int accPort = mSession.getServerProfileForStatistic() != null ? mSession
                .getServerProfileForStatistic().getServerPort() : 0;
        String cmd = mRecvData.getCommand();
        int retCode = mRetCode;
        long sentTime = mRequeset.getSentTime();
        long responseTime = System.currentTimeMillis();
        int reqSize = mRequeset.getSize();
        int responseSize = mRecvData.getResponseSize();
        int seqNo = mRequeset.getSeqNo();
        String clientIp = mSession.getClientIp();
        String clientIsp = mSession.getClientIsp();
        // 现在会出现回包中cmd为空的情况
        if (mRequeset.getAfterHandleCallBack() != null) {
            mRequeset.getAfterHandleCallBack().onCallBack(accIp, accPort, cmd, retCode, sentTime,
                    responseTime, reqSize, responseSize, seqNo, clientIp, clientIsp);
            return;
        }
        if (TextUtils.isEmpty(cmd)) {
            cmd = mRequeset.getData() != null ? mRequeset.getData().getCommand() : "";
        }
        if (!TextUtils.isEmpty(cmd)) {
            InternalDataMonitor.getInstance().trace(accIp, accPort, cmd, retCode, sentTime,
                    responseTime, reqSize, responseSize, seqNo, clientIp, clientIsp);
        } else {
            MiLinkLog.e(TAG, "cmd is empty, don't monitor it, seq=" + mRequeset.getSeqNo());
        }
    }

    @Override
    protected void onBusinessCmdTimeout() {
        mRequeset.onDataSendFailed(109, "request time out");
    }

    @Override
    protected void onInternalCmdTimeout() {
        mRequeset.onDataSendFailed(109, "request time out");
    }

    @Override
    protected void onAccNeedRetry() {
        mSession.onAccNeedRetryWithClientInfo(mRequeset);
    }

    @Override
    protected void onUnknowMsnCode(int mnsCode) {
        mRequeset.onDataSendFailed(mnsCode, "unknow mnscode for milinksdk");
    }

//	@SuppressLint("UseSparseArrays")
//	@Override
//	protected void onUpdateChannelPubKey() {
//        
//		MiLinkLog.v(TAG, "onUpdateChannelPubKey handler");
//		if (mRecvData != null && mRecvData.getData() != null) {
//			try {
//				MnsCmdChannelNewPubKeyRsp channelNewPubkey = MnsCmdChannelNewPubKeyRsp.parseFrom(mRecvData.getData());
//
//				if (mSession instanceof SessionForSimpleChannel) {
//					EventBus.getDefault()
//							.post(new MiLinkEventForSimpleChannel.ServerNotificationEvent(
//									MiLinkEventForSimpleChannel.ServerNotificationEvent.EventType.ChannelPubKeyUpdate,
//									channelNewPubkey));
//				} else {
//					EventBus.getDefault().post(new ServerNotificationEvent(
//							ServerNotificationEvent.EventType.ChannelPubKeyUpdate, channelNewPubkey));
//				}
//
//				mRequeset.getListener().onDataSendFailed(129, "channel pub key need update");
//
//			} catch (Exception e) {
//			}
//		}
//	
//	}

}

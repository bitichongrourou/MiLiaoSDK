
package com.mi.milink.sdk.session.persistent;

import com.mi.milink.sdk.debug.InternalDataMonitor;
import com.mi.milink.sdk.debug.MiLinkLog;
import com.mi.milink.sdk.event.MiLinkEvent.ServerNotificationEvent;
import com.mi.milink.sdk.session.simplechannel.SessionForSimpleChannel;

import android.text.TextUtils;
import org.greenrobot.eventbus.EventBus;
public class MnsCodeCopeWaysNoListener extends IMnsCodeCopeWays {
    private static final String CLASSTAG = "MnsCodeCopeWaysNoListener";

    private String TAG;

    public MnsCodeCopeWaysNoListener(Session session) {
        super(session);
        TAG = String.format("[No:%d]%s", session.getSessionNO(), CLASSTAG);
    }

    @Override
    protected void onOk() {
        // push以及response扔给dispatcher
        MiLinkLog.v(TAG, "recv data and to dispatcher");
        MnsPacketDispatcher.getInstance().dispatchPacket(mRecvData);
        mRetCode = mRecvData.getBusiCode();
    }

//    @Override
//	protected void onB2TokenExpired() {
//
//		if (mSession instanceof SessionForSimpleChannel) {
//			EventBus.getDefault().post(new MiLinkEventForSimpleChannel.ServerNotificationEvent(
//					MiLinkEventForSimpleChannel.ServerNotificationEvent.EventType.B2tokenExpired));
//		} else {
//
//			EventBus.getDefault().post(new ServerNotificationEvent(ServerNotificationEvent.EventType.B2tokenExpired));
//		}
//	}

    @Override
    protected void onServerTokenExpired() {
    	if (mSession instanceof SessionForSimpleChannel) {
//    		EventBus.getDefault().post(new MiLinkEventForSimpleChannel.ServerNotificationEvent(
//					MiLinkEventForSimpleChannel.ServerNotificationEvent.EventType.ServiceTokenExpired));
    		return ;
    	}else{
        EventBus.getDefault().post(
                new ServerNotificationEvent(ServerNotificationEvent.EventType.ServiceTokenExpired));
    	}
    }

//    @Override
//	protected void onShouldCheckUpdate() {
//		if (mSession instanceof SessionForSimpleChannel) {
//			EventBus.getDefault().post(new MiLinkEventForSimpleChannel.ServerNotificationEvent(
//					MiLinkEventForSimpleChannel.ServerNotificationEvent.EventType.ShouldUpdate));
//		} else {
//			EventBus.getDefault().post(new ServerNotificationEvent(ServerNotificationEvent.EventType.ShouldUpdate));
//		}
//	}

    @Override
    protected void afterHandle() {
        String cmd = mRecvData.getCommand();
        if (TextUtils.isEmpty(cmd)) {
            cmd = mRequeset.getData() != null ? mRequeset.getData().getCommand() : "";
        }
        if (!TextUtils.isEmpty(cmd)) {
            InternalDataMonitor.getInstance().trace(
                    (mSession.getServerProfileForStatistic() != null ? mSession
                            .getServerProfileForStatistic().getServerIP() : ""),
                    (mSession.getServerProfileForStatistic() != null ? mSession
                            .getServerProfileForStatistic().getServerPort() : 0), cmd, mRetCode,
                    mRequeset.getSentTime(), System.currentTimeMillis(), mRequeset.getSize(),
                    mRecvData.getResponseSize(), mRequeset.getSeqNo());
        } else {
            MiLinkLog.e(TAG, "cmd is empty, don't monitor it, seq=" + mRequeset.getSeqNo());
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
        mSession.onAccNeedRetryWithClientInfo(mRequeset);
    }

    @Override
    protected void onUnknowMsnCode(int mnsCode) {

    }

	@Override
	protected void onUpdateChannelPubKey() {
		// TODO Auto-generated method stub
		
	}
}

package com.mi.milink.sdk.account.manager;

import org.greenrobot.eventbus.EventBus;

import com.mi.milink.sdk.account.ChannelAccount;
import com.mi.milink.sdk.account.IAccount;
import com.mi.milink.sdk.debug.MiLinkLog;
import com.mi.milink.sdk.event.MiLinkEventForSimpleChannel;
import com.mi.milink.sdk.session.common.StreamUtil;

import android.text.TextUtils;

public class MiChannelAccountManager {
	public static boolean hasInit = false;

	private  String TAG = MiChannelAccountManager.class.getSimpleName();

	private boolean mIsLogining = false; // 是否正在登陆

	private IAccount mCurrentAccount ;
//	private static MiChannelAccountManager INSTANCE;

	private int mCurrentAccountType = MiAccountManager.ACCOUNT_TYPE_CHANNEL;

	private EventBus channelEventBus;
	
	public MiChannelAccountManager(EventBus channelEventbus) {
	    MiLinkLog.v(TAG, "new MiChannelAccountManager()");
	    this.channelEventBus = channelEventbus;
		mCurrentAccount = new ChannelAccount();
		hasInit = true;
	}

//	public static MiChannelAccountManager getInstance() {
//		if (INSTANCE == null) {
//			synchronized (MiChannelAccountManager.class) {
//				if (INSTANCE == null) {
//					INSTANCE = new MiChannelAccountManager();
//				}
//			}
//		}
//		return INSTANCE;
//	}

	public String getUserId() {
		return mCurrentAccount.getUserId();
	}

	public void logoff() {
		mIsLogining = false;
		mCurrentAccount.logoffMiLink();
	}

	public void userLogoff() {
		channelEventBus.post(new MiLinkEventForSimpleChannel.ClientActionEvent(MiLinkEventForSimpleChannel.ClientActionEvent.EventType.ClientRequestLogoff));
	}


	public boolean milinkHasLogined() {
		String b2Token = mCurrentAccount.getB2Token();
		return !TextUtils.isEmpty(b2Token);
	}

	public synchronized boolean isLogining() {
		return mIsLogining;
	}

	public synchronized void setIsLogining(boolean isLogin) {
		mIsLogining = isLogin;
	}

	public void setUserId(String appUserId) {
		mCurrentAccount.setUserId(appUserId);
	}

	public void initUserChannelMode() {

		MiLinkLog.v(TAG, "initUseChannelMode");
		mCurrentAccount.generateServiceTokenAndSSecurity();

		// 这边是可以优化的，如果有b2Token就不用登陆了 与标准模式类似，这里加个TODO;可以只是简单回调一下登陆成功的接口
		String userId = mCurrentAccount.getUserId();
		String b2Token = mCurrentAccount.getB2Token();
		if (!TextUtils.isEmpty(userId) && !TextUtils.isEmpty(b2Token)) {
			channelEventBus.post(new MiLinkEventForSimpleChannel.ClientActionEvent(MiLinkEventForSimpleChannel.ClientActionEvent.EventType.ClientRequestCheckConnection));
			return;
		}
		channelEventBus.post(new MiLinkEventForSimpleChannel.ClientActionEvent(MiLinkEventForSimpleChannel.ClientActionEvent.EventType.ClientRequestLogin));
	}

	public byte getBusinessEncByMode() {
		switch (mCurrentAccountType) {
		case MiAccountManager.ACCOUNT_TYPE_CHANNEL:
			return StreamUtil.MNS_ENCODE_CHANNEL_B2_TOKEN;
		default:
			return StreamUtil.MNS_ENCODE_NONE;
		}
	}

	public void logoffMiLink() {
		mIsLogining = false;
		mCurrentAccount.logoffMiLink();
	}

	public boolean isChannelModCurrent() {
		return mCurrentAccountType == MiAccountManager.ACCOUNT_TYPE_CHANNEL;
	}

	public int getCurrentAccountType() {
		return mCurrentAccountType;
	}

	public IAccount getCurrentAccount() {
		return mCurrentAccount;
	}

	public void setKeepAliveTime(int keepAliveTime){
		mCurrentAccount.setKeepAliveTime(keepAliveTime);
	}
	public int getKeepAliveTime() {
		return mCurrentAccount.getKeepAliveTime();
	}

}

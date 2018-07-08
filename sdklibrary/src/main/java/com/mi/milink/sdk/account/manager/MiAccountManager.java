
package com.mi.milink.sdk.account.manager;

import org.greenrobot.eventbus.EventBus;

import com.mi.milink.sdk.account.AnonymousAccount;
import com.mi.milink.sdk.account.ChannelAccount;
import com.mi.milink.sdk.account.IAccount;
import com.mi.milink.sdk.account.MiAccount;
import com.mi.milink.sdk.base.os.timer.AlarmClockService;
import com.mi.milink.sdk.config.HeartBeatManager;
import com.mi.milink.sdk.debug.MiLinkLog;
import com.mi.milink.sdk.event.MiLinkEvent.ClientActionEvent;
import com.mi.milink.sdk.session.common.StreamUtil;

import android.text.TextUtils;

public class MiAccountManager {

    private static final String TAG = MiAccountManager.class.getSimpleName();

    public static final int ACCOUNT_TYPE_STANDARD = 0;

    public static final int ACCOUNT_TYPE_ANONYMOUS = 1;

    public static final int ACCOUNT_TYPE_CHANNEL = 2;

    private int mCurrentAccountType = ACCOUNT_TYPE_STANDARD;

    private boolean mAllowAnonymousMode = false;

    private boolean mIsLogining = false; // 是否正在登陆

    private IAccount mCurrentAccount = MiAccount.getInstance();

    private static MiAccountManager INSTANCE;

    private String deviceToken;

    private MiAccountManager() {
    }

    public static MiAccountManager getInstance() {
        if (INSTANCE == null) {
            synchronized (MiAccountManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MiAccountManager();
                }
            }
        }
        return INSTANCE;
    }

    public void setAnonymousModeSwitch(boolean on) {
        mAllowAnonymousMode = on;
    }

    public boolean isAllowAnonymousMode() {
        return mAllowAnonymousMode;
    }

    public String getUserId() {
        return mCurrentAccount.getUserId();
    }

    public void logoff() {
        mIsLogining = false;
        mCurrentAccount.logoff();
        HeartBeatManager.getInstance().saveConfig();
        if (mAllowAnonymousMode) {
            int lastMode = mCurrentAccountType;
            switchAccountTypeMode(ACCOUNT_TYPE_ANONYMOUS);
            // 如果之前是标准模式，那么现在就是匿名模式。则在主动请求登录一次
            if (lastMode == ACCOUNT_TYPE_STANDARD) {
                EventBus.getDefault().post(
                        new ClientActionEvent(ClientActionEvent.EventType.ClientRequestLogin));
            }
        }
    }

    public void userLogoff() {
        AlarmClockService.stop();
        EventBus.getDefault().post(
                new ClientActionEvent(ClientActionEvent.EventType.ClientRequestLogoff));
    }

    private void switchAccountTypeMode(int accountTypeMode) {
        MiLinkLog.d(TAG, "switchAccountTypeMode turn to " + accountTypeMode);
        switch (accountTypeMode) {
            case ACCOUNT_TYPE_STANDARD: {
                mCurrentAccountType = ACCOUNT_TYPE_STANDARD;
                mCurrentAccount = MiAccount.getInstance();
            }
                break;
            case ACCOUNT_TYPE_ANONYMOUS: {
                mCurrentAccountType = ACCOUNT_TYPE_ANONYMOUS;
                mCurrentAccount = AnonymousAccount.getInstance();
            }
                break;
            case ACCOUNT_TYPE_CHANNEL: {
                mCurrentAccountType = ACCOUNT_TYPE_CHANNEL;
                mCurrentAccount = AnonymousAccount.getInstance();
            }
                break;
            default:
                break;
        }
    }

    public void logoffMiLink() {
        mIsLogining = false;
        mCurrentAccount.logoffMiLink();
    }

    public boolean appHasLogined() {
        String serviceToken = mCurrentAccount.getServiceToken();
        return !TextUtils.isEmpty(serviceToken);
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

    /* 给MiPush使用 */
    private volatile String mMiPush_RegId = null;

    private boolean mIsUploadRegIdToServer = false;

    public boolean hasUploadRegIdToServer() {
        return mIsUploadRegIdToServer;
    }

    public void setHasUploadRegIdToServer(boolean isUpload) {
        mIsUploadRegIdToServer = isUpload;
    }

    public synchronized void setMipushRegId(String regId) {
        MiLinkLog.v(TAG, "setMiPushRegId:" + regId);
        if (!TextUtils.isEmpty(regId) && !regId.equals(mMiPush_RegId)) {
            mMiPush_RegId = regId;
            mIsUploadRegIdToServer = false;
        }
    }

    public String getMiPushRegId() {
        return mMiPush_RegId;
    }

    public synchronized void login(String userId, String serviceToken, String sSecurity,
            byte[] fastLoginExtra, boolean passportInit) {

        String log = String.format(
                "login start,userId=%s,serviceToken=%s,sSecurity=%s,fastLoginExtra.length=%d",
                userId, serviceToken, sSecurity, (fastLoginExtra != null ? fastLoginExtra.length
                        : -1));
        MiLinkLog.w(TAG, log+" passportInit:"+passportInit);
        switchAccountTypeMode(ACCOUNT_TYPE_STANDARD);
        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(serviceToken)
                || TextUtils.isEmpty(sSecurity)) {
            MiLinkLog.v(TAG, "login but argu is wrong,cancel!!!");
            return;
        }
        // 标准模式
        String currentUserId = mCurrentAccount.getUserId();
        // 不是同一个用户了,换用户登录了
        if (!TextUtils.isEmpty(currentUserId) && !currentUserId.equals(userId)) {
            EventBus.getDefault().post(
                    new ClientActionEvent(ClientActionEvent.EventType.ClientNotSameUserLogin));
        }
        String currentServiceToken = mCurrentAccount.getServiceToken();
        String currentSSecurity = mCurrentAccount.getSSecurity();
        String b2Token = mCurrentAccount.getB2Token();
        MiLinkLog.d(TAG, "b2Token=" + b2Token);
        if (!TextUtils.isEmpty(currentUserId) && currentUserId.equals(userId)
                && currentServiceToken.equals(serviceToken) && currentSSecurity.equals(sSecurity)
                && !TextUtils.isEmpty(b2Token)) {
            MiLinkLog.d(TAG, "login but mB2Token is not empty");
            // 必须把以下字段的值更新
            mCurrentAccount.setFastLoginExtra(fastLoginExtra);
            mCurrentAccount.dataChange();
            // 如果已经有b2token 且已经连接 可以只是简单回调一下登陆成功的接口
            EventBus.getDefault()
                    .post(new ClientActionEvent(
                            ClientActionEvent.EventType.ClientRequestCheckConnection));
            return;
        }
        mCurrentAccount.setUserId(userId);
        mCurrentAccount.setServiceToken(serviceToken);
        mCurrentAccount.setSSecurity(sSecurity);
        mCurrentAccount.setFastLoginExtra(fastLoginExtra);
        if (!mPassportInit) {
            mPassportInit = passportInit;
        }
        mCurrentAccount.dataChange();
        EventBus.getDefault().post(
                new ClientActionEvent(ClientActionEvent.EventType.ClientRequestLogin));
    }

    public void initUserChannelMode() {

        MiLinkLog.v(TAG, "initUseChannelMode");
        mAllowAnonymousMode = false;
        switchAccountTypeMode(ACCOUNT_TYPE_CHANNEL);
        mCurrentAccount.generateServiceTokenAndSSecurity();

        // 这边是可以优化的，如果有b2Token就不用登陆了 与标准模式类似，这里加个TODO;可以只是简单回调一下登陆成功的接口
        String userId = mCurrentAccount.getUserId();
        String b2Token = mCurrentAccount.getB2Token();
        if (!TextUtils.isEmpty(userId) && !TextUtils.isEmpty(b2Token)) {
            EventBus.getDefault()
                    .post(new ClientActionEvent(
                            ClientActionEvent.EventType.ClientRequestCheckConnection));
            return;
        }
        EventBus.getDefault().post(
                new ClientActionEvent(ClientActionEvent.EventType.ClientRequestLogin));

    }

    public void initUseAnonymousMode() {
        MiLinkLog.v(TAG, "initUseAnonymousMode");
        mAllowAnonymousMode = true;
        switchAccountTypeMode(ACCOUNT_TYPE_ANONYMOUS);
        mCurrentAccount.generateServiceTokenAndSSecurity();

        // 这边是可以优化的，如果有b2Token就不用登陆了 与标准模式类似，这里加个TODO;可以只是简单回调一下登陆成功的接口
        String userId = mCurrentAccount.getUserId();
        String b2Token = mCurrentAccount.getB2Token();
        if (!TextUtils.isEmpty(userId) && !TextUtils.isEmpty(b2Token)) {
            EventBus.getDefault()
                    .post(new ClientActionEvent(
                            ClientActionEvent.EventType.ClientRequestCheckConnection));
            return;
        }
        EventBus.getDefault().post(
                new ClientActionEvent(ClientActionEvent.EventType.ClientRequestLogin));
    }

    public byte getBusinessEncByMode() {
        switch (mCurrentAccountType) {
            case ACCOUNT_TYPE_ANONYMOUS:
                return StreamUtil.MNS_ENCODE_ANONYMOUS_B2_TOKEN;
            case ACCOUNT_TYPE_STANDARD:
                return StreamUtil.MNS_ENCODE_B2_TOKEN;
            case ACCOUNT_TYPE_CHANNEL:
                return StreamUtil.MNS_ENCODE_CHANNEL_B2_TOKEN;
            default:
                return StreamUtil.MNS_ENCODE_NONE;
        }
    }

    private boolean mPassportInit = false;

    public void setPassportInit(boolean b) {
    	MiLinkLog.w(TAG, "setPassportInit b=" + b);
        mPassportInit = b;
    }

    public boolean getPassportInit() {
        MiLinkLog.w(TAG, "getPassportInit mPassportInit=" + mPassportInit);
        return mPassportInit;
    }

    public boolean isAnonymousModeCurrent() {
        return mCurrentAccountType == ACCOUNT_TYPE_ANONYMOUS;
    }

    public boolean isChannelModCurrent() {
        return mCurrentAccountType == ACCOUNT_TYPE_CHANNEL;
    }

    public int getCurrentAccountType() {
        return mCurrentAccountType;
    }

    public IAccount getCurrentAccount() {
        return mCurrentAccount;
    }

    public String getDeviceToken() {
        return this.deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

}

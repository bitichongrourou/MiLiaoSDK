
package com.mi.milink.sdk.account;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;
import android.util.Base64;

import com.mi.milink.sdk.base.Global;
import com.mi.milink.sdk.debug.MiLinkLog;

public abstract class IAccount {
    protected static final String PREF_USER_ID = "userId"; // 小米id，用于登录

    protected static final String PREF_SERVICE_TOKEN = "serviceToken";

    protected static final String PREF_S_SECURITY = "sSecurity";

    protected static final String PREF_B2_TOKEN = "b2Token";

    protected static final String PREF_B2_SECURITY = "b2Security";

    private static final String PREF_FAST_LOGIN_EXTRA_DATA = "fastLoginExtraData";

    private static final String PREF_PRIVACY_KEY = "privacyKey";
    
    protected volatile String mUserId;

    protected volatile String mServiceToken;

    protected volatile String mSSecurity;

    protected volatile String mB2Token;

    protected volatile String mB2Security;

    protected volatile String mOldB2Token;// 自动换B2时，有可能拿到新B2之后还收到老B2的包

    protected volatile String mOldB2Security;

    private byte[] mFastLoginExtra;

    protected long mMiLinkLoginTime = 0;

    protected abstract String getPrefFileName();

    protected abstract int getAccountType();
    
    protected synchronized void loadAccount() {
        MiLinkLog.v(getTag(), "loadAccount");
        SharedPreferences pref = Global.getContext().getSharedPreferences(getPrefFileName(),
                Context.MODE_PRIVATE);
        mUserId = pref.getString(PREF_USER_ID, "");
        mServiceToken = pref.getString(PREF_SERVICE_TOKEN, "");
        mSSecurity = pref.getString(PREF_S_SECURITY, "");
        mB2Token = pref.getString(PREF_B2_TOKEN, "");
        mB2Security = pref.getString(PREF_B2_SECURITY, "");
        // mUseFastLogin = pref.getBoolean(PREF_USER_FAST_LOGIN, false);
        String fastLoginExtra = pref.getString(PREF_FAST_LOGIN_EXTRA_DATA, "");
        if (!TextUtils.isEmpty(fastLoginExtra)) {
            mFastLoginExtra = Base64.decode(fastLoginExtra.getBytes(), Base64.DEFAULT);
        } else {
            mFastLoginExtra = null;
        }
        mPrivacyKey = pref.getString(PREF_PRIVACY_KEY, "");
    }

    protected synchronized void saveAccount() {
        MiLinkLog.v(getTag(), "saveAccount");
        SharedPreferences pref = Global.getContext().getSharedPreferences(getPrefFileName(),
                Context.MODE_PRIVATE);
        Editor ed = pref.edit();
        ed.putString(PREF_USER_ID, mUserId == null ? "" : mUserId);
        ed.putString(PREF_SERVICE_TOKEN, mServiceToken == null ? "" : mServiceToken);
        ed.putString(PREF_S_SECURITY, mSSecurity == null ? "" : mSSecurity);
        ed.putString(PREF_B2_TOKEN, mB2Token == null ? "" : mB2Token);
        ed.putString(PREF_B2_SECURITY, mB2Security == null ? "" : mB2Security);
        if (mFastLoginExtra != null) {
            ed.putString(PREF_FAST_LOGIN_EXTRA_DATA,
                    new String(Base64.encode(mFastLoginExtra, Base64.DEFAULT)));
        } else {
            ed.putString(PREF_FAST_LOGIN_EXTRA_DATA, "");
        }
        ed.putString(PREF_PRIVACY_KEY, mPrivacyKey == null ? "" : mPrivacyKey);
        ed.commit();
    };

    public void logoff(){
        MiLinkLog.v(getTag(), "logoff");
        mUserId = "";
        mServiceToken = "";
        mSSecurity = "";
        mB2Token = "";
        mB2Security = "";
        mOldB2Token = "";
        mOldB2Security = "";
        mMiLinkLoginTime = 0;
        mFastLoginExtra = null;
        saveAccount();
    };

    public void logoffMiLink(){
        MiLinkLog.v(getTag(), "logoffMiLink");
        mB2Token = "";
        mB2Security = "";
        mOldB2Token = "";
        mOldB2Security = "";
        mMiLinkLoginTime = 0;
        saveAccount();
    }

    public static int NO = 1;
    
    protected int mNo;
    
    protected IAccount() {
        mNo = NO++;
        MiLinkLog.v(getTag(), "IAccount()");
        loadAccount();
    }

    public String getUserId() {
        return mUserId;
    }

    public void setUserId(String mUserId) {
        this.mUserId = mUserId;
    }

    public String getServiceToken() {
        return mServiceToken;
    }

    public void setServiceToken(String mServiceToken) {
        this.mServiceToken = mServiceToken;
    }

    public String getSSecurity() {
        return mSSecurity;
    }

    public void setSSecurity(String mSSecurity) {
        this.mSSecurity = mSSecurity;
    }

    public String getB2Token() {
        MiLinkLog.v(getTag(), "getB2Token=" + mB2Token);
        return mB2Token;
    }

    public void setB2Token(String mB2Token) {
        MiLinkLog.v(getTag(), "setB2Token=" + mB2Token);
        this.mB2Token = mB2Token;
    }

    public String getB2Security() {
        MiLinkLog.v(getTag(), "getB2Security=" + mB2Security);
        return mB2Security;
    }

    public void setB2Security(String mB2Security) {
        MiLinkLog.v(getTag(), "setB2Security=" + mB2Security);
        this.mB2Security = mB2Security;
    }

    public String getOldB2Token() {
        return mOldB2Token;
    }

    public void setOldB2Token(String mOldB2Token) {
        this.mOldB2Token = mOldB2Token;
    }

    public String getOldB2Security() {
        return mOldB2Security;
    }

    public void setOldB2Security(String mOldB2Security) {
        this.mOldB2Security = mOldB2Security;
    }

    public byte[] getFastLoginExtra() {
        return mFastLoginExtra;
    }

    public void setFastLoginExtra(byte[] mFastLoginExtra) {
        this.mFastLoginExtra = mFastLoginExtra;
    }

    public long getMiLinkLoginTime() {
        return mMiLinkLoginTime;
    }

    public void setMiLinkLoginTime(long mMiLinkLoginTime) {
        this.mMiLinkLoginTime = mMiLinkLoginTime;
    }

    public void loginMiLink(byte[] b2Token, byte[] b2Security) {
        if (b2Token != null && b2Token.length > 0 && b2Security != null && b2Security.length > 0) {
            String log = String.format("loginMiLink,b2Token.length=%d,b2Security.length=%d",
                    b2Token.length, b2Security.length);
            MiLinkLog.v(getTag(), log);
            mOldB2Security = mB2Security;
            mOldB2Token = mB2Token;
            try {
                synchronized (this) {
                    String b2TokenStr = new String(b2Token, "UTF-8");
                    String b2SecurityStr = new String(b2Security, "UTF-8");
                    this.mB2Token = b2TokenStr;
                    this.mB2Security = b2SecurityStr;
                    MiLinkLog.v(getTag(), "mB2Token:"+mB2Token);
                    MiLinkLog.v(getTag(), "mB2Security:"+mB2Security);
                }
                this.mMiLinkLoginTime = System.currentTimeMillis();
                saveAccount();
            } catch (UnsupportedEncodingException e) {
                MiLinkLog.v(getTag(), "UnsupportedEncodingException:",e);
            }
        } else {
            MiLinkLog.e(getTag(), "loginMiLink,but some argu is wrong!");
        }
    }

    protected abstract String getTag();

    public void dataChange(){
        saveAccount();
    }
    
    protected String mPrivacyKey;
    
    public abstract String getPrivacyKey();
    
    public abstract void generateServiceTokenAndSSecurity();

	public void setKeepAliveTime(int keepAliveTime) {
	}
	public int getKeepAliveTime() {
		return 0;
	}

	public void setChannelPubKey(Map<Integer, String> channelPubKeyMap) {
		
	}

	public void DelChannelPubKey() {
		
	}
    
}
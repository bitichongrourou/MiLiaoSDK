
package com.mi.milink.sdk.account;

import java.util.Random;

import android.text.TextUtils;

import com.mi.milink.sdk.account.manager.MiAccountManager;
import com.mi.milink.sdk.account.manager.RSAPublicKey;
import com.mi.milink.sdk.account.manager.RSAPublicKey.PublicKeyAndId;
import com.mi.milink.sdk.debug.MiLinkLog;

public class AnonymousAccount extends IAccount {
    private static AnonymousAccount INSTANCE;

    protected AnonymousAccount() {
        super();
    }

    public static AnonymousAccount getInstance() {
        if (INSTANCE == null) {
            synchronized (AnonymousAccount.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AnonymousAccount();
                }
            }
        }
        return INSTANCE;
    }

    @Override
    protected String getPrefFileName() {
        return "milink_anonymous_account";
    }

    @Override
    protected String getTag() {
        return "AnonymousAccount";
    }

    protected String generatePrivacyKey() {
        StringBuilder sb = new StringBuilder(16);
        Random r = new Random();
        for (int i = 0; i < 16; i++) {
            int a = r.nextInt(25) + 97;
            sb.append((char) a);
        }
        return sb.toString();
    }

    @Override
    protected int getAccountType() {
        return MiAccountManager.ACCOUNT_TYPE_ANONYMOUS;
    }

    @Override
    public String getPrivacyKey() {
        if (TextUtils.isEmpty(mPrivacyKey)) {
            this.mPrivacyKey = generatePrivacyKey();
        }
        MiLinkLog.v(getTag(), "mPrivacyKey=" + mPrivacyKey);
        return mPrivacyKey;
    }

    @Override
    public String getServiceToken() {
        MiLinkLog.v(getTag(), "mServiceToken=" + mServiceToken);
        return this.mServiceToken;
    }

    @Override
    public void generateServiceTokenAndSSecurity() {
        if ("0".equals(mServiceToken) || TextUtils.isEmpty(mSSecurity)) {
            PublicKeyAndId keyAndId = RSAPublicKey.getPublicKeyAndId();
            this.mServiceToken = keyAndId.id;
            this.mSSecurity = keyAndId.key;
            MiLinkLog.v(getTag(), "generateServiceTokenAndSSecurity mServiceToken=" + mServiceToken
                    + ",mSSecurity=" + mSSecurity);
        }
    }

    @Override
    public String getSSecurity() {
        MiLinkLog.v(getTag(), "mSSecurity=" + mSSecurity);
        return this.mSSecurity;
    }

}

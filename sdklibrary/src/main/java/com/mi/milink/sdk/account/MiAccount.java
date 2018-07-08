
package com.mi.milink.sdk.account;

import com.mi.milink.sdk.account.manager.MiAccountManager;


/**
 * 用于记录account信息，仅运行在service
 *
 * @author xiaolong
 */

public class MiAccount extends IAccount {

    private static MiAccount INSTANCE;

    protected MiAccount() {
        super();
    }

    public static MiAccount getInstance() {
        if (INSTANCE == null) {
            synchronized (MiAccount.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MiAccount();
                }
            }
        }
        return INSTANCE;
    }

    @Override
    protected String getPrefFileName() {
        return "milink_account";
    }

    @Override
    protected int getAccountType() {
       return MiAccountManager.ACCOUNT_TYPE_STANDARD;
    }

    @Override
    public String getPrivacyKey() {
        throw new IllegalAccessError("stardard mode will never call getPrivacyKey");
    }

    @Override
    public void generateServiceTokenAndSSecurity() {
        throw new IllegalAccessError("stardard mode will never call generateServiceTokenAndSSecurity");
    }

    @Override
    protected String getTag() {
                return "MiAccount";
    }

}

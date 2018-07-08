
package com.mi.milink.sdk.config;

public class MiLinkIpInfoManager extends IIpInfoManager {
    private static final String TAG = "MiLinkIpInfoManager";

    private static MiLinkIpInfoManager sInstance = null;

    private MiLinkIpInfoManager() {
        super();
    }

    public static MiLinkIpInfoManager getInstance() {
        if (sInstance == null) {
            synchronized (MiLinkIpInfoManager.class) {
                if (sInstance == null) {
                    sInstance = new MiLinkIpInfoManager();
                }
            }
        }
        return sInstance;
    }

    @Override
    protected String getOptimumServerFileName() {
        return "optservers";
    }

    @Override
    protected String getBackupServerFileName() {
        return "backupservers";
    }

    @Override
    protected String getRecentlyServerFileName() {
        return "recentlyservers";
    }

    @Override
    protected String getApnIspFileName() {
        return "apnisps";
    }

    @Override
    public void destroy() {
        sInstance = null;
    }

}

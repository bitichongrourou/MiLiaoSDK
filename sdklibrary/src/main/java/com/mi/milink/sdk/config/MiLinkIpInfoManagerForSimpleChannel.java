
package com.mi.milink.sdk.config;

public class MiLinkIpInfoManagerForSimpleChannel extends IIpInfoManager {

//    private  MiLinkIpInfoManagerForSimpleChannel sInstance = null;

    public MiLinkIpInfoManagerForSimpleChannel() {
        super();
    }


    @Override
    protected String getOptimumServerFileName() {
        return "optservers_for_channel_session";
    }

    @Override
    protected String getBackupServerFileName() {
        return "backupservers_for_channel_session";
    }

    @Override
    protected String getRecentlyServerFileName() {
        return "recentlyservers_for_channel_session";
    }

    @Override
    protected String getApnIspFileName() {
        return "apnisps_for_channel_session";
    }

    @Override
    public void destroy() {
//        sInstance = null;
    }

}


package com.mi.milink.sdk.session.persistent;

import com.mi.milink.sdk.session.common.IServerManager;
import com.mi.milink.sdk.session.common.ServerProfile;
import com.mi.milink.sdk.session.common.SessionConst;
import com.mi.milink.sdk.util.CommonUtils;

public class ManualServerManager extends IServerManager {

    private static final String TAG = "ManualServerManager";

    private static ManualServerManager sInstance = null;

    private String assignIp;

    private int assignPort;

    private ManualServerManager() {
        super(null);
    }

    public static ManualServerManager getInstance() {
        if (sInstance == null) {
            synchronized (ManualServerManager.class) {
                if (sInstance == null) {
                    sInstance = new ManualServerManager();
                }
            }
        }
        return sInstance;
    }

    @Override
    public ServerProfile[] reset(boolean isBackgroud) {
        if (CommonUtils.isLegalIp(assignIp)
                && CommonUtils.isLegalPort(assignPort)) {
            return new ServerProfile[] {
                new ServerProfile(assignIp, assignPort, SessionConst.TCP_CONNECTION_TYPE,
                        SessionConst.DOMAIN_IP)
            };
        }
        return new ServerProfile[] {};
    }

    @Override
    public ServerProfile[] getNext(ServerProfile serverProfile, int failReason) {
        return null;
    }

    @Override
    public boolean save(ServerProfile serverProfile) {
        return false;
    }

    public void setIp(String ip) {
        if (CommonUtils.isLegalIp(ip)) {
            this.assignIp = ip;
        }
    }

    public void setPort(int port) {
        if (CommonUtils.isLegalPort(port)) {
            this.assignPort = port;
        }
    }

    @Override
    public void destroy() {
        sInstance = null;
    }
}

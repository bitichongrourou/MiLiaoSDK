
package com.mi.milink.sdk.session.persistent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mi.milink.sdk.base.os.Device.Network;
import com.mi.milink.sdk.base.os.info.NetworkDash;
import com.mi.milink.sdk.config.MiLinkIpInfoManager;
import com.mi.milink.sdk.data.ClientAppInfo;
import com.mi.milink.sdk.data.Const;
import com.mi.milink.sdk.debug.MiLinkLog;
import com.mi.milink.sdk.session.common.IServerManager;
import com.mi.milink.sdk.session.common.OptimumServerData;
import com.mi.milink.sdk.session.common.RecentlyServerData;
import com.mi.milink.sdk.session.common.ServerProfile;
import com.mi.milink.sdk.session.common.SessionConst;

import android.text.TextUtils;

public class MiLinkServerManager extends IServerManager {

    private static final String TAG = "MiLinkServerManager";

    private static MiLinkServerManager sInstance = null;

    private MiLinkServerManager() {
        super(MiLinkIpInfoManager.getInstance());
    }

    public static MiLinkServerManager getInstance() {
        if (sInstance == null) {
            synchronized (MiLinkServerManager.class) {
                if (sInstance == null) {
                    sInstance = new MiLinkServerManager();
                }
            }
        }
        return sInstance;

    }

    @Override
    public ServerProfile[] reset(boolean isBackgroud) {
//        if (true) {
//            return new ServerProfile[] {
//                new ServerProfile(ip, port, SessionConst.TCP_CONNECTION_TYPE,
//                        SessionConst.DOMAIN_IP)
//            };
//        }
    	
        ServerProfile[] serverList = null;
        mTcpServerList.clear();
        mTcpServerListIndex = 0;
        if (ClientAppInfo.isTestChannel()) {
            ServerProfile[] backupServverProfile = mIpInfoManager.getTestBackupIp();
            String ip = backupServverProfile[0].getServerIP();
            if (!TextUtils.isEmpty(ip)) {
                // 加入IP
                for (int port : Const.ServerPort.PORT_ARRAY) {
                    ServerProfile profile = new ServerProfile(ip, port,
                            SessionConst.TCP_CONNECTION_TYPE, SessionConst.DOMAIN_IP);
                    mTcpServerList.add(profile);
                }
            }
        } else {
            ServerProfile recentlyServer = null;
            RecentlyServerData rsd = mIpInfoManager.getRecentlyServerData();
            if (rsd != null) {
                recentlyServer = rsd.getRecentlyServer();
            }
            List<ServerProfile> optimumServerList = null;
            boolean recentInOptimum = false;
            OptimumServerData osd = mIpInfoManager.getCurrentApnOptimumServerData();
            if (osd != null) {
                optimumServerList = osd.getOptimumServers();
                if (optimumServerList != null) {
                    Collections.shuffle(optimumServerList);// 打乱顺序
                    ArrayList<ServerProfile> excludedRecentlyOptimumList = new ArrayList<ServerProfile>();
                    for (ServerProfile serverProfile : optimumServerList) {
                        if (serverProfile != null) {
                            if (recentlyServer != null
                                    && serverProfile.getServerIP().equals(
                                            recentlyServer.getServerIP())) {// 判断最近是否是最优
                                recentInOptimum = true;
                            } else { // 排除最优，最多只选4个， 因为只有4个端口
                                if (excludedRecentlyOptimumList.size() < Const.ServerPort.PORT_ARRAY.length) {
                                    excludedRecentlyOptimumList.add(serverProfile);
                                }
                            }
                        }
                    }
                    addServerProfileInSpecifiedList(excludedRecentlyOptimumList, mTcpServerList);
                }
            }
            for (int port : Const.ServerPort.PORT_ARRAY) { // 域名
                ServerProfile profile = new ServerProfile(mIpInfoManager.getDefaultServer()
                        .getServerIP(), port, SessionConst.TCP_CONNECTION_TYPE,
                        SessionConst.DOMAIN_IP);
                mTcpServerList.add(profile);
            }
            // 保底
            List<ServerProfile> backupServerList = mIpInfoManager.getBackupServerList();
            if (backupServerList != null) {
                Collections.shuffle(backupServerList);
                addServerProfileInSpecifiedList(backupServerList, mTcpServerList);
            }
            if (recentInOptimum) {
                serverList = new ServerProfile[1];
                serverList[0] = recentlyServer;
                MiLinkLog.i(TAG, "reset isBackgroud = " + isBackgroud + ",has recently tcp server"
                        + recentlyServer);
                return serverList;
            }
        }

        serverList = new ServerProfile[DEFAULT_SESSION_COUNT];
        // serverList = new ServerProfile[mTcpSessionNum + mHttpSessionNum];
        for (int i = 0; i < DEFAULT_SESSION_COUNT; i++) {
            serverList[i] = mTcpServerList.get(mTcpServerListIndex++);
        }

        // 仅仅用来打印log
        for (int i = 0; i < serverList.length; i++) {
            MiLinkLog.i(TAG, "reset isBackgroud = " + isBackgroud
                    + ", has no recently server, so try " + DEFAULT_SESSION_COUNT + ", server No."
                    + i + ":" + serverList[i]);
        }
        return serverList;
    }

    @Override
    public ServerProfile[] getNext(ServerProfile serverProfile, int failReason) {
        // if (true) {
        // return null;
        // }
        if (serverProfile == null) {
            MiLinkLog.e(TAG, "getNext, serverProfile == null!!!");
            return null;
        }

        if (!NetworkDash.isAvailable()) {
            MiLinkLog.e(TAG, "getNext, Network is not available!!!");
            return null;
        }

        MiLinkLog.i(TAG, "getNext, failserver info:" + serverProfile + ",failReason = "
                + failReason);

        ServerProfile[] serverList = null;

        if (serverProfile.getProtocol() == SessionConst.TCP_CONNECTION_TYPE) {
            if (serverProfile.getServerType() == SessionConst.RECENTLY_IP) {
                // 如果是tcp的最近使用IP失败
                serverList = new ServerProfile[DEFAULT_SESSION_COUNT];
                for (int i = 0; i < DEFAULT_SESSION_COUNT; i++) {
                    serverList[i] = getNextTcpProfile();
                    MiLinkLog.i(TAG,
                            "getNext, recently tcp failed, and has no rencently http server,so try "
                                    + DEFAULT_SESSION_COUNT + ", server No." + i + ":"
                                    + serverList[i]);
                }
                return serverList;
            } else {
                // tcp其他类型返回，则返回1个ip
                if (mTcpServerListIndex == mTcpServerList.size()) {
                    MiLinkLog.i(TAG, "getNext no tcp server to try");
                    return null;
                } else {
                    serverList = new ServerProfile[1];
                    serverList[0] = getNextTcpProfile();
                    MiLinkLog.i(TAG, "getNext get tcp server," + serverList[0]);
                    return serverList;
                }
            }
        } else {
            return null;
        }
    }

    @Override
    public void destroy() {
        sInstance = null;
    }
    
//    private static MiAccountManager instance;
//	public static IServerManager getInstance(MiAccountManager instance1) {
//		instance = instance1;
//		return instance;
//	}

}

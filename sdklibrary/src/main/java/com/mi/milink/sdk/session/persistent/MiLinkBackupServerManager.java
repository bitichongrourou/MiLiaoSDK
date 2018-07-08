
package com.mi.milink.sdk.session.persistent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.text.TextUtils;

import com.mi.milink.sdk.base.os.Device.Network;
import com.mi.milink.sdk.base.os.info.NetworkDash;
import com.mi.milink.sdk.config.MiLinkIpInfoManager;
import com.mi.milink.sdk.data.ClientAppInfo;
import com.mi.milink.sdk.data.Const;
import com.mi.milink.sdk.debug.MiLinkLog;
import com.mi.milink.sdk.session.common.IServerManager;
import com.mi.milink.sdk.session.common.ServerProfile;
import com.mi.milink.sdk.session.common.SessionConst;

public class MiLinkBackupServerManager extends IServerManager {

    private static final String TAG = "MiLinkBackupServerManager";

    private static final int DEFAULT_SESSION_COUNT = 1;

    private static MiLinkBackupServerManager sInstance = null;

    private MiLinkBackupServerManager() {
        super(MiLinkIpInfoManager.getInstance());
    }

    public static MiLinkBackupServerManager getInstance() {
        if (sInstance == null) {
            synchronized (MiLinkBackupServerManager.class) {
                if (sInstance == null) {
                    sInstance = new MiLinkBackupServerManager();
                }
            }
        }
        return sInstance;
    }

    @Override
    public ServerProfile[] reset(boolean isBackgroud) {
        // 构建服务器配置列表
        ServerProfile[] serverList = null;
        mTcpServerList = new ArrayList<ServerProfile>();
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
            // 保底
            List<ServerProfile> backupServerList = mIpInfoManager.getBackupServerList();
            if (backupServerList != null) {
                Collections.shuffle(backupServerList);
                addServerProfileInSpecifiedList(backupServerList, mTcpServerList);
            }
        }
        if (mTcpServerList.isEmpty()) {
            return null;
        }
        serverList = new ServerProfile[DEFAULT_SESSION_COUNT];
        // serverList = new ServerProfile[mTcpSessionNum + mHttpSessionNum];
        for (int i = 0; i < DEFAULT_SESSION_COUNT; i++) {
            serverList[i] = mTcpServerList.get(mTcpServerListIndex++);
        }

        // 仅仅用来打印log
        for (int i = 0; i < serverList.length; i++) {
            MiLinkLog.i(TAG, "reset , so try backuplist" + DEFAULT_SESSION_COUNT + ", server No."
                    + i + ":" + serverList[i]);
        }
        return serverList;
    }

    @Override
    public ServerProfile[] getNext(ServerProfile serverProfile, int failReason) {
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
        } else {
            return null;
        }
    }

    @Override
    public void destroy() {
        sInstance = null;
    }

}


package com.mi.milink.sdk.session.common;

import java.util.ArrayList;
import java.util.List;

import com.mi.milink.sdk.config.IIpInfoManager;
import com.mi.milink.sdk.connection.DomainManager;
import com.mi.milink.sdk.data.Const;
import com.mi.milink.sdk.debug.MiLinkLog;
import com.mi.milink.sdk.session.persistent.SessionManager;

/**
 * 服务器管理器的抽象类
 * 
 * @author MK
 */
public abstract class IServerManager {

    protected IIpInfoManager mIpInfoManager;
    protected static final int DEFAULT_SESSION_COUNT = 4;

    protected List<ServerProfile> mTcpServerList = new ArrayList<ServerProfile>();;

    protected int mTcpServerListIndex = 0;

    protected IServerManager(IIpInfoManager ipInfoManager) {
        this.mIpInfoManager = ipInfoManager;
    }

    protected static void addServerProfileInSpecifiedList(List<ServerProfile> needToAddList,
            List<ServerProfile> specifiedList) {
        if (Const.ServerPort.PORT_ARRAY.length != 4) {
            MiLinkLog.e("IServerManager", "PORT_ARRAY.length != 4");
            return;
        }
        // PORT_ARRAY.length 变化时，这里也要做相应的调整
        int[][] indexMatrix = new int[][] {
                {
                        0, 0, 0, 0
                }, {
                        0, 0, 1, 1
                }, {
                        0, 0, 1, 2
                }, {
                        0, 1, 2, 3
                },
        };
        int size = needToAddList.size();
        if (size > 4) {
            size = 4;
        }
        if (size <= 4 && size > 0) {
            for (int i = 0; i < Const.ServerPort.PORT_ARRAY.length; i++) {
                specifiedList.add(new ServerProfile(needToAddList.get(indexMatrix[size - 1][i])
                        .getServerIP(), Const.ServerPort.PORT_ARRAY[i],
                        SessionConst.TCP_CONNECTION_TYPE, SessionConst.OPTI_IP));
            }
        }
    }

    /**
     * 重置服务器列表，重新开始
     * 
     * @param isBackGroud 是否为后台模式。 后台模式跑马，不需要最近使用IP
     * @return 服务器列表
     */
    public abstract ServerProfile[] reset(boolean isBackGroud);

    /**
     * 根据当前失败的服务器信息，获取下一个尝试的服务器列表
     * 
     * @param serverProfile 失败的服务器信息
     * @param failReason 失败原因
     *            <ul>
     *            <li>{@link SessionManager#CONN_FAILED}
     *            <li>{@link SessionManager#HANDSHAKE_OTHERERROR_FAILED}
     *            <li>{@link SessionManager#HANDSHAKE_PACKERROR_FAILED}
     *            </ul>
     * @return 服务器列表
     */
    public abstract ServerProfile[] getNext(ServerProfile serverProfile, int failReason);

    /**
     * 保存成功的服务器信息
     * 
     * @param serverProfile 成功的服务器信息
     * @return 保存成功返回true，否则返回false
     */
    public boolean save(ServerProfile serverProfile) {
        if (serverProfile == null) {
            return false;
        }

        // 如果是域名，保存域名解析后的IP
        serverProfile.setServerIP(DomainManager.getInstance().queryDomainIP(
                serverProfile.getServerIP()));

        if (serverProfile.getProtocol() == SessionConst.TCP_CONNECTION_TYPE) {
            mIpInfoManager.setRecentlyServer(new ServerProfile(serverProfile.getServerIP(),
                    serverProfile.getServerPort(), serverProfile.getProtocol(),
                    SessionConst.RECENTLY_IP));
        } else {
            // 无法识别的服务器类型
            return false;
        }
        return true;
    }
    
    public abstract void destroy();
    /**
     * 获取下一个tcp服务器配置信息
     *
     * @return 服务器配置信息
     */
    protected ServerProfile getNextTcpProfile() {
        if (mTcpServerListIndex == mTcpServerList.size()) {
            return null;
        } else {
            return mTcpServerList.get(mTcpServerListIndex++);
        }
    }
}

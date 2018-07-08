
package com.mi.milink.sdk.session.common;

import java.io.Serializable;

/**
 * 服务器配置信息
 *
 * @author MK
 */
public class ServerProfile implements Serializable {
    /**
     * serialVersionUID 保持版本兼容性
     */
    private static final long serialVersionUID = -8956679711781976000L;

    private String mServerIP; // 服务器IP

    private int mServerPort; // 服务器端口

    private String mProxyIP; // 代理IP

    private int mPorxyPort; // 代理端口

    private int mProtocol; // 协议类型

    private int mServerType; // 服务器类型

    /**
     * 构造函数
     *
     * @param serverIP 服务器地址
     * @param serverPort 服务器端口
     * @param protocol 服务器协议
     *            <ul>
     *            <li>{@link SessionConst#NONE_CONNECTION_TYPE}
     *            <li>{@link SessionConst#TCP_CONNECTION_TYPE}
     *            <li>{@link SessionConst#HTTP_CONNECTION_TYPE}
     *            </ul>
     * @param serverType <ul>
     *            <li>{@link SessionConst#OPTI_IP}
     *            <li>{@link SessionConst#REDIRECT_IP}
     *            <li>{@link SessionConst#RECENTLY_IP}
     *            <li>{@link SessionConst#DOMAIN_IP}
     *            <li>{@link SessionConst#BACKUP_IP}
     *            <li>{@link SessionConst#CDN_IP}
     *            <li>{@link SessionConst#TEST_IP}
     *            </ul>
     */
    public ServerProfile(String serverIP, int serverPort, int protocol, int serverType) {
        this(serverIP, serverPort, null, 0, protocol, serverType);
    }

    /**
     * 构造函数
     *
     * @param serverIP 服务器地址
     * @param serverPort 服务器端口
     * @param proxyIP 代理地址
     * @param porxyPort 代理端口
     * @param protocol 服务器协议
     *            <ul>
     *            <li>{@link SessionConst#NONE_CONNECTION_TYPE}
     *            <li>{@link SessionConst#TCP_CONNECTION_TYPE}
     *            <li>{@link SessionConst#HTTP_CONNECTION_TYPE}
     *            </ul>
     * @param serverType <ul>
     *            <li>{@link SessionConst#OPTI_IP}
     *            <li>{@link SessionConst#REDIRECT_IP}
     *            <li>{@link SessionConst#RECENTLY_IP}
     *            <li>{@link SessionConst#DOMAIN_IP}
     *            <li>{@link SessionConst#BACKUP_IP}
     *            <li>{@link SessionConst#CDN_IP}
     *            <li>{@link SessionConst#TEST_IP}
     *            </ul>
     */
    public ServerProfile(String serverIP, int serverPort, String proxyIP, int porxyPort,
            int protocol, int serverType) {
        this.mServerIP = serverIP;
        this.mServerPort = serverPort;
        this.mProxyIP = proxyIP;
        this.mPorxyPort = porxyPort;
        this.mProtocol = protocol;
        this.mServerType = serverType;
    }

    /**
     * 比较两个服务器的地址和端口是否相同
     *
     * @param serverProfile 需要比较的服务器信息
     * @return 相同返回true，否则返回false
     */
    public boolean equals(ServerProfile serverProfile) {
        if (mServerIP == null || mServerPort == 0 || serverProfile == null)
            return false;

        if (mServerIP.equals(serverProfile.getServerIP()) == false)
            return false;

        if (mServerPort != serverProfile.getServerPort())
            return false;

        // 都没有代理IP，认为是相等的
        if (mProxyIP == null && serverProfile.getProxyIP() == null)
            return true;

        // 一个有代理IP，一个没有代理IP，认为是不相等的
        if ((mProxyIP != null && serverProfile.getProxyIP() == null)
                || (mProxyIP == null && serverProfile.getProxyIP() != null))
            return false;

        // 代理IP不相同，认为是不相等的
        if (mProxyIP != null && mProxyIP.equals(serverProfile.getProxyIP()) == false)
            return false;

        // 代理端口不同，认为是不相等的
        if (mPorxyPort != serverProfile.getPorxyPort())
            return false;

        return true;
    }

    /**
     * 当前服务器信息是否比对方更优
     *
     * @param serverProfile 需要比较的服务器信息
     * @return 如果比对方优，返回true;否则，返回false
     */
    public boolean isBetterThan(ServerProfile serverProfile) {
        if (serverProfile == null)
            return true;

        if (mServerIP == null || mServerPort == 0)
            return false;

        if (mProtocol == SessionConst.TCP_CONNECTION_TYPE) {
            // 如果自己是重定向IP，肯定是最好的
            if (mServerType == SessionConst.REDIRECT_IP)
                return true;

            // 只有对方是tcp时，才不比它好；否则都是自己最好
            if (serverProfile.getProtocol() == SessionConst.TCP_CONNECTION_TYPE) {
                if (serverProfile.getServerType() == SessionConst.REDIRECT_IP)
                    return false;
                else {
                    return true;
                }
            } else
                return true;
        }
        return false;
    }

    /**
     * @return the mServerType
     */
    public int getServerType() {
        return mServerType;
    }

    /**
     * @param serverType the mServerType to set
     */
    public void setServerType(int serverType) {
        this.mServerType = serverType;
    }

    /**
     * @return the mServerIP
     */
    public String getServerIP() {
        return mServerIP;
    }

    /**
     * @param serverIP the mServerIP to set
     */
    public void setServerIP(String serverIP) {
        this.mServerIP = serverIP;
    }

    /**
     * @return the mServerPort
     */
    public int getServerPort() {
        return mServerPort;
    }

    /**
     * @param serverPort the mServerPort to set
     */
    public void setServerPort(int serverPort) {
        this.mServerPort = serverPort;
    }

    /**
     * @return the mProxyIP
     */
    public String getProxyIP() {
        return mProxyIP;
    }

    /**
     * @param proxyIP the mProxyIP to set
     */
    public void setProxyIP(String proxyIP) {
        this.mProxyIP = proxyIP;
    }

    /**
     * @return the mPorxyPort
     */
    public int getPorxyPort() {
        return mPorxyPort;
    }

    /**
     * @param porxyPort the mPorxyPort to set
     */
    public void setPorxyPort(int porxyPort) {
        this.mPorxyPort = porxyPort;
    }

    /**
     * @return the mProtocol
     */
    public int getProtocol() {
        return mProtocol;
    }

    /**
     * @param mProtocol the mProtocol to set
     */
    public void setProtocol(int protocol) {
        this.mProtocol = protocol;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[ ");
        sb.append("sIP=").append(mServerIP);
        sb.append(", sPort=").append(mServerPort);
        sb.append(", pIP=").append(mProxyIP);
        sb.append(", pPort=").append(mPorxyPort);
        sb.append(", protocol=").append( SessionConst.getProtocol(mProtocol));
        sb.append(", type=").append(SessionConst.getSeverType(mServerType));
        sb.append(" ]");
        return sb.toString();
    }
}

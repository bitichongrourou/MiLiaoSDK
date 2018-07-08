
package com.mi.milink.sdk.session.common;

import java.io.Serializable;

/**
 * @author MK
 */
public class RecentlyServerData extends ServerData implements Serializable {
    private static final long serialVersionUID = -8451531193007968621L;

    private ServerProfile mRecentlyServer = null;

    public void setRecentlyServer(ServerProfile recentlyTcpServerProfile) {
        mRecentlyServer = recentlyTcpServerProfile;
    }

    public ServerProfile getRecentlyServer() {
        return mRecentlyServer;
    }

    @Override
    public String toString() {
        return "[recentlyTcpServerProfile = "
                + (mRecentlyServer != null ? mRecentlyServer.toString() : null) + ",timeStamp = "
                + getTimeStamp() + "]";
    }
}

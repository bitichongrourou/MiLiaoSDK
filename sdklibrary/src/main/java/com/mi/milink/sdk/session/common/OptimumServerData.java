
package com.mi.milink.sdk.session.common;

import java.io.Serializable;
import java.util.List;

import com.mi.milink.sdk.util.CommonUtils;

/**
 * @author MK
 */
public class OptimumServerData extends ServerData implements Serializable {
    private static final long serialVersionUID = -8399070197793626196L;

    private List<ServerProfile> mOptimumServers = null;

    public void setOptimumServers(List<ServerProfile> optimumServers) {
        mOptimumServers = optimumServers;
    }

    public List<ServerProfile> getOptimumServers() {
        return mOptimumServers;
    }

    @Override
    public String toString() {
        return "[optimum servers = " + CommonUtils.join(mOptimumServers, ";") + ",timeStamp = "
                + getTimeStamp() + "]";
    }
}


package com.mi.milink.sdk.speedtest;

import java.io.Serializable;

import org.json.JSONObject;

import com.mi.milink.sdk.base.Global;
import com.mi.milink.sdk.base.os.info.NetworkDash;

/**
 * UDP 测速数据类
 * @author MK
 *
 */
public class SpeedTestReportUdpTestInfo implements Serializable {

    private static final long serialVersionUID = -7489351145667058626L;

    private static final String KEY_U_CLIENT_IP = "u_cip";

    private static final String KEY_U_CLIENT_ISP = "u_net_type";
    
    private static final String KEY_U_READ_TIMEOUT = "u_read_timeout";

    private static final String KEY_U_APN = "u_apn";

    private static final String KEY_U_SERVER_IP = "u_sip";

    private static final String KEY_U_PORT = "u_port";

    private static final String KEY_U_B_STATUS = "u_b_status";

    private static final String KEY_U_B_RTT = "u_b_rtt";

    private static final String KEY_U_S_STATUS = "u_s_status";

    private static final String KEY_U_S_RTT = "u_s_rtt";

    public String clientIp;
    
    public int readTimeout = 0;

    public String clientIsp;

    public String apn;

    public String serverIp;

    public int port = 0;

    public int bigStatus = 0;

    public long bigRtt = 0;

    public int smallStatus = 0;

    public long smallRtt = 0;

    public SpeedTestReportUdpTestInfo() {
        clientIp = Global.getClientIp();
        clientIsp = Global.getClientIsp();
        apn = NetworkDash.getApnName();
    }

    public JSONObject toJSONObject() {
        JSONObject object = new JSONObject();
        try {
            object.put(KEY_U_CLIENT_IP, clientIp);
            object.put(KEY_U_READ_TIMEOUT, readTimeout);
            object.put(KEY_U_CLIENT_ISP, clientIsp);
            object.put(KEY_U_APN, apn);
            object.put(KEY_U_SERVER_IP, serverIp);
            object.put(KEY_U_PORT, port);
            object.put(KEY_U_B_STATUS, bigStatus);
            object.put(KEY_U_B_RTT, bigRtt);
            object.put(KEY_U_S_STATUS, smallStatus);
            object.put(KEY_U_S_RTT, smallRtt);

        } catch (Exception e) {
        }
        return object;
    }
    
    @Override
    public String toString() {
        return toJSONObject().toString();
    }

}

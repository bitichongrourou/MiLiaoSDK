package com.mi.milink.sdk.speedtest;

import java.io.Serializable;

import org.json.JSONObject;

import com.mi.milink.sdk.base.Global;
import com.mi.milink.sdk.base.os.info.NetworkDash;
/**
 * TCP 测速数据类
 * @author MK
 *
 */
public class SpeedTestReportTcpTestInfo implements Serializable {
    
    private static final long serialVersionUID = -8004880902871144705L;
    
    private static final String KEY_T_CLIENT_IP = "t_cip";
    private static final String KEY_T_CONNECT_TIMEOUT = "t_connect_timeout";
    private static final String KEY_T_READ_TIMEOUT = "t_read_timeout";
    private static final String KEY_T_CLIENT_ISP = "t_net_type";
    private static final String KEY_T_APN = "t_apn";
    private static final String KEY_T_SERVER_IP = "t_sip";
    private static final String KEY_T_PORT = "t_port";
    private static final String KEY_T_CONNECT_TIME = "t_connect_time";
    private static final String KEY_T_STATUS = "t_status";
    private static final String KEY_T_RTT = "t_rtt";
    
    public String clientIp;
    
    public int connectTimeout = 0;
    
    public int readTimeout = 0;
    
    public String clientIsp;
    
    public String apn;
    
    public String serverIp;
    
    public int port = 0;
    
    public long connectTime = 0;
    
    public int status = 0;
    
    public long rtt = 0;
    
    public SpeedTestReportTcpTestInfo() {
        clientIp = Global.getClientIp();
        clientIsp = Global.getClientIsp();
        apn = NetworkDash.getApnName();
    }
    
    public JSONObject toJSONObject() {
        JSONObject object = new JSONObject();
        try {
            object.put(KEY_T_CLIENT_IP, clientIp);
            object.put(KEY_T_CONNECT_TIMEOUT, connectTimeout);
            object.put(KEY_T_READ_TIMEOUT, readTimeout);
            object.put(KEY_T_CLIENT_ISP, clientIsp);
            object.put(KEY_T_APN, apn);
            object.put(KEY_T_SERVER_IP, serverIp);
            object.put(KEY_T_PORT, port);
            object.put(KEY_T_CONNECT_TIME, connectTime);
            object.put(KEY_T_STATUS, status);
            object.put(KEY_T_RTT, rtt);
        } catch (Exception e) {
        }
        return object;
    }
    
    @Override
    public String toString() {
        return toJSONObject().toString();
    }

}


package com.mi.milink.sdk.session.common;

import java.util.ArrayList;

public class OpenSessionSucessReturnInfo {
    private String clientIp;

    private String clientIsp;

    private ArrayList<ServerProfile> optmumServerList;

    private ArrayList<ServerProfile> backupServerList;

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getClientIsp() {
        return clientIsp;
    }

    public void setClientIsp(String clientIsp) {
        this.clientIsp = clientIsp;
    }

    public ArrayList<ServerProfile> getOptmumServerList() {
        return optmumServerList;
    }

    public void setOptmumServerList(ArrayList<ServerProfile> optmumServerList) {
        this.optmumServerList = optmumServerList;
    }

    public ArrayList<ServerProfile> getBackupServerList() {
        return backupServerList;
    }

    public void setBackupServerList(ArrayList<ServerProfile> backupServerList) {
        this.backupServerList = backupServerList;
    }

    public OpenSessionSucessReturnInfo(String clientIp, String clienIsp,
            ArrayList<ServerProfile> optmumServerList, ArrayList<ServerProfile> backupServerList) {
        super();
        this.clientIp = clientIp;
        this.clientIsp = clienIsp;
        this.optmumServerList = optmumServerList;
        this.backupServerList = backupServerList;
    }

}

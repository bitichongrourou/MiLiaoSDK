
package com.mi.milink.sdk.config;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.mi.milink.sdk.base.Global;
import com.mi.milink.sdk.base.os.Device.Network;
import com.mi.milink.sdk.base.os.info.NetworkDash;
import com.mi.milink.sdk.base.os.info.WifiDash;
import com.mi.milink.sdk.data.ClientAppInfo;
import com.mi.milink.sdk.debug.MiLinkLog;
import com.mi.milink.sdk.session.common.OptimumServerData;
import com.mi.milink.sdk.session.common.RecentlyServerData;
import com.mi.milink.sdk.session.common.ServerProfile;
import com.mi.milink.sdk.session.common.SessionConst;

import android.content.Context;
import android.text.TextUtils;

public abstract class IIpInfoManager {

    protected AppIpConfig mIpInfo = null;

    private static final String DEFAULT_OPTIMUM_SERVER_KEY = "other";

    // 保底ip列表
    private List<ServerProfile> mBackupIPList = null;

    // key是isp的文本表示，比如：移动、联通、电信、教育网等
    private ConcurrentHashMap<String, OptimumServerData> mOptimumIpMap = null;

    // key是apn的字符表示
    private ConcurrentHashMap<String, RecentlyServerData> mRcentlyIpMap = null;

    // key是apn的字符表示，value是isp的文本表示
    private ConcurrentHashMap<String, String> mApnIspMap = null;

    protected static final String TAG = "IIpInfoManager";

    protected IIpInfoManager() {
        init();
    }

    protected abstract String getOptimumServerFileName();

    protected abstract String getBackupServerFileName();

    protected abstract String getRecentlyServerFileName();

    protected abstract String getApnIspFileName();

    public abstract void destroy();

    public String getDefaultHost() {
        return mIpInfo.getHost();
    }

    public ServerProfile[] getTestBackupIp() {
        return mIpInfo.getTestBackupIpList();
    }

    public ServerProfile getDefaultServer() {// 保底域名
        return new ServerProfile(getDefaultHost(), 0, SessionConst.TCP_CONNECTION_TYPE, SessionConst.DOMAIN_IP);
    }

    public synchronized OptimumServerData getCurrentApnOptimumServerData() {
        String ispKey = DEFAULT_OPTIMUM_SERVER_KEY;
        String apnKey = getCurrentApn();
        if (!TextUtils.isEmpty(apnKey)) {
            ispKey = getApnIspMap().get(apnKey);
        }
        MiLinkLog.v(TAG, "get current apn optimum server list, apnKey is " + apnKey + ", ispKey is" + ispKey);
        return getOptimumServerData(ispKey);
    }

    /**
     * @param ispKey 是服务器返回的客户端的运营商类型
     * @return
     */
    private synchronized OptimumServerData getOptimumServerData(String ispKey) {
        if (TextUtils.isEmpty(ispKey)) {
            MiLinkLog.v(TAG, "get optimum server list, the value of the key is empty, use default key");
            ispKey = DEFAULT_OPTIMUM_SERVER_KEY;
        } else {
            MiLinkLog.v(TAG, "get optimum server list, key is " + ispKey);
        }
        OptimumServerData osd = getOptimumIpMap().get(ispKey);
        MiLinkLog.v(TAG, "getOptimumServerData serverData:" + osd + ",ispKey:" + ispKey);
        return osd;
    }

    /**
     * @param ispKey     是服务器返回的客户端的运营商类型
     * @param serverList
     */
    public synchronized void setOptmumServerList(String ispKey, List<ServerProfile> serverList) {
        if (serverList != null && !serverList.isEmpty()) {
            if (TextUtils.isEmpty(ispKey)) {
                MiLinkLog.v(TAG, "set optimum server list, but key is empty, use default key");
                ispKey = DEFAULT_OPTIMUM_SERVER_KEY;
            }
            String apnKey = getCurrentApn();
            if (!TextUtils.isEmpty(apnKey)) { // 保存apn和isp对应关系
                getApnIspMap().put(apnKey, ispKey);
                saveApnIspMap();
            }
            OptimumServerData serverData = getOptimumServerData(ispKey);
            if (serverData == null) {
                serverData = new OptimumServerData();
            }
            serverData.setOptimumServers(serverList);
            serverData.setTimeStamp(System.currentTimeMillis());

            getOptimumIpMap().put(ispKey, serverData);
            saveOptimumServerMap();
            MiLinkLog.w(TAG,
                    "setOptmumServerList serverData:" + serverData + ",ispKey:" + ispKey + ",apnKey:" + apnKey);
        } else {
            MiLinkLog.w(TAG, "serverList is null");
        }
    }

    public synchronized RecentlyServerData getRecentlyServerData() {

        String apnKey = getCurrentApn();
        if (!TextUtils.isEmpty(apnKey)) {

            try {
                RecentlyServerData serverData = getRcentlyIpMap().get(apnKey);
                MiLinkLog.v(TAG, "getRecentlyServerData serverData:" + serverData + ",apnKey:" + apnKey);
                return serverData;
            } catch (Exception e) {
                return null;
            }

        }
        return null;
    }

    public synchronized void setRecentlyServer(ServerProfile server) {
        if (server != null) {
            RecentlyServerData serverData = getRecentlyServerData();
            if (serverData == null) {
                serverData = new RecentlyServerData();
            }
            serverData.setRecentlyServer(server);
            serverData.setTimeStamp(System.currentTimeMillis());
            String apnKey = getCurrentApn();
            if (!TextUtils.isEmpty(apnKey)) {
                getRcentlyIpMap().put(apnKey, serverData);
                saveRecentlyServerMap();
                MiLinkLog.v(TAG, "setRecentlyServer serverData:" + serverData + ",apnKey:" + apnKey);
            } else {
                MiLinkLog.v(TAG, "set recently server list, but key is null");
            }
        }
    }

    public synchronized void setBackupServerList(List<ServerProfile> serverList) {
        if (serverList != null && !serverList.isEmpty()) {
            mBackupIPList = serverList; // 保底IP直接覆盖
            saveBackupServerList();
        }
    }

    public synchronized List<ServerProfile> getBackupServerList() {
        if (mBackupIPList == null) {
            try {
                mBackupIPList = (List<ServerProfile>) loadObject(getBackupServerFileName());
            } catch (Exception e) {
                mBackupIPList = null;
            }
            if (mBackupIPList == null) {
                mBackupIPList = new ArrayList<ServerProfile>();
            }
        }
        if (mBackupIPList.isEmpty()) {
            ServerProfile[] defaultBackupIpList = new ServerProfile[0];
            defaultBackupIpList = mIpInfo.getOnlineBackupIpList();
            for (int i = 0; i < defaultBackupIpList.length; i++) {
                mBackupIPList.add(defaultBackupIpList[i]);
            }
        }
        return mBackupIPList;
    }

    public synchronized ConcurrentHashMap<String, OptimumServerData> getOptimumIpMap() {
        if (mOptimumIpMap == null) {
            try {
                mOptimumIpMap = (ConcurrentHashMap<String, OptimumServerData>) loadObject(getOptimumServerFileName());
            } catch (Exception e) {
                mOptimumIpMap = null;
            }
            if (mOptimumIpMap == null) {
                mOptimumIpMap = new ConcurrentHashMap<String, OptimumServerData>();
            }
        }
        return mOptimumIpMap;
    }

    public synchronized ConcurrentHashMap<String, RecentlyServerData> getRcentlyIpMap() {
        if (mRcentlyIpMap == null) {
            try {
                mRcentlyIpMap = (ConcurrentHashMap<String, RecentlyServerData>) loadObject(getRecentlyServerFileName());
            } catch (Exception e) {
                mRcentlyIpMap = null;
            }
            if (mRcentlyIpMap == null) {
                mRcentlyIpMap = new ConcurrentHashMap<String, RecentlyServerData>();
            }
        }
        return mRcentlyIpMap;
    }

    public synchronized ConcurrentHashMap<String, String> getApnIspMap() {
        if (mApnIspMap == null) {
            try {
                mApnIspMap = (ConcurrentHashMap<String, String>) loadObject(getApnIspFileName());
            } catch (Exception e) {
                mApnIspMap = null;
            }
            if (mApnIspMap == null) {
                mApnIspMap = new ConcurrentHashMap<String, String>();
            }
        }
        return mApnIspMap;
    }

    private synchronized boolean saveOptimumServerMap() {
        return saveObject(mOptimumIpMap, getOptimumServerFileName());
    }

    private synchronized boolean saveBackupServerList() {
        return saveObject(mBackupIPList, getBackupServerFileName());
    }

    private synchronized boolean saveRecentlyServerMap() {
        return saveObject(mRcentlyIpMap, getRecentlyServerFileName());
    }

    private synchronized boolean saveApnIspMap() {
        return saveObject(mApnIspMap, getApnIspFileName());
    }

    public static String getCurrentApn() {
        String key = null;
        MiLinkLog.d(TAG, "start getCurrentApn ");
        if (NetworkDash.isMobile()) {
            key = NetworkDash.getApnName();
        } else if (NetworkDash.isWifi()) {
            key = WifiDash.getBSSID();
        } else if (NetworkDash.isEthernet()) {
            key = "ethernet";
        } else {
            // error
            MiLinkLog.i(TAG, "Network(" + NetworkDash.getType() + ") is unkown");
        }
        // 如果获取到的bssid全为0，也不保存
        if ("00:00:00:00:00:00".equals(key)) {
            key = null;
        }
        MiLinkLog.d(TAG, "end getCurrentApn key = " + key);
        return key;
    }

    public static boolean saveObject(Object obj, String fileName) {
        MiLinkLog.i(TAG, "save " + fileName);

        ObjectOutputStream objectOut = null;
        Context context = Global.getApplicationContext();
        if (context == null) {
            MiLinkLog.e(TAG, "save object Global.getApplicationContext() == null");
            return false;
        }
        try {
            FileOutputStream of = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            objectOut = new ObjectOutputStream(new BufferedOutputStream(of));
            objectOut.writeObject(obj);
        } catch (Exception e) {
            MiLinkLog.e(TAG, "writeObject Exception", e);
        }

        try {
            if (objectOut != null) {
                objectOut.close();
            }
        } catch (Exception e) {
            MiLinkLog.e(TAG, "closeObject Exception", e);
        }
        return true;
    }

    public static Object loadObject(String fileName) {
        MiLinkLog.i(TAG, "load " + fileName);
        ObjectInputStream objectIn = null;
        Object obj = null;
        Context context = Global.getApplicationContext();
        if (context == null) {
            MiLinkLog.e(TAG, "load object Global.getApplicationContext() == null");
            return null;
        }

        FileInputStream fis;
        try {
            fis = context.openFileInput(fileName);
        } catch (FileNotFoundException e) {
            MiLinkLog.e(TAG, "load object FileNotFoundException");
            return null;
        }
        try {
            objectIn = new ObjectInputStream(fis);
            obj = objectIn.readObject();
        } catch (Exception e) {
            MiLinkLog.e(TAG, "load readObject Exception", e);
            context.deleteFile(fileName);

            try {
                if (objectIn != null) {
                    objectIn.close();
                }
            } catch (Exception e1) {
                MiLinkLog.e(TAG, "closeObject Exception", e1);
            }
            return null;
        }

        try {
            if (objectIn != null) {
                objectIn.close();
            }
        } catch (Exception e) {
            MiLinkLog.e(TAG, "closeObject Exception", e);
        }

        return obj;
    }

    protected void init() {

        switch (Global.getClientAppInfo().getAppId()) {
            case ClientAppInfo.MILIAO_APP_ID:
            case ClientAppInfo.MILIAO_APP_ID_TEMP: {
//                String MILIAO_DEFAULT_HOST = "milink.chat.mi.com";
                String MILIAO_DEFAULT_HOST = "mixchat.yeejay.com";
                ServerProfile[] MILIAO_DEFAULT_BACKUP_IP_LIST = new ServerProfile[]{
//                        new ServerProfile("58.83.160.100", 0, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP),
//                        new ServerProfile("120.131.6.160", 0, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP),
//                        new ServerProfile("123.59.39.164", 0, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP)};
                        new ServerProfile("123.206.116.59", 14000, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP),
                        new ServerProfile("123.206.116.59", 14000, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP),
                        new ServerProfile("123.206.116.59", 14000, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP)};

                ServerProfile[] MILIAO_TEST_BACKUP_IP_LIST = new ServerProfile[]{
//                        new ServerProfile("111.206.200.91", 0, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP)};
                        new ServerProfile("123.206.116.59", 14000, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP)};
                mIpInfo = new AppIpConfig(MILIAO_DEFAULT_HOST, MILIAO_DEFAULT_BACKUP_IP_LIST, MILIAO_TEST_BACKUP_IP_LIST);
            }

            break;
            /*
            case ClientAppInfo.VTALK_APP_ID: {
                String VTALK_DEFAULT_HOST = "link.g.mi.com";
                ServerProfile[] VTALK_DEFAULT_BACKUP_IP_LIST = new ServerProfile[]{
                        new ServerProfile("120.134.33.114", 0, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP),
                        new ServerProfile("42.62.94.188", 0, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP)};

                ServerProfile[] VTALK_TEST_BACKUP_IP_LIST = new ServerProfile[]{
                        new ServerProfile("42.62.94.226", 0, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP)};

                mIpInfo = new AppIpConfig(VTALK_DEFAULT_HOST, VTALK_DEFAULT_BACKUP_IP_LIST, VTALK_TEST_BACKUP_IP_LIST);
            }

            break;

            case ClientAppInfo.ON_APP_ID: {
                String ON_DEFAULT_HOST = "link.zifei.com";
                ServerProfile[] ON_DEFAULT_BACKUP_IP_LIST = new ServerProfile[]{
                        new ServerProfile("120.134.33.114", 0, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP),
                        new ServerProfile("42.62.94.188", 0, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP)};

                ServerProfile[] ON_TEST_BACKUP_IP_LIST = new ServerProfile[]{
                        new ServerProfile("42.62.94.50", 0, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP)};
                mIpInfo = new AppIpConfig(ON_DEFAULT_HOST, ON_DEFAULT_BACKUP_IP_LIST, ON_TEST_BACKUP_IP_LIST);
            }

            break;
            case ClientAppInfo.FORUM_APP_ID: {
                ServerProfile[] FORUM_DEFAULT_BACKUP_IP_LIST = new ServerProfile[]{
                        new ServerProfile("120.134.33.152", 0, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP)};

                ServerProfile[] FORUM_TEST_BACKUP_IP_LIST = new ServerProfile[]{
                        new ServerProfile("42.62.94.226", 0, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP)};
                mIpInfo = new AppIpConfig("ent.g.mi.com", FORUM_DEFAULT_BACKUP_IP_LIST, FORUM_TEST_BACKUP_IP_LIST);
            }

            break;
            case ClientAppInfo.GAME_CENTER_APP_ID: {
                String GAME_CENTER_DEFAULT_HOST = "migc.g.mi.com";
                ServerProfile[] GAME_CENTER_DEFAULT_BACKUP_IP_LIST = new ServerProfile[]{
                        new ServerProfile("58.83.160.115", 0, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP),
                        new ServerProfile("124.243.204.75", 0, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP)};

                ServerProfile[] GAME_CENTER_TEST_BACKUP_IP_LIST = new ServerProfile[]{
                        new ServerProfile("42.62.94.101", 0, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP)};
                mIpInfo = new AppIpConfig(GAME_CENTER_DEFAULT_HOST, GAME_CENTER_DEFAULT_BACKUP_IP_LIST,
                        GAME_CENTER_TEST_BACKUP_IP_LIST);
            }

            break;
            case ClientAppInfo.XIAOMI_PUSH_APP_ID: {
                ServerProfile[] XIAOMI_PUSH_DEFAULT_BACKUP_IP_LIST = new ServerProfile[]{
                        new ServerProfile("58.83.160.115", 0, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP),
                        new ServerProfile("124.243.204.80", 0, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP)};

                ServerProfile[] XIAOMI_PUSH_TEST_BACKUP_IP_LIST = new ServerProfile[]{
                        new ServerProfile("10.99.184.86", 0, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP)};
                mIpInfo = new AppIpConfig("mipush.g.mi.com", XIAOMI_PUSH_DEFAULT_BACKUP_IP_LIST,
                        XIAOMI_PUSH_TEST_BACKUP_IP_LIST);
            }
            break;
            case ClientAppInfo.SUPPORT_APP_ID: {
                String VTALK_DEFAULT_HOST = "milink.misupport.mi.com";
                ServerProfile[] VTALK_DEFAULT_BACKUP_IP_LIST = new ServerProfile[]{
                        new ServerProfile("124.243.204.139", 0, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP)};

                ServerProfile[] VTALK_TEST_BACKUP_IP_LIST = new ServerProfile[]{
                        new ServerProfile("42.62.94.226", 0, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP)};

                mIpInfo = new AppIpConfig(VTALK_DEFAULT_HOST, VTALK_DEFAULT_BACKUP_IP_LIST, VTALK_TEST_BACKUP_IP_LIST);
            }
            break;
            case ClientAppInfo.MILIAO_2:
            case ClientAppInfo.LIVE_SDK_APP_ID:
            case ClientAppInfo.LIVE_APP_ID: {
                ServerProfile[] LIVE_DEFAULT_BACKUP_IP_LIST = null;

                if (Global.getClientAppInfo().getReleaseChannel().equals("meng_1254_11_android")) {
                    // 海外版的保底ip
                    LIVE_DEFAULT_BACKUP_IP_LIST = new ServerProfile[]{
                            new ServerProfile("103.241.229.132", 0, SessionConst.TCP_CONNECTION_TYPE,
                                    SessionConst.BACKUP_IP),
                            new ServerProfile("103.241.229.133", 0, SessionConst.TCP_CONNECTION_TYPE,
                                    SessionConst.BACKUP_IP),
                            new ServerProfile("118.193.18.228", 0, SessionConst.TCP_CONNECTION_TYPE,
                                    SessionConst.BACKUP_IP),
                            new ServerProfile("118.193.18.229", 0, SessionConst.TCP_CONNECTION_TYPE,
                                    SessionConst.BACKUP_IP)};
                } else {
                    // 国内版的保底ip
                    LIVE_DEFAULT_BACKUP_IP_LIST = new ServerProfile[]{
                            new ServerProfile("58.83.160.92", 0, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP),
                            new ServerProfile("124.243.204.126", 0, SessionConst.TCP_CONNECTION_TYPE,
                                    SessionConst.BACKUP_IP),
                            new ServerProfile("120.92.2.6", 0, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP),};
                }

                ServerProfile[] LIVE_TEST_BACKUP_IP_LIST = new ServerProfile[]{
                        new ServerProfile("111.206.200.91", 0, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP)};
                mIpInfo = new AppIpConfig("milink.zb.mi.com", LIVE_DEFAULT_BACKUP_IP_LIST, LIVE_TEST_BACKUP_IP_LIST);
            }
            break;

            case ClientAppInfo.MI_SHOP_APP_ID: {
                ServerProfile[] LIVE_DEFAULT_BACKUP_IP_LIST = new ServerProfile[]{
                        new ServerProfile("120.92.24.145", 0, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP),
                        new ServerProfile("58.83.177.15", 0, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP)};

                ServerProfile[] LIVE_TEST_BACKUP_IP_LIST = new ServerProfile[]{
                        new ServerProfile("42.62.94.31", 0, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP)};
                mIpInfo = new AppIpConfig("mishop.g.mi.com", LIVE_DEFAULT_BACKUP_IP_LIST, LIVE_TEST_BACKUP_IP_LIST);
            }
            break;

            case ClientAppInfo.CARTOON_APP_ID: {
                ServerProfile[] LIVE_DEFAULT_BACKUP_IP_LIST = new ServerProfile[]{
                        new ServerProfile("42.62.94.50", 0, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP)};

                ServerProfile[] LIVE_TEST_BACKUP_IP_LIST = new ServerProfile[]{
                        new ServerProfile("42.62.94.50", 0, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP),};
                mIpInfo = new AppIpConfig("milink.ac.mi.com", LIVE_DEFAULT_BACKUP_IP_LIST, LIVE_TEST_BACKUP_IP_LIST);
            }

            break;

            case ClientAppInfo.KNIGHTS_APP_ID: {
                ServerProfile[] LIVE_DEFAULT_BACKUP_IP_LIST = new ServerProfile[]{
                        new ServerProfile("58.83.177.14", 0, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP)};

                ServerProfile[] LIVE_TEST_BACKUP_IP_LIST = new ServerProfile[]{
                        new ServerProfile("42.62.94.50", 0, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP),};
                mIpInfo = new AppIpConfig("knights.g.mi.com", LIVE_DEFAULT_BACKUP_IP_LIST, LIVE_TEST_BACKUP_IP_LIST);
            }
            break;
            case ClientAppInfo.GAME_LOGIN_PAY_SDK: {
                ServerProfile[] LIVE_DEFAULT_BACKUP_IP_LIST = new ServerProfile[]{
                        new ServerProfile("58.83.160.173", 0, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP),
                        new ServerProfile("42.62.94.23", 0, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP),
                        new ServerProfile("120.92.24.135", 0, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP)
                };

                ServerProfile[] LIVE_TEST_BACKUP_IP_LIST = new ServerProfile[]{
                        new ServerProfile("42.62.94.12", 0, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP)};
                mIpInfo = new AppIpConfig("gmsdk.g.mi.com", LIVE_DEFAULT_BACKUP_IP_LIST, LIVE_TEST_BACKUP_IP_LIST);
            }
            break;
            case ClientAppInfo.YI_MI_BUY: {
                ServerProfile[] LIVE_DEFAULT_BACKUP_IP_LIST = new ServerProfile[]{
                        new ServerProfile("120.92.24.145", 0, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP),
                        new ServerProfile("58.83.177.15", 0, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP)};

                ServerProfile[] LIVE_TEST_BACKUP_IP_LIST = new ServerProfile[]{
                        new ServerProfile("42.62.94.31", 0, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP)};
                mIpInfo = new AppIpConfig("milink.go.g.mi.com", LIVE_DEFAULT_BACKUP_IP_LIST, LIVE_TEST_BACKUP_IP_LIST);
            }
            break;
            case ClientAppInfo.MI_NEW_GAME_CENTER_APP_ID: {

                ServerProfile[] LIVE_DEFAULT_BACKUP_IP_LIST = new ServerProfile[]{
                        new ServerProfile("58.83.177.14", 0, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP)};

                ServerProfile[] LIVE_TEST_BACKUP_IP_LIST = new ServerProfile[]{
                        new ServerProfile("42.62.94.50", 0, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP),};
                mIpInfo = new AppIpConfig("milink.migc.g.mi.com", LIVE_DEFAULT_BACKUP_IP_LIST, LIVE_TEST_BACKUP_IP_LIST);

            }
            break;
            */
            default:
                break;
        }

        /*
        if (Global.getClientAppInfo().getAppId() >= ClientAppInfo.LIVE_PUSH_SDK_BOTTOM
                && Global.getClientAppInfo().getAppId() < ClientAppInfo.LIVE_PUSH_SDK_UP) {
            ServerProfile[] LIVE_DEFAULT_BACKUP_IP_LIST = new ServerProfile[]{
                    new ServerProfile("123.206.116.59", 11000, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP)};
//                    new ServerProfile("58.83.160.92", 0, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP),
//                    new ServerProfile("124.243.204.126", 0, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP),
//                    new ServerProfile("120.92.2.6", 0, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP),};

            ServerProfile[] LIVE_TEST_BACKUP_IP_LIST = new ServerProfile[]{
//                    new ServerProfile("10.105.44.12", 0, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP)};
                    new ServerProfile("123.206.116.59", 11000, SessionConst.TCP_CONNECTION_TYPE, SessionConst.BACKUP_IP)};
            mIpInfo = new AppIpConfig("milink.ac.mi.com", LIVE_DEFAULT_BACKUP_IP_LIST, LIVE_TEST_BACKUP_IP_LIST);
        }
        */
    }

    protected static class AppIpConfig {
        private String host;

        private ServerProfile[] onlineBackupIpList;

        private ServerProfile[] testBackupIpList;

        public AppIpConfig(String host, ServerProfile[] onlineBackupIpList, ServerProfile[] testBackupIpList) {
            this.host = host;
            this.onlineBackupIpList = onlineBackupIpList;
            this.testBackupIpList = testBackupIpList;
        }

        public String getHost() {
            return host;
        }

        public ServerProfile[] getOnlineBackupIpList() {
            return onlineBackupIpList;
        }

        public ServerProfile[] getTestBackupIpList() {
            return testBackupIpList;
        }

    }
}

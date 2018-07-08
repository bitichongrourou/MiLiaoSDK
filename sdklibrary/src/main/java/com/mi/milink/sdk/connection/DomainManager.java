
package com.mi.milink.sdk.connection;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;

import com.mi.milink.sdk.base.os.Device.Network;
import com.mi.milink.sdk.base.os.info.AccessPoint;
import com.mi.milink.sdk.base.os.info.DnsDash;
import com.mi.milink.sdk.base.os.info.NetworkDash;
import com.mi.milink.sdk.base.os.info.WifiDash;
import com.mi.milink.sdk.config.ConfigManager;
import com.mi.milink.sdk.data.Const;
import com.mi.milink.sdk.debug.InternalDataMonitor;
import com.mi.milink.sdk.debug.MiLinkLog;

/**
 * 域名解析的类
 *
 */
public class DomainManager {
    private static final String TAG = "DomainManager";

    private static final int SLEEP_INTERVAL = 10; // 10ms

    public static final int RET_CODE_DNS_SUCCESS = 0;

    public static final int RET_CODE_DNS_UNKNOWN_HOST = 10000;

    public static final int RET_CODE_DNS_TIME_OUT = 10001;

    public static final int RET_CODE_DNS_LOCAL_EXCEPTION = 10002;

    private String mKey = AccessPoint.NONE.getName();

    private ConcurrentHashMap<String, String> mDomainMap = new ConcurrentHashMap<String, String>();

    private static DomainManager sInstanse = null;

    private ResolveThread[] mDefaultHostThreads = null;

    private final static int THREAD_MAX_COUNT = 5;

    private DomainManager() {
        mDefaultHostThreads = new ResolveThread[THREAD_MAX_COUNT];
    }

    public static synchronized DomainManager getInstance() {
        if (null == sInstanse) {
            sInstanse = new DomainManager();
        }

        return sInstanse;
    }

    public void startResolve(String domain) {
        if (!NetworkDash.isAvailable()) {
            MiLinkLog.i(TAG, "startResolve, but network is unavailable");
            return;
        }
        MiLinkLog.i(TAG, "startResolve");
        if (isNeedResolve()) {
            mDomainMap.clear();
            startDnsThread(domain);
        }
    }

    public String queryDomainIP(String domain) {
        String ip = mDomainMap.get(domain);
        if (ip == null) {
            ip = domain;
        }
        return ip;
    }

    public String getDomainIP(String domain) {
        String ip = mDomainMap.get(domain);
        if (ip == null) {
            long timeout = ConfigManager.getInstance().getDnsTimeout();
            long timepassed = 0;
            long beginTime = System.currentTimeMillis();
            ResolveThread thread = startDnsThread(domain);
            if (thread == null) {
                return null;
            }
            while (true) {
                ip = mDomainMap.get(domain);
                if (ip == null) {
                    if (timepassed > timeout || thread.isCompleted()) {
                        ip = mDomainMap.get(domain);
                        if (timepassed > timeout && ip == null) {
                            statistic(beginTime, domain, ip, RET_CODE_DNS_TIME_OUT);
                        }
                        break;
                    }
                    try {
                        Thread.sleep(SLEEP_INTERVAL);
                    } catch (InterruptedException e) {
                        MiLinkLog.e(TAG, "getDomainIP InterruptedException", e);
                        return null;
                    }
                    timepassed += SLEEP_INTERVAL;
                } else {
                    return ip;
                }
            }
        }
        return ip;
    }

    private synchronized ResolveThread startDnsThread(String domain) {
        int i = 0;
        for (i = 0; i < THREAD_MAX_COUNT; i++) {
            if (mDefaultHostThreads[i] != null && mDefaultHostThreads[i].isAlive()) {
                if (mDefaultHostThreads[i].getKey() != mKey) {
                    mDefaultHostThreads[i].setExpired(true);
                } else {
                    // 两个key相等，都为null时，也要设置当前线程失效
                    if (mKey != null) {
                        return mDefaultHostThreads[i];
                    } else {
                        mDefaultHostThreads[i].setExpired(true);
                    }
                }
            } else {
                MiLinkLog.i(TAG, "startDefaultHostThread");
                mDefaultHostThreads[i] = new ResolveThread(domain, mKey);
                mDefaultHostThreads[i].setDaemon(true);
                mDefaultHostThreads[i].start();
                return mDefaultHostThreads[i];
            }
        }

        if (i == THREAD_MAX_COUNT) {
            MiLinkLog.e(TAG, "startDefaultHostThread running thread is more than "
                    + THREAD_MAX_COUNT);
        }

        return null;
    }

    private String getKey() {
        String key = null;
        if (NetworkDash.isMobile()) {
            key = NetworkDash.getApnName();
        } else if (NetworkDash.isWifi()) {
            key = WifiDash.getBSSID();
        } else {
            // error
            MiLinkLog.e(TAG, "getKey Network(" + NetworkDash.getType() + ") is unkown");
        }

        // 如果获取到的bssid全为0，也不保存
        if ("00:00:00:00:00:00".equals(key)) {
            key = null;
        }

        return key;
    }

    private boolean isNeedResolve() {
        String key = getKey();

        // 如果key是空，强制要求重新解析
        if (key == null) {
            mKey = null;
            return true;
        }

        // 如果key不相等
        if (!key.equalsIgnoreCase(mKey)) {
            mKey = key;
            return true;
        }

        return false;
    }

    private void setDomainIP(String domain, String ip) {
        mDomainMap.put(domain, ip);
    }

    private void statistic(long beginTime, String domian, String ip, int errCode) {
        String detailString = "domain [domain = " + domian + ",ip = " + ip + ",client localDNS = "
                + DnsDash.updateLocalDns() + ", errCode=" + errCode;
        long endTime = System.currentTimeMillis();
        MiLinkLog.w(TAG, detailString + ",timecost = " + (endTime - beginTime) + "ms]");

        if (errCode != RET_CODE_DNS_SUCCESS) {
            InternalDataMonitor.getInstance().trace(domian, 0, Const.MnsCmd.MNS_DNS_FAIL_CMD,
                    errCode, beginTime, endTime, 0, 0, 0);
        }
    }

    private class ResolveThread extends Thread {
        private String mDomain = null;

        private volatile boolean mIsExpired = false;

        private volatile String mKey = null;

        private volatile boolean mIsCompleted = false;

        public ResolveThread(String domain, String key) {
            mDomain = domain;
            mKey = key;
        }

        public void setExpired(boolean isExprired) {
            mIsExpired = isExprired;
        }

        public String getKey() {
            return mKey;
        }

        public boolean isCompleted() {
            return mIsCompleted;
        }

        @Override
        public void run() {
            String ip = null;
            mIsCompleted = false;
            int errCode = RET_CODE_DNS_SUCCESS;
            long beginTime = System.currentTimeMillis();
            try {
                InetAddress inetAddress = InetAddress.getByName(mDomain);
                ip = inetAddress.getHostAddress();
                if (ip != null && mIsExpired == false) {
                    setDomainIP(mDomain, ip);
                }
            } catch (UnknownHostException e) {
                MiLinkLog.e(TAG, "Inet Address Analyze fail exception : ", e);
                errCode = RET_CODE_DNS_UNKNOWN_HOST;
            } catch (Exception e) {
                MiLinkLog.e(TAG, "Inet Address Analyze fail exception : ", e);
                errCode = RET_CODE_DNS_LOCAL_EXCEPTION;
            } catch (java.lang.Error e) {
                MiLinkLog.e(TAG, "Inet Address Analyze fail exception : ", e);
                errCode = RET_CODE_DNS_LOCAL_EXCEPTION;
            }

            mIsCompleted = true;
            statistic(beginTime, mDomain, ip, errCode);
        }
    }
}

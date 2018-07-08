
package com.mi.milink.sdk.config;

import java.util.HashSet;
import java.util.Observable;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import com.mi.milink.sdk.base.Global;
import com.mi.milink.sdk.base.debug.TraceLevel;
import com.mi.milink.sdk.base.os.Device.Network;
import com.mi.milink.sdk.base.os.info.NetworkDash;
import com.mi.milink.sdk.base.os.timer.AlarmClockService;
import com.mi.milink.sdk.debug.MiLinkLog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.TextUtils;

/**
 * 管理各种配置，并且负责持久化和更新
 */
public class ConfigManager extends Observable {
    private static final String TAG = "ConfigManager";

    public static final long SERVICE_SUICIDE_INTERVAL = 10 * 60 * 60 * 1000;

    private static final int DEFAULT_HEART_BEAT_INTERVAL = 5 * 60 * 1000; // 5分钟

    private static final int DEFAULT_SPEED_TEST_INTERVAL = 12 * 60 * 60 * 1000; // 12小时

    private static final String PREF_MNS_SETTINGS_DATA_NAME = "mns_settings_data";

    private static final String PREF_KEY_SUID = "suid";

    private static final String PREF_KEY_SUID_ANONYMOUS = "suid_anonymous";

    private static final String PREF_KEY_HEART_BEAT_INTERVAL = "heart_beat_interval"; // 心跳时间,
                                                                                      // 单位是毫秒
    
    private static final String PREF_KEY_CHANNEL_PUB_KEY = "channel_public_key";//通道模式的key
    
    // private static final String PREF_KEY_CONFIG_TIME_STAMP =
    // "config_time_stamp"; // config的更新时间

    private static final String PREF_KEY_SPEED_TEST_TIP = "speed_test_tip";

    private static final String PREF_KEY_SPEED_TEST_UIP = "speed_test_uip";

    private static final String PREF_KEY_SPEED_TEST_INTERVAL = "speed_test_interval";

    private static final String PREF_KEY_SAMPLE_STATICTIS_FACTOR = "sample_statistics_factor";

    private static final String PREF_KEY_LOG_LEVEL = "log_level";

    private static final int MOBILE_NETWORK_CONNECTION_TIME_OUT = 20 * 1000;

    private static final int WIFI_CONNECTION_TIME_OUT = 15 * 1000;

    private static final int MOBILE_NETWORK_REQUEST_TIME_OUT = 20 * 1000;

    private static final int WIFI_REQUEST_TIME_OUT = 15 * 1000;

    private static final int MOBILE_NETWORK_UPLOAD_STASTIC_INTERVAL = 10 * 60 * 1000;

    private static final int WIFI_UPLOAD_STASTIC_INTERVAL = 5 * 60 * 1000;

    private static final int MOBILE_NETWORK_DNS_TIME_OUT = 6 * 1000;

    private static final int WIFI_DNS_TIME_OUT = 4 * 1000;

    private static final String JSON_KEY_HB = "hb"; // 心跳时间

    private static final String JSON_KEY_ST = "st"; // 测速

    private static final String JSON_KEY_TIP = "tip"; // tcp ip list #分隔

    private static final String JSON_KEY_UIP = "uip";// udp ip list #分隔

    private static final String JSON_KEY_INTL = "intl";// 测速的间隔

    private static final String JSON_KEY_ENGINE_CONFIG_RATIO = "engineConfRatio";// 引擎配置比例

    private static final String JSON_KEY_STATISTICS_FACTOR = "sf";// 抽样上报

    private static final String JSON_KEY_LOG_LEVEL = "logLev";// 日志级别

    private String suid = null; // push device id

    private String suidAnonymous = null;

    private float engineConfigRatio = 0.5f;
    
    private long heartBeatInterval = DEFAULT_HEART_BEAT_INTERVAL; // heart beat
                                                                  // time

    private long speedTestInterval = DEFAULT_SPEED_TEST_INTERVAL;

    private long configTimeStamp = 0;

    private int samplingStatisticsFactor = 5;

    private int logLevel = TraceLevel.ALL;

    private Context context = null;

    private String speedTestTip = null;

    private String speedTestUip = null;
    
    private Set<String> channelPubKeySet = new HashSet<String>(6);
    
    private static ConfigManager sIntance = new ConfigManager();
    
    public static ConfigManager getInstance() {
        return sIntance;
    }

    private ConfigManager() {
        context = Global.getContext();
        loadConfig();
    }

    public int getConnetionTimeout() {
        int timeout = WIFI_CONNECTION_TIME_OUT;
        if (NetworkDash.isMobile()) {
            timeout = MOBILE_NETWORK_CONNECTION_TIME_OUT;
        }
        return timeout;
    }

    public int getRequestTimeout() {
        int timeout = WIFI_REQUEST_TIME_OUT;
        if (NetworkDash.isMobile()) {
            timeout = MOBILE_NETWORK_REQUEST_TIME_OUT;
        }
        return timeout;
    }

    public int getUploadStasticInterval() {
        int interval = MOBILE_NETWORK_UPLOAD_STASTIC_INTERVAL;
        if (NetworkDash.isWifi()) {
            interval = WIFI_UPLOAD_STASTIC_INTERVAL;
        }
        return interval;
    }

    public int getDnsTimeout() {
        int timeout = WIFI_DNS_TIME_OUT;
        if (NetworkDash.isMobile()) {
            timeout = MOBILE_NETWORK_DNS_TIME_OUT;
        }
        return timeout;
    }

    public synchronized String getSpeedTestTcpIps() {
        return speedTestTip;
    }

    private synchronized void updateSpeedTestTcpIps(String tip) {
        MiLinkLog.v(TAG, "speedtest tip is " + tip);
        if (!TextUtils.isEmpty(tip)) {
            if (!tip.equals(speedTestTip)) {
                speedTestTip = tip;
            }
        }
    }

    public synchronized String getSpeedTestUdpIps() {
        return speedTestUip;
    }

    private synchronized void updateSpeedTestUdpIps(String uip) {
        MiLinkLog.v(TAG, "speedtest uip is " + uip);
        if (!TextUtils.isEmpty(uip)) {
            if (!uip.equals(speedTestUip)) {
                speedTestUip = uip;
            }
        }
    }

    public synchronized float getEngineConfigRatio() {
        return engineConfigRatio;
    }

    public synchronized String getSuid() {
        return suid;
    }

    public synchronized void updateSuid(String newSuid) {
        if (!TextUtils.isEmpty(newSuid)) {
            if (!newSuid.equals(suid)) {
                suid = newSuid;
                saveConfig();
            }
        }
    }

    public synchronized String getSuidAnonymous() {
        return suidAnonymous;
    }

    public void updateSuidAnonymous(String newSuidAnonymous) {
        if (!TextUtils.isEmpty(newSuidAnonymous)) {
            if (!newSuidAnonymous.equals(suidAnonymous)) {
                suidAnonymous = newSuidAnonymous;
                saveConfig();
            }
        }
    }

    public synchronized long getSpeedTestInterval() {
        return speedTestInterval;
    }

    /**
     * @param interval 单位是秒
     */
    private synchronized void updateSpeedTestInterval(long interval) {
        MiLinkLog.v(TAG, "speedtest interval from server is " + interval);
        if (interval > 0) {
            interval = interval * 1000;
            if (interval != speedTestInterval) {
                speedTestInterval = interval;
            }
        }
    }

    public synchronized long getHeartBeatInterval() {
        return heartBeatInterval;
    }

    /**
     * @param interval 单位是秒
     */
    private synchronized void updateHeartBeatInterval(long interval) {
        MiLinkLog.v(TAG, "heartbeat interval from server is " + interval);
        if (interval > 0) {
            interval = interval * 1000;
            if (interval != heartBeatInterval) {
                MiLinkLog.w(TAG, "update heat beat interval from " + heartBeatInterval + " to "
                        + interval);
                heartBeatInterval = interval;
                AlarmClockService.resetNextPing();
            }
        }
    }

    public synchronized long getConfigTimeStamp() {
        return configTimeStamp;
    }
    
	public synchronized Set<String> getChannelPubKeys() {
		
		if(channelPubKeySet == null || channelPubKeySet.size() == 0) {
			loadConfig();
		}
		return channelPubKeySet;
	}
	
	public synchronized void updateChannelPubKeySet( Set<String> channelSet){
		channelPubKeySet  = channelSet;
		saveConfig();
	}

    /**
     * 配置日志级别
     * 
     * @param logLev
     */
    private synchronized void updateLogLevel(int logLev) {
        MiLinkLog.v(TAG, "update logLevel =" + logLev);
        if (logLev != logLevel) {
            this.logLevel = logLev;
            MiLinkLog.setLogcatTraceLevel(logLevel);
            MiLinkLog.setFileTraceLevel(logLevel);
        }
    }

    /**
     * @param interval 单位是秒
     */
    private synchronized void updateSamplingStatisticFactor(int factor) {
        MiLinkLog.v(TAG, "update sample statistic factor = " + factor);
        if (factor <= 10 && factor >= 0) {
            if (factor != samplingStatisticsFactor) {
                this.samplingStatisticsFactor = factor;
            }
        }
    }

    // 超时时间影响因子
    private float timeoutMultiply = 1.0f;

    public float getTimeoutMultiply() {
        return timeoutMultiply;
    }

    public synchronized void setTimeoutMultiply(float timeoutMultiply) {
        this.timeoutMultiply = timeoutMultiply;
    }

    public int getSamplingStatisticsFactor() {
        return samplingStatisticsFactor;
    }

    @SuppressLint("NewApi")
    private synchronized void saveConfig() {
        SharedPreferences settingPreferences = context.getSharedPreferences(
                PREF_MNS_SETTINGS_DATA_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settingPreferences.edit();
        // editor.putLong(PREF_KEY_CONFIG_TIME_STAMP, configTimeStamp);
        editor.putLong(PREF_KEY_HEART_BEAT_INTERVAL, heartBeatInterval);

        editor.putLong(PREF_KEY_SPEED_TEST_INTERVAL, speedTestInterval);
        editor.putString(PREF_KEY_SPEED_TEST_TIP, speedTestTip);
        editor.putString(PREF_KEY_SPEED_TEST_UIP, speedTestUip);

        editor.putInt(PREF_KEY_SAMPLE_STATICTIS_FACTOR, samplingStatisticsFactor);
        editor.putInt(PREF_KEY_LOG_LEVEL, logLevel);

        editor.putString(PREF_KEY_SUID, suid);
        editor.putString(PREF_KEY_SUID_ANONYMOUS, suidAnonymous);
        
        editor.putStringSet(PREF_KEY_CHANNEL_PUB_KEY, channelPubKeySet);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            editor.apply();
        } else {
            editor.commit();
        }
    }

    @SuppressLint("NewApi")
	private synchronized void loadConfig() {
        SharedPreferences settingPreferences = context.getSharedPreferences(
                PREF_MNS_SETTINGS_DATA_NAME, Context.MODE_PRIVATE);
        // configTimeStamp =
        // settingPreferences.getLong(PREF_KEY_CONFIG_TIME_STAMP, 0);
        heartBeatInterval = settingPreferences.getLong(PREF_KEY_HEART_BEAT_INTERVAL,
                DEFAULT_HEART_BEAT_INTERVAL);

        speedTestInterval = settingPreferences.getLong(PREF_KEY_SPEED_TEST_INTERVAL,
                DEFAULT_SPEED_TEST_INTERVAL);
        speedTestTip = settingPreferences.getString(PREF_KEY_SPEED_TEST_TIP, "");
        speedTestUip = settingPreferences.getString(PREF_KEY_SPEED_TEST_UIP, "");

        samplingStatisticsFactor = settingPreferences.getInt(PREF_KEY_SAMPLE_STATICTIS_FACTOR, 5);
        logLevel = settingPreferences.getInt(PREF_KEY_LOG_LEVEL, TraceLevel.ALL);

        suid = settingPreferences.getString(PREF_KEY_SUID, "");
        suidAnonymous = settingPreferences.getString(PREF_KEY_SUID_ANONYMOUS, "");
        
        channelPubKeySet = settingPreferences.getStringSet(PREF_KEY_CHANNEL_PUB_KEY, null);
    }

    JSONObject engineMatch = null;

    public synchronized JSONObject getEngineMatch() {
        return engineMatch;
    }

    public synchronized boolean updateConfig(long ts, String jsonConfig) {
        MiLinkLog.v(TAG, "update config from " + configTimeStamp + " to " + ts + ", jsonConfig="
                + jsonConfig);
        if (ts > configTimeStamp) {
            configTimeStamp = ts;
            if (!TextUtils.isEmpty(jsonConfig)) {
                try {
                    JSONObject obj = new JSONObject(jsonConfig);
                    if(obj.length()==0){
                        return false;
                    }
                    updateHeartBeatInterval(obj.optInt(JSON_KEY_HB, 0));
                    JSONObject stObj = obj.optJSONObject(JSON_KEY_ST);
                    if (stObj != null) {
                        updateSpeedTestInterval(stObj.optInt(JSON_KEY_INTL, 0));
                        updateSpeedTestTcpIps(stObj.optString(JSON_KEY_TIP, ""));
                        updateSpeedTestUdpIps(stObj.optString(JSON_KEY_UIP, ""));
                        // SessionManager.getInstance().setSpeedTest(0);
                    }
                    try {
                        engineConfigRatio = Float.parseFloat(obj
                                .getString(JSON_KEY_ENGINE_CONFIG_RATIO));
                    } catch (Exception e) {
                        // 这行日志不打出来
//                        MiLinkLog.e(TAG, e);
                    }
                    updateSamplingStatisticFactor(obj.optInt(JSON_KEY_STATISTICS_FACTOR, -1));
                    updateLogLevel(obj.optInt(JSON_KEY_LOG_LEVEL, TraceLevel.ALL));

                    try {
                        engineMatch = obj.getJSONObject("engine_match");
                    } catch (Exception e) {
                        MiLinkLog.e(TAG, e);
                        engineMatch = null;
                    }
                } catch (JSONException e) {
                    MiLinkLog.e(TAG, e);
                }
            }
            saveConfig();
            return true;
        } else {
            return false;
        }
    }

	public void deleteChannelPubKey(String valchannelStoreKey) {
		if(channelPubKeySet == null) 
			return ;
		channelPubKeySet.remove(valchannelStoreKey);
		
		
	}
}

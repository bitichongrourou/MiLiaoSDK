
package com.mi.milink.sdk.debug;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Build;
import android.text.TextUtils;

import com.mi.milink.sdk.account.manager.MiAccountManager;
import com.mi.milink.sdk.base.Global;
import com.mi.milink.sdk.base.os.Device;
import com.mi.milink.sdk.base.os.Device.Network;
import com.mi.milink.sdk.base.os.SimpleRequest;
import com.mi.milink.sdk.base.os.SimpleRequest.LengthPair;
import com.mi.milink.sdk.base.os.SimpleRequest.StringContent;
import com.mi.milink.sdk.base.os.info.DeviceDash;
import com.mi.milink.sdk.base.os.info.NetworkDash;
import com.mi.milink.sdk.client.ipc.ClientLog;
import com.mi.milink.sdk.data.ClientAppInfo;
import com.mi.milink.sdk.data.Const;
import com.mi.milink.sdk.util.CommonUtils;

/**
 * 此监控类主要是给APP来使用，产生的日志是输出在app.log文件中。 toJson方法的实现也有所不同，部分字段没有。
 * 
 * @author MK
 */
public class MiLinkMonitor extends BaseDataMonitor {

    private static final String TAG = "MiLinkMonitor";

    private static MiLinkMonitor sInstance = new MiLinkMonitor();

    private MiLinkMonitor() {
        super(TAG);
        mSamplingStatisticsSwitch = false;
        mLoopPost = true;
        startUpload(mUploadInterval);
    }

    public void doPostDataAtOnce() {
        ClientLog.w(TAG, "doPostDataAtOnce()");
        startUpload(0);
    }

    private boolean mEnable = false;

    public void setEnableWithLogoutStatus(boolean isEnable) {
        mEnable = isEnable;
    }

    @Override
    protected void doPostData() {
        if (NetworkDash.isAvailable() && mMonitorItemMap.size() > 0) {
            ConcurrentHashMap<String, List<MonitorItem>> map = new ConcurrentHashMap<String, List<MonitorItem>>();
            map.putAll(mMonitorItemMap);
            mMonitorItemMap.clear();
            if (mEnable || !TextUtils.isEmpty(MiAccountManager.getInstance().getUserId())) {
                String json = toJson(map);
                ClientLog.v(TAG, "ThreadId=" + Thread.currentThread().getId()
                        + ", doPostData: dataJson=" + json);
                if (!TextUtils.isEmpty(json)) {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("dataJson", json);
                    if (!ClientAppInfo.isTestChannel()) { // 只有在非test版本中才上传数据
                        // 先使用域名
                        try {
                            LengthPair lengthPair = new LengthPair();
                            StringContent result = SimpleRequest.postAsString(
                            		getStasticServerAddr(), params, null, true, lengthPair);
                            if (result != null) {
                                ClientLog.v(TAG,
                                        "doPostData use host report succeed: " + result.getBody());
                                return; // 成功之后返回
                            }
                        } catch (Exception e) {
                            ClientLog.v(TAG, "doPostData use host report failed");
                        }
                        // 走保底IP的逻辑，在传一下
                        try {
                            LengthPair lengthPair = new LengthPair();
                            StringContent result = SimpleRequest.postAsString(
                            		getStaticServerAddIp(), params, null, true,
                                    getStaticServerHost(), lengthPair);
                            if (result != null) {
                                ClientLog.v(TAG,
                                        "doPostData use ip report succeed: " + result.getBody());

                                return;// 成功之后返回
                            }
                        } catch (Exception e) {
                            ClientLog.v(TAG, "doPostData use ip report failed");
                        }
                        ClientLog.v(TAG, "doPostData use host and ip failed");
                        // 把数据存起来，下次再传
                        mMonitorItemMap.putAll(map);
                    }
                }
            }
        }
    }

    /**
     * 没有successIpArray、successPortArray、successApnArray和successApnTypeArray
     * 没有ISP字段
     */
    @Override
    protected String toJson(ConcurrentHashMap<String, List<MonitorItem>> map) {
        String json = "";
        if (null != map) {
            JSONObject root = new JSONObject();
            try {
                root.put(Const.TRACE_AC, Const.TRACE_AC_VALUE);
                root.put(Const.PARAM_APP_ID, String.valueOf(Global.getClientAppInfo().getAppId()));
                root.put(Const.PARAM_PACKET_VID, MiAccountManager.getInstance().getUserId());
                root.put(Const.PARAM_CLIENT_VERSION,
                        String.valueOf(Global.getClientAppInfo().getVersionCode()));
                root.put(Const.PARAM_MI_LINK_VERSION, String.valueOf(Global.getMiLinkVersion()));
                root.put(Const.PARAM_SYSTEM_VERSION,
                        "Android" + String.valueOf(Build.VERSION.RELEASE));
                root.put(Const.PARAM_DEVICE_ID,
                        CommonUtils.miuiSHA1(DeviceDash.getInstance().getDeviceId()));
                root.put(Const.PARAM_DEVICE_INFO, Build.MODEL);
                root.put(Const.PARAM_CHANNEL, Global.getClientAppInfo().getReleaseChannel());
                // 没有ISP字段
                JSONArray array = new JSONArray();
                for (String key : map.keySet()) {
                    List<MonitorItem> itemList = map.get(key);
                    JSONObject item = new JSONObject();
                    item.put("cmd", key);
                    int successTimes = 0;
                    int failedTimes = 0;
                    JSONArray successWasteArray = new JSONArray();
                    // 没有successIpArray、successPortArray、successApnArray和successApnTypeArray
                    JSONArray errorArray = new JSONArray();
                    for (MonitorItem mi : itemList) {
                        if (mi.isSuccess) {
                            successTimes++;
                            if (mi.waste >= 0) {
                                successWasteArray.put(mi.waste);
                            }
                        } else {
                            failedTimes++;
                            JSONObject err = new JSONObject();
                            err.put("apn", mi.apn);
                            err.put("at", mi.apnType);
                            if (!TextUtils.isEmpty(mi.accip)) {
                                err.put("accip", mi.accip);
                            }
                            if (mi.port > 0) {
                                err.put("accport", mi.port);
                            }
                            err.put("errCode", mi.errorCode);
                            err.put("seq", mi.seq);
                            err.put("waste", mi.waste);
                            errorArray.put(err);
                        }
                    }
                    item.put("successTimes", successTimes);
                    if (successWasteArray.length() > 0) {
                        item.put("successWasteArray", successWasteArray);
                    }
                    item.put("failedTimes", failedTimes);
                    item.put("failedInfo", errorArray);
                    array.put(item);
                }
                if (array.length() > 0) {
                    root.put(Const.PARAM_DATA, array);
                    json = root.toString();
                }
            } catch (JSONException e) {
                ClientLog.e(TAG, "toJson", e);
            }

        }
        return json;
    }

    public static MiLinkMonitor getInstance() {
        return sInstance;
    }

}

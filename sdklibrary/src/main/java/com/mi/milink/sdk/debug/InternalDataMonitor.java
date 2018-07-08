
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
import com.mi.milink.sdk.account.manager.MiChannelAccountManager;
import com.mi.milink.sdk.base.Global;
import com.mi.milink.sdk.base.os.Device;
import com.mi.milink.sdk.base.os.Device.Network;
import com.mi.milink.sdk.base.os.SimpleRequest;
import com.mi.milink.sdk.base.os.SimpleRequest.LengthPair;
import com.mi.milink.sdk.base.os.SimpleRequest.StringContent;
import com.mi.milink.sdk.base.os.info.DeviceDash;
import com.mi.milink.sdk.base.os.info.NetworkDash;
import com.mi.milink.sdk.data.ClientAppInfo;
import com.mi.milink.sdk.data.Const;
import com.mi.milink.sdk.util.CommonUtils;

/**
 * 内部的数据监控类，主要是SDK自己使用，产生的日志是输出在m.log文件中。
 * 
 * @author MK
 */
public class InternalDataMonitor extends BaseDataMonitor {

	private static final String TAG = "InternalDataMonitor";

	private static InternalDataMonitor sInstance = new InternalDataMonitor();

	private InternalDataMonitor() {
		super(TAG);
		if (Global.getClientAppInfo().getAppId() == ClientAppInfo.GAME_LOGIN_PAY_SDK) {
			mSamplingStatisticsSwitch = false;
		} else {
			mSamplingStatisticsSwitch = true;
		}
	}

	public void doPostDataAtOnce() {
		MiLinkLog.w(TAG, "doPostDataAtOnce()");
		startUpload(0);
	}

	private byte alarmArriveTimes = 0;

	public synchronized void onAlarmArrive() {
		mLoopPost = false;
		alarmArriveTimes++;
		if (alarmArriveTimes >= 2) {
			doPostDataAtOnce();
			alarmArriveTimes = 0;
		}
	}

	private long mLastUploadTime = 0;

	@Override
	protected void doPostData() {
		if (mMonitorItemMap.isEmpty()) {
			MiLinkLog.v(TAG, "dopost but map is empty!");
			MiLinkLog.w(TAG, "mMonitorItemMap em()");
			return;
		}
		if (System.currentTimeMillis() - mLastUploadTime < 5 * 60 * 1000) {
			MiLinkLog.v(TAG, "dopost but has uploaded just now,cancel this!");
			return;
		}
		if (!NetworkDash.isAvailable()) {
			MiLinkLog.v(TAG, "dopost but network is available");
			return;
		}
		// 开始上报
		ConcurrentHashMap<String, List<MonitorItem>> map = new ConcurrentHashMap<String, List<MonitorItem>>();
		map.putAll(mMonitorItemMap);
		mMonitorItemMap.clear();
		if (!TextUtils.isEmpty(MiAccountManager.getInstance().getUserId()) || MiChannelAccountManager.hasInit) {
			String json = toJson(map);
			MiLinkLog.v(TAG, "ThreadId=" + Thread.currentThread().getId() + ", doPostData: dataJson=" + json);
			if (!TextUtils.isEmpty(json)) {
				mLastUploadTime = System.currentTimeMillis();
				Map<String, String> params = new HashMap<String, String>();
				params.put("dataJson", json);
				if (!ClientAppInfo.isTestChannel()) { // 只有在非test版本中才上传数据
					// 先使用域名
					try {
						LengthPair lengthPair = new LengthPair();
						StringContent result = SimpleRequest.postAsString(getStasticServerAddr(), params, null, true,
								lengthPair);
						TrafficMonitor.getInstance().traffic("c.do", lengthPair.compressLength);
						TrafficMonitor.getInstance().print();
						TrafficMonitor.getInstance().printDetail();
						if (result != null) {
							MiLinkLog.v(TAG, "doPostData use host report succeed: " + result.getBody());
							return; // 成功之后返回
						}
					} catch (Exception e) {
						MiLinkLog.v(TAG, "doPostData use host report failed");
					}
					// 走保底IP的逻辑，在传一下
					try {
						LengthPair lengthPair = new LengthPair();
						StringContent result = SimpleRequest.postAsString(getStaticServerAddIp(), params, null, true,
								getStaticServerHost(), lengthPair);
						TrafficMonitor.getInstance().traffic("c.do", lengthPair.compressLength);
						TrafficMonitor.getInstance().print();
						TrafficMonitor.getInstance().printDetail();
						if (result != null) {
							MiLinkLog.v(TAG, "doPostData use ip report succeed: " + result.getBody());

							return;// 成功之后返回
						}
					} catch (Exception e) {
						MiLinkLog.v(TAG, "doPostData use ip report failed");
					}
					MiLinkLog.v(TAG, "doPostData use host and ip failed");
					// 把数据存起来，下次再传
					mMonitorItemMap.putAll(map);
				}
			}
		}
	}

	@Override
	protected String toJson(ConcurrentHashMap<String, List<MonitorItem>> map) {
		String json = "";
		if (null != map) {
			JSONObject root = new JSONObject();
			try {
				root.put(Const.TRACE_AC, Const.TRACE_AC_VALUE);
				root.put(Const.PARAM_APP_ID, String.valueOf(Global.getClientAppInfo().getAppId()));
				root.put(Const.PARAM_PACKET_VID, MiAccountManager.getInstance().getUserId());
				root.put(Const.PARAM_CLIENT_VERSION, String.valueOf(Global.getClientAppInfo().getVersionCode()));
				root.put(Const.PARAM_MI_LINK_VERSION, String.valueOf(Global.getMiLinkVersion()));
				root.put(Const.PARAM_SYSTEM_VERSION, "Android" + String.valueOf(Build.VERSION.RELEASE));
				root.put(Const.PARAM_DEVICE_ID, CommonUtils.miuiSHA1(DeviceDash.getInstance().getDeviceId()));
				root.put(Const.PARAM_DEVICE_INFO, Build.MODEL);
				root.put(Const.PARAM_CHANNEL, Global.getClientAppInfo().getReleaseChannel());
				JSONArray array = new JSONArray();
				for (String key : map.keySet()) {
					List<MonitorItem> itemList = map.get(key);
					JSONObject item = new JSONObject();
					item.put("cmd", key);
					int successTimes = 0;
					int failedTimes = 0;
					JSONArray successWasteArray = new JSONArray();
					JSONArray successIpArray = new JSONArray();
					JSONArray successPortArray = new JSONArray();
					JSONArray successApnArray = new JSONArray();
					JSONArray successApnTypeArray = new JSONArray();
					JSONArray clientIpArray = new JSONArray();
					JSONArray clientIspArray = new JSONArray();
					JSONArray errorArray = new JSONArray();
					for (MonitorItem mi : itemList) {
						if (mi.isSuccess) {
							successTimes++;
							if (mi.waste >= 0) {
								successWasteArray.put(mi.waste);
							}
							// 这4个数组有着对应关系，序号相同的属于一个组，核心是accip，所以4个一起添加
							if (!TextUtils.isEmpty(mi.accip)) {
								successIpArray.put(mi.accip);
								successPortArray.put(mi.port);
								successApnArray.put(mi.apn);
								successApnTypeArray.put(mi.apnType);
								if (Const.MnsCmd.MNS_HAND_SHAKE.equals(mi.cmd)
										|| Const.MnsCmd.MNS_FIRST_HEARTBEAT.equals(mi.cmd)) {
									clientIpArray.put(mi.clientIp);
									clientIspArray.put(mi.clientIsp);
								}
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
					if (successIpArray.length() > 0) {
						item.put("successIp", successIpArray);
					}
					if (successPortArray.length() > 0) {
						item.put("successPort", successPortArray);
					}
					if (successApnArray.length() > 0) {
						item.put("successApn", successApnArray);
					}
					if (successApnTypeArray.length() > 0) {
						item.put("successApnType", successApnTypeArray);
					}
					if (clientIpArray.length() > 0) {
						item.put("clientIp", clientIpArray);
					}
					if (clientIspArray.length() > 0) {
						item.put("clientIsp", clientIspArray);
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
				MiLinkLog.e(TAG, "toJson", e);
			}

		}
		return json;
	}

	public static InternalDataMonitor getInstance() {
		return sInstance;
	}
}

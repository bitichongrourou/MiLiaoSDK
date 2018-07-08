
package com.mi.milink.sdk.base.os.info;

import java.lang.reflect.Method;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.mi.milink.sdk.base.Global;
import com.mi.milink.sdk.base.os.Device;

/**
 * 设备信息获取类 为了保证入口的简洁和代码的可读性，请使用{@link com.tencent.base.os.Device#getInfo()}方法
 * 
 * @author MK
 */
public class DeviceDash implements NetworkStateListener {
	private static final DeviceDash instance = new DeviceDash();

	public static DeviceDash getInstance() {
		return instance;
	}

	private String mDeviceInfo = null;

	private String mDeviceSimplifiedInfo = null;

	private String mDeviceId = null;

	public DeviceDash() {
		NetworkDash.addListener(this);
	}

	public String getDeviceInfo() {
		if (mDeviceInfo == null || mDeviceInfo.length() < 1) {
			return updateDeviceInfo();
		}

		return mDeviceInfo;
	}

	public String getDeviceSimplifiedInfo() {
		if (mDeviceSimplifiedInfo == null || mDeviceSimplifiedInfo.length() < 1) {
			updateDeviceInfo();
		}

		return mDeviceSimplifiedInfo;
	}

	// 获取本机mac地址
	public String getMacAddress() {
		String macAddress = "00:00:00:00:00:00";
		try {
			WifiManager wifi = (WifiManager) Global.getSystemService(Context.WIFI_SERVICE);
			macAddress = wifi.getConnectionInfo().getMacAddress();
		} catch (Exception e) {
		}
		return macAddress;
	}

	public String updateDeviceInfo() {
		WindowManager manager = (WindowManager) Global.getSystemService(Context.WINDOW_SERVICE);
		TelephonyManager mTelephonyMgr = (TelephonyManager) Global.getSystemService(Context.TELEPHONY_SERVICE);
		DisplayMetrics displayMetrics = new DisplayMetrics();
		// 这个manager可能为空,加TC
		try {
			manager.getDefaultDisplay().getMetrics(displayMetrics);
		} catch (Exception e) {
		}
		StringBuilder builder = new StringBuilder();
		{
			String device_id = null;

			try {
				device_id = mTelephonyMgr.getDeviceId();
			} catch (Exception e) {
				try {
					device_id = Settings.Secure.getString(Global.getApplicationContext().getContentResolver(),
							Settings.Secure.ANDROID_ID);
				} catch (Exception e2) {
					device_id = "N/A";
				}
			}

			String apn;
			if (NetworkDash.isWifi()) {
				apn = "wifi";
			} else if (NetworkDash.is2G()) {
				apn = "2G";
			} else if (NetworkDash.is3G()) {
				apn = "3G";
			} else if (NetworkDash.isEthernet()) {
				apn = "ethernet";
			} else {
				apn = "wan";
			}

			builder.append("imei=").append(device_id).append('&');

			builder.append("model=").append(android.os.Build.MODEL).append('&');
			builder.append("os=").append(android.os.Build.VERSION.RELEASE).append('&');
			builder.append("apilevel=").append(android.os.Build.VERSION.SDK_INT).append('&');
			builder.append("macaddress=").append(getMacAddress()).append('&');

			builder.append("network=").append(apn).append('&');
			// builder.append("sdcard=").append(Device.Storage.hasExternal() ? 1
			// : 0).append('&');
			// builder.append("sddouble=").append("0").append('&');
			builder.append("display=").append(displayMetrics.widthPixels).append('*')
					.append(displayMetrics.heightPixels).append('&');
			builder.append("manu=").append(android.os.Build.MANUFACTURER).append('&');
			builder.append("gv=").append(Global.getClientAppInfo().getGv()).append('&');
			builder.append("versioncode=").append(Global.getClientAppInfo().getVersionCode());

			try {
				Class onwClass = Class.forName("android.os.SystemProperties");
				Method m = onwClass.getDeclaredMethod("get", String.class, String.class);
				String miui = (String) m.invoke(null, "ro.miui.ui.version.name", "");
				builder.append("&");
				builder.append("miui=").append(miui);
				String subversion = (String) m.invoke(null, "ro.build.version.incremental", "");
				builder.append("&");
				builder.append("subversion=").append(subversion);
			} catch (Exception e) {
			}
			mDeviceSimplifiedInfo = builder.toString();
			builder.append('&');
			builder.append("wifi=").append(WifiDash.getWifiInfo()).append('&');
			// builder.append("storage=").append(getStorageInfo()).append('&');
			builder.append("cell=").append(NetworkDash.getCellLevel()).append('&');

			// 不更新DNS信息，怕占用时间
			DnsInfo dnsInfo = DnsDash.getLocalDns();

			// 完全没有的时候再更新好了
			if (dnsInfo == null) {
				dnsInfo = DnsDash.updateLocalDns();
			}

			builder.append("dns=").append((dnsInfo == null ? "N/A" : dnsInfo.toString()));
		}

		mDeviceInfo = builder.toString();

		return mDeviceInfo;
	}

	// private String getStorageInfo() {
	// StorageInfo innerInfo = StorageDash.getInnerInfo();
	// StorageInfo extInfo = StorageDash.getExternalInfo();
	//
	// String resu = String.format("{IN : %s |EXT: %s}",
	// (innerInfo == null) ? "N/A" : innerInfo.toString(), (extInfo == null) ?
	// "N/A"
	// : extInfo.toString());
	//
	// return resu;
	// }

	@Override
	public void onNetworkStateChanged(NetworkState lastState, NetworkState newState) {
		// 网络变动时刷新设备信息
		updateDeviceInfo();
	}

	public String getDeviceId() {
		if (TextUtils.isEmpty(mDeviceId)) {
			TelephonyManager mTelephonyMgr = (TelephonyManager) Global.getSystemService(Context.TELEPHONY_SERVICE);
			try {
				mDeviceId = mTelephonyMgr.getDeviceId();
			} catch (Exception e) {
				mDeviceId = "N/A";
			}
		}
		return mDeviceId;
	}

}

package com.mi.milink.sdk.base.os.info;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.mi.milink.sdk.base.Global;
import com.mi.milink.sdk.util.CommonUtils;

/**
 * WIFI网卡信息收集类 <br>
 * <br>
 *
 * @author MK
 */
public class WifiDash {
	/**
	 * 获得当前接入点的BSSID<br>
	 * <br>
	 * <i>BSSID可以作为WIFI接入点的唯一标识</i>
	 *
	 * @return 形如MAC地址的字符串，{@code XX:XX:XX:XX:XX:XX}
	 * @see android.net.wifi.WifiInfo
	 */
	public static String getBSSID() {
		WifiManager wifiManager = (WifiManager) Global
				.getSystemService(Context.WIFI_SERVICE);

		if (wifiManager == null) {
			return null;
		}

		WifiInfo wifiInfo = null;

		try {
			wifiInfo = wifiManager.getConnectionInfo();
		} catch (Exception e) {
			wifiInfo = null;
		}

		if (wifiInfo == null) {
			return null;
		}

		String bssid = wifiInfo.getBSSID();

		if (CommonUtils.NOT_AVALIBLE.equals(bssid)
				|| "00:00:00:00:00:00".equals(bssid)
				|| "FF:FF:FF:FF:FF:FF".equalsIgnoreCase(bssid)) {
			return null;
		} else {
			return bssid;
		}
	}

	public static int getSignalLevel() {
		Object wifiInfo = queryWifiInfo(CommonUtils.NOT_AVALIBLE);

		if (wifiInfo == CommonUtils.NOT_AVALIBLE) {
			return -1;
		}

		return WifiManager.calculateSignalLevel(
				((WifiInfo) wifiInfo).getRssi(), 5);
	}

	private static Object queryWifiInfo(Object defValue) {
		WifiManager wifiManager = (WifiManager) Global
				.getSystemService(Context.WIFI_SERVICE);

		if (wifiManager == null) {
			return defValue;
		}

		WifiInfo wifiInfo = null;

		try {
			wifiInfo = wifiManager.getConnectionInfo();
		} catch (Exception e) {
			wifiInfo = null;
		}

		if (wifiInfo == null) {
			return defValue;
		}

		return wifiInfo;
	}

	public static String getWifiInfo() {
		WifiManager wifiManager = (WifiManager) Global
				.getSystemService(Context.WIFI_SERVICE);

		if (wifiManager == null) {
			return "[-]";
		}

		WifiInfo wifiInfo = null;

		try {
			wifiInfo = wifiManager.getConnectionInfo();
		} catch (Exception e) {
			wifiInfo = null;
		}

		if (wifiInfo == null) {
			return "[-]";
		}

		String ssid = wifiInfo.getSSID();

		String signal = String.valueOf(WifiManager.calculateSignalLevel(
				wifiInfo.getRssi(), 5));

		String speed = String.valueOf(wifiInfo.getLinkSpeed()) + " "
				+ WifiInfo.LINK_SPEED_UNITS;

		String bssid = wifiInfo.getBSSID();

		StringBuffer buffer = new StringBuffer();

		buffer.append('[').append(signal).append(", ").append(ssid)
				.append(", ").append(speed).append(", ").append(bssid)
				.append(']');

		return buffer.toString();
	}
}

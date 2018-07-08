package com.mi.milink.sdk.base.os.info;

import java.net.InetAddress;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;

import com.mi.milink.sdk.base.Global;
import com.mi.milink.sdk.base.data.Convert;
import com.mi.milink.sdk.base.os.Console;
import com.mi.milink.sdk.base.os.dns.DnsMain;

public class DnsDash {
	private static DnsInfo localDnsInfo = null;

	/**
	 * 获得优选DNS地址，支持超时
	 *
	 * @param domain
	 *            域名/地址
	 * @param timeout
	 *            超时时间，单位ms
	 * @return DNS解析结果
	 */
	public static InetAddress[] getHostByName(String domain, long timeout) {
		return DnsMain.getBetterHostByName(domain, timeout);
	}

	public static DnsInfo updateLocalDns() {
		DnsInfo dnsInfo = new DnsInfo();

		if (NetworkDash.isWifi()) {
			WifiManager wifiManager = (WifiManager) Global
					.getSystemService(Context.WIFI_SERVICE);

			if (wifiManager != null) {
				try {
					DhcpInfo info = wifiManager.getDhcpInfo();

					if (info != null) {
						dnsInfo.setWifiPreDns(Convert.intToIPv4(info.dns1));
						dnsInfo.setWifiAltDns(Convert.intToIPv4(info.dns2));
					}
				} catch (Exception e) {
					//
				}
			}
		} else {
			String systemDNS1 = Console.execute("getprop net.dns1", 1500);
			String systemDNS2 = Console.execute("getprop net.dns2", 1500);

			dnsInfo.setCurrPreDns(systemDNS1);
			dnsInfo.setCurrAltDns(systemDNS2);
		}

		setLocalDns(dnsInfo);

		return getLocalDns();
	}

	public static DnsInfo getLocalDns() {
		synchronized (DnsInfo.class) {
			return localDnsInfo;
		}
	}

	public static void setLocalDns(DnsInfo currDnsInfo) {
		synchronized (DnsInfo.class) {
			DnsDash.localDnsInfo = currDnsInfo;
		}
	}
}

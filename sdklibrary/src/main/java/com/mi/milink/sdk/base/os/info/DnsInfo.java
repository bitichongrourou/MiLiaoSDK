package com.mi.milink.sdk.base.os.info;

/**
 * DNS信息
 *
 */
public class DnsInfo {
	protected String currPreDns = "none";

	protected String currAltDns = "none";

	protected String wifiPreDns = "none";

	protected String wifiAltDns = "none";

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();

		buffer.append("[").append(currPreDns == null ? "none" : currPreDns)
				.append(",");
		buffer.append(currAltDns == null ? "none" : currAltDns).append(";");
		buffer.append(wifiPreDns == null ? "none" : wifiPreDns).append(";");
		buffer.append(wifiAltDns == null ? "none" : wifiAltDns).append("]");

		return buffer.toString();
	}

	/**
	 * 获取当前主选DNS
	 *
	 * @return -
	 */
	public String getCurrPreDns() {
		return currPreDns;
	}

	public void setCurrPreDns(String currPreDns) {
		this.currPreDns = currPreDns;
	}

	/**
	 * 获取当前备选DNS
	 *
	 * @return -
	 */
	public String getCurrAltDns() {
		return currAltDns;
	}

	public void setCurrAltDns(String currAltDns) {
		this.currAltDns = currAltDns;
	}

	/**
	 * 获取当前WIFI网卡主选DNS
	 *
	 * @return -
	 */
	public String getWifiPreDns() {
		return wifiPreDns;
	}

	public void setWifiPreDns(String wifiPreDns) {
		this.wifiPreDns = wifiPreDns;
	}

	/**
	 * 获取当前WIFI网卡备选DNS
	 *
	 * @return -
	 */
	public String getWifiAltDns() {
		return wifiAltDns;
	}

	public void setWifiAltDns(String wifiAltDns) {
		this.wifiAltDns = wifiAltDns;
	}
}

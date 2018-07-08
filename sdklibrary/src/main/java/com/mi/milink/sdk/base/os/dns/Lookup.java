package com.mi.milink.sdk.base.os.dns;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * 到 dns server查询指定域名ip地址
 * 
 * @author ethamhuang
 */
public class Lookup {

	private String dnsServerAddress = "";

	public Lookup(String dnsAddress) throws UnknownHostException {
		// LogUtility.w("dnstest", "DNS SERVER：" + dnsAddress);
		this.dnsServerAddress = dnsAddress;
	}

	// ///////////////////////////////////////////////////////////

	/**
	 * 设置DNS ADDRESS
	 * 
	 * @param dnsAddress
	 */
	public void setDnsAddress(String dnsAddress) {
		// LogUtility.w("dnstest", "DNS SERVER：" + dnsAddress);
		this.dnsServerAddress = dnsAddress;
	}

	/**
	 * 查询指定域名ip信息
	 * 
	 * @param domain
	 *            域名
	 * @return
	 * @throws Exception
	 */
	public InetAddress[] run(String domain, long timeout) throws IOException,
			SocketTimeoutException, WireParseException, UnknownHostException,
			Exception {

		InetAddress[] address = null;

		// long startTime = System.currentTimeMillis();
		// long endTime;
		// LogUtility.w("dnstest", "装包："+startTime);

		RequestPacket reqPacket = new RequestPacket(domain);
		byte[] queryData = reqPacket.getQueryData();
		// endTime = System.currentTimeMillis();
		// LogUtility.w("dnstest", ">>装包DONE："+endTime);
		// LogUtility.e("dnstest", ">>装包耗时："+ (endTime - startTime));

		if (queryData == null) {
			return null;
		}

		try {

			// startTime = System.currentTimeMillis();
			// LogUtility.w("dnstest", "DNS请求："+startTime);
			UdpClient uc = new UdpClient();
			uc.setTimeout(timeout);
			byte[] responseData = uc.sendrecv(dnsServerAddress, queryData);
			// endTime = System.currentTimeMillis();
			// LogUtility.w("dnstest", ">>DNS请求DONE："+endTime);
			// LogUtility.e("dnstest", ">>DNS请求耗时："+(endTime - startTime));

			if (responseData != null) {
				// startTime = System.currentTimeMillis();
				// LogUtility.w("dnstest", "解包："+startTime);
				ResponsePacket repPacket = new ResponsePacket(new DNSInput(
						responseData), domain);
				if (repPacket.getReqId() == reqPacket.getReqId()) {
					address = repPacket.getByAddress();
					if (address != null && address.length > 0) {

						// 添加到缓存
						HostCacheManager.getInstance().addCache(domain,
								address, repPacket.getExpireTime());
					}
				}
				// endTime = System.currentTimeMillis();
				// LogUtility.w("dnstest", ">>解包 DONE："+endTime);
				// LogUtility.e("dnstest", ">>解包耗时："+ (endTime - startTime));
			}
		} catch (WireParseException e) {

			throw e;
		} catch (SocketTimeoutException e) {

			throw e;
		} catch (IOException e) {

			throw e;
		}

		return address;
	}
}

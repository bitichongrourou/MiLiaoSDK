package com.mi.milink.sdk.base.os.dns;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import com.mi.milink.sdk.base.debug.CustomLogcat;

/**
 * DNS解析入口类。主要提供从指定DNS服务器查询更优的HOST的方法
 * 
 * @author ethamhuang
 */
public class DnsMain {

	private final static String TAG = DnsMain.class.getName();

	/**
	 * 从指定DNS服务器查询更优的HOST
	 * 
	 * @param domain
	 * @return InetAddress数组，包含域名下的所有IP信息，如果域名不对或者查询失败将返回null
	 * @author ethamhuang
	 */
	public static InetAddress[] getBetterHostByName(String domain, long timeout) {

		// try
		// {
		// // 此处代码在于探测系统InetAddress.getByAddress(String host, byte[] addr)
		// // 能不能正常工作，在有些手机的ROM里，比如HTC G14/三星I9100/360 AK47等手机，
		// // InetAddress.getByAddress(String host, byte[] addr) 方法会返回一个
		// // hostName为null（传入的host不为null）的InetAddress 对象，比如对象名为add，
		// // 此时，如果再调用add的getHostName()方法，会返回add对象的IP地址。故在做解析前
		// // 先探测一下InetAddress.getByAddress(String host, byte[] addr)方法。
		// InetAddress add = InetAddress.getByAddress("test.com", new byte[] {
		// 10, 10, 10, 10 });
		// if (add == null || add.getHostName() == null ||
		// add.getHostName().equals(add.getHostAddress()))
		// {
		// return null;
		// }
		// }
		// catch (UnknownHostException e)
		// {
		// LogUtility.e(TAG, "getBetterHostByName>>>", e);
		// }

		// 这里传过来的domain有可能是一个url，先从url里获取host
		domain = getHostName(domain);
		CustomLogcat.w("DNSResolve", "hostName:" + domain + ",timeout:" + timeout);
		CustomLogcat.v(TAG, "get better host for name:" + domain);

		// 防止domain为null或者为空格
		if (domain == null || domain.trim().length() <= 0) {
			return null;
		}

		domain = domain.trim();

		InetAddress[] address = null;

		/** 从缓存里查找 */
		// modify: 在这里关闭DNS缓存功能，防止解析到192.0.0.1等错误的IP被缓存下来
		// 注释无用代码
		// address = null;//HostCacheManager.g().getCacheItemByHost(domain);
		// if (address != null)
		// {
		// return address;
		// }

		// /////////////////////////////开始做网络查找////////////////////////////

		Lookup lookup = null;

		// 优先从114查询
		try {

			lookup = new Lookup(DnsConstants.DNS_SERVER_ADDRESS_114);
			address = lookup.run(domain, timeout);

			if (address != null && address.length > 0) {
				return address;
			} else {
				CustomLogcat.e(TAG, "114 - Address == null ? WTF ?!");
			}

		} catch (UnknownHostException e) {
			CustomLogcat.e(TAG, "UnknownHostException cause[" + domain
					+ "][114.114.114.114]." + e.getMessage());
		} catch (WireParseException e) {
			CustomLogcat.e(TAG, "WireParseException cause[" + domain
					+ "][114.114.114.114]." + e.getMessage());
		} catch (SocketTimeoutException e) {
			CustomLogcat.e(TAG, "SocketTimeoutException cause[" + domain
					+ "][114.114.114.114]." + e.getMessage());
		} catch (IOException e) {
			CustomLogcat.e(TAG, "IOException cause[" + domain + "][114.114.114.114]."
					+ e.getMessage());
		} catch (Exception e) {
			CustomLogcat.e(TAG, "Exception cause[" + domain + "][114.114.114.114]."
					+ e.getMessage());
		}

		// /////////////////////////////////////////////////

		// 114查询不到，转至8.8查询
		// try
		// {
		//
		// if (lookup == null)
		// {
		// lookup = new Lookup(DnsConstants.DNS_SERVER_ADDRESS_8);
		//
		// }
		// else
		// {
		// lookup.setDnsAddress(DnsConstants.DNS_SERVER_ADDRESS_8);
		// }
		//
		// address = lookup.run(domain, timeout);
		// if (address != null && address.length > 0)
		// {
		// return address;
		// }
		//
		// }
		// catch (UnknownHostException e)
		// {
		// Trace.e(TAG, "UnknownHostException cause[" + domain + "][8.8.8.8]." +
		// e.getMessage());
		// }
		// catch (WireParseException e)
		// {
		// Trace.e(TAG, "WireParseException cause[" + domain + "][8.8.8.8]." +
		// e.getMessage());
		// }
		// catch (SocketTimeoutException e)
		// {
		// Trace.e(TAG, "SocketTimeoutException cause[" + domain + "][8.8.8.8]."
		// + e.getMessage());
		// }
		// catch (IOException e)
		// {
		// Trace.e(TAG, "IOException cause[" + domain + "][8.8.8.8]." +
		// e.getMessage());
		// }
		// catch (Exception e)
		// {
		// Trace.e(TAG, "Exception cause[" + domain + "][8.8.8.8]." +
		// e.getMessage());
		// }
		//
		// // 8.8解析错误不做上报
		//
		// // /////////////////////////////////////////
		//
		// // 指定DNS服务器查询不到，从默认的DNS查询
		// try
		// {
		//
		// Trace.w("dnstest", "DNS SERVER：LDNS");
		// address = InetAddress.getAllByName(domain);
		//
		// if (address != null && address.length > 0)
		// {
		// /** 这里取第一个IP用于统计上报 */
		// // info = "ip=" + address[0].getHostAddress() + "&dns=ldns";
		// return address;
		// }
		//
		// }
		// catch (UnknownHostException e)
		// {
		// Trace.e(TAG, "UnknownHostException cause[" + domain + "][LDNS]." +
		// e.getMessage());
		// }

		/** 代码走到这里，证明已经将114DNS解析的错误上报，这里不再做解析错误的重复上报 */

		/** 所有DNS都查询不到，直接返回null */
		return null;

	}

	/**
	 * 从URL中截取host
	 * 
	 * @param url
	 * @return
	 */
	public static String getHostName(String url) {
		if (url == null)
			return "";

		url = url.trim();
		String host = url.toLowerCase();
		int end;

		if (host.startsWith("http://")) {
			end = url.indexOf("/", 8);
			if (end > 7) {
				host = url.substring(7, end);
			} else {
				host = url.substring(7);
			}

		} else if (host.startsWith("https://")) {
			end = url.indexOf("/", 9);
			if (end > 8) {
				host = url.substring(8, end);
			} else {
				host = url.substring(8);
			}
		} else {
			end = url.indexOf("/", 1);
			if (end > 1) {
				host = url.substring(0, url.indexOf("/", 1));
			} else {
				host = url;
			}
		}

		return host;
	}
}

package com.mi.milink.sdk.base.os.dns;

/**
 * 包含dns查询时用到的常量
 * 
 * @author ethamhuang
 */
public class DnsConstants {

	/** 114 DNS Server 地址 */
	public static final String DNS_SERVER_ADDRESS_114 = "114.114.114.114";

	/** 8 DNS server 地址 */
	public static final String DNS_SERVER_ADDRESS_8 = "8.8.8.8";

	/** 连接DNS Server端口号 */
	public static final int DNS_PORT = 53;

	/** DNS Query type, IP address 查询 */
	public static final int QTYPE_A = 1;

	/** DNS Query type, 别名查询 */
	public static final int QTYPE_CNAME = 5;

}

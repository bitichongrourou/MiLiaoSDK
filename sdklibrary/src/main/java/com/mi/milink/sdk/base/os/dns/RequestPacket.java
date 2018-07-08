package com.mi.milink.sdk.base.os.dns;

/**
 * DNS查询请求数据包
 * 
 * @author ethamhuang
 */
public class RequestPacket {

	private String domain; // 域名

	private int reqId; // 请求id

	private static byte[] header; // 请求包头部数据

	private static byte[] question; // 请求包查询数据

	static {
		header = new byte[] { 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0 };
		question = new byte[] { 0, 0, 1, 0, 1 };
	}

	/**
	 * 创建一个请求包
	 * 
	 * @param dstDomain
	 *            目标查询域名
	 */
	public RequestPacket(String dstDomain) {
		this.domain = dstDomain;

		// 生成一个唯一ID
		reqId = AtomicRequestId.getInstance().getId();
	}

	// /////////////////////////////////////////

	/**
	 * 获取查询数据
	 * 
	 * @return
	 */
	public byte[] getQueryData() {
		if (domain == null) {
			return null;
		}

		domain = domain.trim().toLowerCase();
		if (domain.length() == 0) {
			return null;
		}

		int len = header.length + question.length + domain.length() + 1;
		byte[] queryData = new byte[len];

		String[] domainArr = domain.split("\\.");
		int pos = header.length;
		for (int i = 0; i < domainArr.length; i++) {
			queryData[pos] = (byte) domainArr[i].length();
			pos += 1;
			byte[] tmp = domainArr[i].getBytes();
			System.arraycopy(tmp, 0, queryData, pos, tmp.length);
			pos += tmp.length;
		}

		System.arraycopy(header, 0, queryData, 0, header.length);
		System.arraycopy(question, 0, queryData, pos, question.length);

		warpReqId(queryData);

		return queryData;
	}

	public int getReqId() {
		return reqId;
	}

	public int getType() {
		return DnsConstants.QTYPE_A; // 为address查询
	}

	// ////////////////////////////////////////////////////////////////////
	/**
	 * 封装请求id
	 * 
	 * @param data
	 */
	private void warpReqId(byte[] data) {
		// Log.w("dnstest", "reqId:" + reqId);

		data[0] = (byte) ((reqId >>> 8) & 0xFF);
		data[1] = (byte) ((reqId) & 0xFF);
	}

}

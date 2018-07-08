package com.mi.milink.sdk.base.os.dns;

/**
 * 应答记录实体类
 * 
 * @author ethamhuang
 */
public class AnswerRecord {

	/** SERVER 应答的域名 */
	public String domain;

	/** SERVER 应答的IP，以byte数组形式存在 */
	public byte[] ip;

	/** SERVER 应答的ANSWER类型，一般跟QTYPE一致 */
	public int type;

	/** SERVER 应答的TTL，用来做缓存的有效时长 */
	public long ttl;

	/** SERVER 应答的CLASS，一般跟QCLASS保持一致 */
	public int qclass;

	// ///////////////////////////////////////////////

	public AnswerRecord() {

	}

	public AnswerRecord(String domain, byte[] ip, int type, long ttl, int qclass) {
		this.domain = domain;
		this.ip = ip;
		this.type = type;
		this.ttl = ttl;
		this.qclass = qclass;
	}

}

package com.mi.milink.sdk.base.os.dns;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import com.mi.milink.sdk.base.debug.CustomLogcat;

/**
 * DNS响应包解析类
 * 
 * @author ethamhuang
 */
public class ResponsePacket {
	private DNSInput in;

	private int[] counts = new int[4];

	private int reqId;

	private int flags;

	private ArrayList[] sections;

	private long expireTime = 0;

	private String host = "";

	private static final int MAXLABEL = 64;

	private static final int LABEL_NORMAL = 0;

	private static final int LABEL_COMPRESSION = 0xC0;

	private static final int LABEL_MASK = 0xC0;

	private static final int SECTION_QUESTION = 0;

	private static final int SECTION_ADDRESS = 1;

	private byte[] label = new byte[MAXLABEL];

	private StringBuilder nameBuilder = new StringBuilder();

	public ResponsePacket(DNSInput in, String host) throws WireParseException,
			UnknownHostException, Exception {
		this.in = in;
		this.host = host;

		sections = new ArrayList[4];

		initHeader();

		/** 检查响应包有无错误 */
		check(flags);
		parseAnswer();
	}

	@SuppressWarnings("unchecked")
	public ArrayList<AnswerRecord> getAnswers() {

		return sections[SECTION_ADDRESS];
	}

	/**
	 * 获取host的InetAddress信息
	 * 
	 * @return
	 */
	public InetAddress[] getByAddress() {

		if (sections[SECTION_ADDRESS] != null
				&& sections[SECTION_ADDRESS].size() > 0) {

			ArrayList<InetAddress> list = new ArrayList<InetAddress>();
			for (int i = 0; i < sections[SECTION_ADDRESS].size(); i++) {
				AnswerRecord ar = (AnswerRecord) sections[SECTION_ADDRESS]
						.get(i);
				try {

					InetAddress add = InetAddress
							.getByAddress(ar.domain, ar.ip);

					// 在有些手机的ROM里，比如HTC G14/三星I9100/360 AK47等手机，
					// InetAddress.getByAddress(String host, byte[] addr)
					// 方法会返回一个
					// hostName为null（传入的host不为null）的InetAddress 对象，比如对象名为add，
					// 此时，如果再调用add的getHostName()方法，会返回add对象的IP地址
					if (add != null && add.getHostName() != null
							&& !add.getHostName().equals(add.getHostAddress())) {
						list.add(add);
					}
				} catch (UnknownHostException e) {
					CustomLogcat.e("ResponsePacket", "getByAddress>>>", e);
				}
			}

			return list.toArray(new InetAddress[list.size()]);
		}

		return null;
	}

	public long getExpireTime() {
		return expireTime;
	}

	public int getReqId() {

		return reqId;
	}

	// //////////////////////////////////////////////

	/**
	 * 初始化头部数据
	 */
	private void initHeader() throws WireParseException {
		reqId = in.readU16();
		flags = in.readU16();

		for (int i = 0; i < counts.length; i++)
			counts[i] = in.readU16();
	}

	@SuppressWarnings("unchecked")
	private void parseAnswer() throws WireParseException {
		try {
			for (int i = 0; i < 2; i++) {// 只读QUESTIONG、ANSWER部分
				int count = counts[i];
				if (count > 0)
					sections[i] = new ArrayList<AnswerRecord>(count);
				for (int j = 0; j < count; j++) {

					AnswerRecord ar = new AnswerRecord();

					if (i == SECTION_QUESTION) {

						ar.domain = retrieveName();
						ar.type = in.readU16();
						ar.qclass = in.readU16();

						sections[i].add(ar);
					} else {

						/** 走这个方法目的不在于获取域名，而在于将in的索引值移到正确的位置 ， */
						/** 否则将影响后续数值的读取 */
						retrieveName();

						/** 从QUESTION中获取域名 */
						ar.domain = host; // ((AnswerRecord)sections[SECTION_QUESTION].get(0)).domain;
						ar.type = in.readU16();
						ar.qclass = in.readU16();
						ar.ttl = in.readU32();

						// Log.e("dnstest", "TTL:" + ar.ttl);

						in.setActive(in.readU16());
						ar.ip = in.readByteArray();

						// 直接丢弃非ADDRESS类型的记录
						if (ar.type == DnsConstants.QTYPE_A) {
							setExpireTime(ar.ttl);
							sections[i].add(ar);
						}
					}
				}
			}
		} catch (WireParseException e) {
			throw e;
		}
	}

	private String retrieveName() throws WireParseException {
		int len, pos;
		boolean done = false;
		boolean savedState = false;

		/** 清空namebuilder */
		if (nameBuilder.length() > 0) {
			nameBuilder.delete(0, nameBuilder.length());
		}

		while (!done) {
			len = in.readU8();
			switch (len & LABEL_MASK) {
			case LABEL_NORMAL:
				if (len == 0) {

					done = true;
				} else {

					in.readByteArray(label, 0, len);
					nameBuilder.append(ByteBase.byteString(label, len));
					nameBuilder.append(".");
				}
				break;
			case LABEL_COMPRESSION:
				pos = in.readU8();
				pos += ((len & ~LABEL_MASK) << 8);

				if (pos >= in.current() - 2)
					throw new WireParseException("bad compression");
				if (!savedState) {
					in.save();
					savedState = true;
				}
				in.jump(pos);

				break;
			default:
				throw new WireParseException("bad label type");
			}
		}
		if (savedState) {
			in.restore();
		}

		if (nameBuilder.length() > 0) {
			nameBuilder.deleteCharAt(nameBuilder.length() - 1);
		}
		return nameBuilder.toString();
	}

	/**
	 * 对flags进行RCODE的判断，比如当RCODE为3时，抛出一个UnknownHostException
	 * 
	 * @param flags
	 * @throws UnknownHostException
	 * @throws Exception
	 */
	private void check(int flags) throws UnknownHostException, Exception {
		String flagsBinaryString = Integer.toBinaryString(flags);
		if (flagsBinaryString.length() < 4) {
			throw new Exception("exception cause [FBS - " + flagsBinaryString
					+ "]");
		}

		String rcode = flagsBinaryString
				.substring(flagsBinaryString.length() - 4);

		if (rcode.equals("0011")) {

			throw new UnknownHostException("Unable to resolve host \"" + host
					+ "\": No address associated with hostname");
		} else if (!rcode.equals("0000")) {

			throw new Exception("exception cause [RCODE - " + rcode
					+ "][HOST - " + host + "]");
		}
	}

	/**
	 * 设置过期时间，过期间隔为一个ttl
	 * 
	 * @param ttl
	 */
	private void setExpireTime(long ttl) {
		if (expireTime == 0 && ttl > 0) {
			expireTime = System.currentTimeMillis() + (ttl * 1000);// ttl 为秒
		}
	}
}

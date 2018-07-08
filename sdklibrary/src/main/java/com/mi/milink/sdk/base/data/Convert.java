package com.mi.milink.sdk.base.data;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * 数据类型转换通用类
 * 任何人不要修改此类已有方法
 * @author MK
 *
 */
public class Convert {
	public static final char[] NUMBERIC_CHAR = new char[] { '0', '1', '2', '3', '4',
			'5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', };

	/**
	 * 用两整数组成一个长整数
	 *
	 * @param hi
	 *            高位
	 * @param lo
	 *            低位
	 * @return 长整数
	 */
	public final static long makeLong(int hi, int lo) {
		return (((long) hi << 32) | (lo & 0x00000000ffffffffL));
	}

	/**
	 * 取长整数的低位
	 *
	 * @param val
	 *            长整数
	 * @return 低位
	 */
	public final static int low(long val) {
		return (int) val;
	}

	/**
	 * 取长整数的高位
	 *
	 * @param val
	 *            长整数
	 * @return 高位
	 */
	public final static int high(long val) {
		return (int) (val >>> 32);
	}

	/**
	 * 字符串 → 整数的安全转换
	 *
	 * @param str
	 *            整数字符串
	 * @param def
	 *            默认值
	 * @return 整数
	 */
	public final static int strToInt(String str, int def) {
		try {
			return Integer.parseInt(str);
		} catch (NumberFormatException e) {
			return def;
		}
	}

	/**
	 * 字符串 → 字节的安全转换
	 *
	 * @param str
	 *            字节值字符串
	 * @param def
	 *            默认值
	 * @return 字节值
	 */
	public final static byte strToByte(String str, byte def) {
		try {
			return Byte.parseByte(str);
		} catch (NumberFormatException e) {
			return def;
		}
	}

	/**
	 * 将字节数组转换为十六进制字符串表示
	 *
	 * @param bytes
	 *            字节数组
	 * @return 十六进制字符串
	 */
	public static String bytesToHexStr(byte[] bytes) {
		if (bytes == null) {
			return null;
		}

		if (bytes.length == 0) {
			return "";
		}

		StringBuilder builder = new StringBuilder();

		int x = 0;
		int y = 0;

		for (byte b : bytes) {
			x = ((b + 256) % 256) / 16;
			y = ((b + 256) % 256) % 16;
			builder.append(NUMBERIC_CHAR[x]).append(NUMBERIC_CHAR[y]);
		}

		return builder.toString();
	}

	/**
	 * 将字节数组转换为十六进制字符串表示
	 *
	 * @param bytes
	 *            字节数组
	 * @param Length
	 *            长度
	 * @return 十六进制字符串
	 */
	public static String bytesToHexStr(byte[] bytes, int Length) {
		if (bytes == null) {
			return null;
		}

		if (bytes.length == 0) {
			return "";
		}

		int len = bytes.length > Length ? Length : bytes.length;

		StringBuilder builder = new StringBuilder();

		int x = 0;
		int y = 0;

		for (int i = 0; i < len; i++) {
			byte b = bytes[i];
			x = ((b + 256) % 256) / 16;
			y = ((b + 256) % 256) % 16;
			builder.append(NUMBERIC_CHAR[x]).append(NUMBERIC_CHAR[y])
					.append(' ');
		}

		return builder.toString();
	}

	/**
	 * 将十六进制字符串表示转换为字节数组，自动补0
	 *
	 * @param hexStr
	 *            十六进制字符串
	 * @return 字节数组 <br>
	 *         <b>包含非法字符或源数据为空时返回null</b>
	 */
	public static byte[] hexStrToBytes(String hexStr) {
		if (hexStr == null) {
			return null;
		}

		if (hexStr.length() == 0) {
			return new byte[0];
		}

		int strLen = hexStr.length();
		int bytesLen = (strLen + 1) / 2;

		byte[] bytes = new byte[bytesLen];

		for (int i = 0; i < bytesLen; i++) {
			char a = hexStr.charAt(i * 2);
			char b = '0';

			if ((i * 2 + 1) < strLen) {
				b = hexStr.charAt(i * 2 + 1);
			}

			int ia = hexCharToInt(a);
			int ib = hexCharToInt(b);

			if (ia < 0 || ib < 0) // 包含非法字符
			{
				return null;
			}

			bytes[i] = (byte) (ia * 16 + ib);
		}

		return bytes;
	}

	/**
	 * 将十六进制字符表示转换为数值
	 *
	 * @param ch
	 *            十六进制字符
	 * @return 对应数值<br>
	 *         <b>非十六进制字符返回-1</b>
	 */
	public static int hexCharToInt(char ch) {
		if (ch <= '9' && ch >= '0') {
			return ch - '0';
		} else if (ch <= 'F' && ch >= 'A') {
			return ch - 'A' + 10;
		} else if (ch <= 'f' && ch >= 'a') {
			return ch - 'a' + 10;
		} else {
			return -1;
		}
	}

	/**
	 * 将IPv4地址数组转换为字符串
	 *
	 * @param ip
	 *            IPv4地址数组
	 * @return IP字符串
	 */
	public static String IPv4ToStr(byte[] ip) {
		if (ip == null) {
			return "";
		}

		StringBuilder ipBuilder = new StringBuilder();

		for (int i = 0; i < ip.length; i++) {
			if (i != 0) {
				ipBuilder.append('.');
			}

			ipBuilder.append((ip[i] + 256) % 256);

		}

		return ipBuilder.toString();
	}

	/**
	 * 将IPIPv4字符串转换为IP地址数组
	 *
	 * @param ipString
	 *            IPv4字符串，形如"192.168.1.1"
	 * @return
	 */
	public static byte[] strToIPv4(String ipString) {
		if (ipString == null) {
			return null;
		}

		String[] byteStrings = ipString.split("\\.");

		if (byteStrings == null) {
			return null;
		}

		byte[] ip = new byte[byteStrings.length];

		for (int i = 0; i < ip.length; i++) {
			ip[i] = (byte) Integer.parseInt(byteStrings[i]);
		}

		return ip;
	}

	/**
	 * 整数IPv4地址转换为IPv4字符串
	 *
	 * @param ip
	 *            整数
	 * @return IPv4字符串，形如"192.168.1.1"
	 */
	public static String intToIPv4(int ip) {
		StringBuffer buffer = new StringBuffer();

		buffer.append(ip & 0xFF).append(".");
		buffer.append((ip >> 8) & 0xFF).append(".");
		buffer.append((ip >> 16) & 0xFF).append(".");
		buffer.append((ip >> 24) & 0xFF);

		return buffer.toString();
	}

	public static String intToIPv4_Reverse(int ip) {
		StringBuffer buffer = new StringBuffer();

		buffer.append((ip >> 24) & 0xFF).append(".");
		buffer.append((ip >> 16) & 0xFF).append(".");
		buffer.append((ip >> 8) & 0xFF).append(".");
		buffer.append(ip & 0xFF);

		return buffer.toString();
	}

	/*
	 * ↓ 字节数组和常见类型转换方法集合，来自原 ByteConvert 类
	 */
	public static byte[] longToBytes(long n) {
		byte[] b = new byte[8];

		b[7] = (byte) (n & 0xff);
		b[6] = (byte) (n >> 8 & 0xff);
		b[5] = (byte) (n >> 16 & 0xff);
		b[4] = (byte) (n >> 24 & 0xff);
		b[3] = (byte) (n >> 32 & 0xff);
		b[2] = (byte) (n >> 40 & 0xff);
		b[1] = (byte) (n >> 48 & 0xff);
		b[0] = (byte) (n >> 56 & 0xff);

		return b;
	}

	public static void longToBytes(long n, byte[] array, int offset) {
		array[7 + offset] = (byte) (n & 0xff);
		array[6 + offset] = (byte) (n >> 8 & 0xff);
		array[5 + offset] = (byte) (n >> 16 & 0xff);
		array[4 + offset] = (byte) (n >> 24 & 0xff);
		array[3 + offset] = (byte) (n >> 32 & 0xff);
		array[2 + offset] = (byte) (n >> 40 & 0xff);
		array[1 + offset] = (byte) (n >> 48 & 0xff);
		array[0 + offset] = (byte) (n >> 56 & 0xff);
	}

	public static long bytesToLong(byte[] array) {
		return ((((long) array[0] & 0xff) << 56)
				| (((long) array[1] & 0xff) << 48)
				| (((long) array[2] & 0xff) << 40)
				| (((long) array[3] & 0xff) << 32)
				| (((long) array[4] & 0xff) << 24)
				| (((long) array[5] & 0xff) << 16)
				| (((long) array[6] & 0xff) << 8) | (((long) array[7] & 0xff) << 0));
	}

	public static long bytesToLong(byte[] array, int offset) {
		return ((((long) array[offset + 0] & 0xff) << 56)
				| (((long) array[offset + 1] & 0xff) << 48)
				| (((long) array[offset + 2] & 0xff) << 40)
				| (((long) array[offset + 3] & 0xff) << 32)
				| (((long) array[offset + 4] & 0xff) << 24)
				| (((long) array[offset + 5] & 0xff) << 16)
				| (((long) array[offset + 6] & 0xff) << 8) | (((long) array[offset + 7] & 0xff) << 0));
	}

	public static byte[] intToBytes(int n) {
		byte[] b = new byte[4];

		b[3] = (byte) (n & 0xff);
		b[2] = (byte) (n >> 8 & 0xff);
		b[1] = (byte) (n >> 16 & 0xff);
		b[0] = (byte) (n >> 24 & 0xff);

		return b;
	}

	public static void intToBytes(int n, byte[] array, int offset) {
		array[3 + offset] = (byte) (n & 0xff);
		array[2 + offset] = (byte) (n >> 8 & 0xff);
		array[1 + offset] = (byte) (n >> 16 & 0xff);
		array[offset] = (byte) (n >> 24 & 0xff);
	}

	public static int bytesToInt(byte b[]) {
		return b[3] & 0xff | (b[2] & 0xff) << 8 | (b[1] & 0xff) << 16
				| (b[0] & 0xff) << 24;
	}

	public static int bytesToInt(byte b[], int offset) {
		return b[offset + 3] & 0xff | (b[offset + 2] & 0xff) << 8
				| (b[offset + 1] & 0xff) << 16 | (b[offset] & 0xff) << 24;
	}

	public static byte[] uintToBytes(long n) {
		byte[] b = new byte[4];

		b[3] = (byte) (n & 0xff);
		b[2] = (byte) (n >> 8 & 0xff);
		b[1] = (byte) (n >> 16 & 0xff);
		b[0] = (byte) (n >> 24 & 0xff);

		return b;
	}

	public static void uintToBytes(long n, byte[] array, int offset) {
		array[3 + offset] = (byte) (n);
		array[2 + offset] = (byte) (n >> 8 & 0xff);
		array[1 + offset] = (byte) (n >> 16 & 0xff);
		array[offset] = (byte) (n >> 24 & 0xff);
	}

	public static long bytesToUint(byte[] array) {
		return (array[3] & 0xff) | ((long) (array[2] & 0xff)) << 8
				| ((long) (array[1] & 0xff)) << 16
				| ((long) (array[0] & 0xff)) << 24;
	}

	public static long bytesToUint(byte[] array, int offset) {
		return (array[offset + 3] & 0xff)
				| ((long) (array[offset + 2] & 0xff)) << 8
				| ((long) (array[offset + 1] & 0xff)) << 16
				| ((long) (array[offset] & 0xff)) << 24;
	}

	public static byte[] shortToBytes(short n) {
		byte[] b = new byte[2];

		b[1] = (byte) (n & 0xff);
		b[0] = (byte) ((n >> 8) & 0xff);

		return b;
	}

	public static void shortToBytes(short n, byte[] array, int offset) {
		array[offset + 1] = (byte) (n & 0xff);
		array[offset] = (byte) ((n >> 8) & 0xff);
	}

	public static short bytesToShort(byte[] b) {
		return (short) (b[1] & 0xff | (b[0] & 0xff) << 8);
	}

	public static short bytesToShort(byte[] b, int offset) {
		return (short) (b[offset + 1] & 0xff | (b[offset] & 0xff) << 8);
	}

	public static byte[] ushortToBytes(int n) {
		byte[] b = new byte[2];

		b[1] = (byte) (n & 0xff);
		b[0] = (byte) ((n >> 8) & 0xff);

		return b;
	}

	public static void ushortToBytes(int n, byte[] array, int offset) {
		array[offset + 1] = (byte) (n & 0xff);
		array[offset] = (byte) ((n >> 8) & 0xff);
	}

	public static int bytesToUshort(byte b[]) {
		return b[1] & 0xff | (b[0] & 0xff) << 8;
	}

	public static int bytesToUshort(byte b[], int offset) {
		return b[offset + 1] & 0xff | (b[offset] & 0xff) << 8;
	}

	public static byte[] ubyteToBytes(int n) {
		byte[] b = new byte[1];

		b[0] = (byte) (n & 0xff);

		return b;
	}

	public static void ubyteToBytes(int n, byte[] array, int offset) {
		array[0] = (byte) (n & 0xff);
	}

	public static int bytesToUbyte(byte[] array) {
		return array[0] & 0xff;
	}

	public static int bytesToUbyte(byte[] array, int offset) {
		return array[offset] & 0xff;
	}

	public static String bytesToASCIIString(byte[] values) {
		if (null == values)
			return "";

		return bytesToASCIIString(values, values.length);
	}

	public static String bytesToASCIIString(byte[] values, int Length) {
		try {
			if (null == values)
				return "";

			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < Length; i++) {
				sb.append((char) ((values[i] + 256) % 256));
			}

			return sb.toString();
		} catch (Exception e) {
			return null;
		}
	}

	public static String bytesToStr(byte[] array) {
		if (array == null) {
			return null;
		} else {
			return new String(array);
		}
	}

	public static byte[] readByte(ByteArrayInputStream reader, int length)
			throws IOException {
		byte[] b = new byte[length];
		reader.read(b, 0, b.length);
		return b;
	}

	public static void readBytes(ByteArrayInputStream reader, byte[] result,
			int start, int len) throws IOException {
		reader.read(result, start, len);
	}

	public static boolean compare(byte[] left, byte[] right) {
		if (left.length != right.length)
			return false;

		for (int i = 0; i < right.length; i++) {
			if (left[i] != right[i])
				return false;
		}

		return true;
	}
	
}

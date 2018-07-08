package com.mi.milink.sdk.base.os.dns;

import java.text.DecimalFormat;

/**
 * byte、string的转换操作类
 * 
 * @author ethamhuang
 */
public class ByteBase {

	private static final DecimalFormat byteFormat = new DecimalFormat();

	/**
	 * @param array
	 * @return
	 */
	public static String byteString(byte[] array, int len) {

		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < len; i++) {
			int b = array[i] & 0xFF;
			if (b <= 0x20 || b >= 0x7f) {
				sb.append('\\');
				sb.append(byteFormat.format(b));
			} else if (b == '"' || b == '(' || b == ')' || b == '.' || b == ';'
					|| b == '\\' || b == '@' || b == '$') {
				sb.append('\\');
				sb.append((char) b);
			} else
				sb.append((char) b);
		}
		return sb.toString();
	}
}

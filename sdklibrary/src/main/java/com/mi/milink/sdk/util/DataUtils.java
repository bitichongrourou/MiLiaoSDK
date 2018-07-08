package com.mi.milink.sdk.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.Writer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.os.Parcel;

/**
 * 数据工具集
 *
 *
 */
public class DataUtils {
	/**
	 * MD5加密
	 */
	public static byte[] strToMd5(String str) {
		byte[] resu = null;

		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");

			md5.update(str.getBytes());

			resu = md5.digest();
		} catch (NoSuchAlgorithmException e) {
			resu = str.getBytes();
		}

		return resu;
	}

	/**
	 * 向Parcel写入数组
	 */
	public static void writeParcelBytes(Parcel parcel, byte[] data) {
		if (data == null) {
			parcel.writeInt(-1);
		} else {
			parcel.writeInt(data.length);
			parcel.writeByteArray(data);
		}
	}

	/**
	 * 从Parcel中读取数组
	 */
	public static byte[] readParcelBytes(Parcel parcel) {
		int len = parcel.readInt();

		if (len > -1) {
			byte[] resu = new byte[len];

			parcel.readByteArray(resu);

			return resu;
		} else {
			return null;
		}
	}

	/**
	 * 便利方法：清空缓冲区
	 *
	 * @param buffer
	 */
	public static void zeroMemory(byte[] buffer) {
		if (buffer == null) {
			return;
		}

		for (int i = 0; i < buffer.length; i++) {
			buffer[i] = (byte) 0;
		}
	}

	/**
	 * 便利方法：关闭文件或流数据对象
	 *
	 * @param object
	 *            文件或流数据对象
	 * @return 是否成功调用关闭方法
	 */
	public static boolean closeDataObject(Object object) {
		if (object == null) {
			return false;
		}

		try {
			if (object instanceof InputStream) {
				((InputStream) object).close();
			} else if (object instanceof OutputStream) {
				((OutputStream) object).close();
			} else if (object instanceof Reader) {
				((Reader) object).close();
			} else if (object instanceof Writer) {
				((Writer) object).close();
			} else if (object instanceof RandomAccessFile) {
				((RandomAccessFile) object).close();
			} else {
				return false;
			}

			return true;
		} catch (IOException e) {
			return false;
		}
	}
}

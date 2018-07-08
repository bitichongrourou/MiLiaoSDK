package com.mi.milink.sdk.base.os.info;

import android.os.Environment;

import com.mi.milink.sdk.base.Global;

/**
 * 存储器信息收集类 <br>
 * <br>
 *
 * @author MK
 */
public class StorageDash {
	/**
	 * 是否有外部存储
	 *
	 * @return -
	 */
	public static boolean hasExternal() {
		return Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState());
	}

	/**
	 * 是否有只读的外部存储
	 *
	 * @return -
	 */
	public static boolean hasExternalReadable() {
		String state = Environment.getExternalStorageState();

		return Environment.MEDIA_MOUNTED.equals(state)
				|| (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state));
	}

	/**
	 * 获得外部存储器的信息
	 *
	 * @return -
	 */
	public static StorageInfo getExternalInfo() {
		if (!hasExternalReadable()) {
			return null;
		}

		return StorageInfo.fromFile(Environment.getExternalStorageDirectory());
	}

	/**
	 * 获得内部存储器的信息
	 *
	 * @return -
	 */
	public static StorageInfo getInnerInfo() {
		return StorageInfo.fromFile(Global.getFilesDir());
	}
}

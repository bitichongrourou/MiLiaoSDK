package com.mi.milink.sdk.base.os;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.os.Environment;
import android.text.TextUtils;

import com.mi.milink.sdk.base.Global;
import com.mi.milink.sdk.base.debug.CustomLogcat;
import com.mi.milink.sdk.base.os.info.StorageDash;
import com.mi.milink.sdk.debug.MiLinkLog;
import com.mi.milink.sdk.util.CommonUtils;
import com.mi.milink.sdk.util.FileUtils;

/**
 *
 * @author MK
 *
 */
public class Native {
	private static final String TAG = "LibraryLoader";

	private static final String DEFAULT_LIB_DIR_NAME = "qzlib";

	private static final String DEFAULT_ASSETS_SO_DIR_NAME = "lib/armeabi-v7a";

	private static final String PREFENCE_NAME = "guarder";

	private static final String LIB_URL = "http://data.game.xiaomi.com/lib/lib.zip";

	// 存下上次找到这个lib的路径， 用来告诉第三方你应该在哪里找这个so，
	// key: libname, value: 实际路径 / 为空的话是默认
	private static final HashMap<String, String> REAL_SO_PATH = new HashMap<String, String>();

	/**
	 * 加载库
	 *
	 * @param libName
	 * @return
	 */
	public static boolean loadLibrary(final String libName) {
		File soFile = null;

		String soFileName = "lib" + libName + ".so";
		soFile = new File(getLibDir(), soFileName);

		// 先设置默认路径
		REAL_SO_PATH.put(libName, null);

		// 1.先加载系统目录下的so
		try {
			CustomLogcat.d(TAG, "try to load library: " + libName + " from system lib");
			System.loadLibrary(libName);
			return true;
		} catch (UnsatisfiedLinkError e) {
			CustomLogcat.e(TAG, "cannot load library " + libName + " from system lib",
					e);
		} catch (Exception e) {
			CustomLogcat.e(TAG, "cannot load library " + libName + " from system lib",
					e);
		} catch (Error e) {
			CustomLogcat.e(TAG, "cannot load library " + libName + " from system lib",
					e);
		}

		// 2.再判断自己目录下文件是否存在(新版本没复制过，也要复制一次).如果不存在，拷贝一次
		if (!soFile.exists()
				|| !hasCopiedInSpecifiedVersion(getDefaultVersionName(),
						soFileName)) {
			// 如果asset下没有so文件，就下载
			if (!isFileInAssetsPath(soFileName)) {
				// 如果asset下没有so文件，就下载
				boolean isdownloadSuc = downloadNativeLibs();
				if (isdownloadSuc) {
					setCopiedInSpecifiedVersion(getDefaultVersionName(),
							soFileName, true);
				} else {
					return false;
					// 下载失败，返回
				}
			} else {
				CustomLogcat.e(TAG, soFileName + " not exist,try to forceCopy!");

				try {
					forceCopySoFile(getDefaultVersionName(), soFileName);
				} catch (NativeException e) {
					// 拷贝失败，不急着返回，后面还会再拷贝一次
					CustomLogcat.e(TAG, soFileName + "forceCopy failed!", e);
				} catch (Exception e) {
					CustomLogcat.e(TAG, soFileName + "forceCopy failed!", e);
				}
			}
		}

		// 3.文件存在了，尝试加载自己目录下的so
		if (soFile.exists()) {
			try {
				String soFilePath = soFile.getAbsolutePath();
				CustomLogcat.d(TAG, "try to load library: " + soFilePath
						+ " from qzlib");
				System.load(soFilePath);
				REAL_SO_PATH.put(libName, soFilePath);
				return true;
			} catch (UnsatisfiedLinkError e) {
				CustomLogcat.e(TAG, "cannot load library " + soFile.getAbsolutePath()
						+ " from qzlib", e);
			} catch (Exception e) {
				CustomLogcat.e(TAG, "cannot load library " + soFile.getAbsolutePath()
						+ " from qzlib", e);
			} catch (Error e) {
				CustomLogcat.e(TAG, "cannot load library " + soFile.getAbsolutePath()
						+ " from qzlib", e);
			}
		}

		// 4.仍然失败，判断文件是否完整
		boolean isNeedCopyAgain = false;
		if (isFileInAssetsPath(soFileName)) {
			// 在asset里有so的前提下，比较完整性
			if (!soFile.exists()) { // 如果文件不存在则直接拷贝
				CustomLogcat.e(TAG, "Copy Lib For NOT_EXIST");
				isNeedCopyAgain = true;
			} else if (!isSameLength(soFileName)) { // 文件存在但长度不一致直接覆盖\
				CustomLogcat.e(TAG, "Copy Lib For DIFF_LENGTHS");
				isNeedCopyAgain = true;
			} else if (!isSameMd5(soFileName)) { // MD5不一致
				CustomLogcat.e(TAG, "Copy Lib For DEBUG_AND_MD5");
				isNeedCopyAgain = true;
			}
		}

		// 5.文件很完整，下载so
		if (isNeedCopyAgain == false) {
			boolean isdownloadSuc = downloadNativeLibs();
			if (isdownloadSuc)
				setCopiedInSpecifiedVersion(getDefaultVersionName(),
						soFileName, true);
			else
				return false;
			// 下载失败，返回
		}

		// 6.文件不完整，再拷贝一次
		if (isNeedCopyAgain) {
			CustomLogcat.e(TAG, soFileName
					+ " is something wrong,try to forceCopy again!");
			try {
				forceCopySoFile(getDefaultVersionName(), soFileName);
			} catch (NativeException e) {
				// 拷贝失败
				CustomLogcat.e(TAG, soFileName + "forceCopy failed again!", e);
			} catch (Exception e) {
				CustomLogcat.e(TAG, soFileName + "forceCopy failed again!", e);
			}
		}

		// 7.拷贝了，文件还不存在，直接返回失败
		if (!soFile.exists()) {
			CustomLogcat.e(TAG, soFileName
					+ "forceCopy done,but the sofile is not exist!");
			return false;
		}

		// 8.最后一次尝试
		try {
			String soFilePath = soFile.getAbsolutePath();
			CustomLogcat.e(TAG, "try to load library: " + soFilePath
					+ " from qzlib again!");
			System.load(soFilePath);
			REAL_SO_PATH.put(libName, soFilePath);
			return true;
		} catch (UnsatisfiedLinkError e) {
			CustomLogcat.e(TAG, "cannot load library " + soFile.getAbsolutePath()
					+ " from qzlib again", e);
		} catch (Exception e) {
			CustomLogcat.e(TAG, "cannot load library " + soFile.getAbsolutePath()
					+ " from qzlib again", e);
		} catch (Error e) {
			CustomLogcat.e(TAG, "cannot load library " + soFile.getAbsolutePath()
					+ " from qzlib again", e);
		}

		REAL_SO_PATH.put(libName, null);
		return false;
	}

	/**
	 * 得到Native库文件的完全路径
	 *
	 * @param libDirName
	 *            指定的so存放文件夹名
	 * @param soFileName
	 *            so文件名（libXXXX.so）
	 * @return
	 */
	public static String getSoPath(String libDirName, String soFileName) {
		String libPath = getInstallPath() + File.separator + libDirName;

		return (libPath + File.separator + soFileName);
	}

	public static File getLibDir() {
		String libDirPath = getInstallPath();

		return new File(libDirPath + File.separator + DEFAULT_LIB_DIR_NAME);
	}

	private static String getAssetsPath(String fileName) {
		return DEFAULT_ASSETS_SO_DIR_NAME + File.separator + fileName;
	}

	public static String getSORealPath(String libName) {
		if (!REAL_SO_PATH.containsKey(libName)) {
			return null;
		}
		return REAL_SO_PATH.get(libName);
	}

	private static boolean isFileInAssetsPath(String fileName) {
		try {
			String[] fileList = Global.getContext().getAssets()
					.list(DEFAULT_ASSETS_SO_DIR_NAME);
			if (fileList == null)
				return false;
			for (String file : fileList) {
				if (fileName.equalsIgnoreCase(file))
					return true;
			}
		} catch (IOException e) {
			CustomLogcat.e(TAG, "isFileInAssetsPath" + fileName, e);
		}
		return false;
	}

	/**
	 * 比较asset目录下的md5跟lib下的md5是否一致,防止覆盖安装时未能覆盖so文件
	 */
	private static boolean isSameMd5(String fileName) {
		boolean illegal = false;

		try {
			String assetSoMd5 = getFileMd5(Global.getAssets().open(
					getAssetsPath(fileName)));

			if (TextUtils.isEmpty(assetSoMd5)) {
				return false;
			}

			String libSoMd5 = getFileMd5(new FileInputStream(new File(
					getLibDir(), fileName)));

			illegal = assetSoMd5.equals(libSoMd5);

			String destFile = getLibDir() + File.separator + fileName;

			CustomLogcat.e(TAG, getAssetsPath(fileName) + " md5 = " + assetSoMd5 + ","
					+ destFile + " md5 = " + libSoMd5);
		} catch (FileNotFoundException e) {
			MiLinkLog.e(TAG,e);
		} catch (IOException e) {
		    MiLinkLog.e(TAG,e);
		}

		return illegal;
	}

	/**
	 * 判断asset下的so跟安装目录下的so文件大小是否一致
	 */
	private static boolean isSameLength(String fileName) {
		String destPath = new File(getLibDir(), fileName).getAbsolutePath();
		File destFile = new File(destPath);

		AssetManager assetManager = Global.getAssets();
		String assetPath = getAssetsPath(fileName);
		boolean tryStream = false;
		boolean sameLength = false;
		try {
			try {
				AssetFileDescriptor fd = assetManager.openFd(assetPath);

				// 如果ASSERT中没有对应文件(e.g. 读取ASSERT列表失败），则认为他们相同，以后者为准
				if (fd == null) {
					return true;
				}

				CustomLogcat.e(TAG, assetPath + " size = " + fd.getLength() + ","
						+ destFile + " size = " + destFile.length());

				if (destFile.length() == fd.getLength()) {
					// same file already exists.
					sameLength = true;
				}
			} catch (FileNotFoundException e) {
				// 如果ASSERT中没有对应文件(e.g. 读取ASSERT列表失败），则认为他们相同，以后者为准
				return true;
			} catch (IOException e) {
				// this file is compressed. cannot determine it's size.
				tryStream = true;
			}

			if (tryStream) {
				InputStream in = assetManager.open(assetPath);

				try {
					CustomLogcat.e(TAG,
							assetPath + " estimated size = " + in.available()
									+ "," + destFile + " size = "
									+ destFile.length());

					if (destFile.length() == in.available()) {
						sameLength = true;
					}
				} catch (IOException e) {
					// do nothing.
				} finally {
					in.close();
				}
			}
		} catch (Exception e) {

		}

		return sameLength;
	}

	private static String getFileMd5(InputStream inputStream) {
		return encrypt(inputStream, "MD5");
	}

	private static void copySoFile(String fileName) throws NativeException {
		CustomLogcat.i(TAG, "try to copy " + fileName);

		String assetPath = getAssetsPath(fileName);
		try {
			String destPath = getLibDir().getAbsolutePath();
			copyAssetLib(fileName, assetPath, destPath);
		} catch (Throwable e) {
			throw new NativeException("copy file:" + fileName + " failed!", e);
		}
	}

	private static String getDefaultVersionName() {
		String versionName = null;

		try {
			versionName = Global.getPackageManager().getPackageInfo(
					Global.getPackageName(), 0).versionName;
		} catch (Exception e) {
			versionName = String.valueOf(System.currentTimeMillis());
		}

		return versionName;
	}

	/**
	 * 强制拷贝so文件
	 */
	public static void forceCopySoFile(String soVersionName, String... soFiles)
			throws NativeException {
		if (soFiles != null) {
			for (String fileName : soFiles) {
				copySoFile(fileName);
				setCopiedInSpecifiedVersion(soVersionName, fileName, true);
			}
		}
	}

	private static boolean hasCopiedInSpecifiedVersion(String soVersionName,
			String soFileName) {
		SharedPreferences preferences = Global.getSharedPreferences(
				PREFENCE_NAME, Context.MODE_PRIVATE);

		String key = getCopiedKey(soVersionName, soFileName);

		return preferences.getBoolean(key, false);
	}

	private static void setCopiedInSpecifiedVersion(String soVersionName,
			String soFileName, boolean checked) {
		SharedPreferences preferences = Global.getSharedPreferences(
				PREFENCE_NAME, Context.MODE_PRIVATE);

		String key = getCopiedKey(soVersionName, soFileName);

		preferences.edit().putBoolean(key, checked).commit();
	}

	private static String getCopiedKey(String soVersionName, String soFileName) {
		return "check_" + soVersionName + "_" + soFileName;
	}

	@SuppressLint("SdCardPath")
	private static String getInstallPath() {
		File dirFile = Global.getFilesDir();
		if (dirFile == null) {
			dirFile = Global.getCacheDir();
		}
		if (dirFile != null) {
			return dirFile.getParent();
		} else {
			return "/data/data/" + Global.getPackageName();
		}
	}

	private static boolean downloadNativeLibs() {
		File root = StorageDash.hasExternal() ? new File(
				Environment.getExternalStorageDirectory(), "mi"
						+ File.separator + "milink") : Global.getCacheDir();

		File file = new File(root, "milink_network_lib.zip");

		// 尝试删除临时文件
		if (file.exists()) {
			file.delete();
		}

		CustomLogcat.w(TAG, "Prepare to Download Native Libs From Network ... ");

		CustomLogcat.w(TAG, "Url = " + LIB_URL);

		// 尝试下载Native库
		boolean resu = Http.isSuccess(Http.download(LIB_URL, file));

		CustomLogcat.w(TAG, "Download Native Libs => " + resu);

		if (!resu) {
			// 通知下载失败
			return false;
		}

		CustomLogcat.w(TAG, "Prepare to Install Native Libs ...");

		// 尝试解压Native库文件
		resu = FileUtils.unzip(file, Native.getLibDir());

		CustomLogcat.w(TAG, "Install Native Libs => " + resu);

		if (file.exists()) {
			file.delete();
		}

		return resu;
	}

	/* 工具方法 ↓ */
	private static final char[] digits = new char[] { /**/
	'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e',
			'f' };

	public static String encrypt(InputStream is, String algorithm) {
		String result = null;

		try {
			MessageDigest digest = MessageDigest.getInstance(algorithm);
			int count;
			byte[] buffer = new byte[1024];
			while ((count = is.read(buffer)) > 0) {
				digest.update(buffer, 0, count);
			}
			result = bytes2HexStr(digest.digest());

		} catch (IOException e) {

		} catch (NoSuchAlgorithmException e) {

		} finally {
			CommonUtils.closeDataObject(is);
		}

		return result;
	}

	private static String bytes2HexStr(byte[] bytes) {
		if (bytes == null || bytes.length == 0) {
			return null;
		}

		char[] buf = new char[2 * bytes.length];

		for (int i = 0; i < bytes.length; i++) {
			byte b = bytes[i];
			buf[2 * i + 1] = digits[b & 0xF];
			b = (byte) (b >>> 4);
			buf[(2 * i)] = digits[b & 0xF];
		}

		return new String(buf);
	}

	private static synchronized boolean copyAssetLib(String libName,
			String inFilePath, String outFileDir) throws Throwable {
		Context context = Global.getContext();

		boolean result = false;

		if (context == null || inFilePath == null) {
			return result;
		}

		if (outFileDir == null || outFileDir.trim().length() == 0) {
			CustomLogcat.e(TAG, "not define lib out path");
			outFileDir = context.getFilesDir().getAbsolutePath();// 当前目录为默认目录
		}

		File outputDirFile = new File(outFileDir);
		File outputFile = null;

		outputDirFile.mkdirs();

		InputStream is = null;
		FileOutputStream os = null;

		CustomLogcat.d(TAG, "copy lib:" + inFilePath + " to " + outFileDir);

		try {
			is = context.getAssets().open(inFilePath);

			outputFile = new File(outFileDir, libName);

			if (outputFile.exists()) {
				delete(outputFile);
			}

			outputFile.createNewFile();

			os = new FileOutputStream(outputFile);

			byte[] buffer = new byte[4096];

			int len = 0;

			while (is.available() > 0) {
				len = is.read(buffer);
				if (len > 0) {
					os.write(buffer, 0, len);
				} else {
					break;
				}
			}
			os.close();
			is.close();

			result = true;
		} catch (Throwable e) {
			CommonUtils.closeDataObject(os);
			CommonUtils.closeDataObject(is);
			delete(outputFile);

			throw e;
		}

		return result;
	}

	public static void delete(File file) {
		delete(file, false);
	}

	public static void delete(File file, boolean ignoreDir) {
		if (file == null || !file.exists()) {
			return;
		}
		if (file.isFile()) {
			file.delete();
			return;
		}

		File[] fileList = file.listFiles();
		if (fileList == null) {
			return;
		}

		for (File f : fileList) {
			delete(f, ignoreDir);
		}
		if (!ignoreDir)
			file.delete();
	}

	public static class NativeException extends RuntimeException {
		private static final long serialVersionUID = 411247780482311098L;

		public NativeException() {
			super();
		}

		public NativeException(String string) {
			super(string);
		}

		public NativeException(String string, Throwable e) {
			super(string, e);
		}

	}
}


package com.mi.milink.sdk.debug;

import com.mi.milink.sdk.base.debug.FileTracer;
import com.mi.milink.sdk.base.debug.LogcatTracer;
import com.mi.milink.sdk.base.debug.TraceLevel;
import com.mi.milink.sdk.data.Const;
import com.mi.milink.sdk.data.Option;

/**
 * MiLink日志记录类
 * 
 * @author MK
 * @see MiLinkTracer
 */
public final class MiLinkLog extends MiLinkTracer {
	private static volatile MiLinkLog sInstance = null;

	public static MiLinkLog getInstance() {
		if (sInstance == null) {
			synchronized (MiLinkLog.class) {
				if (sInstance == null) {
					sInstance = new MiLinkLog();
				}
			}
		}

		return sInstance;
	}

	/**
	 * 设置最大的单日日志文件大小<br>
	 *
	 * @param maxSize
	 *            单位：字节
	 */
	public static void setMaxFolderSize(long maxSize) {
		int blockCount = (int) (maxSize / Const.Debug.DefFileBlockSize);

		if (!Const.Debug.InfiniteTraceFile) {
			if (blockCount < 1) {
				blockCount = Const.Debug.DefFileBlockCount;
			}
		} else {
			blockCount = 1024;
		}

		Option.putInt(Const.Debug.FileBlockCount, blockCount).commit();
	}

	/**
	 * 设置日志文件最长保管周期<br>
	 * 如果设置的日期小于1天，则设置为默认周期，
	 *
	 * @param maxPeriod
	 *            单位: 毫秒
	 */
	public static void setMaxKeepPeriod(long maxPeriod) {
		if (maxPeriod < 24 * 60 * 60 * 1000L) {
			maxPeriod = Const.Debug.DefFileKeepPeriod;
		}

		Option.putLong(Const.Debug.FileKeepPeriod, maxPeriod).commit();
	}

	/**
	 * 设置文件日志追踪级别<br>
	 * {@code level}应该是{@link TraceLevel} 中的某个常量或者其中若干个的按位或操作的结果<br>
	 * 如果{@code level} >{@link TraceLevel#ALL} 或者{@code level} <0, 那么级别设置为
	 *
	 * @param level
	 *            参考级别常量
	 */
	public static void setFileTraceLevel(int level) {
		int traceLevel = level;

		if (level > ALL || level < 0) {
			traceLevel = Const.Debug.DefFileTraceLevel;
		}

		Option.putInt(Const.Debug.FileTraceLevel, traceLevel).commit();
	}

	/**
	 * 设置logcat日志追踪级别<br>
	 * {@code level}应该是{@link TraceLevel} 中的某个常量或者其中若干个的按位或操作的结果<br>
	 * 如果{@code level} >{@link TraceLevel#ALL} 或者{@code level} <0, 那么级别设置为
	 *
	 * @param level
	 *            参考级别常量
	 */
	public static void setLogcatTraceLevel(int level) {
		int traceLevel = level;

		if (level > ALL || level < 0) {
			traceLevel = Const.Debug.DefLogcatTraceLevel;
		}

		Option.putInt(Const.Debug.LogcatTraceLevel, traceLevel).commit();
	}

	public static void v(String tag, String msg) {
		getInstance().trace(VERBOSE, tag, msg, null);
	}

	public static void v(String tag, String msg, Throwable tr) {
		getInstance().trace(VERBOSE, tag, msg, tr);
	}

	public static void d(String tag, String msg) {
		getInstance().trace(DEBUG, tag, msg, null);
	}

	public static void d(String tag, String msg, Throwable tr) {
		getInstance().trace(DEBUG, tag, msg, tr);
	}

	public static void i(String tag, String msg) {
		getInstance().trace(INFO, tag, msg, null);
	}

	public static void i(String tag, String msg, Throwable tr) {
		getInstance().trace(INFO, tag, msg, tr);
	}

	public static void w(String tag, String msg) {
		getInstance().trace(WARN, tag, msg, null);
	}

	public static void w(String tag, String msg, Throwable tr) {
		getInstance().trace(WARN, tag, msg, tr);
	}

	public static void e(String tag, String msg) {
		getInstance().trace(ERROR, tag, msg, null);
	}

	public static void e(String tag, Throwable tr) {
		getInstance().trace(ERROR, tag, "", tr);
	}

	public static void e(String tag, String msg, Throwable tr) {
		getInstance().trace(ERROR, tag, msg, tr);
	}

	protected MiLinkLog() {
		super();
		try {
			fileTracer = new FileTracer(SERVICE_CONFIG);
			logcatTracer = new LogcatTracer();
			MiLinkTracer.setInstance(this);
			onSharedPreferenceChanged(null, Const.Debug.FileTraceLevel);
			onSharedPreferenceChanged(null, Const.Debug.LogcatTraceLevel);
		} catch (Exception e) {

		}
	}
}

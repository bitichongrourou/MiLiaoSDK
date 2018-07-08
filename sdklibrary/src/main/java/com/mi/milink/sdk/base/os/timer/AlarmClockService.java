/**
 * Name : AlarmClockService.java<br>
 * Description : Android Alarm System Wrap Class<br>
 */

package com.mi.milink.sdk.base.os.timer;

import java.lang.reflect.Method;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.AlarmClock;

import com.mi.milink.sdk.base.Global;
import com.mi.milink.sdk.config.HeartBeatManager;
import com.mi.milink.sdk.data.Const;
import com.mi.milink.sdk.debug.MiLinkLog;

/**
 * Android Alarm 定时服务 <br>
 * <br>
 * 用法参见 {@link AlarmClock}
 *
 * @author MK
 * @see AlarmClock
 */
public class AlarmClockService {
	private final static String TAG = "AlarmClockService";

	private static boolean sStop = true;

	private static void setExact(AlarmManager mgr, long triggerAtMillis, PendingIntent operation) {
		Class<AlarmManager> clazz = AlarmManager.class;
		try {
			Method method = clazz.getMethod("setExact", new Class[] { int.class, long.class, PendingIntent.class });
			method.invoke(mgr, AlarmManager.RTC_WAKEUP, triggerAtMillis, operation);
		} catch (Exception e) {
			MiLinkLog.e(TAG, e);
		}
	}

	static PendingIntent pendingIntent = null;

	/**
	 * 添加一个定时器
	 *
	 * @param clock
	 *            定时器对象
	 * @return 设置是否成功
	 */
	public static boolean start(long interval) {
		sStop = false;
		try {
			Intent intentToFire = new Intent(Const.Service.ActionName);
			intentToFire.setPackage(Global.getPackageName());

			pendingIntent = PendingIntent.getBroadcast(Global.getContext(), 987, intentToFire,
					PendingIntent.FLAG_UPDATE_CURRENT);
			AlarmManager alarmManager = (AlarmManager) Global.getSystemService(Context.ALARM_SERVICE);
			long nextPing = getNextPing(interval);
			if (Build.VERSION.SDK_INT >= 19) {
				setExact(alarmManager, nextPing, pendingIntent);
			} else {
				alarmManager.set(AlarmManager.RTC_WAKEUP, nextPing, pendingIntent);
			}
			return true;
		} catch (Exception e) {
			// AlarmService, getBroadcast有可能爆出NullPointer异常，捕获之
			return false;
		}
	}

	public static boolean start() {
		// return start(ConfigManager.getInstance().getHeartBeatInterval());

		return start(HeartBeatManager.getInstance().getHeartBeatInterval());

	}

	public static boolean startIfNeed() {
		if (sStop) {
			return start(HeartBeatManager.getInstance().getHeartBeatInterval());
		}
		return false;
	}

	public static boolean stop() {
		sStop = true;
		try {
			AlarmManager alarmManager = (AlarmManager) Global.getSystemService(Context.ALARM_SERVICE);
			if (pendingIntent != null) {
				alarmManager.cancel(pendingIntent);
			}
			return true;
		} catch (Exception e) {
			// AlarmService, getBroadcast有可能爆出NullPointer异常，捕获之
			return false;
		}
	}

	public static void resetNextPing() {
		mNextPingTs = 0;
	}

	private static long mNextPingTs = 0;

	private static long getNextPing(long interval) {
		MiLinkLog.w("MiLinkAlarm", "internal=" + interval + ",Ts = " + mNextPingTs);
		// if (mNextPingTs == 0) {
		// mNextPingTs = System.currentTimeMillis()
		// + (interval - SystemClock.elapsedRealtime() % interval);
		// } else {
		// mNextPingTs = mNextPingTs + interval;
		// if (mNextPingTs < System.currentTimeMillis()) {
		// mNextPingTs = System.currentTimeMillis() + interval;
		// }
		// }
		// TODO::: 去掉miui对齐
		mNextPingTs = System.currentTimeMillis() + interval;
		MiLinkLog.w("MiLinkAlarm", "next Ts = " + mNextPingTs);
		return mNextPingTs;
	}
}

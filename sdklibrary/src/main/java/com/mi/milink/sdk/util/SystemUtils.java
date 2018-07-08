
package com.mi.milink.sdk.util;

import android.app.ActivityManager;
import android.content.Context;

import java.util.List;

import com.mi.milink.sdk.base.Global;

public class SystemUtils {
	public static int getPidByProcessName(String serviceProcessName) {
		try {
			ActivityManager activityManager = (ActivityManager) Global.getSystemService(Context.ACTIVITY_SERVICE);
			if (activityManager == null) {
				return -1;
			}
			List<ActivityManager.RunningAppProcessInfo> list = activityManager.getRunningAppProcesses();
			if (list != null) {
				for (ActivityManager.RunningAppProcessInfo appProcess : list) {
					if (appProcess.processName.equals(serviceProcessName)) {
						return appProcess.pid;
					}
				}
			}
			return -1;
		} catch (Exception e) {
		}
		return -1;
	}
}

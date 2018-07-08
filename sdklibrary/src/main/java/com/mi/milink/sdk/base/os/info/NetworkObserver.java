package com.mi.milink.sdk.base.os.info;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;

import com.mi.milink.sdk.base.Global;

/**
 * 蜂窝网络/数据网络变化接收器<br>
 *
 * @see NetworkStateListener
 * @author MK
 */
abstract class NetworkObserver extends BroadcastReceiver {
	private PhoneStateListener signalListener;

	private volatile int cellLevel = -1;

	/*
	 * 提示: android.telephony.SignalStrength是Android 2.1(API Level 7)及以后才提供的
	 */
	private final boolean cellListenEnabled = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ECLAIR_MR1);

	/**
	 * 当网络发生变化时回调
	 */
	public abstract void onNetworkChanged();

	public void startListen() {
		IntentFilter intentFilter = new IntentFilter(
				ConnectivityManager.CONNECTIVITY_ACTION);

		try {
			Global.registerReceiver(this, intentFilter);
		} catch (Exception e) {
		}
	}

	public void stopListen() {
		Global.unregisterReceiver(this);
	}

	/**
	 * 获得手机信号格数，和状态栏的提示理论上保持一致 <br>
	 * <br>
	 * <b>至少需要Android 2.1及以上的API Level (7)</b>
	 *
	 * @return 从弱到强 0..4<br>
	 *         如果不支持或者尚未获得，返回 -1
	 */
	public int getCellLevel() {
		return cellLevel;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		/*
		 * 提示：PhoneStateListener内置了一个Handler，需要一个Looper线程，
		 * BASE库本身不提供任何Looper线程，新建一个又感觉有点浪费，故放在OnReceive里使用主线程来初始化
		 */
		if (cellListenEnabled) {
			if (signalListener == null) {
				synchronized (this) {
					if (signalListener == null) {
						initSignalListen();
					}
				}
			}
		}

		if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
			onNetworkChanged();
		}
	}

	@SuppressLint("InlinedApi")
	private void initSignalListen() {
		if (!cellListenEnabled) {
			return;
		}

		signalListener = new PhoneStateListener() {
			@Override
			public void onSignalStrengthsChanged(SignalStrength signalStrength) {
				cellLevel = getCellLevel(signalStrength);

				super.onSignalStrengthsChanged(signalStrength);
			}
		};

		TelephonyManager telephonyManager = (TelephonyManager) Global
				.getSystemService(Context.TELEPHONY_SERVICE);

		if (telephonyManager != null) {
			telephonyManager.listen(signalListener,
					PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
		} else {
			// 监听失败
			signalListener = null;
		}
	}

	/*
	 * 这些方法来自android.telephony.SignalStrength
	 */
	private int getCellLevel(SignalStrength signalStrength) {
		int level = 0;

		if (signalStrength == null) {
			return -1;
		}

		if (signalStrength.isGsm()) {
			level = getGsmLevel(signalStrength);
		} else {
			int cdmaLevel = getCdmaLevel(signalStrength);
			int evdoLevel = getEvdoLevel(signalStrength);

			if (evdoLevel == 0) {
				level = cdmaLevel;
			} else if (cdmaLevel == 0) {
				level = evdoLevel;
			} else {
				level = evdoLevel > cdmaLevel ? cdmaLevel : evdoLevel;
			}
		}

		return level;
	}

	private int getGsmLevel(SignalStrength signalStrength) {
		int asu = signalStrength.getGsmSignalStrength();

		// ASU ranges from 0 to 31 - TS 27.007 Sec 8.5
		// asu = 0 (-113dB or less) is very weak
		// signal, its better to show 0 bars to the user in such cases.
		// asu = 99 is a special case, where the signal strength is unknown.
		if (asu <= 2 || asu == 99)
			return 0;
		else if (asu >= 12)
			return 4;
		else if (asu >= 8)
			return 3;
		else if (asu >= 5)
			return 2;
		else
			return 1;
	}

	// CDMA信号显示
	private int getCdmaLevel(SignalStrength signalStrength) {
		final int cdmaDbm = signalStrength.getCdmaDbm();
		final int cdmaEcio = signalStrength.getCdmaEcio();
		int levelDbm = 0;
		int levelEcio = 0;

		if (cdmaDbm >= -75)
			levelDbm = 4;
		else if (cdmaDbm >= -85)
			levelDbm = 3;
		else if (cdmaDbm >= -95)
			levelDbm = 2;
		else if (cdmaDbm >= -100)
			levelDbm = 1;
		else
			levelDbm = 0;

		// Ec/Io are in dB*10
		if (cdmaEcio >= -90)
			levelEcio = 4;
		else if (cdmaEcio >= -110)
			levelEcio = 3;
		else if (cdmaEcio >= -130)
			levelEcio = 2;
		else if (cdmaEcio >= -150)
			levelEcio = 1;
		else
			levelEcio = 0;

		return (levelDbm < levelEcio) ? levelDbm : levelEcio;
	}

	// EVDO网络显示 CDMA2000 3G信号显示（例如电信天翼3G）
	private int getEvdoLevel(SignalStrength signalStrength) {
		int evdoDbm = signalStrength.getEvdoDbm();
		int evdoSnr = signalStrength.getEvdoSnr();
		int levelEvdoDbm = 0;
		int levelEvdoSnr = 0;

		if (evdoDbm >= -65)
			levelEvdoDbm = 4;
		else if (evdoDbm >= -75)
			levelEvdoDbm = 3;
		else if (evdoDbm >= -90)
			levelEvdoDbm = 2;
		else if (evdoDbm >= -105)
			levelEvdoDbm = 1;
		else
			levelEvdoDbm = 0;

		if (evdoSnr >= 7)
			levelEvdoSnr = 4;
		else if (evdoSnr >= 5)
			levelEvdoSnr = 3;
		else if (evdoSnr >= 3)
			levelEvdoSnr = 2;
		else if (evdoSnr >= 1)
			levelEvdoSnr = 1;
		else
			levelEvdoSnr = 0;

		return (levelEvdoDbm < levelEvdoSnr) ? levelEvdoDbm : levelEvdoSnr;
	}
}

package com.mi.milink.sdk.session.persistent;

import com.mi.milink.sdk.account.IAccount;
import com.mi.milink.sdk.base.os.info.NetworkDash;
import com.mi.milink.sdk.debug.MiLinkLog;
import com.mi.milink.sdk.proto.PushPacketProto.MilinkLogReq;

public class UploadLogManager {
	public static final String TAG = "UploadLogManager";

	static SessionForUploadLog mSessionForUploadLog = null;

	static long mLastUploadTime = 0;

	static boolean uploadStatus = false;

	static Object lock = new Object();

	static boolean uploading = false;

	public static boolean uploadMilinkLog(MilinkLogReq logReq, IAccount account,boolean force) {
		if (uploading) {
			MiLinkLog.e(TAG, "already uploading = true,cancel");
			return false;
		}
		uploading = true;
		uploadStatus = false;
		MiLinkLog.e(TAG, "ServerNotificationEvent requireUploadLog");
		long now = System.currentTimeMillis();
		if (!NetworkDash.isWifi()) {
			if (now - mLastUploadTime < 3600 * 1000 * 6 && !force) {
				MiLinkLog.e(TAG, "not wifi,cancel upload log this time.");
				return false;
			} else {
				
			}
		} else {
			if (now - mLastUploadTime < 2 * 60 * 1000 && !force) {
				MiLinkLog.e(TAG, "wifi,cancel upload log this time.");
				return false;
			} else {
			}
		}
		if (mSessionForUploadLog != null) {
			mSessionForUploadLog.close();
		}
		mSessionForUploadLog = new SessionForUploadLog(logReq, account, new SessionForUploadLog.UploadLogListener() {

			@Override
			public void success() {
				uploadStatus = true;
				mLastUploadTime = System.currentTimeMillis();
				synchronized (lock) {
					lock.notifyAll();
				}
			}

			@Override
			public void failed() {
				synchronized (lock) {
					lock.notifyAll();
				}
			}
		});
		mSessionForUploadLog.openSession();
		try {
			synchronized (lock) {
				MiLinkLog.w(TAG, "wait for upload.");
				lock.wait(20 * 1000);
			}
		} catch (InterruptedException e) {
			MiLinkLog.e(TAG, e);
		}
		if (mSessionForUploadLog != null) {
			mSessionForUploadLog.close();
		}
		mSessionForUploadLog = null;
		uploading = false;
		MiLinkLog.w(TAG, "upload uploadStatus="+uploadStatus);
		return uploadStatus;
	}

}

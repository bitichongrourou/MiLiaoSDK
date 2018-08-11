
package com.mi.milink.sdk.service;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;

import com.mi.milink.sdk.base.Global;
import com.mi.milink.sdk.base.os.LevelPromote;
import com.mi.milink.sdk.base.os.timer.AlarmClockService;
import com.mi.milink.sdk.client.ClientConstants;
import com.mi.milink.sdk.debug.MiLinkLog;
import com.mi.milink.sdk.util.PreloadClearUtil;
import com.mi.milink.sdk.util.SystemUtils;

@SuppressLint("NewApi")
public class MiLinkService extends Service {

    private static final String TAG = "MiLinkService";

    @Override
    public IBinder onBind(Intent intent) {
        MiLinkLog.w(TAG, "MiLink Service Binded");
        return MnsServiceBinder.getInstance();
    }

    @Override
    public void onRebind(Intent intent) {
        onBind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // 如果这样，要求业务退出时如有需要，立即登出
        // MnsBinder.Instance.stop();
        MiLinkLog.w(TAG, "MiLink Service UnBinded");
        return true;
    }

    @Override
    public int onStartCommand(Intent intent1, int flags, int startId) {
        MiLinkLog.w(TAG, "MiLink Service Started ,and onStartCommandReturn=" + 1);
        String packetName = Global.getPackageName();
        // 如果app进程没有活
        if (SystemUtils.getPidByProcessName(packetName) == -1) {
            Intent intent = new Intent(ClientConstants.ACTION_DISPATCH_MSG);
            intent.setPackage(Global.getClientAppInfo().getPackageName());
            Global.sendBroadcast(intent);
        }
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        PreloadClearUtil.clearResources();

        LevelPromote.promoteApplicationLevelInMIUI();
        long begin = System.currentTimeMillis();

        // MnsProtect.check(begin);
        Thread.setDefaultUncaughtExceptionHandler(new MiLinkExceptionHandler());

        // TODO 连接调试器
        // if (Const.Debug.NeedAttached) {
        // Debug.waitForDebugger();
        // }

        // 设置启动时间
        Global.STARTUP_TIME = SystemClock.elapsedRealtime();

        // 启动时钟
        AlarmClockService.start();
        long end = System.currentTimeMillis();

        //android 8.0后必须启动前台Service
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            startForeground(833991 , new Notification());
        }

        MiLinkLog.w(TAG, "MiLink Service Created, pid=" + android.os.Process.myPid() + ", cost="
                + (end - begin));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MiLinkLog.v(TAG, "MiLink Service end");
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        MiLinkLog.v(TAG, "onTaskRemoved");
        if (Build.VERSION.SDK_INT > 14) {
            Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
            restartServiceIntent.setPackage(getPackageName());

            PendingIntent restartServicePendingIntent = PendingIntent.getService(
                    getApplicationContext(), 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT);
            AlarmManager alarmService = (AlarmManager) getApplicationContext().getSystemService(
                    Context.ALARM_SERVICE);
            alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000,
                    restartServicePendingIntent);
            super.onTaskRemoved(rootIntent);
        }
    }
}

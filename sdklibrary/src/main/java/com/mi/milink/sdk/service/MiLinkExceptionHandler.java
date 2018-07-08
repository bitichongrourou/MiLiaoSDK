
package com.mi.milink.sdk.service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Debug;
import android.text.format.Formatter;
import android.text.format.Time;

import com.mi.milink.sdk.base.Global;
import com.mi.milink.sdk.data.Const;
import com.mi.milink.sdk.debug.MiLinkLog;

/**
 * 未捕获的异常处理类
 * 
 * @author MK
 */
public class MiLinkExceptionHandler implements UncaughtExceptionHandler {
    private static String TAG = MiLinkExceptionHandler.class.getName();

    // 系统默认的异常处理器
    private static final UncaughtExceptionHandler sDefaultHandler = Thread
            .getDefaultUncaughtExceptionHandler();

    private String mPhoneModel;

    private String mPhoneSdk;

    public MiLinkExceptionHandler() {
        // 获取基本信息
        mPhoneModel = android.os.Build.MODEL;
        mPhoneSdk = android.os.Build.VERSION.SDK;
    }

    /*
     * (non-Javadoc)
     * @see
     * java.lang.Thread.UncaughtExceptionHandler#uncaughtException(java.lang
     * .Thread, java.lang.Throwable)
     */
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        final Writer stackResult = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(stackResult);
        ex.printStackTrace(printWriter);

        StringBuilder sb = new StringBuilder();

        Time tmtxt = new android.text.format.Time();
        tmtxt.setToNow();
        String sTime = tmtxt.format("%Y-%m-%d %H:%M:%S");

        sb.append("\t\n==================LOG=================\t\n");
        sb.append("PHONE_MODEL:" + mPhoneModel + "\t\n");
        sb.append("ANDROID_SDK:" + mPhoneSdk + "\t\n");
        sb.append(sTime + "\t\n");
        sb.append(stackResult.toString());
        sb.append("\t\n==================MemoryInfo=================\t\n");
        sb.append(getMemoryInfo(Global.getContext()));
        sb.append("\t\n--------------------------------------\t\n");
        MiLinkLog.i(TAG, sb.toString());
        MiLinkLog.getInstance().flush();
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {

        }

        MiLinkLog.i(TAG, "sDefaultHandler=" + sDefaultHandler);
        arouseMiLink();
        if (sDefaultHandler != null) {
            sDefaultHandler.uncaughtException(thread, ex);
        }
        // stop之后，不应该再有输出日志的代码执行，切记！！！
        MiLinkLog.getInstance().stop();
    }

    private String getMemoryInfo(Context context) {
        String text = "";
        try {
            text += "\ntotalMemory()=" + toMib(context, Runtime.getRuntime().totalMemory());
            text += "\nmaxMemory()=" + toMib(context, Runtime.getRuntime().maxMemory());
            text += "\nfreeMemory()=" + toMib(context, Runtime.getRuntime().freeMemory());
            android.os.Debug.MemoryInfo mi2 = new android.os.Debug.MemoryInfo();
            Debug.getMemoryInfo(mi2);
            text += "\ndbg.mi.dalvikPrivateDirty=" + toMib(mi2.dalvikPrivateDirty);
            text += "\ndbg.mi.dalvikPss=" + toMib(mi2.dalvikPss);
            text += "\ndbg.mi.dalvikSharedDirty=" + toMib(mi2.dalvikSharedDirty);
            text += "\ndbg.mi.nativePrivateDirty=" + toMib(mi2.nativePrivateDirty);
            text += "\ndbg.mi.nativePss=" + toMib(mi2.nativePss);
            text += "\ndbg.mi.nativeSharedDirty=" + toMib(mi2.nativeSharedDirty);
            text += "\ndbg.mi.otherPrivateDirty=" + toMib(mi2.otherPrivateDirty);
            text += "\ndbg.mi.otherPss" + toMib(mi2.otherPss);
            text += "\ndbg.mi.otherSharedDirty=" + toMib(mi2.otherSharedDirty);

            text += "\nTotalPrivateDirty=" + toMib(mi2.getTotalPrivateDirty());
            text += "\nTotalPss=" + toMib(mi2.getTotalPss());
            text += "\nTotalSharedDirty=" + toMib(mi2.getTotalSharedDirty());

        } catch (Exception e) {
        }
        return text;
    }

    private String toMib(Context context, long nativeHeapSize) {
        return Formatter.formatFileSize(context, nativeHeapSize);
    }

    private String toMib(int size) {
        return String.format("%.2fMB", size / 1024.0);
    }

    private void arouseMiLink() {
        Intent serviceIntent = new Intent();
        serviceIntent.putExtra(Const.Extra.OnStartCommandReturn, 1);
        serviceIntent.setComponent(new ComponentName(Global.getContext(), Const.IPC.ServiceName));
        PendingIntent pendingIntent = PendingIntent.getService(Global.getContext(), 0,
                serviceIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        // 重启应用
        AlarmManager mgr = (AlarmManager) Global.getContext().getSystemService(
                Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 500, pendingIntent);
        MiLinkLog.i(TAG, "arouseMiLink");
    }
}

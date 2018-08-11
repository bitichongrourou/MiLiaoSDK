/**
 * Name : ClockReceiver.java<br>
 * Description : Broadcast Receiver of Android Alarm System<br>
 */

package com.mi.milink.sdk.receiver;

import java.util.Calendar;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Process;
import android.os.SystemClock;
import android.provider.AlarmClock;
import android.text.TextUtils;

import com.mi.milink.sdk.account.manager.MiAccountManager;
import com.mi.milink.sdk.base.Global;
import com.mi.milink.sdk.base.os.info.NetworkDash;
import com.mi.milink.sdk.base.os.timer.AlarmClockService;
import com.mi.milink.sdk.config.ConfigManager;
import com.mi.milink.sdk.data.Const;
import com.mi.milink.sdk.debug.MiLinkLog;
import com.mi.milink.sdk.event.MiLinkEvent.SystemNotificationEvent;

import org.greenrobot.eventbus.EventBus;

/**
 * Android Alarm 广播接收器 <br>
 * <br>
 * 运行时接收: 在代码中注册这个类的子类，并过滤指定的{@link AlarmClock} 对象的 {@code getPrefixName()的Action
 * <br>
 * <br> 持续化接收: 在AndroidManifest.xml中声明本类或者本类的子类，接受的Action为指定的{@link AlarmClock}
 * 对象的 {@code getPrefixName()}<br>
 * <br>
 * <b>注意: 不要覆盖onReceive方法</b>
 *
 * @see BroadcastReceiver
 * @see AlarmClock
 */
public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public final void onReceive(Context context, Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            String packageName = intent.getPackage();
            MiLinkLog.d("MiLinkAlarm", "action=" + action + ",package=" + packageName);
            // 保证是自己app和action发出的闹钟。
            if (action != null && action.equals(Const.Service.ActionName) && packageName != null
                    && packageName.equals(Global.getPackageName())) {

                if (!MiAccountManager.getInstance().appHasLogined()) {
                    MiLinkLog.w("MiLinkAlarm", "app not login cancel");
                    AlarmClockService.stop();
                    return;
                }
                boolean needSuicide = false;
                // 是否需要自杀
                if (SystemClock.elapsedRealtime() - Global.STARTUP_TIME > ConfigManager.SERVICE_SUICIDE_INTERVAL) {
                    Calendar c = Calendar.getInstance();
                    long hour = c.get(Calendar.HOUR_OF_DAY);
                    // 在凌晨1-8点允许自杀
                    if (hour >= 1 && hour <= 8) {
                        needSuicide = true;
                    }
                }

                if (needSuicide) {
                    MiLinkLog.w("MiLinkAlarm", "milinkservice will be suicide , after 10s Launch");
                    AlarmClockService.start(10 * 1000);
                    int pid = Process.myPid();
                    MiLinkLog.d("MiLinkAlarm", "suicide now!!! pid=" + pid);
                    android.os.Process.killProcess(pid);
                } else {
                	if(NetworkDash.isAvailable()){
                		AlarmClockService.start();
                	}else{
                		AlarmClockService.stop();
                	}
                    // 如果是单进程应用不能启动这个。
                    String processName = Global.getClientAppInfo().getServiceProcessName();
                    if (!TextUtils.isEmpty(processName)) {
                        Intent serviceIntent = new Intent();
                        serviceIntent.putExtra(Const.Extra.OnStartCommandReturn, 1);
                        serviceIntent
                                .setComponent(new ComponentName(context, Const.IPC.ServiceName));

                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                            //android 8.0后必须启动前台Service
                            context.startForegroundService(serviceIntent);
                        }else {
                            context.startService(serviceIntent);
                        }
                    }
                    EventBus.getDefault().post(
                            new SystemNotificationEvent(
                                    SystemNotificationEvent.EventType.AlarmArrived));
                }
            }
        }
    }
}


package com.mi.milink.sdk.base;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;

import com.mi.milink.sdk.data.ClientAppInfo;
import com.mi.milink.sdk.data.Const;
import com.mi.milink.sdk.data.Option;
import com.mi.milink.sdk.debug.MiLinkLog;

/**
 * 全局运行时环境<br>
 * <br>
 * 该类静态包装了{@link android.content.ContextWrapper}的全部方法，可以在不存在或者不方便传递
 * {@code Context} 的情况下使用当前的{@code Application}作为{@code Context}<br>
 *
 * <pre>
 * e.g.
 * 
 * public boolean updateNetworkInfo()
 * {
 * 	#//获得Android连通性服务实例
 * 	ConnectivityManager manager = (ConnectivityManager) Global.getSystemService(Context.CONNECTIVITY_SERVICE);
 * 
 * 	NetworkInfo info = manager.getActiveNetworkInfo();
 * }
 * </pre>
 *
 * ① 若没有自定义{@code Application}的需要，请在AndroidManifest.xml中设定其 android:name
 * 属性为com.mi.milink.sdk.base.BaseApplication<br>
 * <br>
 * ② 若已经使用其他的{@code Application}的子类作为自己的Application，请在使用BASE库之前， 在其
 * {@code Application.onCreate()} 方法中调用 {@code Global.init(Application)} <br>
 * <br>
 * 若没有初始化{@code Global}，使用本类的静态方法会得到{@link BaseLibException} 的运行时异常，请检查
 * {@Application}的初始化代码或AndroidManifest.xml中的声明
 *
 * @author MK
 */
public class Global {

    private static final String TAG = Const.Tag.Service;

    private static final byte PROTOCOL_VERSION = 3; // milink 协议版本

    // 每次如果有影响监控数据的改动或者重大修改，要增加version
    private static final int VERSION = 02;

    // 某个version的BUG修复，请增加sub_version。如果version改动，把sub_version置0
    private static final int SUB_VERSION = 01;

    private static boolean isInit = false;

    private static Context context;

    private static boolean isDebug = false;

    private static ClientAppInfo clientAppInfo = null;

    private static String clientIp; // 客户端的IP

    private static String clientIsp; // 客户端的ISP，联通、移动等

    private static int pid;

    // ------------------------------------------------------------------------------
    // 启动时间
    // ------------------------------------------------------------------------------
    public static long STARTUP_TIME = SystemClock.elapsedRealtime();

    /**
     * service空间中全局的自增sequence
     */
    private static AtomicInteger uniqueSeqNO = new AtomicInteger(1);

    /**
     * 获得MNS服务启动时长
     * 
     * @return delta, 单位ms
     */
    public static final long startupTimespan() {
        return SystemClock.elapsedRealtime() - STARTUP_TIME;
    }

    public final static void init(Context ctx, ClientAppInfo appInfo) {
        isInit = true;
        pid = android.os.Process.myPid();
        setContext(ctx);
        setClientAppInfo(appInfo);
        
    }
    

    public static Handler getMainHandler(){
        return  new Handler(Global.getMainLooper());
    }
    
    public static int getPid() {
        return pid;
    }

    public static void setClientIp(String ip) {
        clientIp = ip;
    }

    public static String getClientIp() {
        return clientIp;
    }

    public static void setClientIsp(String isp) {
        clientIsp = isp;
    }

    public static String getClientIsp() {
        return clientIsp != null ? clientIsp : "";
    }

    public static boolean isInit() {
        return isInit;
    }

    public final static Context getContext() {
        if (context == null) {
            throw new BaseLibException(
                    "Global's Context is NULL, have your Application in manifest "
                            + "subclasses BaseApplication or Call 'Global.init(this)' in your own Application ? ");
        }

        return context;
    }

    public final static void setContext(Context context) {
        Global.context = context;

        try {
            ApplicationInfo info = context.getApplicationInfo();

            isDebug = ((info.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0);

            if (isDebug) {
                android.util.Log.w("Mns.Global.Runtime", "DEBUG is ON");
            }
        } catch (Exception e) {
            isDebug = false;
        }
    }

    public final static ClientAppInfo getClientAppInfo() {
        if (clientAppInfo == null) {
            recoveryClient();
            if (clientAppInfo == null) {
                throw new BaseLibException(
                        "Global's clientAppInfo is NULL, have your Application in manifest "
                                + "subclasses BaseApplication or Call 'Global.init(this)' in your own Application ? ");
            }
        }
        return clientAppInfo;
    }

    public final static void setClientAppInfo(ClientAppInfo info) {
        Global.clientAppInfo = info;
        protectClient();
    }

    private final static void protectClient() {
        Option.putString(Const.Protection.Client, getClientAppInfo().toString()).commit();
//        MiLinkLog.e(TAG, "Client Protection Saved : " + getClientAppInfo().toString());
    }

    private final static void recoveryClient() {
        String savedInstance = Option.getString(Const.Protection.Client, null);
        if (savedInstance == null || savedInstance.length() < 1) {
            return;
        } else {
            MiLinkLog.e(TAG, "Client Protection Loaded : " + savedInstance);
            try {
                ClientAppInfo client = new ClientAppInfo(savedInstance);
                setClientAppInfo(client);
            } catch (Exception e) {
                MiLinkLog.e(TAG, "Client Protection Failed", e);
            }
        }
    }

    public final static void cancelProtection() {
        Option.remove(Const.Protection.Client);
        MiLinkLog.e(TAG, "Client Protection Cleared : " + getClientAppInfo());
    }

    /**
     * 判断当前进程是否是主进程<br>
     * <br>
     * <i>子进程的名称包含':'，以此为依据</i>
     *
     * @return 是主进程，或不是主进程<s>，这是一个问题</s>
     */
    public final static boolean isMainProcess() {
        String processName = currentProcessName();

        return processName == null ? false : (processName.indexOf(':') < 1);
    }

    public final static boolean isDebug() {
        return isDebug;
    }

    /**
     * 获得当前进程的进程名 <br>
     * <br>
     * <b>这个过程包含用轮询实现，所以不要总是使用</b>
     *
     * @return 当前进程的进程名，任何异常情况将得到 null
     */
    public final static String currentProcessName() {
        ActivityManager manager = (ActivityManager) Global
                .getSystemService(Context.ACTIVITY_SERVICE);

        if (manager == null) {
            return null;
        }

        List<RunningAppProcessInfo> processInfos = manager.getRunningAppProcesses();

        if (processInfos == null) {
            return null;
        }

        int pid = Process.myPid();

        for (RunningAppProcessInfo processInfo : processInfos) {
            if (pid == processInfo.pid) {
                return processInfo.processName;
            }
        }

        return null;
    }

    /*
     * 下面为 Android.Context 的同名静态方法包装 ↓
     */
    public final static AssetManager getAssets() {
        return getContext().getAssets();
    }

    public final static PackageManager getPackageManager() {
        return getContext().getPackageManager();
    }

    public final static Looper getMainLooper() {
        return getContext().getMainLooper();
    }

    public final static Context getApplicationContext() {
        return getContext().getApplicationContext();
    }

    public final static String getPackageName() {
        return getContext().getPackageName();
    }

    public final static ApplicationInfo getApplicationInfo() {
        return getContext().getApplicationInfo();
    }

    public final static SharedPreferences getSharedPreferences(String name, int mode) {
        return getContext().getSharedPreferences(name, mode);
    }
    
    public final static File getFilesDir() {
        return getContext().getFilesDir();
    }

    public final static File getCacheDir() {
        return getContext().getCacheDir();
    }


    public final static void sendBroadcast(Intent intent) {
        getContext().sendBroadcast(intent);
    }

    public final static void sendBroadcast(Intent intent, String receiverPermission) {
        getContext().sendBroadcast(intent, receiverPermission);
    }

    public final static void sendOrderedBroadcast(Intent intent, String receiverPermission) {
        getContext().sendOrderedBroadcast(intent, receiverPermission);
    }

    public final static void sendOrderedBroadcast(Intent intent, String receiverPermission,
            BroadcastReceiver resultReceiver, Handler scheduler, int initialCode,
            String initialData, Bundle initialExtras) {
        getContext().sendOrderedBroadcast(intent, receiverPermission, resultReceiver, scheduler,
                initialCode, initialData, initialExtras);
    }

    public final static void sendStickyBroadcast(Intent intent) {
        getContext().sendStickyBroadcast(intent);
    }

    public final static void sendStickyOrderedBroadcast(Intent intent,
            BroadcastReceiver resultReceiver, Handler scheduler, int initialCode,
            String initialData, Bundle initialExtras) {
        getContext().sendStickyOrderedBroadcast(intent, resultReceiver, scheduler, initialCode,
                initialData, initialExtras);
    }

    public final static void removeStickyBroadcast(Intent intent) {
        getContext().removeStickyBroadcast(intent);
    }

    public final static Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        return getContext().registerReceiver(receiver, filter);
    }

    public final static Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter,
            String broadcastPermission, Handler scheduler) {
        return getContext().registerReceiver(receiver, filter, broadcastPermission, scheduler);
    }

    public final static void unregisterReceiver(BroadcastReceiver receiver) {
        getContext().unregisterReceiver(receiver);
    }

    public final static Object getSystemService(String name) {
        try {
            return getContext().getSystemService(name);
        } catch (Exception e) {
            return null;
        }
    }

    public final static int getSequence() {
        return uniqueSeqNO.getAndIncrement();
    }

    public final static int getMiLinkVersion() {
        return VERSION;
    }

    public final static int getMiLinkSubVersion() {
        return SUB_VERSION;
    }

    public final static byte getMiLinkProtocolVersion() {
        return PROTOCOL_VERSION;
    }
//    
//    private static boolean isIpModle = false;
//    public static void setIPModle(boolean ipModle) {
//    	isIpModle = ipModle;
//    	MiLinkLog.d(TAG, "setIPModle = "+ isIpModle);
//    }
//    public static boolean getIpModle(){
//    	return isIpModle;
//    }
    

}

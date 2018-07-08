
package com.mi.milink.sdk.client.ipc.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Toast;

import com.mi.milink.sdk.aidl.IEventCallback;
import com.mi.milink.sdk.aidl.IPacketCallback;
import com.mi.milink.sdk.aidl.IService;
import com.mi.milink.sdk.aidl.PacketData;
import com.mi.milink.sdk.base.Global;
import com.mi.milink.sdk.base.os.HandlerThreadEx;
import com.mi.milink.sdk.base.os.timer.AlarmClockService;
import com.mi.milink.sdk.client.IPacketListener;
import com.mi.milink.sdk.client.ipc.ClientLog;
import com.mi.milink.sdk.data.Const;
import com.mi.milink.sdk.debug.MiLinkMonitor;
import com.mi.milink.sdk.util.SystemUtils;

/**
 * MiLink服务的远端调用封装 <br>
 * <br>
 * 同时，它也是一个{@link #MiLinkOberser}的事件的派发器，使用Java的Observer/Observable机制
 *
 * @author MK
 */
public class MiLinkServiceHost extends Observable implements ServiceConnection {

    protected static final String TAG = Const.Tag.Client;

    private static final int MILINK_OPEN_OPEN_SERVICE_SUCCESS = 0;

    private static final int MILINK_OPEN_OPEN_SERVICE_FAILED = 1;

    private static final int MILINK_OPEN_BIND_SERVICE_SUCCESS = 0;

    private static final int MILINK_OPEN_BIND_SERVICE_FIRST_FAILED = 1;

    private static final int MILINK_OPEN_BIND_SERVICE_SECOND_FAILED = 2;

    private static final int MILINK_OPEN_GET_REMOTE_SERVICE_SUCCESS = 0;

    private static final int MILINK_OPEN_GET_REMOTE_SERVICE_FAILED = 1;

    private static final int SERVICE_STOP_THRESHOLD = 2;

    private static final int SERVICE_START_THRESHOLD = 3;

    protected Context context;

    protected volatile IService remoteService; // 远端服务

    private volatile int servicePid = Integer.MIN_VALUE; // 服务端PID

    private volatile boolean serviceConnecting = false; // 正在连接标志位

    private volatile Object SERVICE_LOCK = new Object(); // 服务连接同步锁

    private volatile boolean userStartService = false; // 客户端启动主动启动Service

    private volatile int restartTimes = 0;

    private HandlerThreadEx eventCenter; // 服务事件接收线程

    private Handler.Callback eventCallback = new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            ClientLog.v(TAG, "receive event callback: " + msg.what);
            // 处理内部事件
            if (onHandleInternalServiceEvent(msg)) {
                return false;
            }

            // 标记事件到来
            setChanged();
            try {
                // 通知监视器
                notifyObservers(msg);
            } catch (Exception e) {
            }
            return false;
        }
    };

    // TODO 时间回调等
    protected com.mi.milink.sdk.client.IEventListener mEventListener;

    protected IPacketListener mPacketListener;

    private IEventCallback.Stub mIEventCallback;

    private IPacketCallback.Stub mIPacketCallback;

    protected String mMiPushRegId = null;
    
    protected int mLogLevel = 63;

    protected boolean mGlobalPushFlag = false;

    protected MiLinkServiceHost(Context context) {
        this.context = context;

        // 创建 服务事件接收线程
        eventCenter = new HandlerThreadEx("MiLinkEventNotifier", true,
                Process.THREAD_PRIORITY_BACKGROUND, eventCallback);
        mIEventCallback = new IEventCallback.Stub() {

            @Override
            public void onEventShouldCheckUpdate() throws RemoteException {
                if (mEventListener != null) {
                    mEventListener.onEventShouldCheckUpdate();
                }
            }

            @Override
            public void onEventServiceTokenExpired() throws RemoteException {
                if (mEventListener != null) {
                    mEventListener.onEventServiceTokenExpired();
                }
            }

            @Override
            public void onEventKickedByServer(int type, long time, String device)
                    throws RemoteException {
                if (mEventListener != null) {
                    mEventListener.onEventKickedByServer(type, time, device);
                }
            }

            @Override
            public void onEventInvalidPacket() throws RemoteException {
                if (mEventListener != null) {
                    mEventListener.onEventInvalidPacket();
                }
            }

            @Override
            public void onEventGetServiceToken() throws RemoteException {
                if (mEventListener != null) {
                    mEventListener.onEventGetServiceToken();
                }
            }
        };

        mIPacketCallback = new IPacketCallback.Stub() {

            @Override
            public boolean onReceive(List<PacketData> message) throws RemoteException {
                if (mPacketListener != null) {
                    mPacketListener.onReceive((ArrayList<PacketData>) message);
                    return true;
                }
                return false;
            }
        };

    }

    private boolean isStartServiceThreadRunning = false;

    private Runnable mStartServiceRunnable = new Runnable() {

        @Override
        public void run() {
            isStartServiceThreadRunning = true;
            long openStartTime = System.currentTimeMillis();
            int stopCount = 0;
            // 这个循环一定不能在主线程运行 否则造成onServiceConnected得不到机会运行
            while (remoteService == null && stopCount < SERVICE_STOP_THRESHOLD) {
                ClientLog.e(TAG, "getRemoteService, but remoteService = null, stopCount="
                        + stopCount);
                int startCount = 0;
                while (remoteService == null && startCount++ < SERVICE_START_THRESHOLD) {
                    ClientLog.e(TAG, "try startService, startCount=" + startCount);
                    try {
                        serviceConnecting = false;
                        boolean result = startService(Reason.Restart);
                        if (result) {
                            synchronized (SERVICE_LOCK) {
                                try {
                                    SERVICE_LOCK.wait(3 * 1000L);
                                } catch (InterruptedException e) {
                                }
                            }
                        } else {
                            // 增加至1秒钟一次的启动服务
                            SystemClock.sleep(1000L);
                        }
                    } catch (Exception e) {
                        // maybe SecurityException : unable to find app for
                        // caller
                        // ...
                        ClientLog.e(TAG,
                                "startService(Reason.Restart) exception  :" + e.getMessage());
                        SystemClock.sleep(3000L);
                    }
                }
                if (remoteService == null) {
                    stopCount++;
                    stopService(Reason.UserCall);
                }
            }
            if (remoteService == null) {
                ClientLog.e(TAG, "mns service start failed ,create system.log. stopCount="
                        + stopCount);
                ClientLog.generateSystemLog();
                MiLinkMonitor.getInstance().trace("", 0, Const.MnsCmd.MNS_GET_REMOTE_SERVICE,
                        MILINK_OPEN_GET_REMOTE_SERVICE_FAILED, openStartTime,
                        System.currentTimeMillis(), 0, 0, 0);
                MiLinkMonitor.getInstance().doPostDataAtOnce();
                Handler h = new Handler(Global.getMainLooper());
                // 尝试引导用户重启app。
                ClientLog.e(TAG, "begin guide to setting page");
                {
                    h.post(new Runnable() {
                        
                        @Override
                        public void run() {
                        	try{
                            String SCHEME = "package";
                            String APP_PKG_NAME_21 = "com.android.settings.ApplicationPkgName";
                            String APP_PKG_NAME_22 = "pkg";
                            String APP_DETAILS_PACKAGE_NAME = "com.android.settings";
                            String APP_DETAILS_CLASS_NAME = "com.android.settings.InstalledAppDetails";

                            Intent intent = new Intent();
                            String packageName = Global.getPackageName();
                            final int apiLevel = Build.VERSION.SDK_INT;
                            if (apiLevel >= 9) { // above 2.3
                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts(SCHEME, packageName, null);
                                intent.setData(uri);
                            } else { // below 2.3
                                final String appPkgName = (apiLevel == 8 ? APP_PKG_NAME_22
                                        : APP_PKG_NAME_21);
                                intent.setAction(Intent.ACTION_VIEW);
                                intent.setClassName(APP_DETAILS_PACKAGE_NAME,
                                        APP_DETAILS_CLASS_NAME);
                                intent.putExtra(appPkgName, packageName);
                            }
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent);
                            Toast.makeText(context, "Service启动失败,请点击“结束运行”后再次启动APP", Toast.LENGTH_LONG).show();
                        	}catch(Exception e){
                        		
                        	}
                        }
                    });
                }                
                ClientLog.e(TAG, "结束启动引导");
                
                ClientLog.e(TAG, "after 10s, kill app！！！！！");
                h.postDelayed(mKillRunnable, 10 * 1000);
            } else {
                MiLinkMonitor.getInstance().trace("", 0, Const.MnsCmd.MNS_GET_REMOTE_SERVICE,
                        MILINK_OPEN_GET_REMOTE_SERVICE_SUCCESS, openStartTime,
                        System.currentTimeMillis(), 0, 0, 0);
            }
            isStartServiceThreadRunning = false;
        }
    };

    public IService getRemoteServiceProxy() {
        if (remoteService != null) {
            return remoteService;
        }
        if (Looper.myLooper() != null && Looper.myLooper() == Looper.getMainLooper()) {
            ClientLog.e(TAG, "dangerous!!!!getRemoteService in main Thread is not safe!!!");
            if (!isStartServiceThreadRunning) {
                new Thread(mStartServiceRunnable).start();
            }
        } else {
            if (!isStartServiceThreadRunning) {
                mStartServiceRunnable.run();
            }
        }
        return remoteService;
    }

    private Runnable mKillRunnable = new Runnable() {

        @Override
        public void run() {
            killService();
            killApp();
        }
    };

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        ClientLog.e(TAG, "onServiceConnected()");
        synchronized (this) {
            try {
                if (serviceConnecting) {
                    serviceConnecting = false;
                } else {
                    ClientLog
                            .e(TAG, "Ghost's Call? Nobody binds service but Callback here. WTF!!!");
                }
                remoteService = IService.Stub.asInterface(service);
                /* 统一在这里将Binder都对应起来 */
                remoteService.setEventCallBack(mIEventCallback);
                remoteService.setPacketCallBack(mIPacketCallback);
                // 将客户端信息提供给服务端，并得到服务的PID
                Bundle clientInfo = new Bundle();
                {
                    // clientInfo.putParcelable(Const.IPC.ClientInfo,
                    // getClient());
                    clientInfo.putParcelable(Const.IPC.ClientNotifier, eventCenter.getMessenger());
                }
                servicePid = remoteService.setClientInfo(clientInfo);
                if(!TextUtils.isEmpty(mMiPushRegId)){
                    remoteService.setMipushRegId(mMiPushRegId);
                }
                // 设置日志级别
                remoteService.setMilinkLogLevel(mLogLevel);
                remoteService.setGlobalPushFlag(mGlobalPushFlag);
                if (servicePid == Integer.MIN_VALUE) {
                    stopService(Reason.ClientError);
                }
                onMilinkServiceReady();
            } catch (Exception e) {
                stopService(Reason.ClientError);
            }
            if (remoteService != null) {
                ClientLog.e(TAG, "onServiceConnected got a binder");
            }
            // 无论连接成功失败，通知正在等待服务连接完毕的线程
            synchronized (SERVICE_LOCK) {
                SERVICE_LOCK.notifyAll();
            }
            
        }
    }

    protected void onMilinkServiceReady() {
		
	}

	@Override
    public void onServiceDisconnected(ComponentName name) {
        ClientLog.e(TAG, "onServiceDisconnected()");
        synchronized (this) {
            // 增加失败计数
            restartTimes++;

            stopService(Reason.Disconnect);
        }
    }

    /**
     * 如果是服务发来的内部消息，不需要通知客户端
     *
     * @param msg 消息
     * @return
     */
    protected boolean onHandleInternalServiceEvent(Message msg) {
        if (msg.what == Const.Event.RUNTIME_CHANGED) {
            return true;
        }

        return false;
    }

    /**
     * 结束MNS服务的进程
     */
    public void killService() {
        ClientLog.e(TAG, "Service[" + servicePid + "] will be Terminated");
        setServicePidByServiceName();
        // 停止alarm
        AlarmClockService.stop();
        Process.killProcess(servicePid);
    }

    private void killApp() {
        int appid = Process.myPid();
        ClientLog.e(TAG, "app[" + appid + "] will be Terminated beccause getRemoteService==null");
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {

        }
        Process.killProcess(appid);
    }

    public void setServicePidByServiceName() {
        String serviceProcessName = Global.getClientAppInfo().getServiceProcessName();
        servicePid = SystemUtils.getPidByProcessName(serviceProcessName);
        ClientLog.e(TAG, "serviceProcess pid = " + servicePid);
    }

    /**
     * 判断服务当前是否可用 <br>
     * <br>
     * 服务的同步调用一定要预先判定后再调用{@code remoteService()}
     *
     * @return 远端服务可用/不可用
     */
    public boolean isServiceAvailable() {
        return (remoteService != null);
    }

    /**
     * 判断服务当前是否可用并且可通信（略有消耗）<br>
     * <br>
     *
     * @return 可用并可以通信
     */
    public boolean isServiceAlive() {
        try {
            return isServiceAvailable();
        } catch (Exception e) {
            ClientLog.e(TAG, "Remote Service is Dead");
            return false;
        }
    }

    public int getServicePid() {
        if (remoteService != null) {
            return servicePid;
        }
        return -1;
    }

    /**
     * 初始化服务，启动服务防止被系统按照绑定机制肆意回收
     */
    private void initService() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(context, Const.IPC.ServiceName));
        intent.setPackage(Global.getPackageName());
        long openStartTime = System.currentTimeMillis();
        ComponentName cn = context.startService(intent);
        if (cn == null) {
            ClientLog.e(TAG, "start service failed");
            MiLinkMonitor.getInstance().trace("", 0, Const.MnsCmd.MNS_START_SERVICE,
                    MILINK_OPEN_OPEN_SERVICE_FAILED, openStartTime, System.currentTimeMillis(), 0,
                    0, 0);
        } else {
            ClientLog.e(TAG, "startService ComponentName = " + cn.toString());
            ClientLog.e(TAG, "start service success");
            MiLinkMonitor.getInstance().trace("", 0, Const.MnsCmd.MNS_START_SERVICE,
                    MILINK_OPEN_OPEN_SERVICE_SUCCESS, openStartTime, System.currentTimeMillis(), 0,
                    0, 0);
        }
    }

    /**
     * 启动MNS服务，需要提供原因
     *
     * @param reason 启动原因
     * @return 启动成功/失败
     */
    private boolean startService(Reason reason) {
        synchronized (this) {

            // 用户主动启动，做个标记
            if (Reason.UserCall.equals(reason)) {
                userStartService = true;
            }

            // 已经在连接了，静候佳音
            if (serviceConnecting) {
                return true;
            }

            // 为了防止丢失后的Service不记得被Start过，再来一次好了
            initService();

            Intent intent = new Intent();
            intent.setComponent(new ComponentName(context, Const.IPC.ServiceName));
            intent.setPackage(Global.getPackageName());
            long openStartTime = System.currentTimeMillis();
            boolean result = context.bindService(intent, this, Context.BIND_AUTO_CREATE);

            if (!result) {
                MiLinkMonitor.getInstance().trace("", 0, Const.MnsCmd.MNS_BIND_SERVICE,
                        MILINK_OPEN_BIND_SERVICE_FIRST_FAILED, openStartTime,
                        System.currentTimeMillis(), 0, 0, 0);
                ClientLog.i(TAG, "bindService() first time failed!!");
                // throw new BaseLibException("Cannot Connect to Mns Service : "
                // + intent.getComponent());

                /* 如果服务绑定失败，休息一下，再尝试一次，如果仍然失败，按照服务失败定论，主动通知服务失败的消息 */
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e) {

                }
                openStartTime = System.currentTimeMillis();
                result = context.bindService(intent, this, Context.BIND_AUTO_CREATE);

                if (!result) {
                    ClientLog.i(TAG, "bindService() second time failed too!!");
                    MiLinkMonitor.getInstance().trace("", 0, Const.MnsCmd.MNS_BIND_SERVICE,
                            MILINK_OPEN_BIND_SERVICE_SECOND_FAILED, openStartTime,
                            System.currentTimeMillis(), 0, 0, 0);
                    stopService(Reason.SystemFatal);

                    // 还是通过主线程通知，为了保证逻辑，加200ms延时
                    new Handler(context.getMainLooper()).postDelayed(new Runnable() {

                        @Override
                        public void run() {
                            ClientLog
                                    .i(TAG,
                                            "bindService() twice failed , then inform the client by called onServiceConnected()");
                            // 通知服务启动失败
                            onServiceConnected(new ComponentName(context, Const.IPC.ServiceName),
                                    null);
                        }
                    }, 200);

                    return false;
                }
            }

            MiLinkMonitor.getInstance().trace("", 0, Const.MnsCmd.MNS_BIND_SERVICE,
                    MILINK_OPEN_BIND_SERVICE_SUCCESS, openStartTime, System.currentTimeMillis(), 0,
                    0, 0);

            ClientLog.i(TAG, "bindService() success!!");

            // 如果调用bindService成功，则标记服务正在启动
            if (result) {
                serviceConnecting = true;
            }

            return result;
        }
    }

    public void stopService() {
        stopService(Reason.UserCall);
    }
    
    private void stopService(Reason reason) {
        ClientLog.v(TAG, "stopService" + reason);
        synchronized (this) {
            try {
                serviceConnecting = false;
                if (Reason.UserCall.equals(reason)) {
                    AlarmClockService.stop();
                    userStartService = false;
                    context.unbindService(this);

                    Intent intent = new Intent();

                    intent.setComponent(new ComponentName(context, Const.IPC.ServiceName));

                    context.stopService(intent);
                    ClientLog.v(TAG, "stopService over");
                }
            } catch (Exception e) {
            }
            remoteService = null;
        }
    }

    /**
     * 服务启动/关闭的理由，调试用
     */
    private enum Reason {
        UserCall("用户调用"), Restart("断开后重连"),

        Disconnect("服务主动断开"), ClientError("发生错误断开"), RemoteDead("服务挂了"), SystemFatal("服务启动失败");

        private String reason;

        private Reason(String reasonStr) {
            reason = reasonStr;
        }

        @Override
        public String toString() {
            return reason;
        }
    }

    public enum ServiceStartResult {
        Success, SystemError, NativeUnzipFailed, NativeLoadFailed
    }

    /**
     * mns服务启动监听器
     */
    public interface OnServiceStartListener {
        void onServiceStarted(ServiceStartResult result);
    }

}

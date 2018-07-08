
package com.mi.milink.sdk.base.os;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;

/**
 * Android HandlerThread包装类<br>
 * <br>
 * 自带Handler和Messenger，并支持随时调用不必关心Handler和Messenger的初始化工作
 *
 * @author MK
 */
public class HandlerThreadEx implements Handler.Callback {
    private String name;

    private int priority = Process.THREAD_PRIORITY_DEFAULT;

    private boolean ipcable;

    private HandlerThread thread;

    private Handler handler;

    private Messenger messenger;

    private Handler.Callback callback;

    /**
     * 创建一个HandlerThread，支持IPC通信，优先级为默认优先级 THREAD_PRIORITY_DEFAULT
     *
     * @param name 线程名称
     * @param callback 消息回调
     */
    public HandlerThreadEx(String name, Handler.Callback callback) {
        this(name, true, callback);
    }

    /**
     * 创建一个HandlerThread，优先级为默认优先级 THREAD_PRIORITY_DEFAULT
     *
     * @param name 线程名称
     * @param ipcable 是否需要IPC通信
     * @param callback 消息回调
     */
    public HandlerThreadEx(String name, boolean ipcable, Handler.Callback callback) {
        this(name, ipcable, Process.THREAD_PRIORITY_DEFAULT, callback);
    }

    /**
     * 创建一个HandlerThread
     *
     * @param name 线程名称
     * @param ipcable 是否需要IPC通信
     * @param priority 线程优先级
     * @param callback 消息回调
     */
    public HandlerThreadEx(String name, boolean ipcable, int priority, Handler.Callback callback) {
        setName(name);
        setIpcable(ipcable);
        setPriority(priority);
        setCallback(callback);

        start();
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (callback != null) {
            return callback.handleMessage(msg);
        }

        return false;
    }

    protected synchronized void start() {
        if ((thread != null) && (thread.isAlive()) && (handler != null)
                && ((ipcable ? (messenger != null) : true))) {
            return;
        }

        if (thread == null) {
            thread = new HandlerThread(getName(), getPriority());
        }

        if (!thread.isAlive()) {
            thread.start();
        }

        if (thread.isAlive()) {
            handler = new Handler(thread.getLooper(), this);
        }

        if (ipcable) {
            if (handler != null) {
                messenger = new Messenger(handler);
            }
        }
    }

    public synchronized void stop() {
        if ((thread == null) || (!thread.isAlive())) {
            return;
        }

        thread.quit();
        thread = null;
    }

    public Handler getHandler() {
        start();

        return handler;
    }

    public Messenger getMessenger() {
        start();

        return messenger;
    }

    public boolean isIpcable() {
        return ipcable;
    }

    protected void setIpcable(boolean ipcable) {
        this.ipcable = ipcable;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;

        if (thread != null && thread.isAlive()) {
            thread.setName(name);
        }
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setCallback(Handler.Callback callback) {
        this.callback = callback;
    }
}

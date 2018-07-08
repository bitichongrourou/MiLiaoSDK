package com.mi.milink.sdk.base;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;

/**
 * 定制的一个带有MessageQueue的thread，并且提供一个对应的Handler对象,
 * 可以重写processMessage(Message msg)这个方法，来实现期望的处理过程;
 * <p/>
 * 可以使用自定义的Handler
 * <p/>
 * Created by MK on 15-3-18.
 */
public abstract class CustomHandlerThread {

    protected HandlerThread mHandlerThread;
    protected Handler mHandler;

    public CustomHandlerThread(String name) {
        this(name, Process.THREAD_PRIORITY_DEFAULT);
    }

    public CustomHandlerThread(final String name, int priority) {
        mHandlerThread = new HandlerThread(name, priority);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper()) {

            @Override
            public void handleMessage(Message msg) {
                try {
                    processMessage(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        };
    }

    public Message obtainMessage() {
        return mHandler.obtainMessage();
    }

    public void sendMessage(Message msg) {
        mHandler.sendMessage(msg);
    }

    public void sendMessageDelayed(Message msg, long delayMillis) {
        mHandler.sendMessageDelayed(msg, delayMillis);
    }

    public void removeMessage(int what) {
        mHandler.removeMessages(what);

    }

    public void removeMessage(int what, Object obj) {
        mHandler.removeMessages(what, obj);
    }
    
    public final boolean post(Runnable r) {
        return mHandler.post(r);
    }

    public final boolean postDelayed(Runnable r, long delayMillis) {
        return mHandler.postDelayed(r, delayMillis);
    }

    protected abstract void processMessage(Message msg);

    /**
     * 销毁该线程，清空该线程内的所有已有的任务
     */
    public void destroy() {
        mHandlerThread.quit();
    }

    public Looper getLooper() {
        return mHandlerThread.getLooper();
    }

    public void setHandler(Handler h) {
        if (h != null) {
            if (h.getLooper() != getLooper()) {
                throw new IllegalArgumentException("Looper对象不一致，请使用CustomHandlerThread.getLooper()构造Handler对象");
            }
            this.mHandler = h;
        }
    }

    public Handler getHandler() {
        return mHandler;
    }

    public HandlerThread getHandlerThread() {
        return mHandlerThread;
    }
}


package com.mi.milink.sdk.connection;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import android.util.Log;
import android.util.SparseArray;

import com.mi.milink.sdk.base.Global;
import com.mi.milink.sdk.base.os.Native;
import com.mi.milink.sdk.data.ClientAppInfo;
import com.mi.milink.sdk.debug.MiLinkLog;
import com.mi.milink.sdk.session.common.MsgProcessor;
import com.mi.milink.sdk.session.common.SessionConst;

/**
 * 连接的通用实现类
 * 
 * @author MK
 */
public class ConnectionImpl implements IConnection {
    private final static String CLASSTAG = "ConnectionImpl";

    private ConcurrentHashMap<Integer, Object> mMsgObjectMap = new ConcurrentHashMap<Integer, Object>();

    private AtomicInteger mReferenceNo = new AtomicInteger(1);

    private static volatile boolean sIsLoaded = false;

    private int mType = SessionConst.NONE_CONNECTION_TYPE;

    private MsgProcessor mMsgProc = null;

    private IConnectionCallback mCallback = null;

    private long mNativeContext; // accessed by native methods

    private String TAG;

    private int mCreatorSessionNO;
    static {
        try {
            String soBase = "connectionbase";
            String soMilink = "milinkconnection";
            boolean isLoadedSo1, isLoadedSo2;
            isLoadedSo1 = Native.loadLibrary(soBase);
            isLoadedSo2 = Native.loadLibrary(soMilink);
            if (isLoadedSo1 == false && isLoadedSo2 == true) {
                // 如果第一个load失败了，第二个load成功了，就再尝试load第一个
                isLoadedSo1 = Native.loadLibrary(soBase);
            }
            sIsLoaded = isLoadedSo1 && isLoadedSo2;
            native_init();
        } catch (UnsatisfiedLinkError e) {
            MiLinkLog.e(CLASSTAG, "System.loadLibrary failed", e);
            sIsLoaded = false;
        } catch (Exception e) {
            MiLinkLog.e(CLASSTAG, "System.loadLibrary failed", e);
            sIsLoaded = false;
        }
        if(Global.getClientAppInfo().getAppId()==ClientAppInfo.MI_SHOP_APP_ID){
        	sIsLoaded = true;
        }
        MiLinkLog.i(CLASSTAG, "loadLibrary return " + sIsLoaded);
    }

    public static boolean isLibLoaded() {
        return sIsLoaded;
    }

    public ConnectionImpl(int sessionNO, int type) {
        if (!sIsLoaded) {
            return;
        }
        mCreatorSessionNO = sessionNO;
        TAG = String.format("[No:%d]%s", sessionNO, CLASSTAG);
        mType = type;
        try {
            /*
             * Native setup requires a weak reference to our object. It's easier
             * to create it here than in C++.
             */
            native_setup(new WeakReference<ConnectionImpl>(this), mType, 1);
        } catch (Throwable e) {
            MiLinkLog.e(TAG, "native_setup failed", e);
        }
    }

    @Override
    protected void finalize() {
        MiLinkLog.v(TAG, "finalize connection");
        try {
            native_finalize();
        } catch (Exception e) {
            MiLinkLog.e(TAG, "finalize failed", e);
        }
    }

    private static native final void native_init();

    private native final void native_setup(Object ConnectionImpl_this, int type, int dfFlag);

    private native final void native_finalize();

    @Override
    public boolean postMessage(int uMsg, Object lParam, int wParam, MsgProcessor msgProc) {
        if (!sIsLoaded) {
            MiLinkLog.e(TAG, "postMessage failed:lib is unloaded");
            return false;
        }

        mMsgProc = msgProc;
        Integer index = 0;
        if (lParam != null) {
            index = mReferenceNo.getAndIncrement();
            mMsgObjectMap.put(index, lParam);
        }
        try {
            return postMessage(uMsg, index, wParam);
        } catch (Exception e) {
            MiLinkLog.e(TAG, "postMessage failed", e);
        }
        return false;
    }

    @Override
    public void setCallback(IConnectionCallback callback) {
        mCallback = callback;
    }

    @Override
    public String getServerIP() {
        return null;
    }

    @Override
    public int getServerPort() {
        return 0;
    }

    @Override
    public int getConnectionType() {
        return mType;
    }

    @Override
    public native boolean start();

    @Override
    public native boolean stop();

    @Override
    public native void wakeUp();

    public native boolean postMessage(int uMsg, int lParam, int wParam);

    @Override
    public native boolean connect(final String serverIP, final int serverPort,
            final String proxyIP, final int proxyPort, final int timeOut, final int mss);

    @Override
    public native boolean disconnect();

    @Override
    public native boolean sendData(byte[] buf, int cookie, int sendTimeout);

    @Override
    public native void removeSendData(int cookie);

    @Override
    public native void removeAllSendData();

    @Override
    public native boolean isSendDone(int cookie);

    @Override
    public native boolean isRunning();

    // ///////////////回调函数///////////////////
    /*
     * Do not change these values without updating their counterparts in
     * com_mi_milink_sdk_connection_ConnectionImpl.cpp!
     */
    private static final int MSG_ID_ON_START = 0;

    private static final int MSG_ID_ON_CONNECT = 1;

    private static final int MSG_ID_ON_DISCONNECT = 2;

    private static final int MSG_ID_ON_ERROR = 3;

    private static final int MSG_ID_ON_TIMEOUT = 4;

    private static final int MSG_ID_ON_RECV = 5;

    private static final int MSG_ID_ON_SENDBEGIN = 6;

    private static final int MSG_ID_ON_SENDEND = 7;

    private static final int MSG_ID_ON_MSGPROC = 8;

    private static SparseArray<String> sMsgMap = new SparseArray<String>();

    static {
        sMsgMap.put(MSG_ID_ON_START, "onStart");
        sMsgMap.put(MSG_ID_ON_CONNECT, "onConnect");
        sMsgMap.put(MSG_ID_ON_DISCONNECT, "onDisconnect");
        sMsgMap.put(MSG_ID_ON_ERROR, "onError");
        sMsgMap.put(MSG_ID_ON_TIMEOUT, "onTimeout");
        sMsgMap.put(MSG_ID_ON_RECV, "onRecv");
        sMsgMap.put(MSG_ID_ON_SENDBEGIN, "onSendBegin");
        sMsgMap.put(MSG_ID_ON_SENDEND, "onSendEnd");
        sMsgMap.put(MSG_ID_ON_MSGPROC, "onMsgProc");
    }

    private static List<ConnectPrintLogCallback> sLogCallbackList = new ArrayList<ConnectPrintLogCallback>();

    private static void postEventFromNative(Object ConnectionImpl_ref, int what, int arg1,
            int arg2, Object obj) {
        try {
            ConnectionImpl conn = (ConnectionImpl) ((WeakReference) ConnectionImpl_ref).get();
            if (conn == null) {
                return;
            }

            MiLinkLog.v(CLASSTAG, "postEventFromNative msg:" + sMsgMap.get(what) + ", arg1=" + arg1
                    + ", arg2=" + arg2 + " to SessionNo:" + conn.mCreatorSessionNO);

            switch (what) {
                case MSG_ID_ON_START:
                    conn.onStart();
                    break;
                case MSG_ID_ON_CONNECT:
                    conn.onConnect(arg1 != 0, arg2);
                    break;
                case MSG_ID_ON_DISCONNECT:
                    conn.onDisconnect();
                    break;
                case MSG_ID_ON_ERROR:
                    conn.onError(arg1);
                    break;
                case MSG_ID_ON_TIMEOUT:
                    conn.onTimeOut(arg1, arg2);
                    break;
                case MSG_ID_ON_RECV:
                    conn.onRecv((byte[]) (obj));
                    break;
                case MSG_ID_ON_SENDBEGIN:
                    conn.onSendBegin(arg1);
                    break;
                case MSG_ID_ON_SENDEND:
                    conn.onSendEnd(arg1);
                    break;
                case MSG_ID_ON_MSGPROC:
                    conn.onMsgProc(arg1, obj, arg2);
                    break;
                default:
                    Log.e(CLASSTAG, "Unknown message type " + what);
                    break;
            }
        } catch (Exception e) {
            MiLinkLog.e(CLASSTAG, "postEventFromNative Exception", e);
        }
    }

    public final static void printLog(int prio, String text) {
        MiLinkLog.d("native", text);
        notifyConnectPrintLogCallback(prio, text);
    }

    public static synchronized void addConnectPrintLogCallback(ConnectPrintLogCallback callBack) {
        if (callBack != null) {
            sLogCallbackList.add(callBack);
        }
    }

    public static synchronized void removeConnectPrintLogCallback(ConnectPrintLogCallback callBack) {
        if (callBack != null) {
            sLogCallbackList.remove(callBack);
        }
    }

    private static synchronized void notifyConnectPrintLogCallback(int prio, String text) {
        for (int i = 0; i < sLogCallbackList.size(); i++) {
            sLogCallbackList.get(i).printLogCallback(prio, text);
        }
    }

    // 线程启动
    public boolean onStart() {
        if (mCallback == null) {
            return false;
        }
        return mCallback.onStart();
    }

    // 连接消息
    public boolean onConnect(boolean isSuccess, int errorCode) {
        if (mCallback == null) {
            return false;
        }
        return mCallback.onConnect(isSuccess, errorCode);
    }

    // 断开消息
    public boolean onDisconnect() {
        if (mCallback == null) {
            return false;
        }
        return mCallback.onDisconnect();
    }

    // 错误消息
    public boolean onError(int socketStatus) {
        if (mCallback == null) {
            return false;
        }
        return mCallback.onError(socketStatus);
    }

    // 超时消息
    public boolean onTimeOut(int dwCookie, int nReason) {
        if (mCallback == null) {
            return false;
        }
        return mCallback.onTimeOut(dwCookie, nReason);
    }

    // 收到数据
    public boolean onRecv(byte[] pcBuf) {
        if (mCallback == null) {
            return false;
        }
        return mCallback.onRecv(pcBuf);
    }

    // 发送开始
    public boolean onSendBegin(int dwCookie) {
        if (mCallback == null) {
            return false;
        }
        return mCallback.onSendBegin(dwCookie);
    }

    // 发送结束
    public boolean onSendEnd(int dwCookie) {
        if (mCallback == null) {
            return false;
        }
        return mCallback.onSendEnd(dwCookie);
    }

    // 消息处理函数
    public boolean onMsgProc(int uMsg, Object lParam, int wParam) {
        if (mMsgProc == null) {
            return false;
        }
        Object object = mMsgObjectMap.remove(lParam);

        mMsgProc.onMsgProc(uMsg, object, wParam);
        return true;
    }

    public static interface ConnectPrintLogCallback {
        public void printLogCallback(int prio, String text);
    }

}

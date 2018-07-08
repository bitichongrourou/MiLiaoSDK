
package com.mi.milink.sdk.client.ipc.internal;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.os.AsyncTask;
import android.os.RemoteException;

import com.mi.milink.sdk.aidl.ISendCallback;
import com.mi.milink.sdk.aidl.PacketData;
import com.mi.milink.sdk.client.SendPacketListener;
import com.mi.milink.sdk.client.ipc.ClientLog;

/**
 * client实现的发消息的callback
 * 
 * @author xiaolong
 */

public class MnsSendPacketListener extends ISendCallback.Stub {

    private static final String TAG = "MnsSendPacketListener";

    private static RejectedExecutionHandler rehHandler = new RejectedExecutionHandler() {

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            ClientLog.v(TAG, "Thread pool executor: reject work, put into backup pool");
        }
    };

    private static final ThreadPoolExecutor RESPONSE_EXEXUTOR = new ThreadPoolExecutor(2, 4, 30,
            TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(4), rehHandler);

    private SendPacketListener mListener;

    public MnsSendPacketListener(SendPacketListener l) {
        mListener = l;
    }

    @Override
    public void onRsponse(final PacketData response) throws RemoteException {
        if (mListener != null) {
            new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground(Void... params) {
                    mListener.onResponse(response);
                    return null;
                }
            }.executeOnExecutor(RESPONSE_EXEXUTOR);
        }
    }

    @Override
    public void onFailed(final int errCode,final String errMsg) throws RemoteException {
        if (mListener != null) {
            new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground(Void... params) {
                    mListener.onFailed(errCode,errMsg);
                    return null;
                }
            }.executeOnExecutor(RESPONSE_EXEXUTOR);
        }
    }


}

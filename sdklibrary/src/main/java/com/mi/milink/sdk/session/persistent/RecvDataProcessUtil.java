
package com.mi.milink.sdk.session.persistent;

import android.util.Log;

import com.mi.milink.sdk.aidl.PacketData;
import com.mi.milink.sdk.debug.MiLinkLog;
import com.mi.milink.sdk.session.common.Request;

/**
 * 接收数据的处理器，是一个线程级的单例
 * 
 * @author chengsimin
 */
public class RecvDataProcessUtil {
    public static String TAG = "RecvDataProcessUtil";

    private boolean mHasSelected = false;;

    private Session mSession;

    private IMnsCodeCopeWays nowUtil;

    private MnsCodeCopeWaysHasListener mHasListenerUtil;

    private MnsCodeCopeWaysNoListener mNoListenerUtil;

    private MnsCodeCopeWaysWithPush mPushUtil;

    public RecvDataProcessUtil(Session session) {
        this.mSession = session;
    }

    public RecvDataProcessUtil selectHandleUtil(PacketData recvData, Request request) {
        if (request != null) {
            if (request.hasListenter()) {
                if (mHasListenerUtil == null) {
                    mHasListenerUtil = new MnsCodeCopeWaysHasListener(mSession);
                }
                nowUtil = mHasListenerUtil;
            } else {
                if (mNoListenerUtil == null) {
                    mNoListenerUtil = new MnsCodeCopeWaysNoListener(mSession);
                }
                nowUtil = mNoListenerUtil;
            }
        } else {
            if (mPushUtil == null) {
                mPushUtil = new MnsCodeCopeWaysWithPush(mSession);
            }
            nowUtil = mPushUtil;
        }
        nowUtil.setParam(recvData, request);
        mHasSelected = true;
        return this;
    }

    public boolean handle() {
        Log.w("wangchuntao","handle ");
        if (!mHasSelected) {
            MiLinkLog.e(TAG, "has not select data process util");
            return false;
        } else {
            nowUtil.handleMnsCode();
            mHasSelected = false;
            return true;
        }
    }
}

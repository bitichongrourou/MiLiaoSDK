
package com.mi.milink.sdk.session.persistent;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.os.Message;
import android.util.Log;

import com.mi.milink.sdk.aidl.PacketData;
import com.mi.milink.sdk.base.CustomHandlerThread;
import com.mi.milink.sdk.base.Global;
import com.mi.milink.sdk.client.IPacketListener;
import com.mi.milink.sdk.client.ipc.MiLinkClientIpc;
import com.mi.milink.sdk.debug.MiLinkLog;
import com.mi.milink.sdk.event.MiLinkEvent;

import org.greenrobot.eventbus.EventBus;

/**
 * 数据包的分发器
 * 
 * @author MK
 */
public class MnsPacketDispatcher extends CustomHandlerThread {
    private static final String TAG = "MnsPacketDispatcher";

    private static final int MAX_BATCH_COUNT = 10;

    private static final int MAX_DISPATCH_PACKET_DELAY_TIME = 500;

    private static final int MSG_ADD_PACKET = 1;

    private static final int MSG_DISPATCH_PACKET = 2;

    private static MnsPacketDispatcher sInstance = new MnsPacketDispatcher();

    private IPacketListener mListener;

    private final List<PacketData> mPacketCache = new ArrayList<PacketData>(32); // 初始容量使用32

    private volatile long mDispatchPacketDelayTime;

    private MnsPacketDispatcher() {
        super(TAG);
        mDispatchPacketDelayTime = 0;
        MiLinkLog.v(TAG, "MnsPacketDispatcher created, threadId=" + Thread.currentThread().getId());
    }

    public static MnsPacketDispatcher getInstance() {
        return sInstance;
    }

    private long mDispatchPacketDelayTimeWhenScreenOn = 0;

    /**
     * 设置是否需要延迟交付
     * 
     * @param delayMillis 单位是毫秒, 最大值是500，超过500会被设置成500
     */
    public void setDispatchPacketDelayTime(long delayMillis) {
        if (delayMillis > MAX_DISPATCH_PACKET_DELAY_TIME) {
            this.mDispatchPacketDelayTimeWhenScreenOn = MAX_DISPATCH_PACKET_DELAY_TIME;
        } else {
            this.mDispatchPacketDelayTimeWhenScreenOn = delayMillis;
        }
        mDispatchPacketDelayTime = mDispatchPacketDelayTimeWhenScreenOn;
    }

    public void setCallback(IPacketListener callback) {
        Log.w("wangchuntao","setCallback:"+(callback == null));
        if (callback != null) {
            MiLinkLog.v(TAG, "register packet callback. callback=" + callback);
            mListener = callback;
            removeMessage(MSG_DISPATCH_PACKET);
            mHandler.sendEmptyMessage(MSG_DISPATCH_PACKET);
        } else {
            MiLinkLog.v(TAG, "register packet callback, but callback is null");
        }
    }

    public void dispatchPacket(PacketData data) {
        Log.w("wangchuntao","dispatchPacket");
        if (data != null) {
            MiLinkLog.v(TAG, "dispatch packet data, seq=" + data.getSeqNo());
            Message msg = obtainMessage();
            if (mDispatchPacketDelayTime > 0) {
                msg.what = MSG_ADD_PACKET;
                msg.obj = data;
            } else {
                msg.what = MSG_DISPATCH_PACKET;
                msg.obj = data;
            }
            sendMessage(msg);
        } else {
            MiLinkLog.v(TAG, "dispatch packet data, but data is null");
        }
    }

    private void execDispatch() {
        Log.w("wangchuntao","execDispatch CacheSize:"+mPacketCache.size());
        MiLinkLog.v(TAG, "DISPATCH_PACKET, mPacketCache.size=" + mPacketCache.size());
        if (!mPacketCache.isEmpty()) {
            Log.w("wangchuntao","mPacketCache 不为空");
            ArrayList<PacketData> list = new ArrayList<PacketData>();
            list.addAll(mPacketCache);
            mPacketCache.clear();// 清空cache
            Log.w("wangchuntao","mListener :"+(mListener == null));
            if (mListener != null) {
                mListener.onReceive(list);
            }else {
                Intent intent = new Intent(MiLinkClientIpc.ACTION_NO_LISTENER);
                Global.getApplicationContext().sendBroadcast(intent);
                EventBus.getDefault().post(new MiLinkEvent.ServerNotificationEvent(MiLinkEvent.ServerNotificationEvent.EventType.ListenerShouldUpdate));
            }
        }
    }

    @Override
    protected void processMessage(Message msg) {
        Log.w("wangchuntao","processMessage what:"+msg.what);
        switch (msg.what) {
            case MSG_ADD_PACKET: {
                PacketData packet = (PacketData) msg.obj;
                if (packet != null) {
                    MiLinkLog.v(TAG, "ADD_PACKET, seq=" + packet.getSeqNo());
                    mPacketCache.add(packet);
                    removeMessage(MSG_DISPATCH_PACKET);
                    if (mDispatchPacketDelayTime > 0) {
                        if (mPacketCache.size() >= MAX_BATCH_COUNT) {
                            execDispatch();
                        } else {
                            mHandler.sendEmptyMessageDelayed(MSG_DISPATCH_PACKET,
                                    mDispatchPacketDelayTime);
                        }
                    } else {
                        execDispatch();
                    }
                }
            }
                break;
            case MSG_DISPATCH_PACKET: {
                if (msg.obj != null) {
                    Log.w("wangchuntao","msg.obj 不为null");
                    PacketData packet = (PacketData) msg.obj;
                    mPacketCache.add(packet);
                }
                execDispatch();
            }
                break;
            default:
                MiLinkLog.e(TAG, "handleMessage unknown msgid = " + msg.what);
                break;
        }
    }

    /**
     * 亮屏时的发包时延
     */
    public void setDispatchPacketDelayTimeWhenScreenOn() {
        mDispatchPacketDelayTime = mDispatchPacketDelayTimeWhenScreenOn;
    }

    public void setDispatchPacketDelayTimeWhenScreenOff() {
        mDispatchPacketDelayTime = 0;
    }

}

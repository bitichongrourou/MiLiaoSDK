
package com.mi.milink.sdk.mipush;

import android.content.Context;

import com.xiaomi.mipush.sdk.MiPushMessage;

public interface MiPushMessageListener {
    /**
     * 透传消息的到达时触发
     * @param context
     * @param message
     */
    public void onReceivePassThroughMessage(Context context, MiPushMessage message);

    /**
     * 点击通知栏消息时触发
     * @param context
     * @param message
     */
    public void onNotificationMessageClicked(Context context, MiPushMessage message);

    /**
     * 当通知栏有消息到达时触发
     * @param context
     * @param message
     */
    public void onNotificationMessageArrived(Context context, MiPushMessage message);

}

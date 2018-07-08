
package com.mi.milink.sdk.client;

import java.util.Observable;
import java.util.Observer;

import android.os.Message;

import com.mi.milink.sdk.client.ipc.ClientLog;
import com.mi.milink.sdk.client.ipc.internal.MiLinkServiceHost;
import com.mi.milink.sdk.data.Const;

/**
 * * MiLink服务的消息监听器<br>
 * <br>
 * MiLink服务的消息通过提交给服务的消息接收线程通知到每一个{@code MiLinkObserver}
 * ，<b>这些事件将严重影响客户端的逻辑走向，因此约定客户端必须至少持有并注册一个这样的对象</b><br>
 * <br>
 * 若要监听这些事件，实现一个/多个消息监听器，并在{@link MiLinkServiceHost}注册它们。 <br>
 * <br>
 * p.s. 使用Java的Observable/Observer机制实现
 *
 * @author MK
 * @see MiLinkServiceHost
 */
public abstract class MiLinkObserver implements Observer, Const.Event {

    /**
     * 事件：MNS服务请求重启 <br>
     * <br>
     * <i> "Hello, Client. I wanna play a game. In the past 12 hours, you get
     * tickets and transfer data via the Mns Service. You never care the state
     * of the Service, and assume Service's guilt ... Now you've got a occasion
     * to reset all these. The Process Id of the Service is provided as
     * parameter. You can kill her process by
     * {@code android.os.Process.killProcess()} or give her a chance to proceed
     * working —— <b>Live or Die, make your choice." <b></i>
     *
     * @param servicePid MNS服务的进程ID
     */
    public abstract void onSuicideTime(int servicePid);

    /**
     * 事件：全局错误，主要是服务器3000错误 <br>
     * <br>
     * <i><b>“来人呐，快来人呐，机房着火啦……”</i><b>
     *
     * @param errCode 错误码
     * @param errMsg 错误信息
     */
    public abstract void onInternalError(int errCode, String errMsg);

    /**
     * 事件：服务初始化成功 <br>
     * <br>
     * <i><b>“当初是你要分开，分开就分开，如今又要用通知，把我加回来，回调不是你想来，想来就能来，让……”</i><b><br>
     * <br>
     * <i><b>“其实你仔细想想，这个时间点我何必给你呢 ... ”</i><b> 要首先看到这个事件才能接收到其他的事件
     *
     * @param timePoint 初始化完成时间点，单位ms
     * @see System#currentTimeMillis()
     */
    public abstract void onServiceConnected(long timePoint);

    /**
     * 服务器连接状态更新
     *
     * @param state 服务器连接当前状态
     * @param newState
     */
    public abstract void onServerStateUpdate(int oldState, int newState);

    /**
     * 登录状态更新
     *
     * @param state 登录当前状态
     */
    public abstract void onLoginStateUpdate(int state);

    /**
     * 服务器事件派发
     *
     * @hide
     */
    @SuppressWarnings("unchecked")
    @Override
    public void update(Observable observable, Object data) {
    	ClientLog.v("MiLinkObserver", "update data:"+data);
        if (data == null || !(data instanceof Message)) {
            return;
        }

        Message event = (Message) data;

        // 设置类加载器，防止Bundle不认识其写入的类
        if (event.peekData() != null) {
            event.peekData().setClassLoader(getClass().getClassLoader());
        }

        switch (event.what) {
        // 通知：服务器链接状态更新
            case SERVER_STATE_UPDATED: {
                // 纯粹地让逻辑看上去很清晰
                int oldState = event.arg1;
                int newState = event.arg2;
                onServerStateUpdate(oldState, newState);
            }
                break;
            // 通知：服务连接
            case SERVICE_CONNECTED: {
                // 纯粹地让逻辑看上去很清晰
                Long timePoint = (event.peekData() == null ? 0L : event.peekData().getLong(
                        Const.Event.Extra));
                onServiceConnected(timePoint);
            }
                break;
            // 通知：MNS服务请求自杀
            case SUICIDE_TIME: {
                // 纯粹地让逻辑看上去很清晰
                int servicePid = event.arg1;
                onSuicideTime(servicePid);
            }
                break;
            // 通知：全局错误
            case MNS_INTERNAL_ERROR: {
                // 纯粹地让逻辑看上去很清晰
                int errCode = event.arg1;
                String errMsg = (event.peekData() == null ? null : event.peekData().getString(
                        Const.Event.Extra));
                onInternalError(errCode, errMsg);
            }
                break;
            case MI_LINK_LOGIN_STATE_CHANGED: {
                int state = event.arg2;
                onLoginStateUpdate(state);
            }
        }

    }

}

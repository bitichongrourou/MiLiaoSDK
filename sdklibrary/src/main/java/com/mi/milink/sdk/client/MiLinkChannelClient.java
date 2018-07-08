
package com.mi.milink.sdk.client;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.mi.milink.sdk.account.ChannelAccount;
import com.mi.milink.sdk.account.manager.MiChannelAccountManager;
import com.mi.milink.sdk.aidl.PacketData;
import com.mi.milink.sdk.base.Global;
import com.mi.milink.sdk.base.MessageTask;
import com.mi.milink.sdk.client.ipc.ClientLog;
import com.mi.milink.sdk.data.ClientAppInfo;
import com.mi.milink.sdk.debug.MiLinkLog;
import com.mi.milink.sdk.event.MiLinkEventForSimpleChannel.ClientActionEvent;
import com.mi.milink.sdk.event.MiLinkEventForSimpleChannel.SessionManagerNotificationEvent;
import com.mi.milink.sdk.event.MiLinkEventForSimpleChannel.SessionManagerStateChangeEvent;
import com.mi.milink.sdk.mipush.MiPushManager;
import com.mi.milink.sdk.mipush.MiPushManager.MiPushRegisterListener;
import com.mi.milink.sdk.session.common.ResponseListener;
import com.mi.milink.sdk.session.persistent.MnsPacketDispatcher;
import com.mi.milink.sdk.session.persistent.UploadLogManager;
import com.mi.milink.sdk.session.simplechannel.SessionManagerForSimpleChannel;

import android.text.TextUtils;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public class MiLinkChannelClient {

	private final String TAG = "MiLinkChannelClient";

	private MiLinkObserver mMiLinkObserver;

	private IEventListener mEventCallback;

	boolean isInit = false;

	private SessionManagerForSimpleChannel sessionManager;

	private MiChannelAccountManager accountManager;

	private EventBus channelEventBus;

	public MiLinkChannelClient() {
		channelEventBus = new EventBus();
		init();
	}

	private void init() {
		// Thread.setDefaultUncaughtExceptionHandler(new
		// MiLinkExceptionHandler());
		MiLinkLog.w(TAG, "MiLinkChannelClient no ipc build,host version="+Global.getClientAppInfo().getVersionCode());
	}

	public void setMilinkStateObserver(MiLinkObserver l) {
		this.mMiLinkObserver = l;
	}

	public void setPacketListener(IPacketListener l) {
		Log.w("wangchuntao","MiLinkChannelClient setPacketListener:"+(l == null));
		MnsPacketDispatcher.getInstance().setCallback(l);
	}

	public void setEventListener(IEventListener l) {
		this.mEventCallback = l;
	}

	public void setDispatchPacketDelayMillis(int delayTime) {
		MnsPacketDispatcher.getInstance().setDispatchPacketDelayTime(delayTime);
	}

	public synchronized boolean uploadMilinkLog(boolean force) {
		try {
			if (accountManager == null) {
				MiLinkLog.d(TAG, "uploadMilinkLog accountManager==null");
				accountManager = new MiChannelAccountManager(channelEventBus);
			}
			return UploadLogManager.uploadMilinkLog(null, accountManager.getCurrentAccount(), force);
		} catch (Exception e) {
		}
		return false;
	}

	/**
	 * 通道登录
	 */
	public synchronized void initUseChannelMode() {
		if (!channelEventBus.isRegistered(this)) {
			channelEventBus.register(this);
		}
		if (accountManager == null) {
			MiLinkLog.d(TAG, "initUseChannelMode accountManager==null");
			accountManager = new MiChannelAccountManager(channelEventBus);
		}
		if (sessionManager == null) {
			MiLinkLog.d(TAG, "initUseChannelMode sessionManager==null");
			sessionManager = new SessionManagerForSimpleChannel(channelEventBus, accountManager);
			if (!channelEventBus.isRegistered(sessionManager)) {
				channelEventBus.register(sessionManager);
			}
		}
		sessionManager.initApp();
		accountManager.initUserChannelMode();
		isInit = true;
		if (ClientAppInfo.isSupportMiPush()) {
			MiPushManager.getInstance().registerMiPush(null, new MiPushRegisterListener() {

				@Override
				public void onSetMiPushRegId(String regId) {
					// MiAccountManager.getInstance().setMipushRegId(regId);
				}
			});
		}
	}

	/**
	 * 
	 * @return 通道模式下的id
	 */
	public static long getAnonymousAccountId() {
		try {
			return Long.parseLong(ChannelAccount.getInstance().getUserId());
		} catch (Exception e) {
			return 0;
		}
	}

	public synchronized void setKeepAliveTime(int keepAliveTime) {
		if (accountManager != null)
			accountManager.setKeepAliveTime(keepAliveTime);
	}

	/**
	 * 异步发包
	 * 
	 * @param data
	 *            发送的数据
	 * @param timeout
	 *            超时时间
	 * @param l
	 *            发包成功或者失败的回调监听，如果设置为null，包会从IPacketListener的onReceive中回来。
	 */
	public synchronized void sendAsyncWithResponse(PacketData data, int timeout, final ResponseListener l) {
		if (isInit == false)
			initUseChannelMode();
		if (sessionManager != null)
			sessionManager.sendData(data, timeout, l);
	}

	public synchronized void sendAsync(PacketData data, int timeout) {
		if (isInit == false)
			initUseChannelMode();
		if (sessionManager != null)
			sessionManager.sendData(data, timeout, null);
	}

	public synchronized void sendAsync(PacketData data) {
		if (isInit == false)
			initUseChannelMode();
		if (sessionManager != null)
			sessionManager.sendData(data, 0, null);
	}

	/**
	 * 登出，会断开连接，并清空token等信息
	 */
	public synchronized void logoff() {
		MiLinkLog.i(TAG, "logoff");
		if (accountManager != null) {
			accountManager.userLogoff();
		}

		if (channelEventBus.isRegistered(this)) {
			channelEventBus.unregister(this);
		}
		if (channelEventBus.isRegistered(sessionManager)) {
			channelEventBus.unregister(sessionManager);
		}
		accountManager = null;
		sessionManager = null;
	}

	/**
	 * 强制重连，会使milinksdk断开连接再重新连接
	 */
	public void forceReconnect() {
		MiLinkLog.i(TAG, "forceReconnet");
		channelEventBus.post(new ClientActionEvent(ClientActionEvent.EventType.ClientForceOpen));
	}

	public void setMiLinkLogLevel(int level) {
		MiLinkLog.setLogcatTraceLevel(level);
		MiLinkLog.setFileTraceLevel(level);

		ClientLog.setLogcatTraceLevel(level);
		ClientLog.setFileTraceLevel(level);

	}

	/**
	 * 得到连接状态，值在 Const.SessionState 中，0、1、2分别代表未连接，正在连接，已连接
	 * 
	 * @return 连接状态的int值
	 */
	public int getMiLinkConnectState() {
		return sessionManager.getSessionState();
	}

	/**
	 * @return milink若已经登录，返回true，否则返回false；
	 */
	public synchronized boolean isMiLinkLogined() {
		try {
			if (sessionManager != null) {
				MiLinkLog.i(TAG, "isMiLinkLogined:" + sessionManager.isMilinkLogined());
				return sessionManager.isMilinkLogined();
			}
			MiLinkLog.i(TAG, "false isMiLinkLogined");
		} catch (Exception e) {
		}
		return false;
	}

	public PacketData sendDataBySimpleChannel(final PacketData packet, final int timeout) {

		if (isInit == false)
			initUseChannelMode();

		if (packet == null) {
			throw new IllegalArgumentException(" packet is null");
		}
		if (TextUtils.isEmpty(packet.getCommand())) {
			throw new IllegalArgumentException("Packet's command is null");
		}
		MessageTask result = new MessageTask() {

			@Override
			public void doSendWork() {
				sendAsyncWithResponse(packet, timeout, new ResponseListener() {
					@Override
					public void onDataSendSuccess(int errCode, PacketData data) {
						if (!isCancelled() && !isDone()) {
							set(data);
						}
					}

					@Override
					public void onDataSendFailed(int errCode, String errMsg) {
						if (!isCancelled() && !isDone()) {
							setException(new MiLinkException(errCode, errMsg));
						}
					}

				});
			}
		}.start();
		try {
			// client也加一个超时保护，防止service没有回调
			return result.getChannelResult(timeout + 5000, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			MiLinkLog.e(TAG, "task InterruptedException", e);
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause != null && (cause instanceof MiLinkException)) {
				// 如果能确定milink的具体业务，打印出来。防止什么错误抛出TimeoutException。
				MiLinkLog.e(TAG, "", cause);
			} else {
				MiLinkLog.e(TAG, "task ExecutionException", e);
			}
		} catch (CancellationException e) {
			MiLinkLog.e(TAG, "task CancellationException", e);
		} catch (TimeoutException e) {
			MiLinkLog.e(TAG, "task TimeoutException, detailName=" + e.getClass().getName());
		}
		return null;
	}

	/** 回调 **/
	protected void onSessionStateChanged(int oldState, int newState) {
		if (mMiLinkObserver != null) {
			mMiLinkObserver.onServerStateUpdate(oldState, newState);
		}
	}

	protected void onLoginStateChanged(int newState) {
		if (mMiLinkObserver != null) {
			mMiLinkObserver.onLoginStateUpdate(newState);
		}
	}

	protected void onEventGetServiceToken() {
		if (mEventCallback != null) {
			mEventCallback.onEventGetServiceToken();
		}
	}

	protected void onEventInvilidPacket() {
		if (mEventCallback != null) {
			mEventCallback.onEventInvalidPacket();
		}
	}

	@Subscribe
	public void onEvent(SessionManagerStateChangeEvent event) {
		switch (event.mEventType) {
		case LoginStateChange: {
			onLoginStateChanged(event.mNewState);
		}
			break;
		case SessionStateChange: {
			onSessionStateChanged(event.mOldState, event.mNewState);
		}
			break;
		default:
			break;
		}
	}

	@Subscribe
	public void onEvent(SessionManagerNotificationEvent event) {
		switch (event.mEventType) {
		case GetServiceToken: {
			onEventGetServiceToken();
		}
			break;
		case RecvInvalidPacket:
			onEventInvilidPacket();
		default:
			break;
		}
	}
}

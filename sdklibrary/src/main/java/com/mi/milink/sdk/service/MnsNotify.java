package com.mi.milink.sdk.service;

import java.io.Serializable;

import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.mi.milink.sdk.data.Const;

/**
 * MNS客户端通知接口
 * 
 * @author MK
 * 
 */
public class MnsNotify {

	private static Messenger CLIENT_MESSENGER = null;

	public static void setMessenger(Messenger messenger) {
		CLIENT_MESSENGER = messenger;
	}

	public static Messenger getMessenger() {
		return CLIENT_MESSENGER;
	}

	/**
	 * 向客户端发送事件
	 * 
	 * @param event
	 *            事件编号
	 * @return 发送成功/失败
	 */
	public final static boolean sendEvent(int event) {
		return sendEvent(event, 0, null, null);
	}

	/**
	 * 向客户端发送事件
	 * 
	 * @param event
	 *            事件编号
	 * @param arg1
	 *            整数参数
	 * @return 发送成功/失败
	 */
	public final static boolean sendEvent(int event, int arg1) {
		return sendEvent(event, arg1, null, null);
	}

	public final static boolean sendEvent(int event, int arg1, Object object) {
		return sendEvent(event, arg1, object, null);
	}

	/**
	 * 向客户端发送事件
	 * 
	 * @param event
	 *            事件编号
	 * @param arg1
	 *            整数参数
	 * @param object
	 *            对象参数
	 * @return 发送成功/失败
	 */
	public static boolean sendEvent(int event, int arg1, Object object,
			String extra) {
		Messenger messenger = getMessenger();

		if (messenger != null) {
			Message message = Message.obtain();
			message.what = event;
			message.arg1 = arg1;

			if (object != null) {
				if (object instanceof String) {
					message.getData().putString(Const.Event.Extra,
							object.toString());
				} else if (object instanceof Integer) {
					message.arg2 = (Integer) object;
				} else if (object instanceof Long) {
					message.getData().putLong(Const.Event.Extra, (Long) object);
				} else if (object instanceof Serializable) {
					message.getData().putSerializable(Const.Event.Extra,
							(Serializable) object);
				}
			}

			if (extra != null) {
				message.getData().putString(Const.Event.Extra2, extra);
			}

			try {
				messenger.send(message);

				return true;
			} catch (RemoteException e) {
				return false;
			}
		}

		return false;
	}

}

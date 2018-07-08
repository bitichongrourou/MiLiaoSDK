package com.mi.milink.sdk.mipush.receiver;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Message;
import android.text.TextUtils;

import com.mi.milink.sdk.client.ipc.ClientLog;
import com.mi.milink.sdk.mipush.MiPushManager;
import com.xiaomi.mipush.sdk.ErrorCode;
import com.xiaomi.mipush.sdk.MiPushClient;
import com.xiaomi.mipush.sdk.MiPushCommandMessage;
import com.xiaomi.mipush.sdk.MiPushMessage;
import com.xiaomi.mipush.sdk.PushMessageReceiver;

/**
 * 1、PushMessageReceiver是个抽象类，该类继承了BroadcastReceiver。
 * 2、需要将自定义的DemoMessageReceiver注册在AndroidManifest.xml文件中
 * <receiver android:exported="true" android:name=
 * "com.xiaomi.mipushdemo.DemoMessageReceiver"> <intent-filter>
 * <action android:name="com.xiaomi.mipush.RECEIVE_MESSAGE" /> </intent-filter>
 * <intent-filter> <action android:name="com.xiaomi.mipush.ERROR" />
 * </intent-filter> <intent-filter>
 * <action android:name="com.xiaomi.mipush.MESSAGE_ARRIVED" /></intent-filter>
 * </receiver>
 * 3、DemoMessageReceiver的onReceivePassThroughMessage方法用来接收服务器向客户端发送的透传消息
 * 4、DemoMessageReceiver的onNotificationMessageClicked方法用来接收服务器向客户端发送的通知消息，
 * 这个回调方法会在用户手动点击通知后触发
 * 5、DemoMessageReceiver的onNotificationMessageArrived方法用来接收服务器向客户端发送的通知消息，
 * 这个回调方法是在通知消息到达客户端时触发。另外应用在前台时不弹出通知的通知消息到达客户端也会触发这个回调函数
 * 6、DemoMessageReceiver的onCommandResult方法用来接收客户端向服务器发送命令后的响应结果
 * 7、DemoMessageReceiver的onReceiveRegisterResult方法用来接收客户端向服务器发送注册命令后的响应结果
 * 8、以上这些方法运行在非UI线程中
 * 
 * @author mayixiang
 */
public class MiPushMessageReceiver extends PushMessageReceiver {

	private static String TAG = "MiPushMessageReceiver";
	private String mRegId;
	private long mResultCode = -1;
	private String mReason;
	private String mCommand;
	private String mMessage;
	private String mTopic;
	private String mAlias;
	private String mAccount;
	private String mStartTime;
	private String mEndTime;

	@Override
	public void onReceivePassThroughMessage(Context context, MiPushMessage message) {
		ClientLog.w(TAG, "onReceivePassThroughMessage is called. " + message.toString());

		if (!TextUtils.isEmpty(message.getTopic())) {
			mTopic = message.getTopic();
		} else if (!TextUtils.isEmpty(message.getAlias())) {
			mAlias = message.getAlias();
		}

		MiPushManager.getInstance().onReceivePassThroughMessage(context, message);
	}

	@Override
	public void onNotificationMessageClicked(Context context, MiPushMessage message) {
		ClientLog.w(TAG, "onNotificationMessageClicked is called. " + message.toString());

		if (!TextUtils.isEmpty(message.getTopic())) {
			mTopic = message.getTopic();
		} else if (!TextUtils.isEmpty(message.getAlias())) {
			mAlias = message.getAlias();
		}

		MiPushManager.getInstance().onNotificationMessageClicked(context, message);
	}

	@Override
	public void onNotificationMessageArrived(Context context, MiPushMessage message) {
		ClientLog.w(TAG, "onNotificationMessageArrived is called. " + message.toString());

		if (!TextUtils.isEmpty(message.getTopic())) {
			mTopic = message.getTopic();
		} else if (!TextUtils.isEmpty(message.getAlias())) {
			mAlias = message.getAlias();
		}

		MiPushManager.getInstance().onNotificationMessageArrived(context, message);
	}

	@Override
	public void onCommandResult(Context context, MiPushCommandMessage message) {
		ClientLog.w(TAG, "onCommandResult is called. " + message.toString());
		String command = message.getCommand();
		List<String> arguments = message.getCommandArguments();
		String cmdArg1 = ((arguments != null && arguments.size() > 0) ? arguments.get(0) : null);
		String cmdArg2 = ((arguments != null && arguments.size() > 1) ? arguments.get(1) : null);
		String log = "";
		if (MiPushClient.COMMAND_REGISTER.equals(command)) {
			if (message.getResultCode() == ErrorCode.SUCCESS) {
				MiPushManager.getInstance().setMiPushRegId(cmdArg1);
				MiPushManager.getInstance().bindAliasByUserId();
				MiPushManager.getInstance().setRegisting(false);
			} else {
				ClientLog.w(TAG, "COMMAND_REGISTER failed");
				MiPushManager.getInstance().registerMiPush(false);
			}
		} else if (MiPushClient.COMMAND_SET_ALIAS.equals(command)) {
			if (message.getResultCode() == ErrorCode.SUCCESS) {
				ClientLog.w(TAG, "setAlias success!");
				MiPushManager.getInstance().setAlias(cmdArg1);
			} else {
				ClientLog.w(TAG, "COMMAND_SET_ALIAS failed");
				// 设置失败了
				MiPushManager.getInstance().registerMiPush(true);
			}
		} else if (MiPushClient.COMMAND_UNSET_ALIAS.equals(command)) {
			if (message.getResultCode() == ErrorCode.SUCCESS) {
				mAlias = cmdArg1;
				ClientLog.w(TAG, "unsetAlias success!");
				MiPushManager.getInstance().clearAlias();
			} else {
				ClientLog.w(TAG, "unsetAlias failed!");
				MiPushManager.getInstance().logoff();
//				MiPushManager.getInstance().setAlias(cmdArg1);
			}
		} else if (MiPushClient.COMMAND_SET_ACCOUNT.equals(command)) {
			if (message.getResultCode() == ErrorCode.SUCCESS) {
				mAccount = cmdArg1;
			} else {
			}
		} else if (MiPushClient.COMMAND_UNSET_ACCOUNT.equals(command)) {
			if (message.getResultCode() == ErrorCode.SUCCESS) {
				mAccount = cmdArg1;
			} else {
			}
		} else if (MiPushClient.COMMAND_SUBSCRIBE_TOPIC.equals(command)) {
			if (message.getResultCode() == ErrorCode.SUCCESS) {
				mTopic = cmdArg1;
			} else {
			}
		} else if (MiPushClient.COMMAND_UNSUBSCRIBE_TOPIC.equals(command)) {
			if (message.getResultCode() == ErrorCode.SUCCESS) {
			} else {
			}
		} else if (MiPushClient.COMMAND_SET_ACCEPT_TIME.equals(command)) {
			if (message.getResultCode() == ErrorCode.SUCCESS) {
				mStartTime = cmdArg1;
				mEndTime = cmdArg2;
			} else {
			}
		} else {
			log = message.getReason();
		}

		Message msg = Message.obtain();
	}

	@Override
	public void onReceiveRegisterResult(Context context, MiPushCommandMessage message) {
		ClientLog.w(TAG, "onReceiveRegisterResult is called. " + message.toString());
		String command = message.getCommand();
		List<String> arguments = message.getCommandArguments();
		String cmdArg1 = ((arguments != null && arguments.size() > 0) ? arguments.get(0) : null);
		String log;
		if (MiPushClient.COMMAND_REGISTER.equals(command)) {
			if (message.getResultCode() == ErrorCode.SUCCESS) {
				mRegId = cmdArg1;
			} else {
			}
		} else {
			log = message.getReason();
		}

		Message msg = Message.obtain();
	}

	@SuppressLint("SimpleDateFormat")
	public static String getSimpleDate() {
		return new SimpleDateFormat("MM-dd hh:mm:ss").format(new Date());
	}

}

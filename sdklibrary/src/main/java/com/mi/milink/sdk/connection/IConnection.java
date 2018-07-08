package com.mi.milink.sdk.connection;

import com.mi.milink.sdk.session.common.MsgProcessor;

/**
 * 连接接口，定义了连接对象共同的行为
 * @author MK
 *
 */
public interface IConnection {
	// 启动线程
	public boolean start();

	// 关闭线程
	public boolean stop();

	// 唤起线程
	public void wakeUp();

	// 发送线程消息
	public boolean postMessage(int uMsg, Object lParam, int wParam,
			MsgProcessor msgProc);

	public boolean isRunning();

	// 连接
	public boolean connect(final String serverIP, final int serverPort,
			final String proxyIP, final int proxyPort, final int timeOut,
			final int mss);

	// 断开
	public boolean disconnect();

	// 发送数据
	public boolean sendData(byte[] buf, int cookie, int sendTimeout);

	// 设置回调接口
	public void setCallback(IConnectionCallback callback);

	// 删除发送数据
	public void removeSendData(int cookie);

	// 删除所有发送数据
	public void removeAllSendData();

	// 是否已发送完成
	public boolean isSendDone(int cookie);

	// 获取服务器IP
	public String getServerIP();

	// 获取服务器port
	public int getServerPort();

	// 获取连接类型
	public int getConnectionType();
}

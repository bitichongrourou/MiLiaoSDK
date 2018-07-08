package com.mi.milink.sdk.connection;

/**
 * 连接过程的回调接口，通过实现此回调接口可以监控连接不同的过程
 *
 * @author MK
 *
 */
public interface IConnectionCallback {
	/**
	 * 连接线程启动
	 *
	 * @return 成功处理返回true;否则返回false
	 */
	public boolean onStart();

	/**
	 * 连接结果的通知
	 *
	 * @param isSuccess
	 *            连接成功为true，连接失败为false
	 * @param errorCode
	 *            连接的错误码,包括errno
	 * @see Error
	 * @return 成功处理返回true;否则返回false
	 */
	public boolean onConnect(boolean isSuccess, int errorCode);

	/**
	 * 断开连接的通知
	 *
	 * @return 成功处理返回true;否则返回false
	 */
	public boolean onDisconnect();

	/**
	 * 连接出错的通知
	 *
	 * @param socketStatus
	 *            错误码，包括errno
	 * @return 成功处理返回true;否则返回false
	 */
	public boolean onError(int socketStatus);

	/**
	 * 请求超时的通知
	 *
	 * @param dwSeqNo
	 *            超时请求的序列号
	 * @param nReason
	 *            超时的原因
	 *            <ul>
	 *            <li>{@link Error#WRITE_TIME_OUT}
	 *            <li>{@link Error#READ_TIME_OUT}
	 *            </ul>
	 * @return 成功处理返回true;否则返回false
	 */
	public boolean onTimeOut(int dwSeqNo, int nReason);

	/**
	 * 接收数据的通知
	 *
	 * @param pcBuf
	 *            数据块
	 * @return 成功处理返回true;否则返回false
	 */
	public boolean onRecv(byte[] pcBuf);

	/**
	 * 开始发送的通知 --- 用于计时
	 *
	 * @param dwSeqNo
	 *            请求的序列号
	 * @return 成功处理返回true;否则返回false
	 */
	public boolean onSendBegin(int dwSeqNo);

	/**
	 * 结束发送的通知 --- 用于计时
	 *
	 * @param dwSeqNo
	 *            请求的序列号
	 * @return 成功处理返回true;否则返回false
	 */
	public boolean onSendEnd(int dwSeqNo);
}

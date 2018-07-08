package com.mi.milink.sdk.session.common;

/**
 * 消息处理函数的回调接口
 *
 * @author MK
 */
public interface MsgProcessor {
	/**
	 * 消息处理函数
	 *
	 * @param uMsg
	 *            消息ID
	 * @param lParam
	 *            对象类型参数
	 * @param wParam
	 *            基本类型参数
	 */
	public void onMsgProc(int uMsg, Object lParam, int wParam);
}

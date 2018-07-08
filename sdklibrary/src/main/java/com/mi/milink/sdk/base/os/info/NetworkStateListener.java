package com.mi.milink.sdk.base.os.info;

/**
 * 网络变化监听器
 *
 * @see NetworkDash.addListener()
 * @see NetworkDash.removeListener()
 * @author MK
 */
public interface NetworkStateListener {
	/**
	 * 当网络状态变化时，触发该事件 <br>
	 * <br>
	 * <b>该事件将在主线程中执行，不要执行耗时或阻塞操作，以免引发ANR</b>
	 *
	 * @param lastState
	 *            之前的网络状态
	 * @param newState
	 *            现在的网络状态
	 */
	public void onNetworkStateChanged(NetworkState lastState,
			NetworkState newState);
}


package com.mi.milink.sdk.session.common;

import com.mi.milink.sdk.account.IAccount;
import com.mi.milink.sdk.aidl.PacketData;
import com.mi.milink.sdk.config.ConfigManager;

public class Request {

	private PacketData data;

	private IAccount ownerAccount;

	private long sentTime = 0; // 发送时间

	private long createdTime; // 创建时间

	// 超时时间
	private int timeOut = ConfigManager.getInstance().getRequestTimeout();

	private boolean isPing = false;

	private boolean isInternal = false;

	private ResponseListener listener;

	private byte encodeType = StreamUtil.MNS_ENCODE_NONE;

	private int size = 0;

	private boolean hasCallback = false;// 是否已经回调了

	private boolean hasRetry = false;// 是否重试过

	public void setHasRetry() {
		hasRetry = true;
	}

	public void setInternal(boolean isInternal) {
		this.isInternal = isInternal;
	}

	/**
	 * 是否是系统的request，否则全部扔回给app
	 */

	private int retryCount = 0; // 重试次数

	private AfterHandleCallBack afterHandleCallBack = null;

	/**
	 * @param data
	 *            不要为null
	 * @param l
	 * @param isPing
	 * @param isInternal
	 * @param encodeType
	 * @param shouldCached
	 */
	public Request(PacketData data, ResponseListener l, byte encodeType, IAccount requestAccount) {
		this.data = data;
		this.listener = l;
		this.createdTime = System.currentTimeMillis();
		this.encodeType = encodeType;
		this.ownerAccount = requestAccount;
	}

	public AfterHandleCallBack getAfterHandleCallBack() {
		return afterHandleCallBack;
	}

	public void setAfterHandleCallBack(AfterHandleCallBack afterHandleCallBack) {
		this.afterHandleCallBack = afterHandleCallBack;
	}

	public boolean isInternalRequest() {
		return this.isInternal;
	}

	public void setPing(boolean isPing) {
		this.isPing = isPing;
	}

	public void setSentTime(long time) {
		this.sentTime = time;
	}

	public long getCreatedTime() {
		return createdTime;
	}

	public long getSentTime() {
		return sentTime;
	}

	public int getValidTime() {
		return data.getValidTime();
	}

	public int getSeqNo() {
		return data.getSeqNo();
	}

	public PacketData getData() {
		return data;
	}

	private int mHandleSessionNO = 0;

	public void setHandleSessionNO(int handleSessionNO) {
		this.mHandleSessionNO = handleSessionNO;
	}

	public byte[] toBytes() {
		byte[] result = StreamUtil.toUpBytes(String.format("[No:%d]", mHandleSessionNO), data, isPing, encodeType,
				ownerAccount);
		if (result != null) {
			size = result.length;
		}
		return result;
	}

	public boolean isPingRequest() {
		return isPing;
	}

	public int getSize() {
		return size;
	}

	public void setTimeOut(int timeout) {
		if (timeout > 0) {
			if (data.needRetry()) {
				this.timeOut = timeout / 2;
			}
		}
	}

	public int getTimeOut() {
		return timeOut;
	}

	public boolean isTimeout() {
		return System.currentTimeMillis() - sentTime > timeOut * ConfigManager.getInstance().getTimeoutMultiply();
	}

	public boolean requestShouldCached() {
		return data.needCached();
	}

	public int getRetryCount() {
		return retryCount;
	}

	public void addRetryCount() {
		retryCount++;
	}

	public boolean hasListenter() {
		if (listener == null) {
			return false;
		} else {
			return true;
		}
	}

	public void onDataSendSuccess(int errCode, PacketData recvData) {
		if (listener == null) {
			return;
		}
		if (!hasCallback) {
			this.listener.onDataSendSuccess(errCode, recvData);
			hasCallback = true;
		}
	}

	public boolean onDataSendFailed(int errCode, String errMsg) {
		if (listener == null) {
			return false;
		}
		if (!hasCallback) {
			this.listener.onDataSendFailed(errCode, errMsg);
			hasCallback = true;
		}
		return true;
	}

	public boolean isValidNow() {
		return System.currentTimeMillis() - createdTime < getValidTime();
	}

	public IAccount getOwnerAccount() {
		return ownerAccount;
	}

	public static interface AfterHandleCallBack {

		void onCallBack(String accIp, int accPort, String cmd, int retCode, long sentTime, long responseTime,
				int reqSize, int responseSize, int seqNo, String clientIp, String clientIsp);
	}

	public boolean canRetry() {
		// 内部包，允许重试且没有重试过才可以重试
		if (!isInternal && data.needRetry() && !hasRetry && !hasCallback) {
			return true;
		}
		return false;
	}

}

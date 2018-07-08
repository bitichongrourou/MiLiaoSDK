
package com.mi.milink.sdk.aidl;

import android.os.Parcel;
import android.os.Parcelable;

import com.mi.milink.sdk.data.Const;
import com.mi.milink.sdk.util.DataUtils;

/**
 * 用于client与service之间传递包的类
 *
 * @author MK
 */

public class PacketData implements Parcelable {

	private int mSeqNo;

	private int mResponseSize; // service在使用的，不写进parcel

	protected byte[] mData;

	protected String mCommand;

	protected int mMnsCode;

	protected int mBusiCode;

	protected String mMnsErrorMsg;

	protected boolean mIsPushPacket = false;// 下行包(收到的)关心这个字段

	/*
	 * false表示不需要Response. 默认为true一般都需要回包。上行包(发出的)关心这个字段
	 */
	protected boolean mNeedResponse = true;

	/*
	 * 表示当milink没有连接时，是否cache这个包。 默认为true表示需要cache ，即当milink连上之后，自动重发这个包
	 * 上行包(发出的)关心这个字段
	 */
	protected boolean mNeedCached = true;

	/*
	 * 表示包的有效时间，如果当前时间-包创建时间 > 包的有效时间，这个将不会被发送。
	 */
	protected int mValidTime = 60 * 1000;

	/*
	 * 表示这个包是否要带上clientinfo，上行包(发出的)关心这个字段 service在使用的，不写进parcel
	 */
	protected boolean mNeedClientInfo = true;

	/*
	 * 表示server端是否缓存了clientinfo,下行包(收到的)才关心这个字段 service在使用的，不写进parcel
	 */
	protected boolean mHasClientInfo = false;

	/*
	 * 表示是否需要发送失败时重试一次
	 */
	protected boolean mNeedRetry = true;

	public boolean needRetry() {
		return mNeedRetry;
	}

	public void setNeedNeedRetry(boolean needRetry) {
		this.mNeedRetry = needRetry;
	}

	public boolean needCached() {
		return mNeedCached;
	}

	/**
	 * 表示当milink没有连接时，是否cache这个包。默认值为true, 即当milink连上之后，自动重发这个包
	 * 
	 * @param needCached
	 */
	public void setNeedCached(boolean needCached) {
		this.mNeedCached = needCached;
	}

	/**
	 * @param validTime
	 *            包的有效时间，比如5s，如果表示包的有效时间。 如果当前时间-包创建时间 > 包的有效时间，这个包将会被丢弃。
	 */
	public void setValidTime(int validTime) {
		this.mValidTime = validTime;
	}

	public int getValidTime() {
		return mValidTime;
	}

	public boolean needResponse() {
		return mNeedResponse;
	}

	/**
	 * 设置这个数据包是否需要回复包，默认值为true, 即需要有回复包
	 * 
	 * @param needResponse
	 *            true表示需要Response; false不需要Response
	 */
	public void setNeedResponse(boolean needResponse) {
		this.mNeedResponse = needResponse;
	}

	public boolean needClientInfo() {
		return mNeedClientInfo;
	}

	public void setNeedClientInfo(boolean needClientInfo) {
		this.mNeedClientInfo = needClientInfo;
	}

	public boolean hasClientInfo() {
		return mHasClientInfo;
	}

	public void setHasClientInfo(boolean hasClientInfo) {
		this.mHasClientInfo = hasClientInfo;
	}

	public boolean isPushPacket() {
		return mIsPushPacket;
	}

	public void setIsPushPacket(boolean isPushPacket) {
		this.mIsPushPacket = isPushPacket;
	}

	public String getMnsErrorMsg() {
		return mMnsErrorMsg;
	}

	public void setMnsErrorMsg(String mnsErrorMsg) {
		this.mMnsErrorMsg = mnsErrorMsg;
	}

	public byte[] getData() {
		return mData;
	}

	public void setData(byte[] data) {
		this.mData = data;
	}

	public String getCommand() {
		return mCommand;
	}

	public void setCommand(String command) {
		this.mCommand = command;
	}

	public int getMnsCode() {
		return mMnsCode;
	}

	public void setMnsCode(int mnsCode) {
		this.mMnsCode = mnsCode;
	}

	public int getBusiCode() {
		return mBusiCode;
	}

	public void setBusiCode(int busiCode) {
		this.mBusiCode = busiCode;
	}

	public void setResponseSize(int size) {
		mResponseSize = size;
	}

	public int getResponseSize() {
		return mResponseSize;
	}

	private PacketData(Parcel source) {
		readFromParcel(source);
	}

	public void readFromParcel(Parcel source) {
		mData = DataUtils.readParcelBytes(source);
		mSeqNo = source.readInt();
		mCommand = source.readString();
		mMnsCode = source.readInt();
		mBusiCode = source.readInt();
		mMnsErrorMsg = source.readString();
		mIsPushPacket = source.readByte() == 1;
		mNeedResponse = source.readByte() == 1;
		mNeedCached = source.readByte() == 1;
		mValidTime = source.readInt();
	}

	public PacketData() {
	}

	public int getSeqNo() {
		return mSeqNo;
	}

	public void setSeqNo(int seqNo) {
		this.mSeqNo = seqNo;
	}

	public boolean isPingPacket() {
		return Const.MnsCmd.MNS_PING_CMD.equals(mCommand);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		DataUtils.writeParcelBytes(dest, mData);
		dest.writeInt(mSeqNo);
		dest.writeString(mCommand);
		dest.writeInt(mMnsCode);
		dest.writeInt(mBusiCode);
		dest.writeString(mMnsErrorMsg);
		dest.writeByte((byte) (mIsPushPacket ? 1 : 0));
		dest.writeByte((byte) (mNeedResponse ? 1 : 0));
		dest.writeByte((byte) (mNeedCached ? 1 : 0));
		dest.writeInt(mValidTime);
	}

	public static final Parcelable.Creator<PacketData> CREATOR = new Parcelable.Creator<PacketData>() {

		@Override
		public PacketData createFromParcel(Parcel source) {
			PacketData obj = new PacketData(source);
			return obj;
		}

		@Override
		public PacketData[] newArray(int size) {
			return new PacketData[size];
		}
	};
}

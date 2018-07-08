
package com.mi.milink.sdk.session.common;

import java.io.UnsupportedEncodingException;

import com.mi.milink.sdk.base.data.Convert;
import com.mi.milink.sdk.debug.MiLinkLog;
import com.mi.milink.sdk.event.MiLinkEvent.ChannelStatusChangeEvent;

import org.greenrobot.eventbus.EventBus;

/**
 * 数据解析类，负责从网络收到的数据的解析
 * 
 * @author MK
 */
public class ReceiveBuffer {

	private static String CLASSTAG = "ReceiveBuffer";

	private String TAG;

	public final static int SOCKET_RECV_BUFFER = 1 * 1024; // 内存优化，缩小到1k

	private static final int INTEGER_LENGTH = 4;

	private static final int MIN_TCP_PACKAGE_HEADER_LENGTH = 8;

	private static final int MAX_HTTP_PACKAGE_HEADER_LENGTH = 1024 * 2;

	private byte[] mBuffer = null;

	private int mPosition = 0;

	private ReceiveBufferSink mSink;

	private int mCreatorSessionNO;

	public ReceiveBuffer(ReceiveBufferSink bufferSink, int sessionNO, boolean isAssistSession) {
		this.mSink = bufferSink;
		try {
			mBuffer = new byte[SOCKET_RECV_BUFFER];
		} catch (OutOfMemoryError e) {
			MiLinkLog.e(TAG, "ReceiveBuffer init failed", e);
		}
		mCreatorSessionNO = sessionNO;
		TAG = String.format("[No:%d]%s", sessionNO, CLASSTAG);
		this.mIsAssistSession = isAssistSession;
	}

	private boolean mIsAssistSession = false;

	private int mChannelBusy = 0;

	// 读取一片数据后追加到buffer中，防止SOCKET读半个包的可能
	public void append(byte[] theBuffer) throws InvalidPacketExecption {
		if (null == mBuffer) {
			return;
		}

		int length = theBuffer.length;
		if (!mIsAssistSession) {
			// 是主通道才有资格判断通道是否忙
			if (length >= 5000) {
				// 请求进入通道忙状态
				if (++mChannelBusy > 10) {
					EventBus.getDefault()
							.post(new ChannelStatusChangeEvent(ChannelStatusChangeEvent.EventType.channelBusy, null));
					mChannelBusy = 0;
				}
			} else if (length < 1000) {
				if (--mChannelBusy < -5) {
					// 请求进入通道空闲状态
					EventBus.getDefault()
							.post(new ChannelStatusChangeEvent(ChannelStatusChangeEvent.EventType.channelIdle, null));
					mChannelBusy = 0;
				}
			}
		}
		MiLinkLog.v(TAG, "now mBuffer.len=" + mBuffer.length + ",pos=" + mPosition + ",recvLen=" + length);
		// 如果内存不足需要重新分配
		if ((mBuffer.length - mPosition) < length) {
			MiLinkLog.v(TAG, "buffer need to be increased");
			try {
				byte[] tempBuffer = new byte[mPosition + length];
				System.arraycopy(mBuffer, 0, tempBuffer, 0, mPosition);
				System.arraycopy(theBuffer, 0, tempBuffer, mPosition, length);
				mBuffer = tempBuffer;
				mPosition += length;
			} catch (OutOfMemoryError e) {
				MiLinkLog.e(TAG, "append new byte fail ", e);
			}
		} else {
			System.arraycopy(theBuffer, 0, mBuffer, mPosition, length);
			mPosition += length;
		}

		parsePacket();
	}

	// 将内存从oldPos位置移动到内存的起始位置
	private void removeToBegin(int oldPos) {
		if (null == mBuffer) {
			return;
		}
		int length = mPosition - oldPos;
		mPosition = 0;
		for (int i = 0; i < length; i++) {
			mBuffer[mPosition++] = mBuffer[oldPos + i];
		}
	}

	private void parsePacket() throws InvalidPacketExecption {
		MiLinkLog.v(TAG, "parsePacket start");
		while (true) {
			if (parseNormalPacket() == false) {
				break;
			}
		}
		int len = mBuffer.length;
		if (mPosition == 0 && len > SOCKET_RECV_BUFFER) {
			MiLinkLog.v(TAG, "reset buffer size: " + len);
			mBuffer = new byte[SOCKET_RECV_BUFFER]; // 调整缓存到初始大小,节约内存
		}
	}

	private long getPacketLen() throws InvalidPacketExecption {
		MiLinkLog.i(TAG, "getPacketLen start, mPosition=" + mPosition);
		if (mPosition < MIN_TCP_PACKAGE_HEADER_LENGTH) {
			if (mPosition != 0) {
				MiLinkLog.i(TAG, "getPacketLen [position = " + mPosition + "] < TCP_PACKAGE_HEADER_LENGTH("
						+ MIN_TCP_PACKAGE_HEADER_LENGTH + ")");
			}
			return -1;
		}

		// 判断是否有http头部
		if (BufferUtil.isHttpHead(mBuffer)) {
			MiLinkLog.i(TAG, "getPacketLen isHttpHead");
			int httpHeaderEndPos = BufferUtil.findHttpHeaderEndFromByte(mBuffer);
			if (httpHeaderEndPos <= 0) {
				// 没有找到\r\n\r\n结束符
				if (mPosition > MAX_HTTP_PACKAGE_HEADER_LENGTH) {
					// 超过MAX_HTTP_PACKAGE_HEADER_LENGTH，还没有找到\r\n\r\n，认为收到了错误包
					MiLinkLog.i(TAG,
							"HTTP CONTENT : " + Convert.bytesToASCIIString(mBuffer, MAX_HTTP_PACKAGE_HEADER_LENGTH));
					throw new InvalidPacketExecption("wrong packet，cannot find http header end",
							InvalidPacketExecption.ERROR_CODE_NO_HTTP_HEAD_END);
				}
				// 长度不够，继续收
				return -1;
			}

			// 去掉所有的http头部信息
			removeToBegin(httpHeaderEndPos - 1);
		}

		if (!BufferUtil.isMNSHead(mBuffer)) {
			// 不是mns头部，抛出异常报错
			MiLinkLog.i(TAG, "no mns head: length=" + mBuffer.length + "; "
					+ Convert.bytesToHexStr(mBuffer, MAX_HTTP_PACKAGE_HEADER_LENGTH));
			try {
				MiLinkLog.w(TAG, "no mns head , try to get string : " + new String(mBuffer, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
			}
			throw new InvalidPacketExecption("wrong packet，no mns head", InvalidPacketExecption.ERROR_CODE_NO_MNS_HEAD);
		}

		return Convert.bytesToUint(mBuffer, INTEGER_LENGTH);
	}

	private boolean parseNormalPacket() throws InvalidPacketExecption {
		int maxSize = (int) 2097152L;

		long packetLen = 0;
		packetLen = getPacketLen();

		MiLinkLog.i(TAG, "parseNormalPacket start, packetLen = " + packetLen + ", mPosition=" + mPosition);

		// 长度不够
		if (packetLen == -1) {
			return false;
		}

		if (packetLen < MIN_TCP_PACKAGE_HEADER_LENGTH) {
			// 长度出错，抛出异常报错
			throw new InvalidPacketExecption("[wrong packetlen = " + packetLen + "]",
					InvalidPacketExecption.ERROR_CODE_LENGTH_TOO_SMALL);
		}

		if (packetLen > maxSize) {
			// 长度出错，抛出异常报错
			throw new InvalidPacketExecption("[wrong packetlen = " + packetLen + "]",
					InvalidPacketExecption.ERROR_CODE_LENGTH_TOO_BIG);
		}

		if (packetLen > mPosition) {
			int len = mBuffer.length;
			if (packetLen > len) {
				MiLinkLog.v(TAG, "increased mBuffer to " + (packetLen + 5120));
				try {
					byte[] tempBuffer = new byte[(int) (packetLen + 5120)];
					System.arraycopy(mBuffer, 0, tempBuffer, 0, mPosition);
					mBuffer = tempBuffer;
				} catch (OutOfMemoryError e) {
					MiLinkLog.e(TAG, "append new byte fail ", e);
				}
			}
			return false;
		}

		MiLinkLog.i(TAG, "parseNormalPacket [packetLen = " + packetLen + "]");
		byte[] recvBuf = new byte[(int) packetLen];
		System.arraycopy(mBuffer, 0, recvBuf, 0, (int) packetLen);
		removeToBegin((int) packetLen);
		if (mSink != null) {
			mSink.onRecvDownStream(mCreatorSessionNO, recvBuf);
		}

		return true;
	}

	public void reset() {
		mPosition = 0;
	}

	public static interface ReceiveBufferSink {

		// 收到完整的downstream数据
		public boolean onRecvDownStream(int sessionNO, byte[] pcBuf);

		// 收到包头后，增加超时时间
		public boolean onAddTimeout(int sessionNO, int seqNo);
	}

}


package com.mi.milink.sdk.session.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Random;

import android.text.TextUtils;
import android.util.Base64;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.mi.milink.sdk.account.AnonymousAccount;
import com.mi.milink.sdk.account.ChannelAccount;
import com.mi.milink.sdk.account.IAccount;
import com.mi.milink.sdk.account.MiAccount;
import com.mi.milink.sdk.aidl.PacketData;
import com.mi.milink.sdk.base.Global;
import com.mi.milink.sdk.base.data.Convert;
import com.mi.milink.sdk.base.os.info.DeviceDash;
import com.mi.milink.sdk.data.ClientAppInfo;
import com.mi.milink.sdk.data.Const;
import com.mi.milink.sdk.debug.MiLinkLog;
import com.mi.milink.sdk.proto.DownstreamPacketProto.DownstreamPacket;
import com.mi.milink.sdk.proto.DownstreamPacketProto.ExtraInfo;
import com.mi.milink.sdk.proto.PushPacketProto.SimplePushData;
import com.mi.milink.sdk.proto.UpstreamPacketProto.BusiControl;
import com.mi.milink.sdk.proto.UpstreamPacketProto.TokenInfo;
import com.mi.milink.sdk.proto.UpstreamPacketProto.UpstreamPacket;
import com.mi.milink.sdk.session.persistent.SessionManager;
import com.mi.milink.sdk.util.compress.CompressionFactory;
import com.mi.milink.sdk.util.crypt.Cryptor;

/**
 * 处理业务包与上行下行数据包之间的转换工作
 *
 * @author MK
 */

public class StreamUtil {
	private static final String CLASSTAG = "StreamUtil";

	public static final byte[] MNS = { 'm', 'l', 'p', '\0' };

	public static final byte MNS_ENCODE_NONE = 0;

	public static final byte MNS_ENCODE_B2_TOKEN = 2;

	public static final byte MNS_ENCODE_FAST_LOGIN = 3;

	// 4是游戏sdk的bind 用过了
	public static final byte MNS_ENCODE_B2_TOKEN_FOR_HS = 5;

	public static final byte MNS_ENCODE_ANONYMOUS_FAST_LOGIN = 7;

	public static final byte MNS_ENCODE_ANONYMOUS_B2_TOKEN = 8;

	public static final byte MNS_ENCODE_CHANNEL_FAST_LOGIN = 9;

	public static final byte MNS_ENCODE_CHANNEL_B2_TOKEN = 10;

	private static final int GLOBAL_PUSH_FLAG_MASK = 0x1000;

	private static final int PING_MASK = 0x100;

	private static final int NO_NEED_RESPONSE_MASK = 0x10;

	private static final int NEED_PUSH_ACK_MASK = 0x1;

	private static final String QUA_HEAD = "v1-";

	private static final int DEFAULT_COMPRESS_LEN = 512;

	private static final int BUSI_CONTROL_NO_COMPRESS = 0;

	private static final int BUSI_CONTROL_ZLIB_COMPRESS = 1;

	private static final String B2_FOR_HS = "bsJ0RccDL4JvKAR660A6wzHXxRKRXWPBMowLR4m7mWg=";

	private static final String B2K_FOR_HS = "4N9FcL47REBDdCHL";

	private StreamUtil() {
	}

	public static boolean isPingFlag(int flag) {
		return (flag & PING_MASK) == PING_MASK;
	}

	public static boolean isNoNeedResponseFlag(int flag) {
		return (flag & NO_NEED_RESPONSE_MASK) == NO_NEED_RESPONSE_MASK;
	}

	public static String getQua() {
		// never change the order of these values
		StringBuilder qua = new StringBuilder();
		qua.append(QUA_HEAD);
		qua.append(Global.getClientAppInfo().getPlatformName());
		qua.append("-");
		qua.append(Global.getClientAppInfo().getAppName());
		qua.append("-");
		qua.append(Global.getClientAppInfo().getVersionName());
		qua.append("-");
		qua.append(Global.getClientAppInfo().getReleaseChannel());
		qua.append("-");
		qua.append(Global.getClientAppInfo().getLanguageCode());
		// qua.append("-");
		// qua.append(Global.getMiLinkVersion());
		return qua.toString().toLowerCase();
	}

	/**
	 * 获取上行数据流
	 *
	 * @param packet
	 *            业务包
	 * @return 上行数据流
	 */
	public static byte[] toUpBytes(String userTAG, PacketData packet, boolean isPing, byte encodeType,
			IAccount ownerAccount) {
		String TAG = userTAG + CLASSTAG;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			baos.write(MNS); // 头
			baos.write(new byte[] { 0, 0, 0, 0 }); // 整个长度，在打完包后修改
			baos.write(Global.getMiLinkProtocolVersion());
			baos.write(encodeType);// 加密方式，0 不加密，1 servicetoken 加密，2 b2token 加密
			int flag = Convert.bytesToInt(new byte[] { 0, 0, 0, 0 });
			if (isPing) {
				flag = flag | PING_MASK;
			}
			if (packet != null && !packet.needResponse()) {
				flag = flag | NO_NEED_RESPONSE_MASK;
			}
			if(SessionManager.getInstance().getGlobalPushFlag()){
				flag = flag | GLOBAL_PUSH_FLAG_MASK;
			}
			baos.write(Convert.intToBytes(flag)); // flag暂时全0，仅有ping包与不需要response的包不同
			baos.write(Convert.intToBytes(Global.getClientAppInfo().getAppId())); // appid
			long userId = 0;
			if (!TextUtils.isEmpty(ownerAccount.getUserId())) {
				userId = Long.parseLong(ownerAccount.getUserId());
			}
			baos.write(Convert.longToBytes(userId)); // userid

			String token = null;
			switch (encodeType) {
			case MNS_ENCODE_FAST_LOGIN:
			case MNS_ENCODE_ANONYMOUS_FAST_LOGIN:
			case MNS_ENCODE_CHANNEL_FAST_LOGIN:
				token = ownerAccount.getServiceToken();
				break;
			case MNS_ENCODE_B2_TOKEN:
			case MNS_ENCODE_ANONYMOUS_B2_TOKEN:
			case MNS_ENCODE_CHANNEL_B2_TOKEN:
				if (packet.needClientInfo()) {
					token = ownerAccount.getB2Token();
				}
				break;
			case MNS_ENCODE_B2_TOKEN_FOR_HS:
				token = B2_FOR_HS;
				break;
			case MNS_ENCODE_NONE:
				break;
			default:
				break;
			}
			byte[] tokenBytes = null;
			if (token != null) {
				tokenBytes = token.getBytes("utf-8");
			}
			if (tokenBytes != null) {
				short len = (short) tokenBytes.length;
				baos.write(Convert.shortToBytes(len));
				baos.write(tokenBytes);
			} else {
				short len = (short) 0;
				baos.write(Convert.shortToBytes(len));
			}
			baos.write(Convert.intToBytes(0)); // 压缩前长度
			baos.write(Convert.intToBytes(packet.getSeqNo())); // seqNo
			if (packet != null) { // 非ping包
				if (!isPing) {
					UpstreamPacket upPacket = getUpStream(userTAG, packet, encodeType, ownerAccount);
					byte[] encryptByteAry = encrypt(upPacket.toByteArray(), encodeType, ownerAccount);
					if (encryptByteAry != null) {
						baos.write(encryptByteAry);
					} else {
						return null;
					}
				} else {// ping包填写随机字符串.防止路由器过滤
					String randStr = getRandomString(4);
					baos.write(randStr.getBytes());
				}
			}
			byte[] entireMsg = baos.toByteArray();
			byte[] length = Convert.intToBytes(entireMsg.length);
			entireMsg[4] = length[0];
			entireMsg[5] = length[1];
			entireMsg[6] = length[2];
			entireMsg[7] = length[3];

			MiLinkLog.v(TAG, "up stream packet: seq=" + packet.getSeqNo() + ", isPing=" + isPing + ", flag=" + flag
					+ ", len=" + entireMsg.length);

			return entireMsg;
		} catch (IOException e) {
			MiLinkLog.e(TAG, "byte error", e);
		}
		return null;
	}

	public static String getRandomString(int length) { // length表示生成字符串的长度
		String base = "abcdefghijklmnopqrstuvwxyz0123456789";
		Random random = new Random();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < length; i++) {
			int number = random.nextInt(base.length());
			sb.append(base.charAt(number));
		}
		return sb.toString();
	}

	/**
	 * 从下行数据流中获取业务包
	 *
	 * @param downStream
	 * @return 业务包
	 * @throws IOException
	 */
	public static PacketData getDownPacket(String userTAG, byte[] downStream, GetAccountAdapter mGetAccountAdapter)
			throws IOException {
		String TAG = userTAG + CLASSTAG;
		if (downStream == null) {
			return null;
		}

		MiLinkLog.v(TAG, "recv data: len=" + downStream.length);
		// 去协议头
		ByteArrayInputStream reader = new ByteArrayInputStream(downStream);

		int headerLength = 0;
		byte[] mns = Convert.readByte(reader, 4);
		headerLength += 4;
		// 对MNS的包头进行判断，防止错包
		if (!Convert.compare(mns, MNS)) {
			return null;
		}
		// 2.读取包长
		byte[] b = Convert.readByte(reader, 4);
		headerLength += 4;
		int packetLength = Convert.bytesToInt(b);

		int version = reader.read();// 跳过版本
		headerLength += 1;

		byte encodeType = (byte) reader.read();// 读取encodeType
		headerLength += 1;

		byte[] flagBuf = Convert.readByte(reader, 4);// 跳过FLAG
		headerLength += 4;
		int flag = Convert.bytesToInt(flagBuf);

		Convert.readByte(reader, 4);// 跳过APPID
		headerLength += 4;

		byte[] uin = new byte[8];
		Convert.readBytes(reader, uin, 0, 8);// 跳过uin
		long luin = Convert.bytesToLong(uin);
		headerLength += 8;

		byte[] b2len = new byte[2];
		Convert.readBytes(reader, b2len, 0, 2);
		int ib2len = Convert.bytesToUshort(b2len);
		headerLength += 2;

		byte[] b2 = new byte[ib2len];
		Convert.readBytes(reader, b2, 0, ib2len);
		headerLength += ib2len;

		byte[] compLen = Convert.readByte(reader, 4);// 跳过compLen
		headerLength += 4;

		byte[] bSeqNo = Convert.readByte(reader, 4);// 跳过seqNo
		headerLength += 4;
		int seqNo = Convert.bytesToInt(bSeqNo);
		MiLinkLog.v(TAG, "down stream packet: seq=" + seqNo + ", encodeType=" + encodeType + ", flag=" + flag);
		IAccount ownerAccount = mGetAccountAdapter.getAccount(seqNo);
		if ((flag & PING_MASK) == PING_MASK) {
			PacketData packet = new PacketData();
			packet.setSeqNo(seqNo);
			packet.setCommand(Const.MnsCmd.MNS_PING_CMD);
			return packet;
		}

		byte[] stream = new byte[packetLength - headerLength];
		System.arraycopy(downStream, headerLength, stream, 0, packetLength - headerLength);
		DownstreamPacket downPacket = null;
		try {
			byte[] down = decrypt(stream, b2, encodeType, ownerAccount);
			if (down != null) {
				downPacket = DownstreamPacket.parseFrom(down);
			} else {
				MiLinkLog.w(TAG, "decrypt error, down == null !!!");
				MiLinkLog.w(TAG, "decrypt error stream:" + Convert.bytesToHexStr(stream, 2048 * 1000));
			}
		} catch (InvalidProtocolBufferException e) {
			MiLinkLog.e(TAG, "parser downstream error", e);
			MiLinkLog.e(TAG, "error stream:" + Convert.bytesToHexStr(stream, 2048 * 1000));
		}
		if (downPacket == null) {
			return null;
		}

		String cmd = downPacket.getServiceCmd();
		MiLinkLog.v(TAG,
				"mnscode:" + downPacket.getMnsCode() + ", busicode:" + downPacket.getBusiCode() + ", cmd=" + cmd);
		PacketData packet = new PacketData();
		// 解析ExtraInfo
		ExtraInfo extraInfo = ExtraInfo.parseFrom(downPacket.getExtra());
		if (extraInfo != null && extraInfo.hasHasClientInfo()) {
			MiLinkLog.v(TAG, "cmd=" + cmd + ", hasClientInfo =" + extraInfo.getHasClientInfo());
			packet.setHasClientInfo(extraInfo.getHasClientInfo());
		} else {
			packet.setHasClientInfo(false);
		}

		ByteString downPacketBytes = downPacket.getBusiBuff();
		if (downPacket.hasBusiControl()) {
			BusiControl bc = downPacket.getBusiControl();
			int compFlag = bc.getCompFlag();
			MiLinkLog.v(TAG, "hasBusiContro, compFlag= " + compFlag);
			if (compFlag == BUSI_CONTROL_ZLIB_COMPRESS) {
				byte[] data = CompressionFactory.createCompression(CompressionFactory.METHOD_ZLIB)
						.decompress(downPacketBytes.toByteArray());
				MiLinkLog.v(TAG, "len of decompress is " + data.length);
				if (data.length == bc.getLenBeforeComp()) {
					downPacketBytes = ByteString.copyFrom(data);
				} else {
					MiLinkLog.v(TAG, "len of decompress is not equal origin len, origin len=" + bc.getLenBeforeComp());
				}
			}
		} else {
			MiLinkLog.v(TAG, "hasBusiContro = false");
		}

		if (Const.MnsCmd.MNS_PUSH_CMD.equals(cmd)) { // push 暂时不支持压缩
			if (downPacketBytes == null) {
				return null;
			}
			try {
				packet.setIsPushPacket(true);
				SimplePushData pushPacket = SimplePushData.parseFrom(downPacketBytes);
				downPacketBytes = pushPacket.getPushdata();
				cmd = pushPacket.getCmd();
				if ((flag & NEED_PUSH_ACK_MASK) == NEED_PUSH_ACK_MASK) {
					// 这里seq为负数就代表需要ack
					seqNo = downPacket.getSeq() * -1;
				}
			} catch (InvalidProtocolBufferException e) {
				MiLinkLog.e(TAG, "parser pushdata error", e);
			}
		}

		packet.setData(downPacketBytes == null ? null : downPacketBytes.toByteArray());
		packet.setSeqNo(seqNo);
		packet.setCommand(cmd);
		packet.setMnsCode(downPacket.getMnsCode());
		packet.setBusiCode(downPacket.getBusiCode());
		packet.setResponseSize(packetLength);
		return packet;
	}

	private static UpstreamPacket getUpStream(String userTAG, PacketData packet, int encodeType,
			IAccount ownerAccount) {
		String TAG = userTAG + CLASSTAG;
		UpstreamPacket.Builder builder = UpstreamPacket.newBuilder();
		builder.setSeq(packet.getSeqNo());
		builder.setAppId(Global.getClientAppInfo().getAppId());

		if (packet.needClientInfo()) {
			String qua = getQua();
			// MiLinkLog.v(TAG,
			// "send packet, cmd=" + packet.getCommand() + " seq=" +
			// packet.getSeqNo()
			// + " qua=" + qua + " device=" +
			// DeviceDash.getInstance().getDeviceInfo()
			// + " encodeType=" + encodeType);
			MiLinkLog.v(TAG, "send packet, cmd=" + packet.getCommand() + " seq=" + packet.getSeqNo() + " qua=" + qua
					+ " encodeType=" + encodeType);
			if (encodeType != MNS_ENCODE_NONE) {
				builder.setDeviceInfo(ByteString.copyFromUtf8(DeviceDash.getInstance().getDeviceSimplifiedInfo()));
			}
			builder.setUa(qua);
		} else {
			MiLinkLog.v(TAG, "send packet don't need qua and deviceInfo, seq=" + packet.getSeqNo() + ", cmd="
					+ packet.getCommand());
		}
		builder.setServiceCmd(packet.getCommand());
		byte[] busiBuffData = packet.getData();
		if (!Const.isMnsCmd(packet.getCommand()) && busiBuffData != null) {
			BusiControl.Builder bcBuilder = BusiControl.newBuilder();
			int originLen = busiBuffData.length;
			MiLinkLog.v(TAG, "origin busibuff.size=" + originLen);
			if (originLen > DEFAULT_COMPRESS_LEN) {
				busiBuffData = CompressionFactory.createCompression(CompressionFactory.METHOD_ZLIB)
						.compress(busiBuffData);
				MiLinkLog.v(TAG, "after zlib compress, busibuff.size=" + busiBuffData.length);
				bcBuilder.setCompFlag(BUSI_CONTROL_ZLIB_COMPRESS);
				bcBuilder.setLenBeforeComp(originLen);
			}
			bcBuilder.setIsSupportComp(true);
			builder.setBusiControl(bcBuilder.build());
		}
		builder.setBusiBuff(ByteString.copyFrom(busiBuffData));
		String userId = ownerAccount.getUserId();
		if (!TextUtils.isEmpty(userId)) {
			builder.setMiUid(userId);
			builder.setMiUin(Long.parseLong(userId));
			if (encodeType != MNS_ENCODE_NONE) {
				if (packet.needClientInfo()) {
					TokenInfo.Builder tBuilder = TokenInfo.newBuilder();
					String serviceToken = ownerAccount.getServiceToken();
					byte bt[] = new byte[] {};
					try {
						bt = serviceToken.getBytes("utf-8");
					} catch (UnsupportedEncodingException e) {
					}
					tBuilder.setKey(ByteString.copyFrom(bt));
					tBuilder.setType(MNS_ENCODE_FAST_LOGIN);
					builder.setToken(tBuilder.build());
				}
			}
		} else {
			builder.setMiUin(0);
			builder.setMiUid("0");
		}
		return builder.build();
	}

	private static byte[] encrypt(byte[] stream, byte encodeType, IAccount ownerAccount) {
		try {
			switch (encodeType) {
			case MNS_ENCODE_NONE:
				return stream;
			case MNS_ENCODE_FAST_LOGIN: {
				if (ownerAccount instanceof MiAccount) {
					String ssecurity = ownerAccount.getSSecurity();
					byte[] key = new byte[] {};
					key = ssecurity.getBytes("utf-8");
					if (ClientAppInfo.isMiliaoApp() || ClientAppInfo.isForumApp() || ClientAppInfo.isGameCenterApp()
							|| ClientAppInfo.isXiaoMiPushApp() || ClientAppInfo.isSupportApp()) {
						key = Base64.decode(ssecurity, Base64.DEFAULT);
					}
					return Cryptor.encrypt(stream, key);
				}
			}
				break;
			case MNS_ENCODE_ANONYMOUS_FAST_LOGIN: {
				if (ownerAccount instanceof AnonymousAccount) {
					String ssecurity = ownerAccount.getSSecurity();
					return Cryptor.encryptRSA(stream, ssecurity);
				}
			}
			case MNS_ENCODE_CHANNEL_FAST_LOGIN: {

				if (ownerAccount instanceof ChannelAccount) {
					String ssecurity = ownerAccount.getSSecurity();
					MiLinkLog.v(CLASSTAG, "ssecurity = " + ssecurity);
					return Cryptor.encryptRSA(stream, ssecurity);
				}

			}
				break;
			case MNS_ENCODE_CHANNEL_B2_TOKEN:
			case MNS_ENCODE_ANONYMOUS_B2_TOKEN:
			case MNS_ENCODE_B2_TOKEN: {
				String b2Security = null;
				b2Security = ownerAccount.getB2Security();
				MiLinkLog.v(CLASSTAG, "b2Security = " + b2Security);
				byte[] key = b2Security.getBytes("utf-8");
				return Cryptor.encrypt(stream, key);
			}
			case MNS_ENCODE_B2_TOKEN_FOR_HS: {
				return Cryptor.encrypt(stream, B2K_FOR_HS.getBytes("utf-8"));
			}
			default:
				break;
			}
		} catch (Exception e) {
		}
		return stream;
	}

	private static byte[] decrypt(byte[] stream, byte[] b2Token, byte encodeType, IAccount ownerAccount) {
		try {
			switch (encodeType) {
			case MNS_ENCODE_NONE:
				return stream;
			case MNS_ENCODE_FAST_LOGIN:
				if (ownerAccount instanceof MiAccount) {
					String ssecurity = ownerAccount.getSSecurity();
					byte[] key = new byte[] {};
					key = ssecurity.getBytes("utf-8");
					if (ClientAppInfo.isMiliaoApp() || ClientAppInfo.isForumApp() || ClientAppInfo.isGameCenterApp()
							|| ClientAppInfo.isXiaoMiPushApp() || ClientAppInfo.isSupportApp()) {
						key = Base64.decode(ssecurity, Base64.DEFAULT);
					}
					MiLinkLog.v(CLASSTAG, "MNS_ENCODE_FAST_LOGIN ssecurity key " + ssecurity);
					return Cryptor.decrypt(stream, key);
				}
				break;
			case MNS_ENCODE_ANONYMOUS_FAST_LOGIN:
				if (ownerAccount instanceof AnonymousAccount) {
					String privacyKey = ownerAccount.getPrivacyKey();
					byte[] key = privacyKey.getBytes("utf-8");
					return Cryptor.decrypt(stream, key);
				}
				break;
			case MNS_ENCODE_CHANNEL_FAST_LOGIN:
				if (ownerAccount instanceof ChannelAccount) {
					String privacyKey = ownerAccount.getPrivacyKey();
					byte[] key = privacyKey.getBytes("utf-8");
					return Cryptor.decrypt(stream, key);
				}
				break;
			case MNS_ENCODE_CHANNEL_B2_TOKEN:
			case MNS_ENCODE_ANONYMOUS_B2_TOKEN:
			case MNS_ENCODE_B2_TOKEN: {
				String b2TokenStr = new String(b2Token, "UTF-8");
				byte[] key = new byte[] {};
				String oldB2Token = ownerAccount.getOldB2Token();
				String newB2Token = ownerAccount.getB2Token();
				if (b2TokenStr.equals(newB2Token)) {
					key = ownerAccount.getB2Security().getBytes("UTF-8");
				} else if (b2TokenStr.equals(oldB2Token)) {
					key = ownerAccount.getOldB2Security().getBytes("UTF-8");
				}
				return Cryptor.decrypt(stream, key);
			}
			case MNS_ENCODE_B2_TOKEN_FOR_HS: {
				return Cryptor.decrypt(stream, B2K_FOR_HS.getBytes("UTF-8"));
			}
			default:
				break;
			}
		} catch (Exception e) {
		}
		return stream;
	}

	public static interface GetAccountAdapter {
		public IAccount getAccount(int seq);
	}

}

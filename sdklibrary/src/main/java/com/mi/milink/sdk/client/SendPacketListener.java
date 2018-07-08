package com.mi.milink.sdk.client;

import com.mi.milink.sdk.aidl.PacketData;

/**
 * client异步发送消息的时候，如果需要等待服务器端返回消息，需要实现的listener
 *
 * @author MK
 *
 */

public interface SendPacketListener {
	public void onResponse(PacketData packet);
	public void onFailed(int errCode,String errMsg);
}

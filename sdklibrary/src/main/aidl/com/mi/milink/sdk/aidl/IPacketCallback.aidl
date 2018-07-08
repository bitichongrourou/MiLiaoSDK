package com.mi.milink.sdk.aidl;

import com.mi.milink.sdk.aidl.PacketData;

interface IPacketCallback {
	boolean onReceive(in List<PacketData> message);
}
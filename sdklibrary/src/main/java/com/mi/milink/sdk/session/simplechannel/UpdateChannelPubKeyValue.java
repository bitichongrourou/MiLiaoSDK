package com.mi.milink.sdk.session.simplechannel;

import com.mi.milink.sdk.proto.SystemPacketProto.MnsCmdChannelNewPubKeyRsp;
import com.mi.milink.sdk.session.common.Request;

public class UpdateChannelPubKeyValue {
	public UpdateChannelPubKeyValue() {
	}
	private MnsCmdChannelNewPubKeyRsp channelNewPubkey;
	private Request mRequeset;
	public MnsCmdChannelNewPubKeyRsp getChannelNewPubkey() {
		return channelNewPubkey;
	}
	public void setChannelNewPubkey(MnsCmdChannelNewPubKeyRsp channelNewPubkey) {
		this.channelNewPubkey = channelNewPubkey;
	}
	public Request getmRequeset() {
		return mRequeset;
	}
	public void setmRequeset(Request mRequeset) {
		this.mRequeset = mRequeset;
	}
	
	
}
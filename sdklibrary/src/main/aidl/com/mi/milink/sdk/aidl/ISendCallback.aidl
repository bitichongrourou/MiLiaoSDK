package com.mi.milink.sdk.aidl;

import com.mi.milink.sdk.aidl.PacketData;

interface ISendCallback {
	void onRsponse(in PacketData response);
	void onFailed(in int errCode,in String errMsg);
}
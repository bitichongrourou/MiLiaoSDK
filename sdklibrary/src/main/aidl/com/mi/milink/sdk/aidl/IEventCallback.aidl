package com.mi.milink.sdk.aidl;

interface IEventCallback {
	void onEventGetServiceToken();
	void onEventServiceTokenExpired();
	void onEventShouldCheckUpdate();
	void onEventInvalidPacket();
	void onEventKickedByServer(in int type,in long time,in String device);//时间单位为ms
}
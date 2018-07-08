package com.mi.milink.sdk.client;


public interface IEventListener {
    void onEventGetServiceToken();
    void onEventServiceTokenExpired();
    void onEventShouldCheckUpdate();
    void onEventInvalidPacket();
    void onEventKickedByServer( int type,long time, String device);//时间单位为ms
}
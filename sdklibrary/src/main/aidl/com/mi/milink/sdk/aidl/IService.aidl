package com.mi.milink.sdk.aidl;

import com.mi.milink.sdk.aidl.PacketData;
import com.mi.milink.sdk.aidl.IPacketCallback;
import com.mi.milink.sdk.aidl.IEventCallback;
import com.mi.milink.sdk.aidl.ISendCallback;

interface IService {
	void init(in String appUserId, in String serviceToken, in String sSecurity, in byte[] fastLoginExtra,in boolean passportInit);
	void sendAsyncWithResponse(in PacketData data, in int timeout, ISendCallback callback);
	void logoff();
	void setPacketCallBack(IPacketCallback pCallback);
	void setEventCallBack(IEventCallback eCallback);
	void forceReconnet();
	void fastLogin(in String appUserId, in String serviceToken, in String sSecurity, in byte[] fastLoginExtra);
	void setTimeoutMultiply(in float timeoutMultiply);
	
	int setClientInfo(in Bundle clientInfo);
	int getServerState();
	boolean isMiLinkLogined();
	void setIpAndPortInManualMode(in String ip,in int port);
	String getSuid();
	boolean enableConnectionManualMode(in boolean enable);
	
	void setAllowAnonymousLoginSwitch(in boolean on);
    void initUseAnonymousMode();
    
    void setMipushRegId(in String regId);
    void suspectBadConnection();
    void setMilinkLogLevel(in int level);
    void setLanguage(in String language);
    long getAnonymousAccountId();
    void setGlobalPushFlag(in boolean enable);
}
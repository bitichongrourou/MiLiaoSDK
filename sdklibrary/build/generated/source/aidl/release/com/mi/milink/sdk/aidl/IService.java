/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: C:\\yeejay\\mixchat\\MiLinkSdkCoreAS\\sdklibrary\\src\\main\\aidl\\com\\mi\\milink\\sdk\\aidl\\IService.aidl
 */
package com.mi.milink.sdk.aidl;
public interface IService extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.mi.milink.sdk.aidl.IService
{
private static final java.lang.String DESCRIPTOR = "com.mi.milink.sdk.aidl.IService";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.mi.milink.sdk.aidl.IService interface,
 * generating a proxy if needed.
 */
public static com.mi.milink.sdk.aidl.IService asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.mi.milink.sdk.aidl.IService))) {
return ((com.mi.milink.sdk.aidl.IService)iin);
}
return new com.mi.milink.sdk.aidl.IService.Stub.Proxy(obj);
}
@Override public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_init:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _arg1;
_arg1 = data.readString();
java.lang.String _arg2;
_arg2 = data.readString();
byte[] _arg3;
_arg3 = data.createByteArray();
boolean _arg4;
_arg4 = (0!=data.readInt());
this.init(_arg0, _arg1, _arg2, _arg3, _arg4);
reply.writeNoException();
return true;
}
case TRANSACTION_sendAsyncWithResponse:
{
data.enforceInterface(DESCRIPTOR);
com.mi.milink.sdk.aidl.PacketData _arg0;
if ((0!=data.readInt())) {
_arg0 = com.mi.milink.sdk.aidl.PacketData.CREATOR.createFromParcel(data);
}
else {
_arg0 = null;
}
int _arg1;
_arg1 = data.readInt();
com.mi.milink.sdk.aidl.ISendCallback _arg2;
_arg2 = com.mi.milink.sdk.aidl.ISendCallback.Stub.asInterface(data.readStrongBinder());
this.sendAsyncWithResponse(_arg0, _arg1, _arg2);
reply.writeNoException();
return true;
}
case TRANSACTION_logoff:
{
data.enforceInterface(DESCRIPTOR);
this.logoff();
reply.writeNoException();
return true;
}
case TRANSACTION_setPacketCallBack:
{
data.enforceInterface(DESCRIPTOR);
com.mi.milink.sdk.aidl.IPacketCallback _arg0;
_arg0 = com.mi.milink.sdk.aidl.IPacketCallback.Stub.asInterface(data.readStrongBinder());
this.setPacketCallBack(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_setEventCallBack:
{
data.enforceInterface(DESCRIPTOR);
com.mi.milink.sdk.aidl.IEventCallback _arg0;
_arg0 = com.mi.milink.sdk.aidl.IEventCallback.Stub.asInterface(data.readStrongBinder());
this.setEventCallBack(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_forceReconnet:
{
data.enforceInterface(DESCRIPTOR);
this.forceReconnet();
reply.writeNoException();
return true;
}
case TRANSACTION_fastLogin:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _arg1;
_arg1 = data.readString();
java.lang.String _arg2;
_arg2 = data.readString();
byte[] _arg3;
_arg3 = data.createByteArray();
this.fastLogin(_arg0, _arg1, _arg2, _arg3);
reply.writeNoException();
return true;
}
case TRANSACTION_setTimeoutMultiply:
{
data.enforceInterface(DESCRIPTOR);
float _arg0;
_arg0 = data.readFloat();
this.setTimeoutMultiply(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_setClientInfo:
{
data.enforceInterface(DESCRIPTOR);
android.os.Bundle _arg0;
if ((0!=data.readInt())) {
_arg0 = android.os.Bundle.CREATOR.createFromParcel(data);
}
else {
_arg0 = null;
}
int _result = this.setClientInfo(_arg0);
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_getServerState:
{
data.enforceInterface(DESCRIPTOR);
int _result = this.getServerState();
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_isMiLinkLogined:
{
data.enforceInterface(DESCRIPTOR);
boolean _result = this.isMiLinkLogined();
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_setIpAndPortInManualMode:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
int _arg1;
_arg1 = data.readInt();
this.setIpAndPortInManualMode(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_getSuid:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _result = this.getSuid();
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_enableConnectionManualMode:
{
data.enforceInterface(DESCRIPTOR);
boolean _arg0;
_arg0 = (0!=data.readInt());
boolean _result = this.enableConnectionManualMode(_arg0);
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_setAllowAnonymousLoginSwitch:
{
data.enforceInterface(DESCRIPTOR);
boolean _arg0;
_arg0 = (0!=data.readInt());
this.setAllowAnonymousLoginSwitch(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_initUseAnonymousMode:
{
data.enforceInterface(DESCRIPTOR);
this.initUseAnonymousMode();
reply.writeNoException();
return true;
}
case TRANSACTION_setMipushRegId:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.setMipushRegId(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_suspectBadConnection:
{
data.enforceInterface(DESCRIPTOR);
this.suspectBadConnection();
reply.writeNoException();
return true;
}
case TRANSACTION_setMilinkLogLevel:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
this.setMilinkLogLevel(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_setLanguage:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.setLanguage(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_getAnonymousAccountId:
{
data.enforceInterface(DESCRIPTOR);
long _result = this.getAnonymousAccountId();
reply.writeNoException();
reply.writeLong(_result);
return true;
}
case TRANSACTION_setGlobalPushFlag:
{
data.enforceInterface(DESCRIPTOR);
boolean _arg0;
_arg0 = (0!=data.readInt());
this.setGlobalPushFlag(_arg0);
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.mi.milink.sdk.aidl.IService
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
@Override public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
@Override public void init(java.lang.String appUserId, java.lang.String serviceToken, java.lang.String sSecurity, byte[] fastLoginExtra, boolean passportInit) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(appUserId);
_data.writeString(serviceToken);
_data.writeString(sSecurity);
_data.writeByteArray(fastLoginExtra);
_data.writeInt(((passportInit)?(1):(0)));
mRemote.transact(Stub.TRANSACTION_init, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void sendAsyncWithResponse(com.mi.milink.sdk.aidl.PacketData data, int timeout, com.mi.milink.sdk.aidl.ISendCallback callback) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
if ((data!=null)) {
_data.writeInt(1);
data.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
_data.writeInt(timeout);
_data.writeStrongBinder((((callback!=null))?(callback.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_sendAsyncWithResponse, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void logoff() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_logoff, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void setPacketCallBack(com.mi.milink.sdk.aidl.IPacketCallback pCallback) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((pCallback!=null))?(pCallback.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_setPacketCallBack, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void setEventCallBack(com.mi.milink.sdk.aidl.IEventCallback eCallback) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((eCallback!=null))?(eCallback.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_setEventCallBack, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void forceReconnet() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_forceReconnet, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void fastLogin(java.lang.String appUserId, java.lang.String serviceToken, java.lang.String sSecurity, byte[] fastLoginExtra) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(appUserId);
_data.writeString(serviceToken);
_data.writeString(sSecurity);
_data.writeByteArray(fastLoginExtra);
mRemote.transact(Stub.TRANSACTION_fastLogin, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void setTimeoutMultiply(float timeoutMultiply) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeFloat(timeoutMultiply);
mRemote.transact(Stub.TRANSACTION_setTimeoutMultiply, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public int setClientInfo(android.os.Bundle clientInfo) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
if ((clientInfo!=null)) {
_data.writeInt(1);
clientInfo.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_setClientInfo, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public int getServerState() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getServerState, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public boolean isMiLinkLogined() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_isMiLinkLogined, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void setIpAndPortInManualMode(java.lang.String ip, int port) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(ip);
_data.writeInt(port);
mRemote.transact(Stub.TRANSACTION_setIpAndPortInManualMode, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public java.lang.String getSuid() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getSuid, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public boolean enableConnectionManualMode(boolean enable) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(((enable)?(1):(0)));
mRemote.transact(Stub.TRANSACTION_enableConnectionManualMode, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void setAllowAnonymousLoginSwitch(boolean on) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(((on)?(1):(0)));
mRemote.transact(Stub.TRANSACTION_setAllowAnonymousLoginSwitch, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void initUseAnonymousMode() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_initUseAnonymousMode, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void setMipushRegId(java.lang.String regId) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(regId);
mRemote.transact(Stub.TRANSACTION_setMipushRegId, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void suspectBadConnection() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_suspectBadConnection, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void setMilinkLogLevel(int level) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(level);
mRemote.transact(Stub.TRANSACTION_setMilinkLogLevel, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void setLanguage(java.lang.String language) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(language);
mRemote.transact(Stub.TRANSACTION_setLanguage, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public long getAnonymousAccountId() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
long _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getAnonymousAccountId, _data, _reply, 0);
_reply.readException();
_result = _reply.readLong();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void setGlobalPushFlag(boolean enable) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(((enable)?(1):(0)));
mRemote.transact(Stub.TRANSACTION_setGlobalPushFlag, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_init = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_sendAsyncWithResponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_logoff = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_setPacketCallBack = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_setEventCallBack = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_forceReconnet = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
static final int TRANSACTION_fastLogin = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
static final int TRANSACTION_setTimeoutMultiply = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
static final int TRANSACTION_setClientInfo = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
static final int TRANSACTION_getServerState = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
static final int TRANSACTION_isMiLinkLogined = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
static final int TRANSACTION_setIpAndPortInManualMode = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
static final int TRANSACTION_getSuid = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
static final int TRANSACTION_enableConnectionManualMode = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
static final int TRANSACTION_setAllowAnonymousLoginSwitch = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
static final int TRANSACTION_initUseAnonymousMode = (android.os.IBinder.FIRST_CALL_TRANSACTION + 15);
static final int TRANSACTION_setMipushRegId = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16);
static final int TRANSACTION_suspectBadConnection = (android.os.IBinder.FIRST_CALL_TRANSACTION + 17);
static final int TRANSACTION_setMilinkLogLevel = (android.os.IBinder.FIRST_CALL_TRANSACTION + 18);
static final int TRANSACTION_setLanguage = (android.os.IBinder.FIRST_CALL_TRANSACTION + 19);
static final int TRANSACTION_getAnonymousAccountId = (android.os.IBinder.FIRST_CALL_TRANSACTION + 20);
static final int TRANSACTION_setGlobalPushFlag = (android.os.IBinder.FIRST_CALL_TRANSACTION + 21);
}
public void init(java.lang.String appUserId, java.lang.String serviceToken, java.lang.String sSecurity, byte[] fastLoginExtra, boolean passportInit) throws android.os.RemoteException;
public void sendAsyncWithResponse(com.mi.milink.sdk.aidl.PacketData data, int timeout, com.mi.milink.sdk.aidl.ISendCallback callback) throws android.os.RemoteException;
public void logoff() throws android.os.RemoteException;
public void setPacketCallBack(com.mi.milink.sdk.aidl.IPacketCallback pCallback) throws android.os.RemoteException;
public void setEventCallBack(com.mi.milink.sdk.aidl.IEventCallback eCallback) throws android.os.RemoteException;
public void forceReconnet() throws android.os.RemoteException;
public void fastLogin(java.lang.String appUserId, java.lang.String serviceToken, java.lang.String sSecurity, byte[] fastLoginExtra) throws android.os.RemoteException;
public void setTimeoutMultiply(float timeoutMultiply) throws android.os.RemoteException;
public int setClientInfo(android.os.Bundle clientInfo) throws android.os.RemoteException;
public int getServerState() throws android.os.RemoteException;
public boolean isMiLinkLogined() throws android.os.RemoteException;
public void setIpAndPortInManualMode(java.lang.String ip, int port) throws android.os.RemoteException;
public java.lang.String getSuid() throws android.os.RemoteException;
public boolean enableConnectionManualMode(boolean enable) throws android.os.RemoteException;
public void setAllowAnonymousLoginSwitch(boolean on) throws android.os.RemoteException;
public void initUseAnonymousMode() throws android.os.RemoteException;
public void setMipushRegId(java.lang.String regId) throws android.os.RemoteException;
public void suspectBadConnection() throws android.os.RemoteException;
public void setMilinkLogLevel(int level) throws android.os.RemoteException;
public void setLanguage(java.lang.String language) throws android.os.RemoteException;
public long getAnonymousAccountId() throws android.os.RemoteException;
public void setGlobalPushFlag(boolean enable) throws android.os.RemoteException;
}

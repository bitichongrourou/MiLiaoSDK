/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: C:\\yeejay\\mixchat\\MiLinkSdkCoreAS\\sdklibrary\\src\\main\\aidl\\com\\mi\\milink\\sdk\\aidl\\IEventCallback.aidl
 */
package com.mi.milink.sdk.aidl;
public interface IEventCallback extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.mi.milink.sdk.aidl.IEventCallback
{
private static final java.lang.String DESCRIPTOR = "com.mi.milink.sdk.aidl.IEventCallback";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.mi.milink.sdk.aidl.IEventCallback interface,
 * generating a proxy if needed.
 */
public static com.mi.milink.sdk.aidl.IEventCallback asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.mi.milink.sdk.aidl.IEventCallback))) {
return ((com.mi.milink.sdk.aidl.IEventCallback)iin);
}
return new com.mi.milink.sdk.aidl.IEventCallback.Stub.Proxy(obj);
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
case TRANSACTION_onEventGetServiceToken:
{
data.enforceInterface(DESCRIPTOR);
this.onEventGetServiceToken();
reply.writeNoException();
return true;
}
case TRANSACTION_onEventServiceTokenExpired:
{
data.enforceInterface(DESCRIPTOR);
this.onEventServiceTokenExpired();
reply.writeNoException();
return true;
}
case TRANSACTION_onEventShouldCheckUpdate:
{
data.enforceInterface(DESCRIPTOR);
this.onEventShouldCheckUpdate();
reply.writeNoException();
return true;
}
case TRANSACTION_onEventInvalidPacket:
{
data.enforceInterface(DESCRIPTOR);
this.onEventInvalidPacket();
reply.writeNoException();
return true;
}
case TRANSACTION_onEventKickedByServer:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
long _arg1;
_arg1 = data.readLong();
java.lang.String _arg2;
_arg2 = data.readString();
this.onEventKickedByServer(_arg0, _arg1, _arg2);
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.mi.milink.sdk.aidl.IEventCallback
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
@Override public void onEventGetServiceToken() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_onEventGetServiceToken, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void onEventServiceTokenExpired() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_onEventServiceTokenExpired, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void onEventShouldCheckUpdate() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_onEventShouldCheckUpdate, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void onEventInvalidPacket() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_onEventInvalidPacket, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void onEventKickedByServer(int type, long time, java.lang.String device) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(type);
_data.writeLong(time);
_data.writeString(device);
mRemote.transact(Stub.TRANSACTION_onEventKickedByServer, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_onEventGetServiceToken = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_onEventServiceTokenExpired = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_onEventShouldCheckUpdate = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_onEventInvalidPacket = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_onEventKickedByServer = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
}
public void onEventGetServiceToken() throws android.os.RemoteException;
public void onEventServiceTokenExpired() throws android.os.RemoteException;
public void onEventShouldCheckUpdate() throws android.os.RemoteException;
public void onEventInvalidPacket() throws android.os.RemoteException;
public void onEventKickedByServer(int type, long time, java.lang.String device) throws android.os.RemoteException;
}

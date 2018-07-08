/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: C:\\yeejay\\mixchat\\MiLinkSdkCoreAS\\sdklibrary\\src\\main\\aidl\\com\\mi\\milink\\sdk\\aidl\\ISendCallback.aidl
 */
package com.mi.milink.sdk.aidl;
public interface ISendCallback extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.mi.milink.sdk.aidl.ISendCallback
{
private static final java.lang.String DESCRIPTOR = "com.mi.milink.sdk.aidl.ISendCallback";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.mi.milink.sdk.aidl.ISendCallback interface,
 * generating a proxy if needed.
 */
public static com.mi.milink.sdk.aidl.ISendCallback asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.mi.milink.sdk.aidl.ISendCallback))) {
return ((com.mi.milink.sdk.aidl.ISendCallback)iin);
}
return new com.mi.milink.sdk.aidl.ISendCallback.Stub.Proxy(obj);
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
case TRANSACTION_onRsponse:
{
data.enforceInterface(DESCRIPTOR);
com.mi.milink.sdk.aidl.PacketData _arg0;
if ((0!=data.readInt())) {
_arg0 = com.mi.milink.sdk.aidl.PacketData.CREATOR.createFromParcel(data);
}
else {
_arg0 = null;
}
this.onRsponse(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_onFailed:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
java.lang.String _arg1;
_arg1 = data.readString();
this.onFailed(_arg0, _arg1);
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.mi.milink.sdk.aidl.ISendCallback
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
@Override public void onRsponse(com.mi.milink.sdk.aidl.PacketData response) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
if ((response!=null)) {
_data.writeInt(1);
response.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_onRsponse, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void onFailed(int errCode, java.lang.String errMsg) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(errCode);
_data.writeString(errMsg);
mRemote.transact(Stub.TRANSACTION_onFailed, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_onRsponse = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_onFailed = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
}
public void onRsponse(com.mi.milink.sdk.aidl.PacketData response) throws android.os.RemoteException;
public void onFailed(int errCode, java.lang.String errMsg) throws android.os.RemoteException;
}

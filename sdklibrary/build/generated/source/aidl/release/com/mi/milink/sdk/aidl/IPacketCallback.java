/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: C:\\yeejay\\mixchat\\MiLinkSdkCoreAS\\sdklibrary\\src\\main\\aidl\\com\\mi\\milink\\sdk\\aidl\\IPacketCallback.aidl
 */
package com.mi.milink.sdk.aidl;
public interface IPacketCallback extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.mi.milink.sdk.aidl.IPacketCallback
{
private static final java.lang.String DESCRIPTOR = "com.mi.milink.sdk.aidl.IPacketCallback";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.mi.milink.sdk.aidl.IPacketCallback interface,
 * generating a proxy if needed.
 */
public static com.mi.milink.sdk.aidl.IPacketCallback asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.mi.milink.sdk.aidl.IPacketCallback))) {
return ((com.mi.milink.sdk.aidl.IPacketCallback)iin);
}
return new com.mi.milink.sdk.aidl.IPacketCallback.Stub.Proxy(obj);
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
case TRANSACTION_onReceive:
{
data.enforceInterface(DESCRIPTOR);
java.util.List<com.mi.milink.sdk.aidl.PacketData> _arg0;
_arg0 = data.createTypedArrayList(com.mi.milink.sdk.aidl.PacketData.CREATOR);
boolean _result = this.onReceive(_arg0);
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.mi.milink.sdk.aidl.IPacketCallback
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
@Override public boolean onReceive(java.util.List<com.mi.milink.sdk.aidl.PacketData> message) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeTypedList(message);
mRemote.transact(Stub.TRANSACTION_onReceive, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
}
static final int TRANSACTION_onReceive = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
}
public boolean onReceive(java.util.List<com.mi.milink.sdk.aidl.PacketData> message) throws android.os.RemoteException;
}

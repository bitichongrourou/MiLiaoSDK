
package com.mi.milink.sdk.connection;

import com.mi.milink.sdk.session.common.SessionConst;

/**
 * Tcp连接, ConnectionImpl的子类
 * 
 * @author MK
 */
public class TcpConnection extends ConnectionImpl {
    private String mServerIP = null;

    private int mServerPort = 0;

    private boolean mIsLoaded = false;

    public TcpConnection(int sessionNO, IConnectionCallback callBack) {
        super(sessionNO, SessionConst.TCP_CONNECTION_TYPE);
        mIsLoaded = ConnectionImpl.isLibLoaded();
        setCallback(callBack);
    }

    @Override
    public boolean connect(String serverIP, int serverPort, String proxyIP, int proxyPort,
            int timeOut, int mss) {
        mServerIP = serverIP;
        mServerPort = serverPort;

        if (!mIsLoaded) {
            return false;
        }
        try{
        	return super.connect(mServerIP, mServerPort, proxyIP, proxyPort, timeOut, mss);
        }catch(Exception e){
        	
        }
        return false;
    }
    
    @Override
    public String getServerIP() {
        return mServerIP;
    }

    @Override
    public int getServerPort() {
        return mServerPort;
    }

    /*
     * (non-Javadoc)
     * @see com.mi.milink.sdk.connection.ConnectionImpl#start()
     */
    @Override
    public boolean start() {
        if (!mIsLoaded) {
            return false;
        }
        try{
        	return super.start();
        }catch(Exception e){
        	
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * @see com.mi.milink.sdk.connection.ConnectionImpl#stop()
     */
    @Override
    public boolean stop() {
        if (!mIsLoaded) {
            return false;
        }
        try{
        	return super.stop();
        }catch(Exception e){
        	
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * @see com.mi.milink.sdk.connection.ConnectionImpl#wakeUp()
     */
    @Override
    public void wakeUp() {
        if (!mIsLoaded) {
            return;
        }
        try{
        	super.wakeUp();
        }catch(Exception e){
        	
        }
    }

    /*
     * (non-Javadoc)
     * @see com.mi.milink.sdk.connection.ConnectionImpl#disconnect()
     */
    @Override
    public boolean disconnect() {
        if (!mIsLoaded) {
            return false;
        }
        try{
        	return super.disconnect();
        }catch(Exception e){
        	
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * @see com.mi.milink.sdk.connection.ConnectionImpl#SendData(byte[], int,
     * int, int)
     */
    @Override
    public boolean sendData(byte[] buf, int cookie, int sendTimeout) {
        if (!mIsLoaded) {
            return false;
        }
        try{
        	return super.sendData(buf, cookie, sendTimeout);
        }catch(Exception e){
        	
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * @see com.mi.milink.sdk.connection.ConnectionImpl#removeSendData(int)
     */
    @Override
    public void removeSendData(int cookie) {
        if (!mIsLoaded) {
            return;
        }
        try{
        	super.removeSendData(cookie);
        }catch(Exception e){
        	
        }
    }

    /*
     * (non-Javadoc)
     * @see com.mi.milink.sdk.connection.ConnectionImpl#removeAllSendData()
     */
    @Override
    public void removeAllSendData() {
        if (!mIsLoaded) {
            return;
        }
        try{
        	super.removeAllSendData();
        }catch(Exception e){
        	
        }
    }

    /*
     * (non-Javadoc)
     * @see com.mi.milink.sdk.connection.ConnectionImpl#isSendDone(int)
     */
    @Override
    public boolean isSendDone(int cookie) {
        if (!mIsLoaded) {
            return false;
        }
        try{
        	return super.isSendDone(cookie);
        }catch(Exception e){
        	
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * @see com.mi.milink.sdk.connection.ConnectionImpl#isRunning()
     */
    @Override
    public boolean isRunning() {
        if (!mIsLoaded) {
            return false;
        }
        try{
        	return super.isRunning();
        }catch(Exception e){
        	
        }
        return false;
    }
}

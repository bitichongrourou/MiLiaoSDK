
package com.mi.milink.sdk.session.common;

import com.mi.milink.sdk.aidl.PacketData;

/**
 * 数据包的回调接口，表示该数据是否成功到达server，也即是否收到server的response
 * 
 * @author MK
 */
public interface ResponseListener {

    /**
     * 数据透传成功(收到server的response)回调接口
     * 
     * @param errCode
     * @param data
     */
    void onDataSendSuccess(int errCode, PacketData data);

    /**
     * 数据透传失败(没有收到server的response或者超时了)回调接口
     * 
     * @param errCode
     * @param errMsg
     */
    void onDataSendFailed(int errCode, String errMsg);

}

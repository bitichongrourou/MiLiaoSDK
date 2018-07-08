
package com.mi.milink.sdk.client;

import java.util.ArrayList;

import com.mi.milink.sdk.aidl.PacketData;

/**
 * push接收的listener
 *
 * @author xiaolong
 */

public interface IPacketListener {
    void onReceive(ArrayList<PacketData> list);
}

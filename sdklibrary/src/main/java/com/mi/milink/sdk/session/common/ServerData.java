
package com.mi.milink.sdk.session.common;

public abstract class ServerData {

    private long mTimeStamp = 0;

    public void setTimeStamp(long timeStamp) {
        mTimeStamp = timeStamp;
    }

    public long getTimeStamp() {
        return mTimeStamp;
    }

}

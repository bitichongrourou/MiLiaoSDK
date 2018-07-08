
package com.mi.milink.sdk.client;

public class MiLinkException extends Throwable {

    /**
     * 
     */
    private static final long serialVersionUID = 112893821983L;

    public MiLinkException(int errCode, String errMsg) {
        super(String.format("errCode:%d errMsg:%s", errCode, errMsg));
    }
}

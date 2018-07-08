
package com.mi.milink.sdk.config;

import com.mi.milink.sdk.base.os.info.ServiceProvider;

public enum Operator {
    UNKNOWN((byte) 0), CMCC((byte) 1), UNICOM((byte) 2), CMCT((byte) 3), WIFI((byte) 4);

    private byte _operatorCode;

    private Operator(byte providerCode) {
        _operatorCode = providerCode;
    }

    public byte operatorCode() {
        return _operatorCode;
    }

    public static byte getProviderCode(String serviceProvider) {
        if (serviceProvider.equalsIgnoreCase(ServiceProvider.CHINA_MOBILE.getName()))
            return CMCC.operatorCode();
        else if (serviceProvider.equalsIgnoreCase(ServiceProvider.CHINA_UNICOM.getName()))
            return UNICOM.operatorCode();
        else if (serviceProvider.equalsIgnoreCase(ServiceProvider.CHINA_TELECOM.getName()))
            return CMCT.operatorCode();
        else
            return UNKNOWN.operatorCode();
    }

    boolean equal(Operator operator) {
        return this.operatorCode() == operator.operatorCode();
    }
}

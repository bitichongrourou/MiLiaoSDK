
package com.mi.milink.sdk.session.common;

/**
 * 无效数据包异常类
 * @author MK
 */
public class InvalidPacketExecption extends Exception {

    private static final long serialVersionUID = -4691985160731593680L;

    public static final int ERROR_CODE_NO_MNS_HEAD = 1;

    public static final int ERROR_CODE_LENGTH_TOO_SMALL = 2;

    public static final int ERROR_CODE_LENGTH_TOO_BIG = 3;

    public static final int ERROR_CODE_NO_HTTP_HEAD_END = 4;

    public int errCode = 0;

    public InvalidPacketExecption(String message, int errCode) {
        super(message);
        this.errCode = errCode;
    }

}

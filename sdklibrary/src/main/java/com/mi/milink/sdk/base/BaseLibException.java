package com.mi.milink.sdk.base;

/**
 * 基础库的异常信息
 * 
 * @author MK
 */
public class BaseLibException extends RuntimeException {
	private static final long serialVersionUID = -2945737496904114992L;

	public BaseLibException() {

	}

	public BaseLibException(String detailMessage) {
		super(detailMessage);
	}

	public BaseLibException(Throwable throwable) {
		super(throwable);
	}

	public BaseLibException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

}

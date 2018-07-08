package com.mi.milink.sdk.base.os.dns;

import java.io.IOException;

/**
 * 当DNS 消息无效时将会抛出该异常
 * 
 * @author ethamhuang
 */

public class WireParseException extends IOException {

	/**
	 * 序列ID
	 */
	private static final long serialVersionUID = -4629109360550945761L;

	public WireParseException() {
		super();
	}

	public WireParseException(String s) {
		super(s);
	}

}

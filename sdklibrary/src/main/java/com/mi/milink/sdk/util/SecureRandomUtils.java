package com.mi.milink.sdk.util;

import java.security.SecureRandom;

/**
 * @author michaelwei 修复android伪随机数的bug 封装SecureRandom的构造函数
 */
public final class SecureRandomUtils {
	static {
		try {
			PRNGFixes.apply();
		} catch (SecurityException e) {

		} catch (Throwable e) {

		}

	}

	public final static SecureRandom createSecureRandom() {
		return new SecureRandom();
	}
}

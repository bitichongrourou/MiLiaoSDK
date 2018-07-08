package com.mi.milink.sdk.base.os.dns;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 为生成唯一请求ID提供类。<br/>
 * 采用递增的方式保证ID的唯一性，范围从100到65535。当递增至达到最大值后，将从新从100开始递增
 * 
 * @author ethamhuang
 */
public class AtomicRequestId {

	private static AtomicRequestId reqId = null;

	private final static int initValue = 100; // 初始值

	private final static int maxValue = 0xffff; // 最大值

	private static AtomicInteger reqIdentity = new AtomicInteger(initValue);

	/**
	 * 生成AtomicRequestId 实例
	 * 
	 * @return
	 */
	public synchronized static AtomicRequestId getInstance() {
		if (reqId == null) {
			reqId = new AtomicRequestId();
		}

		return reqId;
	}

	/**
	 * 获取唯一ID
	 * 
	 * @return
	 */
	public synchronized int getId() {
		int id = reqIdentity.getAndIncrement();

		/** 当id超过最大值时，从新初始化AtomicInteger */
		if (id >= maxValue) {
			reqIdentity = new AtomicInteger(initValue);
			id = reqIdentity.getAndIncrement();
		}

		return id;
	}

}

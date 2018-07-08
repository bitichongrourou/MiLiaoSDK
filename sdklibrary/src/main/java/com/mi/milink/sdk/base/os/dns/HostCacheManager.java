package com.mi.milink.sdk.base.os.dns;

import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.Map;

import com.mi.milink.sdk.base.debug.CustomLogcat;
import com.mi.milink.sdk.base.os.Device.Network;
import com.mi.milink.sdk.base.os.info.NetworkDash;
import com.mi.milink.sdk.base.os.info.WifiDash;

/**
 * DNS记录缓存，记录的过期时间以TTL为准，考虑到TTL的有效期一般为分钟级 ，故这里只做内存的缓存，不做物理缓存
 * 
 * @author ethamhuang
 */
public class HostCacheManager {

	private final int MAX_CACHE_SIZE = 128;

	private Cache<String, HostEntity> data = new Cache<String, HostEntity>(
			MAX_CACHE_SIZE);

	private static HostCacheManager manager = null;

	public synchronized static HostCacheManager getInstance() {
		if (manager == null) {
			manager = new HostCacheManager();
		}

		return manager;
	}

	/**
	 * 获取指定Host的响应包信息
	 * 
	 * @param host
	 * @return {@link InetAddress[]}，如果缓存未找到或缓存已经过期将返回null
	 */
	public InetAddress[] getCacheItemByHost(String host) {
		HostEntity he = data.get(host);
		if (he != null) {

			if (!he.isValid()) {
				data.remove(host);
				he = null;
			} else {

				return he.address;
			}
		}

		return null;
	}

	/**
	 * 将响应包添加至缓存
	 * 
	 * @param host
	 * @param packet
	 * @return {@link ResponsePacket}
	 */
	public void addCache(String host, InetAddress[] address, long expireTime) {
		CustomLogcat.i("dnstest", "$$$addCache[" + host + "]");
		HostEntity he = new HostEntity();
		he.expireTime = expireTime;
		he.address = address;
		he.networkType = NetworkDash.isMobile() ? NetworkDash.getApnName()
				: WifiDash.getBSSID();

		if (data.containsKey(host)) {
			data.remove(host);
		}

		data.put(host, he);
	}

	// ////////////////////////////////////////////////////////////////////////

	/**
	 * 缓存数据的存储链表，这里通过{@link LinkedHashMap}实现轻量级的LRU缓存 <br/>
	 * 替换策略(Least Rencetly Used 最近最少使用 )
	 * 
	 * @author ethamhuang
	 * @param <K>
	 *            Key
	 * @param <V>
	 *            Value
	 */
	private class Cache<K, V> extends LinkedHashMap<K, V> {

		/** 序列化版本ID */
		private static final long serialVersionUID = -6940751117906094384L;

		/** 缓存容量，默认为5 */
		private int capacity = 5;

		/** 扩容因子，当容量达最大值时扩容的百分比。经官方测试，0.75为时间和空间的最优值 */
		/** 注：本缓存器实现了固定容量缓存，缓存器会根据LRU策略将超出的缓存删掉，不会进行扩容 */
		private static final float DEFAULT_LOAD_FACTOR = 0.75f;

		/** 资源锁 */
		private Object lock = new Object();

		/**
		 * 创建一个Cache对象
		 * 
		 * @param capacity
		 *            cache的容量，capacity必须大于0，否则将使用默认值
		 */
		public Cache(int capacity) {
			super(capacity, DEFAULT_LOAD_FACTOR, true);

			// 这里为防止capacity被传入负值的容错处理
			// 如果传入的capacity为0或者负值，则使用默认值5
			if (capacity > 0) {
				this.capacity = capacity;
			}

		}

		// /////////////////////////////////////////////

		@Override
		public V get(Object key) {

			synchronized (lock) {
				return super.get(key);
			}
		}

		@Override
		public V put(K key, V value) {
			synchronized (lock) {
				return super.put(key, value);
			}
		}

		@Override
		public void putAll(Map<? extends K, ? extends V> map) {
			synchronized (lock) {

				super.putAll(map);
			}
		}

		@Override
		public V remove(Object key) {
			synchronized (lock) {
				return super.remove(key);
			}
		}

		@Override
		public void clear() {
			synchronized (lock) {
				super.clear();
			}
		}

		@Override
		public boolean containsKey(Object key) {
			synchronized (lock) {
				return super.containsKey(key);
			}
		}

		@Override
		public int size() {

			synchronized (lock) {
				return super.size();
			}
		}

		@Override
		protected boolean removeEldestEntry(java.util.Map.Entry<K, V> eldest) {
			return size() > capacity;
		}

		// ////////////////////////////////////////////////

		/*
		 * public int getCapcity(){ return capacity; }
		 */

	}

	/**
	 * host的缓存实体
	 * 
	 * @author ethamhuang
	 */
	private class HostEntity {

		public long expireTime = 0;

		public InetAddress[] address = null;

		public String networkType = null;

		/***
		 * 判断缓存是否有效
		 * 
		 * @return
		 */
		public boolean isValid() {
			boolean isValid = System.currentTimeMillis() < expireTime;

			if (isValid) {
				String currNetwork = null;

				if (NetworkDash.isMobile()) {
					currNetwork = NetworkDash.getApnName();
				} else {
					currNetwork = WifiDash.getBSSID();
				}

				isValid = (currNetwork == null ? false : currNetwork
						.equalsIgnoreCase(networkType));
			}

			return isValid;
		}
	}

}

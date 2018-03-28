package com.xyz.common.util;

/**
 * 基于redis setnx的 分布式锁 实, 前提是所有的锁都要有锁定时间.
 * 获取锁的时候,需要指定value,在unlock的时候,会根据value判断是否remove
 * 
 * @author: wxf
 * @createdAt: 2017年7月4日
 */
public class LockUtil {
	private static final String LOCK_PREFIX = "LOCK";
	private static final Integer DEFAULT_LOCK_TIME = 20;// 默认锁定时间秒
	private static final Long DEFAULT_SLEEP_TIME = 100L;// 默认sleep时间,100毫秒

	/**
	 * 获取缓存的value,随机值,使不同的锁value不同 (多服务器可以使用redis时间+客户端标识等)
	 * 
	 * @return
	 * @Author: wxf
	 * @Date: 2017年9月27日
	 */
	public static String getLockValue() {
		int random = (int) ((Math.random() * 9 + 1) * 100000);
		long now = System.currentTimeMillis();
		return String.valueOf(now) + String.valueOf(random);
	}

	/**
	 * 获取锁,如果失败,自动重试
	 * 
	 * @param key
	 * @param value
	 * @return
	 * @Author: wxf
	 * @Date: 2017年7月4日
	 */
	public static void lock(String key, String value) {
		lock(key, value, DEFAULT_LOCK_TIME);
	}

	/**
	 * 获取锁,如果失败,自动重试
	 * 
	 * @param key
	 * @param value
	 * @param lockTime
	 *            获取成功后的锁定时间
	 * @return
	 * @Author: wxf
	 * @Date: 2017年7月4日
	 */
	public static void lock(String key, String value, int lockTime) {
		lock(key, value, lockTime, true);
	}

	private static boolean lock(String key, String value, int lockTime, boolean reTry) {
		return lock(key, value, lockTime, reTry, 0, false, 0);
	}

	/**
	 * 获取锁,如果失败,直接返回false
	 * 
	 * @param key
	 * @param value
	 * @return
	 * @Author: wxf
	 * @Date: 2017年7月4日
	 */
	public static boolean tryLock(String key, String value) {
		return tryLock(key, value, DEFAULT_LOCK_TIME);
	}

	/**
	 * 获取锁,如果失败,直接返回false
	 * 
	 * @param key
	 * @param value
	 * @param lockTime
	 *            获取成功后的锁定时间
	 * @return
	 * @Author: wxf
	 * @Date: 2017年7月4日
	 */
	public static boolean tryLock(String key, String value, int lockTime) {
		return lock(key, value, lockTime, false);
	}

	/**
	 * 尝试获取锁,如果获取失败,重试,直到成功或超出指定时间
	 * 
	 * @param key
	 * @param value
	 * @param lockTime
	 *            获取成功后的锁定时间
	 * @param timeOut
	 *            获取锁等待超时时间
	 * 
	 * @return
	 * @Author: wxf
	 * @Date: 2017年7月4日
	 */
	public static boolean tryLock(String key, String value, int lockTime, long timeOutMillis) {
		return lock(key, value, lockTime, true, 0, true, timeOutMillis);
	}

	/**
	 * 释放锁,key对应的value于参数value一致,才删除key
	 * 
	 * @param key
	 * @param value
	 * @Author: wxf
	 * @Date: 2017年7月4日
	 */
	public static boolean unlock(String key, String value) {
		String fullKey = getFullKey(key);
		boolean success = JedisUtil.unlock(fullKey, value);
		if (success) {
			System.out.println("unlock success ; key:" + key + ",value:" + value);
		} else {
			System.out.println("unlock failed ; key:" + key + ",value:" + value);
		}
		return success;
	}

	/**
	 * 获取锁
	 * 
	 * @param key
	 * @param value
	 * @param lockTime
	 *            锁定时间
	 * @param reTry
	 *            失败是否重试
	 * @param curTryTime
	 *            当前尝试次数
	 * @param needTimeOut
	 *            是否需要判断超时时间
	 * @param timeOutMillis
	 *            尝试超时时间(毫秒)
	 * @return
	 * @Author: wxf
	 * @Date: 2017年7月4日
	 */
	private static boolean lock(String key, String value, int lockTime, boolean reTry, int curTryTime,
			boolean needTimeOut, long timeOutMillis) {
		System.out.println(Thread.currentThread().getName() + ",lock come in ; key:" + key + ",value:" + value
				+ ",lockTime:" + lockTime + ",reTry:" + reTry + ",curTryTime:" + curTryTime + ",needTimeOut:"
				+ needTimeOut + ",timeOutMillis:" + timeOutMillis);
		curTryTime++;
		String fullKey = getFullKey(key);

		// setnx 并设置超时时间
		boolean success = JedisUtil.setnx(fullKey, value, (long) lockTime * 1000);
		// 获取成功,直接返回
		if (success) {
			System.out.println("lock success ; key:" + key + ",value:" + value + ",lockTime:" + lockTime + ",reTry:"
					+ reTry + ",curTryTime:" + curTryTime + ",needTimeOut:" + needTimeOut + ",timeOutMillis:"
					+ timeOutMillis);
			return true;
		}

		// 获取失败,不需要重试,直接返回
		if (!reTry) {
			System.out.println("lock failed ; key:" + key + ",value:" + value + ",lockTime:" + lockTime + ",reTry:"
					+ reTry + ",curTryTime:" + curTryTime + ",needTimeOut:" + needTimeOut + ",timeOutMillis:"
					+ timeOutMillis);
			return false;
		}

		// 获取失败, 且已超时,返回
		if (needTimeOut && timeOutMillis <= 0) {
			System.out.println("lock failed ; key:" + key + ",value:" + value + ",lockTime:" + lockTime + ",reTry:"
					+ reTry + ",curTryTime:" + curTryTime + ",needTimeOut:" + needTimeOut + ",timeOutMillis:"
					+ timeOutMillis);
			return false;
		}

		// 获取sleep时间
		long sleepMillis = getSleepMillis(needTimeOut, timeOutMillis);

		// sleep后重新获取锁
		sleep(sleepMillis);

		// 大于100次,打印warning日志
		if (curTryTime > 100) {
			System.out.println("lock warning ; key:" + key + ",value:" + value + ",lockTime:" + lockTime + ",reTry:"
					+ reTry + ",curTryTime:" + curTryTime + ",needTimeOut:" + needTimeOut + ",timeOutMillis:"
					+ timeOutMillis);
		}

		return lock(key, value, lockTime, reTry, curTryTime, needTimeOut, timeOutMillis);
	}

	private static long getSleepMillis(boolean needTimeOut, long timeOutMillis) {
		long sleepMillis = DEFAULT_SLEEP_TIME;
		if (needTimeOut) {
			timeOutMillis = timeOutMillis - DEFAULT_SLEEP_TIME;
			if (timeOutMillis < DEFAULT_SLEEP_TIME) {
				sleepMillis = timeOutMillis;
			}
		}
		return sleepMillis;
	}

	private static void sleep(long sleepMillis) {
		try {
			Thread.sleep(sleepMillis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static String getFullKey(String key) {
		return LOCK_PREFIX + ":" + key;
	}
}

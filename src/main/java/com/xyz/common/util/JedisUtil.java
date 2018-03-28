package com.xyz.common.util;

import java.util.Collections;
import java.util.Objects;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;

public class JedisUtil {

	private static final Long UNLOCK_SUCCESS = 1L;
	private static JedisPool jedisPool = null;
	static {
		GenericObjectPoolConfig config = new GenericObjectPoolConfig();
		jedisPool = new JedisPool(config, "127.0.0.1", 6379, 6000, "test");
	}

	private static Jedis getJedis() throws JedisException {
		Jedis jedis = null;
		try {
			jedis = jedisPool.getResource();
		} catch (JedisException e) {
			e.printStackTrace();
			throw e;
		}
		return jedis;
	}

	protected static void release(Jedis jedis) {
		jedis.close();
	}

	public static boolean setnx(String key, String value, Long expireMillis) {
		Jedis jedis = null;
		boolean flag = false;
		try {
			jedis = getJedis();
			// nx = not exist, px= 单位是毫秒
			String result = jedis.set(key, value, "NX", "PX", expireMillis);
			if (result != null && result.equalsIgnoreCase("OK")) {
				flag = true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			release(jedis);
		}
		return flag;
	}

	@Deprecated
	public static long setnx(String key, String value) {
		Jedis jedis = null;
		long result = 0L;
		try {
			jedis = getJedis();
			result = jedis.setnx(key, value);
		} catch (Exception e) {
			System.out.println("JedisDao::setnxOld: key: " + key + ",value:" + value + " message: " + e.getMessage());
		} finally {
			release(jedis);
		}
		return result;
	}

	public static boolean unlock(String fullKey, String value) {
//		return unlockV1(fullKey, value);
		return unlockV2(fullKey, value);

	}

	private static boolean unlockV2(String fullKey, String value) {
		Jedis jedis = null;
		boolean flag = false;
		try {
			jedis = getJedis();
			String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
			Object result = jedis.eval(script, Collections.singletonList(fullKey), Collections.singletonList(value));
			if (Objects.equals(UNLOCK_SUCCESS, result)) {
				flag = true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			jedis.unwatch();
			release(jedis);
		}
		return flag;
	}

	private static boolean unlockV1(String fullKey, String value) {
		Jedis jedis = null;
		boolean flag = false;
		try {
			jedis = getJedis();
			jedis.watch(fullKey);
			String existValue = jedis.get(fullKey);
			if (Objects.equals(value, existValue)) {
				jedis.del(fullKey);
				flag = true;
			} else {
				System.out.println("unlock failed ; key:" + fullKey + ",value:" + value + ",existValue:" + existValue);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			jedis.unwatch();
			release(jedis);
		}
		return flag;
	}

}

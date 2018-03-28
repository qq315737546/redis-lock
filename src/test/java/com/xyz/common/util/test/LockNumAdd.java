package com.xyz.common.util.test;

import java.util.concurrent.CountDownLatch;

import com.xyz.common.util.LockUtil;

public class LockNumAdd extends NumAdd {
	final CountDownLatch end;

	public LockNumAdd(int threadCount) {
		resetNum();
		end = new CountDownLatch(threadCount);
	}

	@Override
	public void run() {
		for (int i = 0; i < 100; i++) {
			String key = "testLock";
			String value = LockUtil.getLockValue();
			LockUtil.lock(key, value);
			try {
				addNum();
			} finally {
				LockUtil.unlock(key, value);
			}
		}
		end.countDown();
	}

}

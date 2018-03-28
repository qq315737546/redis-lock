package com.xyz.common.util.test;

import java.util.concurrent.CountDownLatch;

public class NoLockNumAdd extends NumAdd {
	final CountDownLatch end;

	public NoLockNumAdd(int threadCount) {
		resetNum();
		end = new CountDownLatch(threadCount);
	}

	@Override
	public void run() {
		for (int i = 0; i < 100; i++) {
			addNum();
		}
		end.countDown();
	}

}

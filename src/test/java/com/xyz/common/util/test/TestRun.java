package com.xyz.common.util.test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestRun {

	public static void main(String[] args) throws InterruptedException {
		int notLockAdd = testNotLockAdd();
		int lockAdd = testLockAdd();
		System.out.println("notLockAdd = " + notLockAdd);
		System.out.println("lockAdd = " + lockAdd);
	}

	private static int testNotLockAdd() throws InterruptedException {
		int threadCount = 10;
		NoLockNumAdd add = new NoLockNumAdd(threadCount);
		ExecutorService exec = Executors.newFixedThreadPool(threadCount);
		for (int i = 0; i < threadCount; i++) {
			exec.submit(add);
		}
		add.end.await();
		int result = add.getNum();
		exec.shutdown();
		return result;
	}

	private static int testLockAdd() throws InterruptedException {
		int threadCount = 10;
		LockNumAdd add = new LockNumAdd(threadCount);
		ExecutorService exec = Executors.newFixedThreadPool(threadCount);
		for (int i = 0; i < threadCount; i++) {
			exec.submit(add);
		}
		add.end.await();
		int result = add.getNum();
		exec.shutdown();
		return result;
	}

}

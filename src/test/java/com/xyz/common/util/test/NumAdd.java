package com.xyz.common.util.test;

public abstract class NumAdd implements Runnable {
	private int num;

	public int getNum() {
		return num;
	}

	public void addNum() {
		sleep();
		num++;
	}

	public void resetNum() {
		num = 0;
	}

	private void sleep() {
		try {
			Thread.sleep(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}

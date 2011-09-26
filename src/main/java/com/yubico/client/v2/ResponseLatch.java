package com.yubico.client.v2;

import java.util.concurrent.CountDownLatch;

/**
 * 
 * @author Simon Buckle <simon@webteq.eu>
 *
 * @param <T>
 */
public class ResponseLatch<T> {

	private T value = null;
	private final CountDownLatch done = new CountDownLatch(1);
	
	public boolean isSet() {
		return (done.getCount() == 0);
	}
	
	public synchronized void setValue(T newValue) {
		if (!isSet()) {
			value = newValue;
			done.countDown();
		}
	}
	
	public T getValue() throws InterruptedException {
		done.await();
		synchronized (this) {
			return value;
		}
	}
}

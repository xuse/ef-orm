package io.github.xuse.test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class ThreadPoolTest {
	@Test
	public void test1() throws InterruptedException {
		ExecutorService executor = new ThreadPoolExecutor(2, 10, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		final CountDownLatch counter=new CountDownLatch(10); 
		for (int i = 0; i < 10; i++) {
			final int id = i;
			executor.execute(new Runnable() {
				public void run() {
					System.out.println(id + "号线程开始工作");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
					}
					counter.countDown();
				}
			});
		}
		counter.await();

	}
}

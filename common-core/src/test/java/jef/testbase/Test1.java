package jef.testbase;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import org.junit.Test;

public class Test1 {
	private Class c = String.class;

	@Test
	public void getClz1() {
		long start = System.nanoTime();
		for (int i = 0; i < 1000; i++) {
			Class c = String.class;
		}
		System.out.println(System.nanoTime() - start);
	}

	private static Class sc = String.class;
	private static final Class sfc = String.class;

	@Test
	public void getClz1_s() {
		long start = System.nanoTime();
		for (int i = 0; i < 1000; i++) {
			Class c = sfc;
		}
		System.out.println(System.nanoTime() - start);
	}

	@Test
	public void getClz2_s() {// 12225 9300//
		long start = System.nanoTime();
		for (int i = 0; i < 1000; i++) {
			Class c = this.sc;
		}
		System.out.println(System.nanoTime() - start);
	}

	@Test
	public void getClz2() {// 12225 9300//
		long start = System.nanoTime();
		for (int i = 0; i < 1000; i++) {
			Class c = this.c;
		}
		System.out.println(System.nanoTime() - start);
	}

	@Test
	public void getClz3() {// 8660
		Class localc = this.c;
		long start = System.nanoTime();
		for (int i = 0; i < 1000; i++) {
			Class c = localc;
		}
		System.out.println(System.nanoTime() - start);
	}
}

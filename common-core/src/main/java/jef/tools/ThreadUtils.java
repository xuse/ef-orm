/*
 * JEF - Copyright 2009-2010 Jiyi (mr.jiyi@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jef.tools;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 用于并发编程得的若干简单小工具
 * 
 * @author Jiyi
 * 
 */
public abstract class ThreadUtils {
	private final static Logger log=LoggerFactory.getLogger(ThreadUtils.class);
	
	public static final int WAIT_INTERRUPTED = 0;
	public static final int WAIT_TIMEOUT = -1;
	public static final int WAIT_NOTIFIED = 1;

	

	/**
	 * 让出指定对象的锁，并且挂起当前线程。只有当—— <li>1. 有别的线程notify了对象，并且锁没有被其他线程占用。</li> <li>2
	 * 有别的线程interrupt了当前线程。</li> 此方法才会返回。
	 * @param obj 锁所在的对象
	 * @return 等待正常结束返回true，异常结束返回false
	 */
	public static final boolean doWait(Object obj) {
		synchronized (obj) {
			try {
				obj.wait();
				return true;
			} catch (InterruptedException e) {
				log.error("",e);
				return false;
			}
		}
	}

	/**
	 * 等待CountDownLatch数据清零
	 * @param cl
	 * @return
	 */
	public static final boolean doAwait(CountDownLatch cl) {
		try {
			cl.await();
			return true;
		} catch (InterruptedException e) {
			return false;
		}
	}
	
	/**
	 * 调用对象的wait方法，并设置超时时间
	 * @param obj 锁所在的对象
	 * @param timeout 超时时间，单位毫秒
	 * @return  超时返回 {@link #WAIT_TIMEOUT};
	 * 正常唤醒{@link #WAIT_NOTIFIED};
	 * 异常打断{@link #WAIT_INTERRUPTED }
	 */
	public static final int doWait(Object obj, long timeout) {
		synchronized (obj) {
			try {
				long expectTimeout = System.currentTimeMillis() + timeout;
				obj.wait(timeout);
				return System.currentTimeMillis() >= expectTimeout ? WAIT_TIMEOUT : WAIT_NOTIFIED;
			} catch (InterruptedException e) {
				return WAIT_INTERRUPTED;
			}
		}
	}
	
	/**
	 * 在新的线程中运行指定的任务
	 * @param runnable Runnable
	 * @return
	 */
	public static final Thread doTask(Runnable runnable) {
		Thread t = new Thread(runnable);
		t.setDaemon(true);
		t.start();
		return t;
	}

	/**
	 * 唤醒一个在等待obj锁的线程
	 */
	public static final void doNotify(Object obj) {
		synchronized (obj) {
			obj.notify();
		}
	}

	/**
	 * 唤醒所有在等待obj的锁的线程。
	 * 
	 * @param obj
	 */
	public static final void doNotifyAll(Object obj) {
		synchronized (obj) {
			obj.notifyAll();
		}
	}

	/**
	 * 当前线程等待若干毫秒
	 * 
	 * @param l
	 *            毫秒数
	 * @return 如果是正常休眠后返回的true，因为InterruptedException被打断的返回false
	 */
	public static final boolean doSleep(long l) {
		if (l <= 0)
			return true;
		try {
			Thread.sleep(l);
			return true;
		} catch (InterruptedException e) {
			return false;
		}
	}

	/**
	 * 对CountDownLatch进行等待。如果正常退出返回true，异常退出返回false
	 * @param cl CountDownLatch
	 * @return 果正常退出返回true，异常退出返回false
	 */
	public static boolean await(CountDownLatch cl) {
		try {
			cl.await();
			return true;
		} catch (InterruptedException e) {
			return false;
		}
	}

	/**
	 * 对CountDownLatch进行等待。如果正常退出返回true，超时或者异常退出返回false
	 * @param cl CountDownLatch
	 * @param millseconds 超时时间，单位毫秒
	 * @return 如果正常退出true。 如果超时或异常退出false
	 */
	public static boolean await(CountDownLatch cl,long millseconds) {
		try {
			return cl.await(millseconds, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			return false;
		}
	}

	/**
	 * Join到指定的线程进行同步，正常结束返回true
	 * @param thread
	 * @return 如果被Interrupt返回false
	 */
	public static boolean doJoin(Thread thread) {
		try {
			thread.join();
			return true;
		} catch (InterruptedException e) {
			return false;
		}
		
	}
}

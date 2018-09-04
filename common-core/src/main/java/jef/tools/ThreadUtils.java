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

import jef.common.log.LogUtil;
import jef.tools.reflect.UnsafeUtils;

/**
 * 用于并发编程得的若干简单小工具
 * 
 * @author Jiyi
 * 
 */
public abstract class ThreadUtils {
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
				LogUtil.exception(e);
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
	 * 判断当前该对象是否已锁。<br> 注意在并发场景下，这一操作只能反映瞬时的状态，仅用于检测，并不能认为本次检测该锁空闲，紧接着的代码就能得到锁。
	 * 
	 * @param obj
	 * @return
	 */
	@SuppressWarnings("restriction")
	public static boolean isLocked(Object obj) {
		sun.misc.Unsafe unsafe = UnsafeUtils.getUnsafe();
		if (unsafe.tryMonitorEnter(obj)) {
			unsafe.monitorExit(obj);
			return false;
		}
		return true;
	}

	/**
	 * 在执行一个同步方法前，可以手工得到锁。<p>
	 * 
	 * 这个方法可以让你在进入同步方法或同步块之前多一个选择的机会。因为这个方法不会阻塞，如果锁无法得到，会返回false。
	 * 如果返回true，证明你可以无阻塞的进入后面的同步方法或同步块。<p>
	 * 
	 * 要注意，用这个方法得到的锁不会自动释放（比如在同步块执行完毕后不会释放），必须通过调用unlock(Object)方法才能释放。 需小心使用。<p>
	 * 
	 * @param obj
	 * @return 如果锁得到了，返回true，如果锁没有得到到返回false
	 */
	@SuppressWarnings("restriction")
	public static boolean tryLock(Object obj) {
		sun.misc.Unsafe unsafe = UnsafeUtils.getUnsafe();
		return unsafe.tryMonitorEnter(obj);
	}

	/**
	 * 释放因为lock/tryLock方法得到的锁
	 * 
	 * @param obj
	 */
	@SuppressWarnings("restriction")
	public static void unlock(Object obj) {
		sun.misc.Unsafe unsafe = UnsafeUtils.getUnsafe();
		unsafe.monitorExit(obj);
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

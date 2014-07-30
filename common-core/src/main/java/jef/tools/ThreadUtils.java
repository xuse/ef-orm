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

import jef.common.log.LogUtil;
import jef.tools.reflect.UnsafeUtils;

/**
 * 用于并发编程得的若干简单小工具
 * 
 * @author Administrator
 * 
 */
public abstract class ThreadUtils {

	/**
	 * 让出指定对象的锁，并且挂起当前线程。只有当—— <li>1. 有别的线程notify了对象，并且锁没有被其他线程占用。</li> <li>2
	 * 有别的线程interrupt了当前线程。</li> 此方法才会返回。
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
	 * 在新的线程中运行指定的任务
	 * 
	 * @param r
	 * @return
	 */
	public static final Thread doTask(Runnable r) {
		Thread t = new Thread(r);
		t.setDaemon(true);
		t.start();
		return t;
	}

	/**
	 * 调用对象的wait方法，并设置超时时间
	 * 
	 * @param obj
	 * @param timeout
	 */
	public static final boolean doWait(Object obj, long timeout) {
		synchronized (obj) {
			try {
				obj.wait(timeout);
				return true;
			} catch (InterruptedException e) {
				LogUtil.exception(e);
				return false;
			}
		}
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
			LogUtil.exception(e);
			return false;
		}
	}

	/**
	 * 判断当前该对象是否已锁。 注意在并发场景下，这一操作只能反映瞬时的状态，仅用于检测，并不能认为本次检测该锁空闲，紧接着的代码就能得到锁。
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
	 * 在执行一个同步方法前，可以手工得到锁。
	 * 
	 * 这个方法可以让你在进入同步方法或同步块之前多一个选择的机会。因为这个方法不会阻塞，如果锁无法得到，会返回false。
	 * 如果返回true，证明你可以无阻塞的进入后面的同步方法或同步块。
	 * 
	 * 要注意，用这个方法得到的锁不会自动释放（比如在同步块执行完毕后不会释放），必须通过调用unlock(Object)方法才能释放。 需小心使用。
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
}

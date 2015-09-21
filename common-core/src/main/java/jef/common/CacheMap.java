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
package jef.common;

import java.util.LinkedHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 用于存放固定大小缓存的Map，线程安全（仅限get,set两个方法）
 * @author Administrator
 * @param <K>
 * @param <V>
 */
public class CacheMap<K, V> extends LinkedHashMap<K, V> {
	private static final long serialVersionUID = 2428383992533927687L;
	private static final float DEFAULT_LOAD_FACTOR = 1f;
	
	/**
	 *<B>关于使用互斥锁而不是读写锁的原因</B>
	 * 这个类作为缓存，为了防止将最近访问过的对象清除出去，所以要记录访问顺序
	 * 由于缓存表在每次get操作时要记录访问顺序(accessOrder = true)，修改内部链表结构，因此即使是get操作也是不应当并发的。
	 * 
	 */
	private final Lock lock = new ReentrantLock();
	
	private final int maxCapacity;

	public CacheMap(int size) {
		super(size, DEFAULT_LOAD_FACTOR, true);
		this.maxCapacity = size;
	}

	
	protected final boolean removeEldestEntry(java.util.Map.Entry<K, V> eldest) {
		return size() > maxCapacity;
	}

	
	public V get(Object key) {
		try {
			lock.lock();
			return super.get(key);
		} finally {
			lock.unlock();
		}
	}

	
	public V put(K key, V value) {
		try {
			lock.lock();
			return super.put(key, value);
		} finally {
			lock.unlock();
		}
	}
}

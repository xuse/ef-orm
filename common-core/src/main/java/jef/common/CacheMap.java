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
	
	//<B>关于使用互斥锁而不是读写锁的原因</B>
	//一般来说读写锁可以允许并发读取访问，因此效率更高，但是此处不使用读写锁而使用互斥锁是因为：
	//1、由于缓存表在每次get操作时要记录访问顺序(accessOrder = true)，修改内部链表结构，因此即使是get操作也是不应当并发的。
	//这个类作为缓存，为了防止将最近访问过的对象清除出去，所以要记录访问顺序
	//2、读写锁结构更复杂，锁闭本身开销可能更大。实测发现使用读写锁的读锁，比普通锁稍慢。
	//3、如果不使用按访问顺序排序方式，则可以考虑使用读写锁以提高吞吐量。
	//4、测试表明，在Map的多线程同步方式上，synchronized 关键字非常慢，比使用锁慢了两个数量级。
	//因此如果考虑重新实现addEntry方法，将recordAccess方法加上synchronized关键字，可能会更慢，不适合采用此方法
	//5、Map同步时ConcurrentHashMap 是最快的，因为它将存储分成多块使用多个锁。综合上述对锁的分析，此处决定使用互斥锁。
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

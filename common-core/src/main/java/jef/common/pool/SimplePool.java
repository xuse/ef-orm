package jef.common.pool;

import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jef.tools.Assert;
import jef.tools.Exceptions;


/**
 * 默认实现.不支持重入
 * 
 * @author jiyi
 * 
 * @param <T>
 */
public class SimplePool<T> implements ObjectPool<T>, Releasable {
	private int max;
	private int min;
	private BlockingQueue<T> freeConns;
	private AtomicInteger used = new AtomicInteger();// 被取走的连接数
	protected ObjectFactory<T> factory;

	/**
	 * 构造
	 * 
	 * @param max
	 *            最大值
	 * @param min
	 *            最小值
	 */
	public SimplePool(ObjectFactory<T> fac, int min, int max) {
		if (min > max)
			min = max;
		this.min = min;
		this.max = max;
		this.factory = fac;
		Assert.notNull(fac);
		freeConns = new LinkedBlockingQueue<T>(max);
		ReleaseThread.getInstance().addPool(this);
	}

	public T poll() {
		try {
			T conn;
			if (freeConns.isEmpty() && used.get() < max) {// 尝试用新连接
				used.incrementAndGet(); //必须立刻累加计数器，否则并发的线程会立刻抢先创建对象，从而超出连接池限制
				conn = factory.create();
			} else {
				conn = freeConns.poll(5000000000L, TimeUnit.NANOSECONDS);// 5秒
				if (conn == null) {
					throw new IllegalStateException("No object avaliable now.");
				}
				used.incrementAndGet();
				conn = factory.ensureOpen(conn);
			}
			return conn;
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public void offer(T conn) {
		boolean success = freeConns.offer(conn);
		used.decrementAndGet();
		if (!success) {
			factory.release(conn);// 塞不下了。肯定是关闭掉
		}
	}

	/**
	 * 获得对象池状态
	 * 
	 * @return
	 */
	public PoolStatus getStatus() {
		int used = this.used.get();
		int free = freeConns.size();
		return new PoolStatus(max, min, used + free, used, free);
	}

	/**
	 * 收缩对象池
	 */
	public synchronized void releaseTillMinSize() {
//		System.out.println("开始收缩"+ freeConns.size()+":"+ used.get());
		if (freeConns.size() > min) {
			T conn;
			while ((conn = freeConns.poll()) != null && freeConns.size() > min) {
				try {
					factory.release(conn);
				} catch (Exception e) {
					Exceptions.log(e);
				}
			}
		}
//		System.out.println("结束收缩"+ freeConns.size()+":"+ used.get());
	}

	/**
	 * 关闭对象池
	 * 
	 * @throws SQLException
	 */
	public void closePool() {
		ReleaseThread.getInstance().removePool(this);
		max = 0;
		min = 0;
		releaseTillMinSize();
		
		
	}
}

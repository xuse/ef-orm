package jef.common.pool;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.collect.MapMaker;

/**
 * 高性能重入对象池实现
 * 
 * 支持重入
 * @author jiyi
 *
 */
public final class ReentrantPool<T> extends SimplePool<T>{
	//重入记录
	final Map<Object, Wrapper<T>> map = new MapMaker().concurrencyLevel(12).weakKeys().makeMap();
	private final AtomicLong pollCount = new AtomicLong();
	private final AtomicLong offerCount = new AtomicLong();
	
	/**
	 * 构造
	 * @param fac 重入对象
	 * @param min
	 * @param max
	 */
	public ReentrantPool(ObjectFactory<T> fac,int min,int max){
		super(fac,min,max);
	}

	public T poll() {
		pollCount.incrementAndGet();
		Thread user = Thread.currentThread();
		Wrapper<T> conn = map.get(user);
		if (conn == null) {
			conn = new Wrapper<T>(super.poll());
			map.put(user, conn);
			conn.setUsedByObject(user);
		} else {
			conn.addUsedByObject();
		}
		return conn.obj;
	}

	public void offer(T o) {
		offerCount.incrementAndGet();
		Thread user = Thread.currentThread();
		Wrapper<T> conn = map.get(user);
		if(conn==null || conn.obj!=o){//不是在同一个线程中归还，问题复杂了。
			conn=null;
			for(Wrapper<T> w:map.values()){
				if(w.obj==o){
					conn=w;
				}
			}
			if(conn==null){//无法定位该记录是否重入。。。
				throw new IllegalStateException("The object "+o+" is unknown in reentrant map.");
			}
		}
		
		Object u = conn.popUsedByObject();
		if (u == null)
			return;// 不是真正的归还
		Wrapper<T> conn1 = map.remove(u);
		if (conn1 != conn) {
			//never happens.
			throw new IllegalStateException("The connection returned not match.");
		}
		super.offer(conn1.obj);
		
	}

	public void closePool() {
		super.closePool();
		for(Wrapper<T> wrapper:map.values()){
			T obj=wrapper.obj;
			if(obj!=null){
				factory.release(obj);
			}
		}
	}
	

	/**
	 * 获得对象池状态
	 * 
	 * @return
	 */
	public PoolStatus getStatus() {
		PoolStatus st=super.getStatus();
		st.setOfferCount(offerCount.get());
		st.setPollCount(pollCount.get());
		return st;
	}
	
	private static class Wrapper<T>{
		private T obj;
		private volatile Object used;
		private volatile int count;
		
		public Wrapper(T create) {
			this.obj=create;
		}

		void setUsedByObject(Object user) {
			this.used=user;
			count++;
		}
		
		Object popUsedByObject() {
			if(--count>0){ 
				//log.debug("不是真正的归还{}还有{}次使用.",used,count);
				return null;
			}else{
				Object o=used;
				used=null;
				return o;
			}
		}

		void addUsedByObject() {
			count++;
		}
	}
}

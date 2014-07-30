package jef.tools;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import jef.tools.string.RandomData;

import org.easyframe.fastjson.util.IdentityHashMap;
import org.junit.Test;

import com.google.common.collect.MapMaker;

/**
 * 性能测试
 * @author jiyi
 *
 */
public class CollectionTest {

	int LOOP=100000;
	private String[] xx=new String[LOOP];
	private int threadCount=8;
	
	@Test
	public void testIdentitySet() throws InterruptedException{
		IdentityHashMap<String,Object> m1=new IdentityHashMap<String,Object>(1024);
		Map<String,Object> m2=new HashMap<String,Object>(1024);
		Map<String,Object> m3=new java.util.IdentityHashMap<String,Object>(1024);
		
		Map<String,Object> m4=new MapMaker().concurrencyLevel(8).initialCapacity(1024).makeMap();
		Map<String,Object> m5=new ConcurrentHashMap<String, Object>(1024);
		Map<String,Object> m6=new Hashtable<String, Object>(1024);
		
		
		warmup();
		for(int i=0;i<LOOP;i++){
			xx[i]=RandomData.randomString(10);
		}

		testMap_(m1);
		if(threadCount==1){
			testMap(m2,"JDK HashMap");  //Will get deadlock under multiple-threads.	
		}
		testMap(m3,"JDK IdentityHashMap             ");
		testMap(m4,"Guava Mapmaker.concurrencyLevel(8)");
		testMap(m5,"JDK ConcurrentHashMap           ");
		testMap(m6,"JDK Hashtable                   ");
	}
	
	private void testMap(Map<String, Object> m2,String name) throws InterruptedException {
		T[] threads=new T[threadCount];
		CountDownLatch c=new CountDownLatch(threadCount);
		for(int i=0;i<threadCount;i++){
			T t=new T();
			threads[i]=t;
			t.m=m2;
			t.c=c;
			t.start();
		}
		c.await();
		long cost1=0;
		long cost2=0;
		for(int i=0;i<threadCount;i++){
			cost1+=threads[i].cost1;
			cost2+=threads[i].cost2;
		}
		m2.clear();
		System.gc();
		System.out.println(name+"\t：" + cost1/threadCount +" "+ cost2/threadCount);
		
	}

	private void testMap_(IdentityHashMap<String, Object> m1) throws InterruptedException {
		T_[] threads=new T_[threadCount];
		CountDownLatch c=new CountDownLatch(threadCount);
		for(int i=0;i<threadCount;i++){
			T_ t=new T_();
			threads[i]=t;
			t.m=m1;
			t.c=c;
			t.start();
		}
		c.await();
		long cost1=0;
		long cost2=0;
		for(int i=0;i<threadCount;i++){
			cost1+=threads[i].cost1;
			cost2+=threads[i].cost2;
		}
		System.out.println("Alibaba Fastjson IdentityHashMap\t：" + cost1/threadCount +" "+ cost2/threadCount);
	}

	private class T extends Thread{
		private CountDownLatch c;
		private Map<String,Object> m;
		private long cost1;
		private long cost2;
		@Override
		public void run() {
			//准备
			Object value=new Object();
			int loop=LOOP;
			String[] xx=CollectionTest.this.xx;
			//Go!!
			long s=System.nanoTime();
			for(int i=0;i<loop;i++){
				m.put(xx[i], value);
			}
			long end=System.nanoTime();
			
			
			long s2=System.nanoTime();
			for(int i=0;i<loop;i++){
				m.get(xx[i]);
			}
			long end2=System.nanoTime();
			cost1=end-s;
			cost2=end2-s2;
			
//			System.out.print(Thread.currentThread().getName()+"开始1:"+s+"  结束1:"+end+" - "+cost1+"   ");
//			System.out.println(Thread.currentThread().getName()+"开始2:"+s2+"  结束2:"+end2+" - "+cost2);
			c.countDown();
		}
		
	}
	private class T_ extends Thread{
		private CountDownLatch c;
		private IdentityHashMap<String,Object> m;
		private long cost1;
		private long cost2;
		@Override
		public void run() {
			//准备
			Object value=new Object();
			int loop=LOOP;
			String[] xx=CollectionTest.this.xx;
			//Go!!
			long s=System.nanoTime();
			for(int i=0;i<loop;i++){
				m.put(xx[i], value);
			}
			long end=System.nanoTime();
			
			
			long s2=System.nanoTime();
			for(int i=0;i<loop;i++){
				m.get(xx[i]);
			}
			long end2=System.nanoTime();
			cost1=end-s;
			cost2=end2-s2;
			
//			System.out.print(Thread.currentThread().getName()+"开始1:"+s+"  结束1:"+end+" - "+cost1+"   ");
//			System.out.println(Thread.currentThread().getName()+"开始2:"+s2+"  结束2:"+end2+" - "+cost2);
			c.countDown();
		}
		
	}
	/**
	 * 预热，防止Intal睿频技术造成CPU在测试开始后由低频专向高频
	 */
	private void warmup() {
		int i=0;
		for(int j=1;j<=10000;j++){
			i+=j;
		}
	}
	
}

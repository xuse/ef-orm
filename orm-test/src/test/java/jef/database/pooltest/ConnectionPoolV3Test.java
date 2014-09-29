package jef.database.pooltest;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import jef.database.DbUtils;
import jef.database.innerpool.IConnection;
import jef.database.innerpool.IUserManagedPool;
import jef.database.innerpool.PoolService;
import jef.tools.ThreadUtils;

import org.easyframe.enterprise.spring.TransactionMode;
import org.junit.Test;
/**
 * 测试连接池性能
 * @throws SQLException 
 */
public class ConnectionPoolV3Test {
	private static final int THREADS=100;
	private static final int LOOPS=800;


	
	@Test
	public void testPoolV2() throws SQLException{
		DataSource ds=DbUtils.createSimpleDataSource("jdbc:derby:./db", "", "");
		final IUserManagedPool pool=PoolService.getPool(ds, 20,TransactionMode.JPA);
		doTest(pool);
	}

	private void doTest(IUserManagedPool pool) throws SQLException {
		System.out.println("连接池初始:"+ pool.getStatus());
		long start=System.currentTimeMillis();
		Thread[] threads=new Thread[THREADS];
		for(int i=0;i<THREADS;i++){
			threads[i]=new DoThread(pool,i,start,this);
		}
		for(int i=0;i<THREADS;i++){
			threads[i].start();
		}
		
		while(count.get()<THREADS){
			ThreadUtils.doWait(pool);
			System.out.println("当前连接池数量:"+ pool.getStatus());
		}
		System.out.println("All thread finished!" + (System.currentTimeMillis()-start)+"ms");
		pool.close();
	}
	private final AtomicInteger count=new AtomicInteger(0);
	static class DoThread extends Thread{
		private IUserManagedPool pool;
		private int id;
		private long start;
		private ConnectionPoolV3Test v3;
		public DoThread(IUserManagedPool pool, int id,long start,ConnectionPoolV3Test v3) {
			this.v3=v3;
			this.id=id;
			this.pool=pool;
			this.start=start;
		}
		@Override
		public void run() {
			for(int i=0;i<LOOPS;i++){
				try {
					IConnection conn=pool.poll();
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
					System.out.println("thread has exception:"+id);
					v3.count.incrementAndGet();
					ThreadUtils.doNotify(pool);
					return;
				}
			}
//			long cost=System.currentTimeMillis()-start;
//			System.out.println("Thread " +id+" done , cost="+cost+"ms.");
			v3.count.incrementAndGet();
			ThreadUtils.doNotify(pool);
		}
	}
	
}

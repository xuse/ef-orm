package jef.database.pooltest;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import jef.database.innerpool.IConnection;
import jef.database.innerpool.IUserManagedPool;
import jef.database.innerpool.ReentrantConnection;
import jef.tools.ThreadUtils;
import junit.framework.Assert;

import org.junit.Before;
import org.junit.Ignore;
import org.slf4j.LoggerFactory;


/**
 * case603
 * 模拟真实场景 很多线程一起跑很长一段时间不断的切换
 * 		有释放连接，关闭连接，查询表，checker线程checkSql
 * 
 * 检查用户线程和check线程使用线程池中的同一个连接执行sql是否会产生异常
 * 
 * 释放连接的时候一定要保证让测试线程休眠否则它会和已经在等待的线程又争取资源
 * 这里让poll()时候 gc的地方休眠的时间等于测试线程释放连接休眠的时间
 * 而且线程的个数等于线程池大小的两倍
 * 这样能够保证每个线程不会因获取不到连接抛出sqlException
 * 
 * 连接池大小为10   线程数量20  线程休眠时间为一次gc等待的时间  
 * 设置jef.properties db.heartbeat.autostart=true
 * @author zhaolong
 *
 */
public class Conn603MixThread extends AbstractTestConnection{
	
	private final int CONN_LOOPS=ConnDBConfigUtil.getIntValue("thread.num",20);
	//线程计数
	private final AtomicInteger count = new AtomicInteger(0);
	//异常计数
	final AtomicInteger countException = new AtomicInteger(0);
	private org.slf4j.Logger log=LoggerFactory.getLogger(Conn603MixThread.class);
	
	@Before
	public void prepare()
			throws SQLException {
//		 TODO Auto-generated method stub
		String url = ConnDBConfigUtil.getStringValue("url");
		String uname = ConnDBConfigUtil.getStringValue("uname");
		String pwd = ConnDBConfigUtil.getStringValue("pwd");
		int POOL_SIZE=ConnDBConfigUtil.getIntValue("pool.size");
		super.prepare(url, uname, pwd, POOL_SIZE);
		ConnPrintOutUtil.print(log,ConnPrintOutUtil.INFO,super.getPool().toString());
	}

	

//	@Test
	@Ignore
	public void testThread() throws InterruptedException{
		IUserManagedPool pool=super.getPool();
		MyThread[] threads=new MyThread[CONN_LOOPS];
		for (int j = 0; j < CONN_LOOPS; j++) {
			threads[j] = new MyThread(pool, j, true,count);
		}
		for(int j=0;j<CONN_LOOPS;j++){
			threads[j].start();
		}
		
		while(count.get()<CONN_LOOPS && countException.get()==0){
			//实时监控连接池的状态
			String msg="当前连接池数量:" + pool.getStatus();
			ConnPrintOutUtil.print(log, ConnPrintOutUtil.INFO, msg);
			ThreadUtils.doWait(super.getPool());
		}
		Thread.sleep(1000);
		//如果结果不抛出异常 则说明case正确
		if(countException.get()==0){
			ConnPrintOutUtil.printSuccess(log);
		}else{
			ConnPrintOutUtil.printFailure(log);
		}
		
	}
	
	
	
	class MyThread extends Thread {

		private IUserManagedPool pool;
		private int index;
		private boolean isRelease = true;
		private AtomicInteger count;
		private Random rd=new Random();

		public MyThread() {
			// TODO Auto-generated constructor stub
		}

		public MyThread(IUserManagedPool pool, int index,boolean isRelease,AtomicInteger count) {
			// TODO Auto-generated constructor stub
			this.pool = pool;
			this.index = index;
			this.isRelease=isRelease;
			this.count=count;
		}

	

		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				
				for(int i=0;i<100;i++){
					//获取连接
//					pool.getConnection(index);
					ReentrantConnection conn=pool.poll();
					
					//进行休眠
					int t=rd.nextInt(50)+1000;
					Thread.sleep(t);
					
					//查询表并且释放连接
					consultTable();
					if (isRelease) {
						pool.offer(conn);
					}
					Thread.sleep(1*1000);
					if(i%10==0){
						ThreadUtils.doNotify(pool);
					}
				}
				
			} catch (SQLException e) {
				// TODO Auto-generated catch blockx
				String msg="thread " + index + " has exception";
				ConnPrintOutUtil.print(log,ConnPrintOutUtil.WARN , msg);
				countException.incrementAndGet();
				count.incrementAndGet();
				ThreadUtils.doNotify(pool);
			}catch (Exception e) {
				// TODO: handle exception
				ConnPrintOutUtil.print(log, ConnPrintOutUtil.ERROR, e.getLocalizedMessage());
				ConnPrintOutUtil.printFailure(log);
			}
			count.incrementAndGet();
			ThreadUtils.doNotify(pool);
		}
		
		
		public void  consultTable()throws SQLException{
			String sql="select count(*) from DEPT";
			IConnection conn=pool.poll();
			PreparedStatement ps=conn.prepareStatement(sql);
			ResultSet rs=ps.executeQuery();
			if(rs.next()){
				if(ConnPrintOutUtil.isOnServer()){
					Assert.assertEquals(4, rs.getInt(1));
				}
			}
		}

	}

	

}

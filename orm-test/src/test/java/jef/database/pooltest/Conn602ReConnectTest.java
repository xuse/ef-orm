package jef.database.pooltest;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import jef.database.innerpool.IConnection;
import jef.database.innerpool.IUserManagedPool;
import jef.tools.ThreadUtils;
import junit.framework.Assert;

import org.junit.Before;
import org.junit.Ignore;
import org.slf4j.LoggerFactory;

/**
 * case602
 * 	连接池内的10线程拿到连接休眠 其余10个线程等待连接
 * 	休眠完后释放连接 
 * 	
 * 	连接池大小10    线程数量20      休眠时间小于5*gc时间
 * @author zhaolong
 *
 */
public class Conn602ReConnectTest extends AbstractTestConnection{
	
	org.slf4j.Logger log=LoggerFactory.getLogger(Conn602ReConnectTest.class);
	private AtomicInteger countThread = new AtomicInteger(0);
	final AtomicInteger countException = new AtomicInteger(0);
	
	@Before
	public void prepare()
			throws SQLException {
		String url = ConnDBConfigUtil.getStringValue("url");
		String uname = ConnDBConfigUtil.getStringValue("uname");
		String pwd = ConnDBConfigUtil.getStringValue("pwd");
		int POOL_SIZE=ConnDBConfigUtil.getIntValue("pool.size");
		super.prepare(url, uname, pwd, POOL_SIZE);
	}
	
//	@Test
	@Ignore
	public void testExceedMax() throws InterruptedException{
		
		IUserManagedPool pool=super.getPool();
		
		//获取线程数量并且打印
		int ThreadNum=ConnDBConfigUtil.getIntValue("thread.num",10);
		ConnPrintOutUtil.print(log, ConnPrintOutUtil.INFO, "thread.num:"+ThreadNum);
		
		//启动线程
		MyThread[] threads=new MyThread[ThreadNum];
		for (int j = 0; j < ThreadNum; j++) {
			threads[j] = new MyThread(pool, j,countThread,true);
		}
		for(int j=0;j<ThreadNum;j++){
			Thread.sleep(100);
			threads[j].start();
		}
		
		//如果所有的线程执行完
		while (countThread.get() < ThreadNum) {
			ThreadUtils.doWait(pool);
		}
		Thread.sleep(1000);
		
		if(!ConnPrintOutUtil.isOnServer()){
			Assert.assertEquals(countException.get(), 0);
		}
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
		AtomicInteger countThread = new AtomicInteger(0);
		

		public MyThread() {
			// TODO Auto-generated constructor stub
		}

		public MyThread(IUserManagedPool pool,int index,AtomicInteger countThread
				) {
			// TODO Auto-generated constructor stub
			this.pool = pool;
			this.index=index;
			this.countThread=countThread;
		}

		public MyThread(IUserManagedPool pool,int index,AtomicInteger countThread, boolean isRelease) {
			// TODO Auto-generated constructor stub
			this(pool,index,countThread);
			this.isRelease = isRelease;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				//获取连接
				IConnection conn=pool.poll();
				Thread.sleep(4*1000);
				if (isRelease) {
					//释放连接
					conn.close();
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch blockx
				e.printStackTrace();
				countException.incrementAndGet();
				countThread.incrementAndGet();
				ThreadUtils.doNotify(pool);
				ConnPrintOutUtil.print(log,ConnPrintOutUtil.INFO , "thread " + index + " has exception");
			}catch(Exception e){
				
			}
			countThread.incrementAndGet();
			ThreadUtils.doNotify(pool);
		}

	}

}

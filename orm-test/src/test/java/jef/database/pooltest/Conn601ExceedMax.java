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
 * case601
 * 	连接池内的连接达到最大数量后，新线程获取连接将产生超时异常
 * 
 * 	参数连接池大小10     线程数量11
 * @author zhaolong
 *
 */
public class Conn601ExceedMax extends AbstractTestConnection{
	
	org.slf4j.Logger log=LoggerFactory.getLogger(Conn601ExceedMax.class);
	private AtomicInteger countThread = new AtomicInteger(0);
	final AtomicInteger countException = new AtomicInteger(0);
	
	@Before
	public void prepare()
			throws SQLException {
//		int POOL_SIZE=10;
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
		
		//获取线程号并且打印
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
			Assert.assertEquals(countException.get(), 1);
		}
		if(countException.get()==1){
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
				IConnection conn=pool.poll();
				Thread.sleep(20*1000);
				if (isRelease) {
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

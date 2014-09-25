package jef.database.pooltest;


import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jef.database.innerpool.IConnection;
import jef.database.innerpool.IUserManagedPool;
import junit.framework.Assert;

import org.junit.Before;
import org.junit.Ignore;
import org.slf4j.LoggerFactory;

/**
 * case608
 * 测试连接超时之后 checker线程是否可以工作将其置空
 *    由于超时断开了但是是同一个线程，所以返回原来的连接，checker进行检查将其置空 
 *    而重新使用pool.getConnection()的时候可以获取新的连接
 * 
 * 注意这个case要把测试线程休眠的时间开的长点,checker线程开的休眠时间要短
 *    因为测试线程休眠时间足够长的话，保证能够pool的连接连接超时
 *    checker线程开的时间短的话，保证测试线程再次调用getConnection的时候已经检查过了
 *    
 *    不用考虑再次获取连接的时间超时问题，因为我们getConnection的时候是重新获取新的连接的
 *    而且checker线程在检查的时候 即使超时也没有关系因为这正是我们想要的操作
 *    
 *    
 *    设置jef.properties db.heartbeat.autostart=true
 *    1个线程即可      设置oracle连接时间为1min
 *    
 * @author zhaolong
 *
 */
public class Conn608CheckerWorkTest extends AbstractTestConnection{
	
	private Integer countException=0;
	private org.slf4j.Logger log=LoggerFactory.getLogger(Conn608CheckerWorkTest.class);
	
	@Before
	public void prepare()
			throws SQLException {
//		 TODO Auto-generated method stub
		String url = ConnDBConfigUtil.getStringValue("url");
		String uname = ConnDBConfigUtil.getStringValue("uname");
		String pwd = ConnDBConfigUtil.getStringValue("pwd");
		int POOL_SIZE=ConnDBConfigUtil.getIntValue("pool.size");
		super.prepare(url, uname, pwd, POOL_SIZE);
	}
	
	
//	@Test
	@Ignore
	public void testCheckerWork() throws SQLException, InterruptedException{

		IUserManagedPool pool=super.getPool();
		MyThread thread=new MyThread(pool);
		thread.start();
		Thread.sleep(6*60*1000);
		
		if(ConnPrintOutUtil.isOnServer()){
			Assert.assertEquals(new Integer(0), countException);
		}
		if(countException==0){
			ConnPrintOutUtil.printSuccess(log);
		}else{
			ConnPrintOutUtil.printFailure(log);
		}
	}
	
	
	class MyThread extends Thread {

		private IUserManagedPool<IConnection> pool;

		public MyThread() {
			// TODO Auto-generated constructor stub
		}

		public MyThread(IUserManagedPool pool) {
			// TODO Auto-generated constructor stub
			this.pool = pool;
		}

	

		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				consultTable();
				//休眠等待断开
				Thread.sleep(5*60*1000);
				
				consultTable();
			} catch (SQLException e) {
				e.printStackTrace();
				// TODO Auto-generated catch blockx
				countException=countException+1;
			}catch (Exception e2) {
				e2.printStackTrace();
				// TODO: handle exception
				countException=countException+1;
			}
		}
		
		
		public void  consultTable()throws SQLException{
			String sql="select count(*) from DEPT";
			IConnection conn=pool.poll();
			PreparedStatement ps=conn.prepareStatement(sql);
			ResultSet rs=ps.executeQuery();
			if(rs.next()){
				//此处的结果可能需要更改
				ConnPrintOutUtil.print(log, ConnPrintOutUtil.INFO, "rows:"+rs.getInt(1));
				if(ConnPrintOutUtil.isOnServer()){
					Assert.assertEquals(4, rs.getInt(1));
				}
			}
		}

	}

}

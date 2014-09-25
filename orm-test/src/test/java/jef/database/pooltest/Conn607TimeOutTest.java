package jef.database.pooltest;


import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import jef.database.innerpool.IConnection;
import jef.database.innerpool.IUserManagedPool;

import org.junit.Before;
import org.junit.Ignore;
import org.slf4j.LoggerFactory;

/**
 * case607
 * 测试超时重连
 * 	由于超时断开了但是是同一个线程，所以返回原来的连接，但是这个连接已经不能够使用。
 * 
 *  设置jef.properties db.heartbeat.autostart=false
 *  设置db连接时间为1min 线程休眠时间为4min
 * @author zhaolong
 *
 */
public class Conn607TimeOutTest extends AbstractTestConnection{
	
	org.slf4j.Logger log=LoggerFactory.getLogger(Conn607TimeOutTest.class);
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
	public void testPoolV2() throws InterruptedException{
		IUserManagedPool<IConnection> pool=super.getPool();

		consultTable(pool);
		Thread.sleep(4*60*1000);
		consultTable(pool);
		
		
		//如果不抛异常 则说明case运行成功
		if(countException.get()==0){
			ConnPrintOutUtil.printSuccess(log);
		}else{
			ConnPrintOutUtil.printFailure(log);
		}
	}
	
	private void  consultTable(IUserManagedPool<IConnection> pool){
		try{
			String sql="select count(*) from DEPT";
			IConnection conn=pool.poll();
			PreparedStatement ps=conn.prepareStatement(sql);
			ResultSet rs=ps.executeQuery();
			if(rs.next()){
				ConnPrintOutUtil.print(log, ConnPrintOutUtil.INFO, "rows:"+rs.getInt(1));
			}
		}catch (SQLException e) {
			// TODO: handle exception
			ConnPrintOutUtil.print(log, ConnPrintOutUtil.ERROR, "sqlException.............");
			countException.incrementAndGet();
		}catch (Exception e) {
			// TODO: handle exception
		}
	}
	

}

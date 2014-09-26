package jef.database.pooltest;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jef.database.innerpool.IConnection;
import jef.database.innerpool.IUserManagedPool;

import org.junit.Before;
import org.junit.Ignore;
import org.slf4j.LoggerFactory;


/**
 * case600
 * 测试同一个线程拿到的连接是否相同
 * 
 * 	连接池大小随意  线程数量1
 * @author zhaolong
 *
 */
public class Conn600IsSameTest extends AbstractTestConnection{
	
	private final int LOOPS =10;
	//判断是否在服务器上 还是eclipse
	org.slf4j.Logger log=LoggerFactory.getLogger(Conn600IsSameTest.class);
	
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
	public void  testIsSame()throws SQLException{
		IUserManagedPool pool=super.getPool();
		//获取刚开始的连接
		IConnection preInPool=pool.poll();
		String table=ConnDBConfigUtil.getStringValue("table.name","DEPT");
		String sql="select count(*) from "+table;
		//循环调用多次 查看得到的连接是否是同一个连接
		for(int i=0;i<LOOPS;i++){
			IConnection afterInPool=pool.poll();
			IConnection conn=afterInPool;
			PreparedStatement ps=conn.prepareStatement(sql);
			ResultSet rs=ps.executeQuery();
			if(rs.next()){
				if(i==0|| i==LOOPS-1){
					ConnPrintOutUtil.print(log, ConnPrintOutUtil.INFO, "rows:"+rs.getInt(1));
				}
			}
			if(afterInPool!=preInPool){
				ConnPrintOutUtil.printFailure(log);
			}
		}
		ConnPrintOutUtil.printSuccess(log);
	}
	

}

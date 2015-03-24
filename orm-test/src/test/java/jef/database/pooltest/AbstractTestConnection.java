package jef.database.pooltest;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.easyframe.enterprise.spring.TransactionMode;

import jef.database.DbUtils;
import jef.database.innerpool.IConnection;
import jef.database.innerpool.IUserManagedPool;
import jef.database.innerpool.PoolService;
import junit.framework.Assert;
/**
 * 连接的抽象类
 * @author zhaolong
 *
 */
public class AbstractTestConnection {
	
	private DataSource ds=null;
	private IUserManagedPool pool=null;

	//连接数据源
	public void prepare(String url,String uname,String pwd,int POOL_SIZE) throws SQLException{
		ds = DbUtils.createSimpleDataSource(url, uname, pwd);
		pool = PoolService.getPool(ds, POOL_SIZE,POOL_SIZE,TransactionMode.JPA);
		System.out.println(pool.toString());
	}
	
	//通用的测试方法
	public void  consultTable()throws SQLException{
		String sql="select count(*) from DEPT";
		IConnection conn=pool.poll();
		PreparedStatement ps=conn.prepareStatement(sql);
		ResultSet rs=ps.executeQuery();
		if(rs.next()){
			System.out.println(rs.getInt(1));
			Assert.assertEquals(4, rs.getInt(1));
		}
	}


	public IUserManagedPool getPool() {
		return pool;
	}


	public void setPool(IUserManagedPool pool) {
		this.pool = pool;
	}


	public DataSource getDs() {
		return ds;
	}


	public void setDs(DataSource ds) {
		this.ds = ds;
	}

}

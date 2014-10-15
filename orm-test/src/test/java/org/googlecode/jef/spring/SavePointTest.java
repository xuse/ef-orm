package org.googlecode.jef.spring;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Savepoint;

import javax.sql.DataSource;

import jef.common.log.LogUtil;
import jef.database.DbUtils;
import jef.database.dialect.AbstractDialect;
import jef.database.test.JDBCUtil;

import org.junit.Test;



public class SavePointTest {
	@Test
	public void testMetadata() throws SQLException{
		String url=AbstractDialect.getProfile("mysql").generateUrl("localhost", 3307, "test");
		DataSource ds=DbUtils.createSimpleDataSource(url, "root", "admin");
		System.out.println(url);
		Connection conn=ds.getConnection();
		
		DatabaseMetaData meta=conn.getMetaData();
		LogUtil.show(meta.getCatalogs());
		
//	    int TRANSACTION_NONE	     = 0;
//	    int TRANSACTION_READ_UNCOMMITTED = 1;
//	    int TRANSACTION_READ_COMMITTED   = 2;
//	    int TRANSACTION_REPEATABLE_READ  = 4;
//	    int TRANSACTION_SERIALIZABLE     = 8;
		System.out.println(meta.getDefaultTransactionIsolation());//返回当前数据局默认隔离级别
		
		System.out.println(meta.getURL());
		System.out.println(meta.getUserName());
		LogUtil.show(meta.getTypeInfo());
	}
	
	
	@Test
	public void testSp() throws SQLException{
		String url=AbstractDialect.getProfile("mysql").generateUrl("localhost", 3307, "test");
		DataSource ds=DbUtils.createSimpleDataSource(url, "root", "admin");
		Connection conn=ds.getConnection();
		
		if(!JDBCUtil.existTable(conn, "tt")){
			JDBCUtil.execute(conn, "create table tt(id integer,name varchar(10))");
		}
		System.out.println("Support:"+conn.getMetaData().supportsSavepoints());
		conn.setAutoCommit(false);
		JDBCUtil.execute(conn,"insert into tt values (1,'a'),(2,'x')");
		Savepoint sp1=conn.setSavepoint("1");
		JDBCUtil.execute(conn,"insert into tt values (3,'b'),(4,'x')");
		Savepoint sp2=conn.setSavepoint("2");
		JDBCUtil.execute(conn,"insert into tt values (5,'c'),(6,'x')");
		conn.rollback(sp2);
//		conn.commit();
		conn.close();
	}

	
}

package org.easyframe.tutorial.lessona;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import jef.codegen.EntityEnhancer;
import jef.common.log.LogUtil;
import jef.database.DbUtils;
import jef.database.datasource.MapDataSourceLookup;
import jef.database.datasource.SimpleDataSource;
import jef.database.meta.MetaHolder;
import jef.database.routing.jdbc.JDataSource;

import org.easyframe.tutorial.lessona.entity.Device;
import org.easyframe.tutorial.lessona.entity.Person2;
import org.junit.BeforeClass;
import org.junit.Test;

public class RoutingJdbcTest {

	private static DataSource ds;

	/**
	 * 准备测试数据
	 * 
	 * @throws SQLException
	 */
	@BeforeClass
	public static void setup() throws SQLException {
		new EntityEnhancer().enhance("org.easyframe.tutorial.lessona");
		MetaHolder.getMeta(Device.class);
		MetaHolder.getMeta(Person2.class);
		
		// 准备多个数据源
		Map<String, DataSource> datasources = new HashMap<String, DataSource>();
		// 创建三个数据库。。。
		datasources.put("datasource1", new SimpleDataSource("jdbc:derby:./db;create=true", null, null));
		datasources.put("datasource2", new SimpleDataSource("jdbc:derby:./db2;create=true", null, null));
		datasources.put("datasource3", new SimpleDataSource("jdbc:derby:./db3;create=true", null, null));
		MapDataSourceLookup lookup = new MapDataSourceLookup(datasources);
		lookup.setDefaultKey("datasource1");// 指定datasource1是默认的操作数据源
		ds=new JDataSource(lookup); 
	}
	
	
	@Test
	public void test1() throws SQLException{
		Connection conn=ds.getConnection();
		Statement st=conn.createStatement();
		boolean flag=st.execute("insert into DeVice(indexcode,name,type,createDate) values('123456', '测试', '办公用品', current_timestamp)");
		System.out.println(flag+"  "+st.getUpdateCount());
		st.close();
	}

	
	@Test
	public void test2() throws SQLException{
		Connection conn=ds.getConnection();
		Statement st=conn.createStatement();
		boolean flag=st.execute("insert into person2(DATA_DESC,NAME,created) values('123456', '测试',current_timestamp)",1);
		ResultSet rs=st.getGeneratedKeys();
		rs.next();
		System.out.println("自增主键返回:"+rs.getInt(1));
		DbUtils.close(rs);
		System.out.println(flag+"  "+st.getUpdateCount());
		st.close();
	}
	
	@Test
	public void test3() throws SQLException{
		executeQuery("select * from device");
		//补充测试无Device表的场合
		
//		当使用多表查询后，由于变为内存分页，此时支持用limit来分页，反倒不支持用数据库原生方式分页，怎么办？
//		如果查询后指向单表，此时SQL错误。。。。
//		考虑，传入SQL一律使用limit进行分页，应用时处理
		executeQuery("select * from device order by indexcode limit 12,3");
	}


	private void executeQuery(String sql) throws SQLException {
		Connection conn=ds.getConnection();
		Statement st=conn.createStatement();
		boolean flag=st.execute(sql);
		if(flag){
			ResultSet rs=st.getResultSet();
			LogUtil.show(rs);
			DbUtils.close(rs);
		}
		DbUtils.close(st);
	}
}

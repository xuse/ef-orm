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
//		executeQuery("select * from device");
		//补充案例。测试无Device表的场合
		
//在使用JDataSource时，因为要实现分库后的分页功能，所以必须从SQL语句中解析出分页参数。
//而不同数据库的分页语句写法变化非常大，（如ORacle，SQLSErver）,因此考虑让传入的SQL语句统一使用LIMIT关键字来描述分页信息。
//但是用户并不关心传入的SQL语句对应的场合是分库的还是单表的。而所有传入的分页语句都使用Limit。
//因此对于那些不支持LIMIT关键字的数据库（derby），我们希望NativeQuery和JDataSource都能正确处理Limit关键字。(OK)

		
		//案例1，此案例必须使用内存分页，正确（OK）

//		executeQuery("select * from device order by indexcode limit 12,3");
		executeQuery("select * from device where indexcode >= '2' and indexcode<='4' order by indexcode limit 12,3");
		
		//案例2，使用limit后，如果是单表，那么将改为数据库分页。(OK)
//		executeQuery("select * from Person2");
//		executeQuery("select * from Person2 order by name limit 2,12");
		
		
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

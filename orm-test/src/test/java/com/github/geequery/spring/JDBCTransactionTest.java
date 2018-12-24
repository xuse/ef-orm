package com.github.geequery.spring;

import static org.junit.Assert.assertEquals;

import java.sql.SQLException;

import org.apache.commons.dbcp.BasicDataSource;
import com.github.geequery.spring.case1.UserJdbcWithoutTransManagerService;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import jef.database.DbClient;
import jef.database.dialect.ColumnType;
import jef.database.meta.TupleMetadata;

@ContextConfiguration(locations = { "classpath:testJdbcWithoutTransManager.xml" })
public class JDBCTransactionTest extends AbstractJUnit4SpringContextTests{
	
	
	/*
	 * mysql> create table t_user (user_name varchar(255),score int,password1 varchar(255));
	 */
	@Test
	public void testJdbcWithoutTransManager() throws SQLException{
		UserJdbcWithoutTransManagerService service = (UserJdbcWithoutTransManagerService) applicationContext.getBean("service1");
		JdbcTemplate jdbcTemplate = (JdbcTemplate) applicationContext.getBean("jdbcTemplate");
		
		BasicDataSource basicDataSource = (BasicDataSource) jdbcTemplate.getDataSource();
		
		checkTable(basicDataSource);
		
		// ①.检查数据源autoCommit的设置
		System.out.println("autoCommit:" + basicDataSource.getDefaultAutoCommit());
		// ②.插入一条记录，初始分数为10
		jdbcTemplate.execute("INSERT INTO t_user (user,password,score) VALUES ('tom','123456',10)");
		// ③.调用工作在无事务环境下的服务类方法,将分数添加20分
		
		service.addScore("tom", 20); 
		
		// ④.查看此时用户的分数
		int score = jdbcTemplate.queryForObject("SELECT score FROM t_user WHERE user='tom'",Integer.class);
		System.out.println("score:" + score);
//		jdbcTemplate.execute("DELETE FROM t_user WHERE user='tom'");
		assertEquals(30, score);
		
	}

	private void checkTable(BasicDataSource bs) throws SQLException {
		DbClient db=new DbClient(bs);
		TupleMetadata table=new TupleMetadata("t_user");
		table.addColumn("user", new ColumnType.Varchar(64));
		table.addColumn("password", new ColumnType.Varchar(64));
		table.addColumn("score", new ColumnType.Int(10));
		db.dropTable(table);
		db.createTable(table);
	}
	
	

}

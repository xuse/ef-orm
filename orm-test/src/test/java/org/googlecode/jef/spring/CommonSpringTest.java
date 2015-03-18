package org.googlecode.jef.spring;

import java.sql.SQLException;

import jef.common.log.LogUtil;
import jef.database.dialect.ColumnType;
import jef.database.meta.TupleMetadata;

import org.easyframe.enterprise.spring.CommonDao;
import org.hsqldb.types.Types;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.company.my.application.LooService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:spring/common-ef-orm2.xml" })
public class CommonSpringTest {
	
	@Autowired
	private CommonDao dao;
	
	@Autowired
	private LooService looService;

	/**
	 * 数据库连接测试
	 * @throws SQLException
	 */
	@Test
	public void test1() throws SQLException{
		LogUtil.show(dao.getNoTransactionSession().getMetaData(null).getDbVersion());
	}
	
	/**
	 * 试用Postgres的 hstore jsonb等功能
	 * @throws SQLException
	 */
	@Test
	public void test2() throws SQLException{
		TupleMetadata table=new TupleMetadata("h_test");
		table.addColumn("id", new ColumnType.AutoIncrement(8));
		table.addColumn("data", new ColumnType.Other("hstore", Types.OTHER,String.class));
		dao.getNoTransactionSession().createTable(table);
		
		
		
		
	}
	@Test
	public void test3() throws SQLException{
		System.out.println(looService.getClass());
		looService.test();
	}

}

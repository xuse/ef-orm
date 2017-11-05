package com.github.geequery.test.spring.JefTransactionTest;

import java.sql.SQLException;

import org.googlecode.jef.spring.entity.BindEntity1;
import org.googlecode.jef.spring.entity.BindEntity2;
import org.googlecode.jef.spring.entity.BindEntity3;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import jef.database.DbClient;
import jef.database.jpa.JefEntityManagerFactory;

@ContextConfiguration(locations = { "classpath:JefTransactionTest2.xml" })
public class JefTransactionTest2 extends AbstractJUnit4SpringContextTests{
	/**
	 * 多数据源，绑定不同的库
	 * 
	 * @throws SQLException
	 */
	@Test
	public void doTest() throws SQLException {
		JefEntityManagerFactory factory = applicationContext.getBean(JefEntityManagerFactory.class);
		DbClient client = factory.getDefault();
		client.dropTable(BindEntity1.class, BindEntity2.class, BindEntity3.class);
		client.createTable(BindEntity1.class, BindEntity2.class, BindEntity3.class);
	}
}

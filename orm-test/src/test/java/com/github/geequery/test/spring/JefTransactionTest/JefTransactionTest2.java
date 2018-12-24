package com.github.geequery.test.spring.JefTransactionTest;

import java.sql.SQLException;

import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import com.github.geequery.spring.entity.BindEntity1;
import com.github.geequery.spring.entity.BindEntity2;
import com.github.geequery.spring.entity.BindEntity3;

import jef.database.DbClient;
import jef.database.SessionFactory;

/**
 * 多数据源测试，绑定不同的库
 * <h3>案例说明</h3>
 * 
 * 
 * 
 * 
 * @author jiyi
 *
 */
@ContextConfiguration(locations = { "classpath:JefTransactionTest2.xml" })
public class JefTransactionTest2 extends AbstractJUnit4SpringContextTests{
	/**
	 * 
	 * 
	 * @throws SQLException
	 */
	@Test
	public void doTest() throws SQLException {
		SessionFactory factory = applicationContext.getBean(SessionFactory.class);
		DbClient client = factory.asDbClient();
		client.dropTable(BindEntity1.class, BindEntity2.class, BindEntity3.class);
		client.createTable(BindEntity1.class, BindEntity2.class, BindEntity3.class);
		
	}
}

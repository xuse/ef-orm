package com.github.geequery.test.spring.JefTransactionTest;

import java.sql.SQLException;

import org.googlecode.jef.spring.entity.BindEntity1;
import org.googlecode.jef.spring.entity.BindEntity2;
import org.googlecode.jef.spring.entity.BindEntity3;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import jef.database.DbClient;
import jef.database.DbClientBuilder;
import jef.database.QB;
import jef.database.datasource.DataSourceInfoImpl;
import jef.database.jpa.JefEntityManagerFactory;

@ContextConfiguration(locations = { "classpath:testDbDatasourceLookup.xml" })
public class TestDbDatasourceLookup extends AbstractJUnit4SpringContextTests {
	@Test
	public void testDbDatasourceLookup() throws SQLException {
		ensureLocalConfig();
		JefEntityManagerFactory factory = applicationContext.getBean(JefEntityManagerFactory.class);
		DbClient client = factory.getDefault();
		client.dropTable(BindEntity1.class, BindEntity2.class, BindEntity3.class);
		client.createTable(BindEntity1.class, BindEntity2.class, BindEntity3.class);
	}

	private void ensureLocalConfig() throws SQLException {
		DbClient db = new DbClientBuilder("${derby.url}", "pomelo", "pomelo", 1).build();
		db.createTable(DataSourceInfoImpl.class);
		db.delete(QB.create(DataSourceInfoImpl.class));

		DataSourceInfoImpl info = new DataSourceInfoImpl();
		info.setDbKey("dataSource");
		info.setUrl("jdbc:mysql://localhost:3306/test");
		info.setUser("root");
		info.setPassword("admin");
		db.insert(info);

		info = new DataSourceInfoImpl();
		info.setDbKey("dataSource2");
		info.setUrl("jdbc:mysql://localhost:3306/test2");
		info.setUser("root");
		info.setPassword("admin");
		db.insert(info);

		info = new DataSourceInfoImpl();
		info.setDbKey("dataSource3");
		info.setUrl("jdbc:mysql://localhost:3306/test3");
		info.setUser("root");
		info.setPassword("admin");
		db.insert(info);

		db.close();
	}
}

package org.googlecode.jef.spring;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import jef.database.datasource.SimpleDataSource;

import com.alibaba.druid.mock.MockDriver;

public class Mocks {

	static class TesterDrtver extends MockDriver {
		public boolean acceptsURL(String url) throws SQLException {
			if (url.startsWith("jdbc:test:")) {
				return true;
			}
			return super.acceptsURL(url);
		}
		public Connection connect(String url, Properties info) throws SQLException {
			return super.connect("jdbc:mock:case1", info);
		}
	}

	static{
		try {
			DriverManager.registerDriver(new org.googlecode.jef.spring.Mocks.TesterDrtver());
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static Connection getMockConnection() {
		String jdbcUrl = "jdbc:test:case1:";

		SimpleDataSource dataSource = new SimpleDataSource();
		dataSource.setUrl(jdbcUrl);
		dataSource.setDriverClass(TesterDrtver.class.getName());
		dataSource.setUsername("");
		dataSource.setPassword("");
		try {
			return dataSource.getConnection();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

}

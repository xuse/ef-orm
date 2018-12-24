package com.github.geequery.support.spring;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.inject.Provider;
import javax.sql.DataSource;

import org.springframework.jdbc.datasource.AbstractDriverBasedDataSource;

import jef.database.DbUtils;
import jef.database.datasource.SimpleDataSource;

/**
 * 简单的数据源封装。提供连接，如果没有使用连接池，还可以提供定制参数的连接。
 * （如果使用了连接池，那么定制参数请参阅连接池的相关文档。目前定制连接参数主要就是Oracle，这一特性偶尔在开发一些数据库小工具的时候还是有用的。）
 * @author jiyi
 *
 */
public final class DataSourceProvider implements Provider<Connection>, CustomConnectionProvider {
	private final DataSource ds;

	public DataSourceProvider(DataSource ds) {
		this.ds = ds;
	}

	@Override
	public Connection get() {
		try {
			return ds.getConnection();
		} catch (SQLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	@Override
	public Connection getConnectionFromDriver(Properties properties) {
		try {
			if (ds instanceof AbstractDriverBasedDataSource) {
				return new SimpleDataSource((AbstractDriverBasedDataSource) ds).getConnectionFromDriver(properties);
			} else {
				return ds.getConnection();
			}
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}
}
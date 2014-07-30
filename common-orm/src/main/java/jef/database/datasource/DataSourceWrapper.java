package jef.database.datasource;

import javax.sql.DataSource;

/**
 * 包装接口。
 * 这个接口的实现是若干适配器，这些适配器能够将DataSource包装为可操作的统一DataSource模型
 * 
 * 
 * @author jiyi
 * @see java.sql.DataSource
 * @see DataSourceInfo
 */
public interface DataSourceWrapper extends DataSourceInfo,DataSource{
	
	/**
	 * 该数据源是否为连接池
	 * @return true if the datasource is a connection pool implementation.
	 */
	boolean isConnectionPool();
	
	/**
	 * 所有DataSourceWrapper都必须提供空构造，然后允许将被包装的DataSource设置进去
	 */
	void setWrappedDataSource(DataSource ds);
}

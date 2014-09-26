package jef.database.innerpool;

import java.sql.Connection;
import java.sql.SQLException;

import jef.database.datasource.IRoutingDataSource;

/**
 * 路由场合下的连接池
 * @author jiyi
 *
 */
public interface IRoutingConnectionPool extends IUserManagedPool{
	/**
	 * 得到路由数据源，这是由多个实际数据源组成的一个容器
	 * @return
	 */
	IRoutingDataSource getRoutingDataSource();
	
	/**
	 * 如果RoutingDataSource中带了一个连接缓存，那么返回缓存的连接。
	 * 如果缓存没有，那么就用datasource创建一个连接
	 * @return 不能为null，总是要返回一个连接
	 */
	public Connection getCachedConnection(String ds)throws SQLException ;
	
	/**
	 * 将连接放回缓存。如果缓存放不下，那么就关闭
	 */
	public void putback(String ds,Connection conn);
}

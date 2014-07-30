package jef.database.innerpool;

import java.sql.SQLException;

import javax.sql.DataSource;

import jef.common.pool.PoolStatus;

/**
 * 最基本的连接池
 * @author jiyi
 *
 */
public interface IPool<T> {
	/**
	 * 获取连接
	 * 
	 * 注意：这个版本开始，连接池不再负责设置AutoCommit的状态
	 * @return
	 * @throws SQLException
	 */
	public T poll() throws SQLException;
	/**
	 * 归还连接
	 * @param conn
	 * @throws SQLException
	 */
	public void offer(T conn);
	
	/**
	 * 查询状态
	 * @return
	 */
	public PoolStatus getStatus();
	
	/**
	 * 获得数据源
	 * @return
	 */
	DataSource getDatasource();
	
	/**
	 * 收缩，关闭多余的连接。具体实现取决于实现内部。
	 * 这个方法是让连接池尽量少占数据库资源
	 */
	public void closeConnectionTillMin();
	
	/**
	 * 关闭连接池，包括释放其中全部的连接
	 */
	void close() throws SQLException;
	
}

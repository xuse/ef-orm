package jef.database.routing.jdbc;

import java.sql.SQLException;

/**
 * @author junyu
 * 
 */
public interface RealSqlExecutor {
	/**
	 * 执行查询，需要执行计划实例
	 * 
	 * @param executionPlan
	 * @return
	 * @throws SQLException 
	 */
	public QueryReturn query() throws SQLException;

	/**
	 * 执行更新，需要执行计划
	 * 
	 * @param executionPlan
	 * @return
	 */
	public UpdateReturn update()throws SQLException;
	
	/**
	 * 主要是回收queryQueue的Rs和Statement
	 * 
	 * @throws SQLException
	 */
	public void clearQueryResource() throws SQLException;
}

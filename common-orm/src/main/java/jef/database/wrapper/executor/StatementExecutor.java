package jef.database.wrapper.executor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public interface StatementExecutor {
	/**
	 * 执行执行的DDL语句 同步方式
	 * 
	 * @param ddls
	 * @throws SQLException 
	 */
	public void executeSql(String... ddls) throws SQLException;

	/**
	 * 执行执行的DDL语句 同步方式
	 * 
	 * @param ddls
	 * @throws SQLException 
	 */
	public void executeSql(List<String> ddls) throws SQLException;

	/**
	 * 关闭运行器
	 */
	public void close();
	
	/**
	 * 设置单句SQL的运行超时
	 * @param seconds
	 */
	public void setQueryTimeout(int seconds) throws SQLException;
	
	/**
	 * 执行DML语句
	 * 注意要人工关闭结果集
	 * @param sql
	 * @return
	 */
	public ResultSet executeQuery(String sql,Object... params)throws SQLException;
	
	/**
	 * 执行DML语句
	 * @param sql
	 * @param params
	 * @return
	 * @throws SQLException
	 */
	public int executeUpdate(String sql,Object... params)throws SQLException;
	
	
	
}

package jef.database.innerpool;

import java.sql.CallableStatement;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 这是真正在JEF内部所使用的数据库连接对象。
 * IConnection对象
 *   当使用事务处理时，每个事务对象都被分配到一个IConnection。
 *   当处理非事务任务时，每个线程都得到一个IConnection。
 *   无论哪种情况ConnectionInPool都由{@link IConnectionPool}对象给出。
 *  <p>
 * @author Administrator
 */
public interface IConnection{
	/**
	 * (物理上)关闭连接
	 * 
	 * 但连接本身如果是个连接池实现，那么其实就相当于归还
	 */
	void closePhysical();

	/**
	 * 设置连接要从哪个数据源获取，当多数据源时，连接是有状态的。通过这个方法设置连接的状态
	 * @param key
	 * @return
	 */
	void setKey(String key);
	
	/**
	 * 是否被占用，即占用计数器是否>0
	 * @return
	 */
	boolean isUsed();
	
	/**
	 * 当多数据源时，这个操作实际上将在所有开启的连接当中都建立SavePoint
	 * 如果不支持SavePoint，返回null
	 * @param savepointName
	 * @return
	 * @throws SQLException
	 */
	Savepoints setSavepoints(String savepointName)throws SQLException;
	
	/**
	 * 确认连接有效，在每次获取连接时执行，因此不可能执行开销太大的方法
	 * @throws SQLException
	 */
	void ensureOpen() throws SQLException;
	
	
	//////////////////////////////////和JDBC实现含义相同/////////////////////////////////
	void setAutoCommit(boolean b) throws SQLException;
	void commit() throws SQLException;
	void rollback()throws SQLException;
	Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException;
	PreparedStatement prepareStatement(String sql, int i) throws SQLException;;
	PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException;
	PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException;
	Statement createStatement() throws SQLException;
	PreparedStatement prepareStatement(String sql) throws SQLException;
	CallableStatement prepareCall(String sql) throws SQLException;
	DatabaseMetaData getMetaData()throws SQLException;
	void setReadOnly(boolean flag) throws SQLException;
	int getTransactionIsolation() throws SQLException;
	void setTransactionIsolation(int level) throws SQLException;
}
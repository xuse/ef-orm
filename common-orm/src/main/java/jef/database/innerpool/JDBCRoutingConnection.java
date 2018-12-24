package jef.database.innerpool;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.Executor;

import javax.persistence.PersistenceException;

import jef.common.SimpleMap;
import jef.database.DbUtils;
import jef.database.datasource.DefaultRoutingDataSource;
import jef.database.exception.InconsistentCommitException;
import jef.tools.StringUtils;

/**
 * 将多连接模拟成单连接进行管理
 * 
 * @author jiyi
 * 
 */
public final class JDBCRoutingConnection implements IConnection {
	/**
	 * 自动提交
	 */
	private boolean autoCommit = true;
	/**
	 * 只读标记
	 */
	private boolean readOnly;
	/**
	 * 事务隔离级别
	 */
	private int isolation = -1;
	/**
	 * 当前的Savapoint序号
	 */
	private int savePointId = 0;
	/**
	 * 连接池
	 */
	private DefaultRoutingDataSource parent;
	/**
	 * 目前已经用到的连接
	 */
	private final Map<String, Connection> connections = new HashMap<String, Connection>(4);
	/**
	 * 目前使用的连接数据源名称
	 */
	private String key;

	/**
	 * 即便提交过程中出现错误，也持续将剩余的连接都提交完。 如果设置为false，那么任意连接的提交错误将终止提交过程。剩余的连接等待回滚。
	 * 
	 */
	private boolean continueCommitEvenException;

	/**
	 * 构造
	 * 
	 * @param parent
	 */
	public JDBCRoutingConnection(DefaultRoutingDataSource parent, boolean isCommitEvenExeption) {
		this.parent = parent;
		this.continueCommitEvenException = isCommitEvenExeption;
	}

	public final void ensureOpen() throws PersistenceException {
		if (parent == null) {
			throw new PersistenceException("Current Routing Connection is closed already!");
		}
	}

	// 一般标记为事务的开始
	public void setAutoCommit(boolean flag) throws SQLException {
		if (autoCommit == flag) {
			return;
		}
		this.autoCommit = flag;
		for (Connection conn : connections.values()) {
			if (conn.getAutoCommit() != flag) {
				conn.setAutoCommit(flag);
			}
		}
	}

	//
	public void setReadOnly(boolean flag) throws SQLException {
		if (readOnly == flag) {
			return;
		}
		readOnly = flag;
		for (Connection conn : connections.values()) {
			if (conn.isReadOnly() != flag) {
				conn.setReadOnly(flag);
			}
		}
	}

	public boolean isContinueCommitEvenException() {
		return continueCommitEvenException;
	}

	public void setContinueCommitEvenException(boolean continueCommitEvenException) {
		this.continueCommitEvenException = continueCommitEvenException;
	}

	public void setKey(String key) {
		if (key != null && key.length() == 0) {
			key = null;
		}
		this.key = key;
	}

	/**
	 * 当有多个连接需要提交时，对每个连接进行提交。 无论过程中是否有异常，都记录提交过程中发生的异常信息，并继续提交后续的连接。
	 */
	public void commit() throws SQLException {
		//如果没有需要提交的连接，直接返回
		if (connections.isEmpty() || autoCommit)
			return;
		ensureOpen();
		//记录提交程度的数据源名称
		List<String> succeed = new ArrayList<String>();
		//
		SimpleMap<String, SQLException> errors = new SimpleMap<String, SQLException>();
		for (Map.Entry<String, Connection> entry : connections.entrySet()) {
			try {
				entry.getValue().commit();
				succeed.add(entry.getKey());
			} catch (SQLException e) {
				errors.add(entry.getKey(), e);
				if (!continueCommitEvenException){
					break;
				}
			} catch (RuntimeException e) {
				errors.add(entry.getKey(), new SQLException(e));
				if (!continueCommitEvenException){
					break;
				}
			}
		}
		if (errors.isEmpty()) {
			return;
		}
		if (succeed.isEmpty()) {// 第一个连接提交就出错，事务依然保持一致
			throw DbUtils.wrapExceptions(errors.values());
		} else { // 第success.size()+1个连接提交出错
			throw new InconsistentCommitException(succeed,errors,connections.size());
		}
	}

	/**
	 * 当有多个连接需要回滚时，对每个连接进行回滚。 无论过程中是否有异常，都记录回滚过程中发生的异常信息，并继续回滚后续的连接。
	 */
	public void rollback() throws SQLException {
		if (connections.isEmpty() || autoCommit)
			return;
		ensureOpen();
		List<String> successed = new ArrayList<String>();
		SimpleMap<String, SQLException> errors = new SimpleMap<String, SQLException>();
		for (Map.Entry<String, Connection> entry : connections.entrySet()) {
			try {
				entry.getValue().rollback();
				successed.add(entry.getKey());
			} catch (SQLException e) {
				errors.add(entry.getKey(), e);
				// 即时前面的连接出错，后面的依然要回滚
			} catch (RuntimeException e) {
				errors.add(entry.getKey(), new SQLException(e));
			}
		}
		if (errors.isEmpty()) {
			return;
		}
		if (successed.isEmpty()) {// 第一个连接提交就出错，事务依然保持一致
			throw DbUtils.wrapExceptions(errors.values());
		} else { // 第success.size()+1个连接提交出错
			Entry<String, SQLException> error = errors.getEntries().get(0);
			String message = StringUtils.concat("Error while rollback data to datasource [", error.getKey(), "], and this is the ", String.valueOf(successed.size() + 1), "th rollback of ", String.valueOf(connections.size()),
					", there must be some data consistency problem, please check it.");
			SQLException ex = DbUtils.wrapExceptions(errors.values());
			SQLException e = new SQLException(message, ex);
			e.setNextException(ex);
			throw e;
		}
	}

	private Connection getConnection() throws SQLException {
		if (key == null) {
			return getConnectionOfKey(parent.getDefaultKey());
		} else {
			return getConnectionOfKey(key);
		}
	}

	/**
	 * 实现
	 * 
	 * @param key
	 * @return
	 * @throws SQLException
	 */
	private Connection getConnectionOfKey(String key) throws SQLException {
		Connection conn = connections.get(key);
		if (conn == null) {
			conn=parent.getDataSource(key).getConnection();
			if (conn.isReadOnly() != readOnly) {
				conn.setReadOnly(readOnly);
			}
			if (isolation >= 0) {
				conn.setTransactionIsolation(isolation);
			}
			if (conn.getAutoCommit() != autoCommit) {
				conn.setAutoCommit(autoCommit);
			}
			connections.put(key, conn);
		}
		return conn;
	}

	public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
		return getConnection().createStatement(resultSetType, resultSetConcurrency);
	}

	public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
		return getConnection().prepareStatement(sql, autoGeneratedKeys);
	}

	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
		return getConnection().prepareStatement(sql, resultSetType, resultSetConcurrency);
	}

	public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
		return getConnection().prepareStatement(sql, columnNames);
	}

	public Statement createStatement() throws SQLException {
		return getConnection().createStatement();
	}

	public PreparedStatement prepareStatement(String sql) throws SQLException {
		return getConnection().prepareStatement(sql);
	}

	public CallableStatement prepareCall(String sql) throws SQLException {
		return getConnection().prepareCall(sql);
	}

	public DatabaseMetaData getMetaData() throws SQLException {
		return getConnection().getMetaData();
	}

	public int getTransactionIsolation() throws SQLException {
		return isolation;
	}

	public void setTransactionIsolation(int level) throws SQLException {
		if (level < 0 || level == isolation) {
			return;
		}
		this.isolation = level;
		for (Connection conn : connections.values()) {
			if (conn.getTransactionIsolation() != level) {
				conn.setTransactionIsolation(level);
			}
		}
	}

	/**
	 * 归还到父池中
	 */
	public void close() {
		for (Connection conn : connections.values()) {
			DbUtils.closeConnection(conn);
		}
	}

	public <T> T unwrap(Class<T> iface) throws SQLException {
		return getConnection().unwrap(iface);
	}

	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return false;
	}

	public String nativeSQL(String sql) throws SQLException {
		return getConnection().nativeSQL(sql);
	}

	public boolean getAutoCommit() throws SQLException {
		return autoCommit;
	}

	public boolean isClosed() throws SQLException {
		return parent == null;
	}

	public boolean isReadOnly() throws SQLException {
		return readOnly;
	}

	public void setCatalog(String catalog) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public String getCatalog() throws SQLException {
		return null;
	}

	public SQLWarning getWarnings() throws SQLException {
		return null;
	}

	public void clearWarnings() throws SQLException {
	}

	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
		return getConnection().prepareCall(sql, resultSetType, resultSetConcurrency);
	}

	public Map<String, Class<?>> getTypeMap() throws SQLException {
		return getConnection().getTypeMap();
	}

	public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void setHoldability(int holdability) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public int getHoldability() throws SQLException {
		return getConnection().getHoldability();
	}

	public Savepoint setSavepoint() throws SQLException {
		ensureOpen();
		List<SQLException> errors = new ArrayList<SQLException>();
		Savepoints sp = new Savepoints(++savePointId);
		for (Connection conn : connections.values()) {
			try {
				Savepoint s = conn.setSavepoint();
				sp.add(conn, s);
			} catch (SQLException e) {
				errors.add(e);
			}
		}
		if (!errors.isEmpty()) {
			throw DbUtils.wrapExceptions(errors);
		}
		return sp;
	}

	public Savepoint setSavepoint(String name) throws SQLException {
		if (name == null) {
			return setSavepoint();
		}
		ensureOpen();
		List<SQLException> errors = new ArrayList<SQLException>();
		Savepoints sp = new Savepoints(name);
		for (Connection conn : connections.values()) {
			try {
				Savepoint s = conn.setSavepoint();
				sp.add(conn, s);
			} catch (SQLException e) {
				errors.add(e);
			}
		}
		if (!errors.isEmpty()) {
			throw DbUtils.wrapExceptions(errors);
		}
		return sp;
	}

	public void rollback(Savepoint savepoint) throws SQLException {
		if (savepoint instanceof Savepoints) {
			((Savepoints) savepoint).doRollback();
		} else {
			throw new SQLException(savepoint + " is not a valid savepoint!");
		}
	}

	public void releaseSavepoint(Savepoint savepoint) throws SQLException {
		if (savepoint instanceof Savepoints) {
			((Savepoints) savepoint).doRelease();
		} else {
			throw new SQLException(savepoint + " is not a valid savepoint!");
		}
	}

	public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
		return getConnection().createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
		return getConnection().prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
		return getConnection().prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
		return getConnection().prepareStatement(sql, columnIndexes);
	}

	public Clob createClob() throws SQLException {
		return getConnection().createClob();
	}

	public Blob createBlob() throws SQLException {
		return getConnection().createBlob();
	}

	public NClob createNClob() throws SQLException {
		return getConnection().createNClob();
	}

	public SQLXML createSQLXML() throws SQLException {
		return getConnection().createSQLXML();
	}

	public boolean isValid(int timeout) throws SQLException {
		return true;
	}

	public void setClientInfo(String name, String value) throws SQLClientInfoException {
		for (Connection conn : this.connections.values()) {
			conn.setClientInfo(name, value);
		}
	}

	public void setClientInfo(Properties properties) throws SQLClientInfoException {
		for (Connection conn : this.connections.values()) {
			conn.setClientInfo(properties);
		}
	}

	public String getClientInfo(String name) throws SQLException {
		return null;
	}

	public Properties getClientInfo() throws SQLException {
		return null;
	}

	public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
		return getConnection().createArrayOf(typeName, elements);
	}

	public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
		return getConnection().createStruct(typeName, attributes);
	}

	public void setSchema(String schema) throws SQLException {
	}

	public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
	}

	public int getNetworkTimeout() throws SQLException {
		return 0;
	}

	@Override
	public String getSchema() throws SQLException {
		return null;
	}

	@Override
	public void abort(Executor executor) throws SQLException {
	}
}

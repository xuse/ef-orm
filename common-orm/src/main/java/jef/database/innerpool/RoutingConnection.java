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

import javax.sql.DataSource;

import jef.database.DbUtils;
import jef.tools.StringUtils;

/**
 * 最简单的路由Connection实现
 * 
 * @author jiyi
 * 
 */
final class RoutingConnection implements ReentrantConnection, Connection {
	private String defaultKey = null;
	private boolean autoCommit;
	private boolean readOnly;
	private int isolation = -1;

	private IRoutingConnectionPool parent;
	private Map<String, Connection> connections = new HashMap<String, Connection>();
	private String key;

	public RoutingConnection(IRoutingConnectionPool parent) {
		this.parent = parent;
	}

	public void closePhysical() {
		if (parent != null) {
			IRoutingConnectionPool parent = this.parent;
			for (Entry<String, Connection> entry : connections.entrySet()) {
				String key = entry.getKey();
				parent.putback(key, entry.getValue());
			}
			connections.clear();// 全部归还
		}
	}

	public final void ensureOpen() throws SQLException {
		if (parent == null) {
			throw new SQLException("Current Routing Connection is closed already!");
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

	public void setKey(String key) {
		if (key != null && key.length() == 0) {
			key = null;
		}
		this.key = key;
	}

	public void commit() throws SQLException {
		ensureOpen();
		List<String> successed = new ArrayList<String>();
		SQLException error = null;
		String errDsName = null;
		for (Map.Entry<String, Connection> entry : connections.entrySet()) {
			try {
				entry.getValue().commit();
				successed.add(entry.getKey());
			} catch (SQLException e) {
				errDsName = entry.getKey();
				error = e;
				break;
			}
		}
		if (error == null) {
			return;
		}
		if (successed.isEmpty()) {// 第一个连接提交就出错，事务依然保持一致
			throw error;
		} else { // 第success.size()+1个连接提交出错
			String message = StringUtils.concat("Error while commit data to datasource [", errDsName, "], and this is the ", String.valueOf(successed.size() + 1), "th commit of ", String.valueOf(connections.size()),
					", there must be some data consistency problem, please check it.");
			SQLException e = new SQLException(message, error);
			e.setNextException(error);
			throw e;
		}
	}

	public void rollback() throws SQLException {
		ensureOpen();
		List<String> successed = new ArrayList<String>();
		SQLException error = null;
		String errDsName = null;
		for (Map.Entry<String, Connection> entry : connections.entrySet()) {
			try {
				entry.getValue().rollback();
				successed.add(entry.getKey());
			} catch (SQLException e) {
				errDsName = entry.getKey();
				error = e;
				break;
			}
		}
		if (error == null) {
			return;
		}
		if (successed.isEmpty()) {// 第一个连接提交就出错，事务依然保持一致
			throw error;
		} else { // 第success.size()+1个连接提交出错
			String message = StringUtils.concat("Error while rollback data to datasource [", errDsName, "], and this is the ", String.valueOf(successed.size() + 1), "th rollback of ", String.valueOf(connections.size()),
					", there must be some data consistency problem, please check it.");
			SQLException e = new SQLException(message, error);
			e.setNextException(error);
			throw e;
		}
	}

	public Savepoints setSavepoints(String savepointName) throws SQLException {
		ensureOpen();
		List<SQLException> errors = new ArrayList<SQLException>();
		Savepoints sp = new Savepoints();
		for (Connection conn : connections.values()) {
			try {
				Savepoint s = conn.setSavepoint(savepointName);
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

	private Connection getConnection() throws SQLException {
		if (key == null) {
			if (defaultKey == null) {
				Entry<String, DataSource> ds = parent.getRoutingDataSource().getDefaultDatasource();
				defaultKey = ds.getKey();
			}
			return getConnectionOfKey(defaultKey);
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
			do {
				conn = parent.getCachedConnection(key);
				if (conn.isClosed()) {
					DbUtils.closeConnection(conn);
					conn = null;
				}
			} while (conn == null);

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

	private volatile Object used;
	private volatile int count;

	public void setUsedByObject(Object user) {
		this.used = user;
		count++;
	}

	public Object popUsedByObject() {
		if (--count > 0) {
			// System.out.println("不是真正的归还"+used+"还有"+count+"次使用.");
			return null;
		} else {
			Object o = used;
			used = null;
			return o;
		}
	}

	public void addUsedByObject() {
		count++;
	}

	public boolean isUsed() {
		return count > 0;
	}

	public void notifyDisconnect() {
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

	public void close() throws SQLException {
		closePhysical();
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
		throw new UnsupportedOperationException();
	}

	public Savepoint setSavepoint(String name) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void rollback(Savepoint savepoint) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void releaseSavepoint(Savepoint savepoint) throws SQLException {
		throw new UnsupportedOperationException();
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

//	public String getSchema() throws SQLException {
//		return getConnection().getSchema();
//	}
//
//	public void abort(Executor executor) throws SQLException {
//		getConnection().abort(executor);
//	}

	public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
	}

	public int getNetworkTimeout() throws SQLException {
		return 0;
	}
}

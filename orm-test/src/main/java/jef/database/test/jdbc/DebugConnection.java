package jef.database.test.jdbc;

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
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

import jef.common.log.LogUtil;

public class DebugConnection implements Connection {
	private DebugDataSource parent;
	private Connection conn;
	
	public DebugConnection(Connection conn,DebugDataSource parent) {
		super();
		this.conn = conn;
	}

	public PreparedStatement prepareStatement(String sql) throws SQLException {
		return conn.prepareStatement(sql);
	}

	public Statement createStatement() throws SQLException {
		return conn.createStatement();
	}

	public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
		return conn.prepareStatement(sql, columnNames);
	}

	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
		return conn.prepareStatement(sql, resultSetType, resultSetConcurrency);
	}

	public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
		return conn.createStatement(resultSetType, resultSetConcurrency);
	}

	public void commit() throws SQLException {
		System.out.println(">>commit");
		conn.commit();
	}

	public PreparedStatement prepareStatement(String sql, int i) throws SQLException {
		return conn.prepareStatement(sql, i);
	}

	public CallableStatement prepareCall(String sql) throws SQLException {
		return conn.prepareCall(sql);
	}

	// 以下是为了实现JDBC规范的 Connection中其他方法，ORM本身不使用********************************
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return conn.unwrap(iface);
	}

	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return conn.isWrapperFor(iface);
	}

	public String nativeSQL(String sql) throws SQLException {
		return conn.nativeSQL(sql);
	}

	public boolean getAutoCommit() throws SQLException {
		return conn.getAutoCommit();
	}

	public boolean isClosed() throws SQLException {
		return conn.isClosed();
	}

	public void setReadOnly(boolean readOnly) throws SQLException {
		conn.setReadOnly(readOnly);
	}

	public boolean isReadOnly() throws SQLException {
		return conn.isReadOnly();
	}

	public void setCatalog(String catalog) throws SQLException {
		conn.setCatalog(catalog);
	}

	public String getCatalog() throws SQLException {
		return conn.getCatalog();
	}

	public void setTransactionIsolation(int level) throws SQLException {
		conn.setTransactionIsolation(level);
	}

	public int getTransactionIsolation() throws SQLException {
		return conn.getTransactionIsolation();
	}

	public SQLWarning getWarnings() throws SQLException {
		return conn.getWarnings();
	}

	public void clearWarnings() throws SQLException {
		conn.clearWarnings();
	}

	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
		return conn.prepareCall(sql, resultSetType, resultSetConcurrency);
	}

	public Map<String, Class<?>> getTypeMap() throws SQLException {
		return conn.getTypeMap();
	}

	public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
		conn.setTypeMap(map);
	}

	public void setHoldability(int holdability) throws SQLException {
		conn.setHoldability(holdability);
	}

	public int getHoldability() throws SQLException {
		return conn.getHoldability();
	}

	public Savepoint setSavepoint() throws SQLException {
		Savepoint sp=conn.setSavepoint();
		parent.event(OperType.create_savepoint, sp);
		return sp;
	}

	public Savepoint setSavepoint(String name) throws SQLException {
		parent.event(OperType.rollback_savepoint, name);
		return conn.setSavepoint(name);
	}

	public void rollback(Savepoint savepoint) throws SQLException {
		parent.event(OperType.rollback_savepoint, savepoint);
		conn.rollback(savepoint);
	}

	public void releaseSavepoint(Savepoint savepoint) throws SQLException {
		parent.event(OperType.release_savepoint, savepoint);
		System.out.println(">>releaseSavepoint "+savepoint);
		conn.releaseSavepoint(savepoint);
	}

	public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
		return conn.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
		return conn.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
		return conn.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
		return conn.prepareStatement(sql, columnIndexes);
	}

	public Clob createClob() throws SQLException {
		return conn.createClob();
	}

	public Blob createBlob() throws SQLException {
		return conn.createBlob();
	}

	public NClob createNClob() throws SQLException {
		return conn.createNClob();
	}

	public SQLXML createSQLXML() throws SQLException {
		return conn.createSQLXML();
	}

	public void setClientInfo(String name, String value) throws SQLClientInfoException {
		conn.setClientInfo(name, value);
	}

	public void setClientInfo(Properties properties) throws SQLClientInfoException {
		conn.setClientInfo(properties);
	}

	public String getClientInfo(String name) throws SQLException {
		return conn.getClientInfo(name);
	}

	public Properties getClientInfo() throws SQLException {
		return conn.getClientInfo();
	}

	public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
		return conn.createArrayOf(typeName, elements);
	}

	public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
		return conn.createStruct(typeName, attributes);
	}

	public DatabaseMetaData getMetaData() throws SQLException {
		return conn.getMetaData();
	}

	public void rollback() throws SQLException {
		System.out.println(">>rollback");
		conn.rollback();
	}

	public boolean isValid(int i) {
		if (conn == null) {
			return true;
		}
		try {
			return conn.isValid(i);
		} catch (SQLException e) {
			LogUtil.exception(e);
			return false;
		}
	}

	public void setAutoCommit(boolean b) throws SQLException {
		System.out.println(">>setAutoCommit "+b);
		conn.setAutoCommit(b);
	}

	public void setSchema(String schema) throws SQLException {
	}

	public String getSchema() throws SQLException {
		return null;
	}

	public void abort(Executor executor) throws SQLException {
	}

	public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
	}

	public int getNetworkTimeout() throws SQLException {
		return 0;
	}

	@Override
	public void close() throws SQLException {
		conn.close();
		parent.event(OperType.close_connection, conn);
	}
	
	public void event(OperType type,Object message) {
		parent.event(type, message);
	}
}

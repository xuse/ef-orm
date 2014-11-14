package jef.database.jdbc.statement;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

public class DelegatingStatement implements Statement {
	protected Statement _stmt = null;

	/**
	 * Create a wrapper for the Statement which traces this Statement to the
	 * Connection which created it and the code which created it.
	 * 
	 * @param s
	 *            the {@link Statement} to delegate all calls to.
	 * @param c
	 *            the {@link DelegatingConnection} that created this statement.
	 */
	public DelegatingStatement(Statement s) {
		_stmt = s;
	}

	/**
	 * Returns my underlying {@link Statement}.
	 * 
	 * @return my underlying {@link Statement}.
	 * @see #getInnermostDelegate
	 */
	public Statement getDelegate() {
		return _stmt;
	}

	/**
	 * This method considers two objects to be equal if the underlying jdbc
	 * objects are equal.
	 */
	public boolean equals(Object obj) {
		Statement delegate = getInnermostDelegate();
		if (delegate == null) {
			return false;
		}
		if (obj instanceof DelegatingStatement) {
			DelegatingStatement s = (DelegatingStatement) obj;
			return delegate.equals(s.getInnermostDelegate());
		} else {
			return delegate.equals(obj);
		}
	}

	public int hashCode() {
		Object obj = getInnermostDelegate();
		if (obj == null) {
			return 0;
		}
		return obj.hashCode();
	}

	/**
	 * If my underlying {@link Statement} is not a <tt>DelegatingStatement</tt>,
	 * returns it, otherwise recursively invokes this method on my delegate.
	 * <p>
	 * Hence this method will return the first delegate that is not a
	 * <tt>DelegatingStatement</tt> or <tt>null</tt> when no non-
	 * <tt>DelegatingStatement</tt> delegate can be found by transversing this
	 * chain.
	 * <p>
	 * This method is useful when you may have nested
	 * <tt>DelegatingStatement</tt>s, and you want to make sure to obtain a
	 * "genuine" {@link Statement}.
	 * 
	 * @see #getDelegate
	 */
	public Statement getInnermostDelegate() {
		Statement s = _stmt;
		while (s != null && s instanceof DelegatingStatement) {
			s = ((DelegatingStatement) s).getDelegate();
			if (this == s) {
				return null;
			}
		}
		return s;
	}

	/** Sets my delegate. */
	public void setDelegate(Statement s) {
		_stmt = s;
	}

	/**
	 * Close this DelegatingStatement, and close any ResultSets that were not
	 * explicitly closed.
	 */
	public void close() throws SQLException {
		_stmt.close();
	}

	protected void handleException(SQLException e) throws SQLException {
		throw e;
	}

	public Connection getConnection() throws SQLException {
		return _stmt.getConnection();
	}

	public ResultSet executeQuery(String sql) throws SQLException {
		return _stmt.executeQuery(sql);
	}

	public ResultSet getResultSet() throws SQLException {
		return _stmt.getResultSet();
	}

	public int executeUpdate(String sql) throws SQLException {
		return _stmt.executeUpdate(sql);
	}

	public int getMaxFieldSize() throws SQLException {
		return _stmt.getMaxFieldSize();
	}

	public void setMaxFieldSize(int max) throws SQLException {
		_stmt.setMaxFieldSize(max);
	}

	public int getMaxRows() throws SQLException {
		return _stmt.getMaxRows();
	}

	public void setMaxRows(int max) throws SQLException {
		_stmt.setMaxRows(max);
	}

	public void setEscapeProcessing(boolean enable) throws SQLException {
		_stmt.setEscapeProcessing(enable);
	}

	public int getQueryTimeout() throws SQLException {
		return _stmt.getQueryTimeout();
	}

	public void setQueryTimeout(int seconds) throws SQLException {
		_stmt.setQueryTimeout(seconds);
	}

	public void cancel() throws SQLException {
		_stmt.cancel();
	}

	public SQLWarning getWarnings() throws SQLException {
		return _stmt.getWarnings();
	}

	public void clearWarnings() throws SQLException {
		_stmt.clearWarnings();
	}

	public void setCursorName(String name) throws SQLException {
		_stmt.setCursorName(name);
	}

	public boolean execute(String sql) throws SQLException {
		return _stmt.execute(sql);
	}

	public int getUpdateCount() throws SQLException {
		return _stmt.getUpdateCount();
	}

	public boolean getMoreResults() throws SQLException {
		return _stmt.getMoreResults();
	}

	public void setFetchDirection(int direction) throws SQLException {
		_stmt.setFetchDirection(direction);
	}

	public int getFetchDirection() throws SQLException {
		return _stmt.getFetchDirection();
	}

	public void setFetchSize(int rows) throws SQLException {
		_stmt.setFetchSize(rows);
	}

	public int getFetchSize() throws SQLException {
		return _stmt.getFetchSize();
	}

	public int getResultSetConcurrency() throws SQLException {
		return _stmt.getResultSetConcurrency();
	}

	public int getResultSetType() throws SQLException {
		return _stmt.getResultSetType();
	}

	public void addBatch(String sql) throws SQLException {
		_stmt.addBatch(sql);
	}

	public void clearBatch() throws SQLException {
		_stmt.clearBatch();
	}

	public int[] executeBatch() throws SQLException {
		return _stmt.executeBatch();
	}

	/**
	 * Returns a String representation of this object.
	 * 
	 * @return String
	 * @since 1.2.2
	 */
	public String toString() {
		return _stmt.toString();
	}

	public boolean getMoreResults(int current) throws SQLException {
		return _stmt.getMoreResults(current);
	}

	public ResultSet getGeneratedKeys() throws SQLException {
		return _stmt.getGeneratedKeys();
	}

	public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
		return _stmt.executeUpdate(sql, autoGeneratedKeys);
	}

	public int executeUpdate(String sql, int columnIndexes[]) throws SQLException {
		return _stmt.executeUpdate(sql, columnIndexes);
	}

	public int executeUpdate(String sql, String columnNames[]) throws SQLException {
		return _stmt.executeUpdate(sql, columnNames);
	}

	public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
		return _stmt.execute(sql, autoGeneratedKeys);
	}

	public boolean execute(String sql, int columnIndexes[]) throws SQLException {
		return _stmt.execute(sql, columnIndexes);
	}

	public boolean execute(String sql, String columnNames[]) throws SQLException {
		return _stmt.execute(sql, columnNames);
	}

	public int getResultSetHoldability() throws SQLException {
		return _stmt.getResultSetHoldability();
	}

	/*
	 * Note was protected prior to JDBC 4 TODO Consider adding build flags to
	 * make this protected unless we are using JDBC 4.
	 */
	public boolean isClosed() throws SQLException {
		return _stmt.isClosed();
	}

	/* JDBC_4_ANT_KEY_BEGIN */

	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return iface.isAssignableFrom(getClass()) || _stmt.isWrapperFor(iface);
	}

	public <T> T unwrap(Class<T> iface) throws SQLException {
		if (iface.isAssignableFrom(getClass())) {
			return iface.cast(this);
		} else if (iface.isAssignableFrom(_stmt.getClass())) {
			return iface.cast(_stmt);
		} else {
			return _stmt.unwrap(iface);
		}
	}

	public void setPoolable(boolean poolable) throws SQLException {
		_stmt.setPoolable(poolable);
	}

	public boolean isPoolable() throws SQLException {
		return _stmt.isPoolable();
	}
	/* JDBC_4_ANT_KEY_END */

	public void closeOnCompletion() throws SQLException {
		// TODO Auto-generated method stub
		
	}

	public boolean isCloseOnCompletion() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}
}
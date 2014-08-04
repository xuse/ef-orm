package jef.database.innerpool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import jef.database.DbUtils;

/**
 * 实现，单连接，受控
 * 
 * @author jiyi
 * 
 */
public final class SingleManagedConnection extends AbstractJDBCConnection implements IManagedConnection, Connection {

	// 连接池
	protected IManagedConnectionPool parent;

	/**
	 * 构造
	 * 
	 * @param conn
	 * @param parent
	 * @throws SQLException
	 */
	SingleManagedConnection(Connection conn, IManagedConnectionPool parent) throws SQLException {
		this.parent = parent;
		this.conn = conn;
	}

	/*
	 * (non-Javadoc)
	 * @see jef.database.innerpool.IConnection#closePhysical()
	 */
	public void closePhysical() {
		if (conn != null) {
			DbUtils.closeConnection(conn);
			conn = null;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see jef.database.innerpool.IManagedConnection#notifyDisconnect()
	 */
	public void notifyDisconnect() {
		this.setInvalid();
		parent.notifyDbDisconnect();
	}

	/*
	 * (non-Javadoc)
	 * @see jef.database.innerpool.IManagedConnection#setInvalid()
	 */
	public void setInvalid() {
		DbUtils.closeConnection(conn);
		conn = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jef.database.innerpool.IConnection#setKey(java.lang.String)
	 */
	public void setKey(String key) {
	}

	/*
	 * (non-Javadoc)
	 * @see jef.database.innerpool.IConnection#ensureOpen()
	 */
	public void ensureOpen() throws SQLException {
		if (conn != null && conn.isClosed()) {// 检测到关闭的连接后，提示全面检测
			notifyDisconnect();
		}
		if (conn == null) {// 试图创建新连接
			conn = parent.getDatasource().getConnection();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see jef.database.innerpool.IConnection#setSavepoints(java.lang.String)
	 */
	public Savepoints setSavepoints(String savepointName) throws SQLException {
		return new Savepoints(conn, conn.setSavepoint(savepointName));
	}

	

	/**
	 * 表示占用与否
	 * 
	 * @param flag
	 */
	private volatile Object used;
	/**
	 * 为了支持连接池的可重入特性，增加的重用计数器
	 */
	private volatile int count;

	/*
	 * (non-Javadoc)
	 * @see jef.database.innerpool.IConnection#setUsedByObject(java.lang.Object)
	 */
	public void setUsedByObject(Object user) {
		this.used = user;
		count++;
	}

	/*
	 * (non-Javadoc)
	 * @see jef.database.innerpool.IConnection#popUsedByObject()
	 */
	public Object popUsedByObject() {
		if (--count > 0) {
			return null;
		} else {
			Object o = used;
			used = null;
			return o;
		}
	}
	/*
	 * (non-Javadoc)
	 * @see jef.database.innerpool.IConnection#addUsedByObject()
	 */
	public void addUsedByObject() {
		count++;
	}
	/*
	 * (non-Javadoc)
	 * @see jef.database.innerpool.IConnection#isUsed()
	 */
	public boolean isUsed() {
		return count > 0;
	}
	

	public void close() throws SQLException {
		parent.offer(this);
	}

	public boolean checkValid(String testSql) throws SQLException {
		if(conn==null)return true;
		PreparedStatement st=conn.prepareStatement(testSql);
		try{
			st.execute();
			return true;
		}finally{
			DbUtils.close(st);
		}
	}
	
	public boolean checkValid(int timeout) throws SQLException {
		if(conn==null)return true;
		return conn.isValid(timeout);
	}
}

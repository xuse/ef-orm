package jef.database.innerpool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.sql.DataSource;

import jef.database.DbUtils;
import jef.database.innerpool.PoolService.CheckableConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jiyi
 * 
 */
final class SingleConnection extends AbstractJDBCConnection implements CheckableConnection,ReentrantConnection{
	private static Logger log = LoggerFactory.getLogger(SingleConnection.class);
	/**
	 * Belongs to the connection pool.
	 */
	private IPool<SingleConnection> parent;
	/**
	 * The connection was locked by the object.
	 */
	private volatile Object used;
	/**
	 * Reentrant count of the lock.
	 */
	private volatile int count;

	/**
	 * 构造
	 * 
	 * @param conn
	 * @param parent
	 * @throws SQLException
	 */
	SingleConnection(Connection connection, IPool<SingleConnection> parent) {
		this.conn = connection;
		this.parent = parent;
	}

	public void closePhysical() {
		if (conn != null) {
			DbUtils.closeConnection(conn);
			conn = null;
		}
	}

	/////////////////////归还逻辑＼重连逻辑＼路由逻辑/////////////////////
	public void close(){
		parent.offer(this);
	}
	
	public void ensureOpen() throws SQLException {
		if (conn != null && conn.isClosed()) {// 检测到关闭的连接后，提示全面检测
			conn=null;
		}
		if (conn == null) {// 试图创建新连接
			long start = System.currentTimeMillis();
			DataSource ds = parent.getDatasource();
			conn = ds.getConnection();
			log.info("Create connection to {}, cost {}ms.", ds, System.currentTimeMillis() - start);
		}
	}
	
	public void setKey(String key) {
	}
	/////////////////有效性检查/////////////////
	public void setInvalid() {
		closePhysical();
	}

	
	public boolean checkValid(String testSql) throws SQLException {
		if (conn == null)
			return true;
		PreparedStatement st = conn.prepareStatement(testSql);
		try {
			st.execute();
			return true;
		} finally {
			DbUtils.close(st);
		}
	}

	public boolean checkValid(int timeout) throws SQLException {
		if (conn == null)
			return true;
		return conn.isValid(timeout);
	}	
	
	///////////////////重入记录////////////////

	public void setUsedByObject(Object user) {
		this.used = user;
		count++;
	}

	public Object popUsedByObject() {
		if (--count > 0) {
			// log.debug("not return connection {} counter:{}.",used,count);
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
}

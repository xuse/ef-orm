package jef.database.innerpool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.persistence.PersistenceException;

import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.statement.ProcessablePreparedStatement;
import jef.database.jdbc.statement.ResultSetLaterProcess;

public class WrapableConnection extends AbstractJDBCConnection implements Connection {
	private final DatabaseDialect profile;
	private final boolean normalTx;
	
	public WrapableConnection(Connection conn, DatabaseDialect profile, boolean normal) {
		super(conn);
		this.profile=profile;
		this.normalTx=normal;
	}

	@Override
	public void close(){
		try {
			conn.close();
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
	}

	public DatabaseDialect getProfile() {
		return profile;
	}

	/*
	 * 准备执行SQL
	 */
	public PreparedStatement prepareStatement(String sql) throws SQLException {
		return profile.wrap(conn.prepareStatement(sql),normalTx);
	}

	/*
	 * 准备执行SQL，插入
	 */
	public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
		return profile.wrap(conn.prepareStatement(sql, columnNames),normalTx);
	}

	/*
	 * 准备执行SQL，插入
	 */
	public PreparedStatement prepareStatement(String sql, int generateKeys) throws SQLException {
		return profile.wrap(conn.prepareStatement(sql, generateKeys),normalTx);
	}

	/*
	 * 准备执行SQL，插入
	 */
	public PreparedStatement prepareStatement(String sql, int[] columnIndexs) throws SQLException {
		return profile.wrap(conn.prepareStatement(sql, columnIndexs),normalTx);
	}

	/*
	 * 准备执行SQL，查询
	 */
	public PreparedStatement prepareStatement(String sql, ResultSetLaterProcess rslp, boolean isUpdatable) throws SQLException {
		PreparedStatement st;
		int rsType = (isUpdatable) ? ResultSet.TYPE_SCROLL_INSENSITIVE : ResultSet.TYPE_FORWARD_ONLY;
		int rsUpdate = isUpdatable ? ResultSet.CONCUR_UPDATABLE : ResultSet.CONCUR_READ_ONLY;
		st = conn.prepareStatement(sql, rsType, rsUpdate);
		if (rslp != null) {
			st = new ProcessablePreparedStatement(st, rslp);
		}
		return profile.wrap(st,normalTx);
	}

	/*
	 * 准备执行SQL，查询
	 */
	public PreparedStatement prepareStatement(String sql, int rsType, int concurType, int hold) throws SQLException {
		return profile.wrap(conn.prepareStatement(sql, rsType, concurType, hold),normalTx);
	}
}

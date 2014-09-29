package jef.database.support.executor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

import javax.persistence.PersistenceException;

import jef.common.log.LogUtil;
import jef.database.BindVariableContext;
import jef.database.BindVariableTool;
import jef.database.DbUtils;
import jef.database.DebugUtil;
import jef.database.ORMConfig;
import jef.database.dialect.DatabaseDialect;
import jef.database.innerpool.IConnection;
import jef.database.innerpool.IUserManagedPool;
import jef.database.wrapper.result.ResultSetWrapper;
import jef.tools.StringUtils;

public class ExecutorImpl implements StatementExecutor{
	IConnection conn;
	Statement st;
	private IUserManagedPool parent;
	private String dbkey;
	private String txId;
	private DatabaseDialect profile;

	public ExecutorImpl(IUserManagedPool parent, String dbkey, String txId) {
		this.parent = parent;
		this.dbkey = dbkey;
		this.txId = txId;
		this.profile = parent.getProfile(dbkey);
		try {
			init();
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
		
	}
	
	private boolean init() throws SQLException {
		try {
			conn = parent.poll();
			conn.setKey(dbkey);
			if(!conn.getAutoCommit()){
				conn.setAutoCommit(true);
			}
			st = conn.createStatement();
			return true;
		} catch (SQLException e) {
			DbUtils.closeConnection(conn);// If error at create statement
											// then close connection.
			conn = null;
			throw e;
		}
	}
	

	private void doSql(Statement st, String txId, String sql) throws SQLException {
		try {
			st.executeUpdate(sql);
		} catch (SQLException e) {
			DebugUtil.setSqlState(e, sql);
			throw e;
		}finally{
			if (ORMConfig.getInstance().isDebugMode()) {
				LogUtil.show(sql + " |" + txId);
			}
		}
	}
	
	
	@Override
	public void executeSql(String... ddls) throws SQLException {
		for(String sql:ddls){
			doSql(st, txId, sql);
		}
	}

	@Override
	public void executeSql(List<String> ddls) throws SQLException {
		for(String sql:ddls){
			doSql(st, txId, sql);
		}
	}

	@Override
	public void close() {
		DbUtils.close(st);
		DbUtils.closeConnection(conn);
	}

	@Override
	public void setQueryTimeout(int seconds) throws SQLException {
		if(st!=null){
			st.setQueryTimeout(seconds);
		}
	}

	@Override
	public ResultSet executeQuery(String sql, Object... params) throws SQLException {
		PreparedStatement ps = conn.prepareStatement(sql);
		for (int i = 0; i < params.length; i++) {
			ps.setObject(i + 1, params[i]);
		}
		ResultSet rs = ps.executeQuery();
		return new ResultSetWrapper(null, ps, rs);
	}

	@Override
	public int executeUpdate(String sql, Object... params) throws SQLException {
		boolean debug = ORMConfig.getInstance().isDebugMode();
		StringBuilder sb = null;
		if (debug)
			sb = new StringBuilder(sql).append("\t|").append(txId);

		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(sql);
			if (params.length > 0) {
				BindVariableContext context = new BindVariableContext(ps, profile, sb);
				BindVariableTool.setVariables(context, Arrays.asList(params));
			}
		} finally {
			if (debug) {
				LogUtil.show(sb);
			}
		}
		try {
			int total = ps.executeUpdate();
			if (debug)
				LogUtil.show(StringUtils.concat("Executed:", String.valueOf(total), "\t |", txId));
			return total;
		} finally {
			DbUtils.close(ps);
		}
	}
}

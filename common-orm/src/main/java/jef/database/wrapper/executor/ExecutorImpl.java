package jef.database.wrapper.executor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

import javax.persistence.PersistenceException;

import org.springframework.util.Assert;

import jef.database.DbUtils;
import jef.database.DebugUtil;
import jef.database.ORMConfig;
import jef.database.dialect.DatabaseDialect;
import jef.database.innerpool.IConnection;
import jef.database.innerpool.IUserManagedPool;
import jef.database.jdbc.result.CloseableResultSet;
import jef.database.support.SqlLog;
import jef.database.wrapper.processor.BindVariableContext;

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
		Assert.notNull(sql);
		SqlLog log=ORMConfig.getInstance().newLogger(sql.length()+60);
		try {
			long start=System.currentTimeMillis();
			log.append(sql).append(" | ",txId);
			st.executeUpdate(sql);
			log.append("\nExecuted: ",System.currentTimeMillis()-start).append("ms");
		} catch (SQLException e) {
			DebugUtil.setSqlState(e, sql);
			throw e;
		}finally{
			log.output();
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
		return new CloseableResultSet(ps, rs);
	}

	@Override
	public int executeUpdate(String sql, Object... params) throws SQLException {
		SqlLog sb = ORMConfig.getInstance().newLogger(sql.length()+64).append(sql,"\t|").append(txId);
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(sql);
			if (params.length > 0) {
				BindVariableContext context = new BindVariableContext(ps, profile, sb);
				context.setVariables(Arrays.asList(params));
			}
			int total = ps.executeUpdate();
			sb.append("\nExecuted:",total).append("\t |",txId);
			return total;
		} finally {
			sb.output();
			DbUtils.close(ps);
		}
	}
}

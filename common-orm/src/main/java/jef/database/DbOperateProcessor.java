package jef.database;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;

import jef.common.Entry;
import jef.common.log.LogUtil;
import jef.database.BindVariableTool.SqlType;
import jef.database.annotation.PartitionResult;
import jef.database.wrapper.clause.BindSql;
import jef.tools.StringUtils;

public final class DbOperateProcessor {



	int processDeletePrepared(OperateTarget db, IQueryableEntity obj, PartitionResult site, long start, BindSql where) throws SQLException {
		int count = 0;
		long parse = System.currentTimeMillis();
		boolean debugMode=ORMConfig.getInstance().isDebugMode();
		for (String tablename : site.getTables()) {
			String sql = "delete from " + tablename + where.getSql();
			StringBuilder sb = null;
			if (debugMode)
				sb = new StringBuilder(sql.length() + 150).append(sql).append(" | ").append(db.getTransactionId());
			PreparedStatement psmt = null;
			try {
				psmt = db.prepareStatement(sql);
				int deleteTimeout=ORMConfig.getInstance().getDeleteTimeout();
				if(deleteTimeout>0){
					psmt.setQueryTimeout(deleteTimeout);
				}
				BindVariableContext context = new BindVariableContext(psmt,db, sb);
				BindVariableTool.setVariables(obj.getQuery(), null, where.getBind(), context);
				psmt.execute();
				count += psmt.getUpdateCount();
			} catch (SQLException e) {
				processError(e, tablename, db);
				db.releaseConnection();// 如果在处理过程中发现异常，方法即中断，就要释放连接，这样就不用在外面再套一层finally
				throw e;
			} catch (RuntimeException e) {
				db.releaseConnection();// 如果在处理过程中发现异常，方法即中断，就要释放连接，这样就不用在外面再套一层finally
			} finally {
				if (debugMode)
					LogUtil.show(sb.append(" | ").append(db.getTransactionId()));
				if (psmt != null)
					psmt.close();
			}
		}
		if (debugMode)
			LogUtil.show(StringUtils.concat("Deleted:", String.valueOf(count), "\t Time cost([ParseSQL]:", String.valueOf(parse - start), "ms, [DbAccess]:", String.valueOf(System.currentTimeMillis() - parse), "ms) |", db.getTransactionId()));
		db.releaseConnection();
		return count;
	}

	/*
	 * 注意，这个方法执行期间会调用连接，因此必须在这个方法执行完后才能释放连接
	 * @param e
	 * @param tablename
	 * @param conn
	 */
	protected void processError(SQLException e, String tablename, OperateTarget conn) {
		conn.notifyDisconnect(e);
		DebugUtil.setSqlState(e, tablename);
	}

	int processDeleteNormal(OperateTarget db, IQueryableEntity obj, PartitionResult site, long start, String where) throws SQLException {
		int count = 0;
		boolean debugMode=ORMConfig.getInstance().isDebugMode();
		long parse = System.currentTimeMillis();
		Statement st=null;
		String tablename=null;
		try {
			st= db.createStatement();
			int deleteTimeout=ORMConfig.getInstance().getDeleteTimeout();
			if(deleteTimeout>0)st.setQueryTimeout(deleteTimeout);
			for (Iterator<String> iter=site.getTables().iterator();iter.hasNext();) {
				tablename =iter.next();
				StringBuilder sql = new StringBuilder("delete from ").append(tablename).append(where);
				try{
					count += st.executeUpdate(sql.toString());	
				}finally{
					if (debugMode)
						LogUtil.show(sql + " | " + db.getTransactionId());	
				}
			}
			if (debugMode)
				LogUtil.show(StringUtils.concat("Deleted:", String.valueOf(count), "\t Time cost([ParseSQL]:", String.valueOf(parse - start), "ms, [DbAccess]:", String.valueOf(System.currentTimeMillis() - parse), "ms) |", db.getTransactionId()));
		} catch (SQLException e) {
			processError(e, tablename, db);
			throw e;
		} finally {
			if (st != null)
				st.close();
			db.releaseConnection();
		}
		return count;
	}

	int processUpdatePrepared(OperateTarget db, IQueryableEntity obj, Entry<List<String>, List<Field>> setValues, BindSql whereValues, PartitionResult p, long start) throws SQLException {
		long parse = System.currentTimeMillis();
		boolean debugMode=ORMConfig.getInstance().isDebugMode();
		List<String> sql = setValues.getKey();
		int result = 0;
		for (String tablename : p.getTables()) {
			String updateSql = StringUtils.concat("update ", tablename, " set ",StringUtils.join(sql,", "), whereValues.getSql());
			StringBuilder sb = null;
			if (debugMode)
				sb = new StringBuilder(updateSql.length() + 150).append(updateSql).append(" | ").append(db.getTransactionId());
			PreparedStatement psmt = null;
			try {
				
				psmt = db.prepareStatement(updateSql);
				int updateTimeout=ORMConfig.getInstance().getUpdateTimeout();
				if(updateTimeout>0){
					psmt.setQueryTimeout(updateTimeout);
				}
				BindVariableContext context = new BindVariableContext(psmt,db, sb);
				BindVariableTool.setVariables(obj.getQuery(), setValues.getValue(), whereValues.getBind(), context);
				psmt.execute();
				int currentUpdateCount = psmt.getUpdateCount();
				result += currentUpdateCount;
				obj.applyUpdate();
			} catch (SQLException e) {
				processError(e, tablename, db);
				throw e;
			} finally {
				if (debugMode)
					LogUtil.show(sb);

				if (psmt != null)
					psmt.close();
			}
		}
		db.releaseConnection();
		if (debugMode)
			showUpdateLogIfTimeoutOrInDebugMode(db, null, start, parse, result);
		return result;
	}

	protected void showUpdateLogIfTimeoutOrInDebugMode(OperateTarget db, String sql, long start, long parse, int result) {
		long dbAccess = System.currentTimeMillis() - parse;
		parse = parse - start;
		LogUtil.show(StringUtils.concat("Updated:", String.valueOf(result), "\t Time cost([ParseSQL]:", String.valueOf(parse), "ms, [DbAccess]:", String.valueOf(dbAccess), "ms) |",db.getTransactionId()));
	}

	int processUpdateNormal(OperateTarget db, IQueryableEntity obj, long start, String where, String update, PartitionResult site) throws SQLException {
		int result = 0;
		long parse = System.currentTimeMillis();
		for (String tablename : site.getTables()) {
			String sql = "update " + tablename + " set " + update + where;
			Statement st = null;
			try {
				st = db.createStatement();
				int updateTimeout=ORMConfig.getInstance().getUpdateTimeout();
				if(updateTimeout>0)st.setQueryTimeout(updateTimeout);
				int currentUpdateCount = st.executeUpdate(sql);
				result += currentUpdateCount;
				obj.applyUpdate();
			} catch (SQLException e) {
				processError(e, tablename, db);
				throw e;
			} finally {
				if (ORMConfig.getInstance().isDebugMode())
					LogUtil.show(sql + " | " + db.getTransactionId());

				if (st != null)
					st.close();
			}
		}
		db.releaseConnection();
		showUpdateLogIfTimeoutOrInDebugMode(db, null, start, parse, result);
		return result;
	}
}

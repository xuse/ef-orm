package jef.database;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;

import jef.common.log.LogUtil;
import jef.database.annotation.PartitionResult;
import jef.database.dialect.DatabaseDialect;
import jef.database.meta.Feature;
import jef.database.query.JoinElement;
import jef.database.query.SqlContext;
import jef.database.wrapper.clause.BindSql;
import jef.database.wrapper.processor.BindVariableContext;
import jef.database.wrapper.processor.BindVariableTool;
import jef.tools.StringUtils;

/**
 * 基本数据库操作
 * 
 * @author jiyi
 * 
 */
public abstract class DeleteProcessor {
	abstract int processDelete(OperateTarget db, IQueryableEntity obj, BindSql where, PartitionResult site, long parseCost) throws SQLException;
	abstract BindSql toWhereClause(JoinElement joinElement,SqlContext context,boolean update,DatabaseDialect profile);
	
	
	static DeleteProcessor get(DatabaseDialect profile,DbClient parent){
		if(profile.has(Feature.NO_BIND_FOR_DELETE)){
			return new NormalImpl(parent);
		}else{
			return new PreparedImpl(parent);
		}
	}
	
	private static final class NormalImpl extends DeleteProcessor {
		private DbClient parent;
		public NormalImpl(DbClient parent) {
			this.parent=parent;
		}

		int processDelete(OperateTarget db, IQueryableEntity obj, BindSql where, PartitionResult site, long parseCost) throws SQLException {
			int count = 0;
			boolean debugMode = ORMConfig.getInstance().isDebugMode();
			long parse = System.currentTimeMillis();
			Statement st = null;
			String tablename = null;
			try {
				st = db.createStatement();
				int deleteTimeout = ORMConfig.getInstance().getDeleteTimeout();
				if (deleteTimeout > 0)
					st.setQueryTimeout(deleteTimeout);
				for (Iterator<String> iter = site.getTables().iterator(); iter.hasNext();) {
					tablename = iter.next();
					StringBuilder sql = new StringBuilder("delete from ").append(tablename).append(where.toString());
					try {
						count += st.executeUpdate(sql.toString());
					} finally {
						if (debugMode)
							LogUtil.show(sql + " | " + db.getTransactionId());
					}
				}
				if (debugMode)
					LogUtil.show(StringUtils.concat("Deleted:", String.valueOf(count), "\t Time cost([ParseSQL]:", String.valueOf(parse - parseCost), "ms, [DbAccess]:", String.valueOf(System.currentTimeMillis() - parse), "ms) |", db.getTransactionId()));
			} catch (SQLException e) {
				DbUtils.processError(e, tablename, db);
				throw e;
			} finally {
				if (st != null)
					st.close();
				db.releaseConnection();
			}
			return count;
		}

		@Override
		BindSql toWhereClause(JoinElement joinElement, SqlContext context, boolean update, DatabaseDialect profile) {
			return parent.rProcessor.toWhereClause(joinElement, context, update, profile);
		}
	}

	private  static final class PreparedImpl extends DeleteProcessor{
		private DbClient parent;
		
		public PreparedImpl(DbClient parent) {
			this.parent=parent;
		}

		int processDelete(OperateTarget db, IQueryableEntity obj, BindSql where, PartitionResult site, long parseCost) throws SQLException {
			int count = 0;
			long parse = System.currentTimeMillis();
			boolean debugMode = ORMConfig.getInstance().isDebugMode();
			for (String tablename : site.getTables()) {
				String sql = "delete from " + tablename + where.getSql();
				StringBuilder sb = null;
				if (debugMode)
					sb = new StringBuilder(sql.length() + 150).append(sql).append(" | ").append(db.getTransactionId());
				PreparedStatement psmt = null;
				try {
					psmt = db.prepareStatement(sql);
					int deleteTimeout = ORMConfig.getInstance().getDeleteTimeout();
					if (deleteTimeout > 0) {
						psmt.setQueryTimeout(deleteTimeout);
					}
					BindVariableContext context = new BindVariableContext(psmt, db.getProfile(), sb);
					BindVariableTool.setVariables(obj.getQuery(), null, where.getBind(), context);
					psmt.execute();
					count += psmt.getUpdateCount();
				} catch (SQLException e) {
					DbUtils.processError(e, tablename, db);
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
				LogUtil.show(StringUtils.concat("Deleted:", String.valueOf(count), "\t Time cost([ParseSQL]:", String.valueOf(parse - parseCost), "ms, [DbAccess]:", String.valueOf(System.currentTimeMillis() - parse), "ms) |", db.getTransactionId()));
			db.releaseConnection();
			return count;
		}

		@Override
		BindSql toWhereClause(JoinElement joinElement, SqlContext context, boolean update, DatabaseDialect profile) {
			return parent.rProcessor.toPrepareWhereSql(joinElement, context, update, profile);
		}
	}
}

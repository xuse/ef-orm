package jef.database;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import jef.common.log.LogUtil;
import jef.database.annotation.PartitionResult;
import jef.database.dialect.DatabaseDialect;
import jef.database.meta.Feature;
import jef.database.query.JoinElement;
import jef.database.query.SqlContext;
import jef.database.wrapper.clause.BindSql;
import jef.database.wrapper.executor.DbTask;
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
	abstract int processDelete0(OperateTarget db, IQueryableEntity obj, BindSql where, PartitionResult site) throws SQLException;

	abstract BindSql toWhereClause(JoinElement joinElement, SqlContext context, boolean update, DatabaseDialect profile);

	static DeleteProcessor get(DatabaseDialect profile, DbClient parent) {
		if (profile.has(Feature.NO_BIND_FOR_DELETE)) {
			return new NormalImpl(parent);
		} else {
			return new PreparedImpl(parent);
		}
	}

	final int processDelete(Session session,final IQueryableEntity obj, final BindSql where, PartitionResult[] sites, long parseCost) throws SQLException {
		long parse = System.currentTimeMillis();
		boolean debugMode = ORMConfig.getInstance().isDebugMode();
		int total=0;
		String dbname = null;
		if (sites.length >= ORMConfig.getInstance().getParallelSelect()) {
			List<DbTask> tasks = new ArrayList<DbTask>();
			final AtomicInteger count=new AtomicInteger();
			for (final PartitionResult site : sites) {
				final OperateTarget target=session.asOperateTarget(site.getDatabase());
				dbname=target.getTransactionId();
				tasks.add(new DbTask(){
					@Override
					public void execute() throws SQLException {
						count.addAndGet(processDelete0(target, obj, where, site));
					}
				});
			}
			DbUtils.parallelExecute(tasks);
			total=count.get();
		} else {
			for (PartitionResult site : sites) {
				OperateTarget target=session.asOperateTarget(site.getDatabase());
				dbname=target.getTransactionId();
				total += processDelete0(target, obj, where, site);
			}
		}
		if (debugMode)
			LogUtil.show(StringUtils.concat("Deleted:", String.valueOf(total), "\t Time cost([ParseSQL]:", String.valueOf(parse - parseCost), "ms, [DbAccess]:", String.valueOf(System.currentTimeMillis() - parse), "ms) |", dbname));
		return total;
	}

	private static final class NormalImpl extends DeleteProcessor {
		private DbClient parent;

		public NormalImpl(DbClient parent) {
			this.parent = parent;
		}

		int processDelete0(OperateTarget db, IQueryableEntity obj, BindSql where, PartitionResult site) throws SQLException {
			int count = 0;
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
						if (ORMConfig.getInstance().isDebugMode())
							LogUtil.show(sql + " | " + db.getTransactionId());
					}
				}
				
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

	private static final class PreparedImpl extends DeleteProcessor {
		private DbClient parent;

		public PreparedImpl(DbClient parent) {
			this.parent = parent;
		}

		int processDelete0(OperateTarget db, IQueryableEntity obj, BindSql where, PartitionResult site) throws SQLException {
			int count = 0;
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
			db.releaseConnection();
			return count;
		}

		@Override
		BindSql toWhereClause(JoinElement joinElement, SqlContext context, boolean update, DatabaseDialect profile) {
			return parent.rProcessor.toPrepareWhereSql(joinElement, context, update, profile);
		}
	}
}

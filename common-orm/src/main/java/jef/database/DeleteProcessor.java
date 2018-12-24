package jef.database;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import jef.database.dialect.DatabaseDialect;
import jef.database.innerpool.WrapableConnection;
import jef.database.query.JoinElement;
import jef.database.query.Query;
import jef.database.query.SqlContext;
import jef.database.routing.PartitionResult;
import jef.database.support.SqlLog;
import jef.database.wrapper.clause.BindSql;
import jef.database.wrapper.executor.DbTask;
import jef.database.wrapper.variable.BindVariableContext;

/**
 * 基本数据库操作
 * 
 * @author jiyi
 * 
 */
public class DeleteProcessor {
	static DeleteProcessor get(DatabaseDialect profile, DbClient parent) {
		return new DeleteProcessor(parent.preProcessor);
	}

	final int processDelete(Session session, final Query<?> obj, final BindSql where, PartitionResult[] sites, long parseCost) throws SQLException {
		long parse = System.currentTimeMillis();
		final SqlLog log = ORMConfig.getInstance().newLogger();
		int total = 0;
		String dbname = null;
		if (sites.length >= ORMConfig.getInstance().getParallelSelect()) {
			List<DbTask> tasks = new ArrayList<DbTask>();
			final AtomicInteger count = new AtomicInteger();
			for (final PartitionResult site : sites) {
				final OperateTarget target = session.selectTarget(site.getDatabase());
				dbname = target.getTransactionId();
				tasks.add(new DbTask() {
					@Override
					public void execute() throws SQLException {
						count.addAndGet(processDelete0(target, obj, where, site, log));
					}
				});
			}
			DbUtils.parallelExecute(tasks);
			total = count.get();
		} else {
			for (PartitionResult site : sites) {
				OperateTarget target = session.selectTarget(site.getDatabase());
				dbname = target.getTransactionId();
				total += processDelete0(target, obj, where, site, log);
			}
		}
		log.append("Deleted:", total).append("\t Time cost([ParseSQL]:", parse - parseCost).append("ms, [DbAccess]:", System.currentTimeMillis() - parse).append("ms) |", dbname);
		log.output();
		return total;
	}

	private SqlProcessor parent;

	public DeleteProcessor(SqlProcessor parent) {
		this.parent = parent;
	}

	int processDelete0(OperateTarget provider, Query<?> query, BindSql where, PartitionResult site, SqlLog sb) throws SQLException {
		int count = 0;
		for (String tablename : site.getTables()) {
			String sql = "delete from " + DbUtils.escapeColumn(provider.getProfile(), tablename) + where.getSql();
			sb.ensureCapacity(sql.length() + 150);
			sb.append(sql).append(provider.toString());

			PreparedStatement psmt = null;
			try(WrapableConnection db=provider.get()) {
				psmt = db.prepareStatement(sql);
				int deleteTimeout = ORMConfig.getInstance().getDeleteTimeout();
				if (deleteTimeout > 0) {
					psmt.setQueryTimeout(deleteTimeout);
				}
				BindVariableContext context = new BindVariableContext(psmt, db.getProfile(), sb);
				context.setVariables(query, null, where.getBind());
				psmt.execute();
				count += psmt.getUpdateCount();
			} finally {
				DbUtils.close(psmt);
				sb.output();
			}
		}
		return count;
	}

	BindSql toWhereClause(JoinElement joinElement, SqlContext context, DatabaseDialect profile) {
		return parent.toWhereClause(joinElement, context, null, profile, false);
	}
}

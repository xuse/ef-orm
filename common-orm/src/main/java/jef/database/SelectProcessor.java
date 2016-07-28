package jef.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import jef.common.Entry;
import jef.common.log.LogUtil;
import jef.database.dialect.DatabaseDialect;
import jef.database.innerpool.PartitionSupport;
import jef.database.jdbc.result.ResultSetContainer;
import jef.database.meta.Feature;
import jef.database.meta.ISelectProvider;
import jef.database.meta.MetaHolder;
import jef.database.query.ComplexQuery;
import jef.database.query.ConditionQuery;
import jef.database.query.ISelectItemProvider;
import jef.database.query.Join;
import jef.database.query.JoinElement;
import jef.database.query.OrderField;
import jef.database.query.Query;
import jef.database.query.SelectsImpl;
import jef.database.query.SingleColumnSelect;
import jef.database.query.SqlContext;
import jef.database.routing.PartitionResult;
import jef.database.support.MultipleDatabaseOperateException;
import jef.database.support.SqlLog;
import jef.database.wrapper.clause.BindSql;
import jef.database.wrapper.clause.CountClause;
import jef.database.wrapper.clause.GroupClause;
import jef.database.wrapper.clause.OrderClause;
import jef.database.wrapper.clause.QueryClause;
import jef.database.wrapper.clause.SelectPart;
import jef.database.wrapper.executor.DbTask;
import jef.database.wrapper.processor.BindVariableContext;
import jef.http.client.support.CommentEntry;
import jef.tools.ArrayUtils;
import jef.tools.PageLimit;
import jef.tools.StringUtils;

public abstract class SelectProcessor {

	static SelectProcessor get(DatabaseDialect profile, DbClient db) {
		if (profile.has(Feature.NO_BIND_FOR_SELECT)) {
			return new NormalImpl(db, db.rProcessor);
		} else {
			return new PreparedImpl(db, db.rProcessor);
		}
	}

	/**
	 * 转换为SQL查询语句
	 * 
	 * @param obj
	 *            请求
	 * @param range
	 *            范围
	 * @param myTableName
	 *            自定义表名（排除）
	 * @param withOrder
	 *            带排序
	 * @return
	 */
	public abstract QueryClause toQuerySql(ConditionQuery obj, PageLimit range, boolean withOrder);

	/**
	 * 形成count的语句 可以返回多个count语句，意味着要执行上述全部语句，然后累加
	 * 
	 * @FIXME 目前看来，当设置了投影操作时，这种转换不太靠谱，需要进一步改进计算方式．比如用group子句来实现distinct操作。
	 */
	public abstract CountClause toCountSql(ConditionQuery obj) throws SQLException;

	protected abstract void processSelect0(OperateTarget db, QueryClause sql, PartitionResult site, ConditionQuery queryObj, ResultSetContainer rs, QueryOption option, SqlLog debug) throws SQLException;

	protected abstract long processCount0(OperateTarget db, BindSql bindSqls,SqlLog debug) throws SQLException;

	protected DbClient db;
	public SqlProcessor parent;

	SelectProcessor(DbClient db, SqlProcessor parent) {
		this.db = db;
		this.parent = parent;
	}

	@Deprecated
	public DatabaseDialect getProfile() {
		return parent.getProfile();
	}

	public DatabaseDialect getProfile(PartitionResult[] sites) {
		return this.parent.getProfile(sites);
	}

	public PartitionSupport getPartitionSupport() {
		return db.getPartitionSupport();
	}

	final static class NormalImpl extends SelectProcessor {

		NormalImpl(DbClient db, SqlProcessor parent) {
			super(db, parent);
		}

		public QueryClause toQuerySql(ConditionQuery obj, PageLimit range, boolean order) {
			QueryClause clause = obj.toQuerySql(this, obj.prepare(), order);
			clause.setPageRange(range);
			return clause;
		}

		protected void processSelect0(OperateTarget db, QueryClause sql, PartitionResult site, ConditionQuery queryObj, ResultSetContainer rs2, QueryOption option, SqlLog debug) throws SQLException {
			Statement st = null;
			ResultSet rs = null;
			BindSql bindSql = sql.getSql(site);
			if (option.holdResult && db.getProfile().has(Feature.TYPE_FORWARD_ONLY)) {
				throw new UnsupportedOperationException("The database " + db.getProfile() + " can not support your 'selectForUpdate' operation.");
			}
			try {
				st = db.createStatement(bindSql.getRsLaterProcessor(), option.holdResult);
				option.setSizeFor(st);
				rs = st.executeQuery(bindSql.toString());
				rs2.add(rs, st, db);
				// 提前将连接归还连接池，用于接下来的查询，但是标记这个连接上还有未完成的查询结果集，因此不允许关闭这个连接。
			} catch (SQLException e) {
				DbUtils.close(rs);
				DbUtils.close(st);
				DbUtils.processError(e, ArrayUtils.toString(sql.getTables(), true), db);
				db.releaseConnection();
				throw e;
			} catch (RuntimeException e) {
				DbUtils.close(rs);
				DbUtils.close(st);
				db.releaseConnection();
				throw e;
			} finally {
				if (ORMConfig.getInstance().isDebugMode())
					LogUtil.show(sql + " | " + db.getTransactionId()); // 每个site的查询语句会合并为一条
				// db.releaseConnection();
			}
		}

		@Override
		public CountClause toCountSql(ConditionQuery obj) throws SQLException {
			CountClause result = new CountClause();
			if (obj instanceof Query<?>) {
				Query<?> query = (Query<?>) obj;
				String myTableName = (String) query.getAttribute("_table_name");
				myTableName = MetaHolder.toSchemaAdjustedName(myTableName);

				PartitionResult[] sites = DbUtils.toTableNames(query.getInstance(), myTableName, query, db.getPartitionSupport());
				SqlContext context = query.prepare();
				for (PartitionResult site : sites) {
					List<String> tablenames = site.getTablesEscaped(db.getProfile(site.getDatabase()));
					for (int i = 0; i < tablenames.size(); i++) {
						BindSql sql = parent.toWhereClause(query, context, false, parent.getPartitionSupport().getProfile(site.getDatabase()));
						result.addSql(site.getDatabase(), StringUtils.concat("select count(*) from ", tablenames.get(i), " t", sql.getSql()));
					}
				}
				return result;
			} else if (obj instanceof Join) {
				DatabaseDialect profile = getProfile();
				Join join = (Join) obj;
				SqlContext context = join.prepare();
				result.addSql(null, "select count(*) from " + join.toTableDefinitionSql(parent, context, profile) + parent.toWhereClause(join, context, false, profile));
				return result;
			} else if (obj instanceof ComplexQuery) {
				ComplexQuery cq = (ComplexQuery) obj;
				cq.prepare();
				return cq.toCountSql(this);
			} else {
				throw new IllegalArgumentException();
			}
		}

		@Override
		protected long processCount0(OperateTarget db, BindSql sql,SqlLog log) throws SQLException {
			Statement st = null;
			try {
				st = db.createStatement();
				int selectTimeout = ORMConfig.getInstance().getSelectTimeout();
				if (selectTimeout > 0)
					st.setQueryTimeout(selectTimeout);
				ResultSet rs = null;
				long result;
				try {
					String str=sql.getSql();
					log.append(str ).append(db);
					rs = st.executeQuery(str);
					rs.next();
					result = rs.getLong(1);
					log.append("\tCount=" , result);
				} catch (SQLException e) {
					DbUtils.processError(e, sql.getSql(), db);
					throw e;
				} finally {
					log.output();
					if (rs != null)
						rs.close();
				}
				return result;
			} finally {
				try {
					if (st != null)
						st.close();
				} catch (SQLException e) {
					LogUtil.exception(e);
				}
				db.releaseConnection();
			}
		}
	}

	final static class PreparedImpl extends SelectProcessor {
		PreparedImpl(DbClient db, SqlProcessor parent) {
			super(db, parent);
		}

		public QueryClause toQuerySql(ConditionQuery obj, PageLimit range, boolean order) {
			QueryClause result = obj.toPrepareQuerySql(this, obj.prepare(), order);
			result.setPageRange(range);
			return result;
		}

		protected void processSelect0(OperateTarget db, QueryClause sqlResult, PartitionResult site, ConditionQuery queryObj, ResultSetContainer rs2, QueryOption option, SqlLog sb) throws SQLException {
			// 计算查询结果集参数
			if (option.holdResult && db.getProfile().has(Feature.TYPE_FORWARD_ONLY)) {
				throw new UnsupportedOperationException("The database " + db.getProfile() + " can not support your 'selectForUpdate' operation.");
			}
			BindSql sql = sqlResult.getSql(site);
			PreparedStatement psmt = null;
			ResultSet rs = null;
			
			sb.ensureCapacity(sql.getSql().length() + 150);
			sb.append(sql.getSql()).append(db);
			try {
				psmt = db.prepareStatement(sql.getSql(), sql.getRsLaterProcessor(), option.holdResult);
				BindVariableContext context = new BindVariableContext(psmt, db.getProfile(), sb);
				context.setVariables(queryObj, null, sql.getBind());
				option.setSizeFor(psmt);
				rs = psmt.executeQuery();
				rs2.add(rs, psmt, db);
			} catch (SQLException e) {
				DbUtils.close(rs);
				DbUtils.close(psmt);
				DbUtils.processError(e, ArrayUtils.toString(sqlResult.getTables(), true), db);
				db.releaseConnection();
				throw e;
			} catch (RuntimeException e) {
				DbUtils.close(rs);
				DbUtils.close(psmt);
				db.releaseConnection();
				throw e;
			} finally {
				sb.output();
			}
		}

		@Override
		public CountClause toCountSql(ConditionQuery obj) throws SQLException {
			if (obj instanceof Query<?>) {
				Query<?> query = (Query<?>) obj;
				CountClause cq = new CountClause();
				String myTableName = (String) query.getAttribute("_table_name");
				myTableName = MetaHolder.toSchemaAdjustedName(myTableName);

				PartitionResult[] sites = DbUtils.toTableNames(query.getInstance(), myTableName, query, db.getPartitionSupport());
				DatabaseDialect profile = getProfile(sites);
				SqlContext context = query.prepare();
				GroupClause groupClause = toGroupAndHavingClause(query, context, profile);
				if (sites.length > 1) {// 多数据库下还要Distinct，没办法了
					if (context.isDistinct()) {
						throw new MultipleDatabaseOperateException("Not Supported, Count with 'distinct'");
					} else if (groupClause.isNotEmpty()) {
						throw new MultipleDatabaseOperateException("Not Supported, Count with 'group'");
					}
				}

				BindSql result = parent.toPrepareWhereSql(query, context, false, profile);
				if (context.isDistinct()) {
					String countStr = toSelectCountSql(context.getSelectsImpl(), context, groupClause.isNotEmpty());
					for (PartitionResult site : sites) {
						for (String table : site.getTablesEscaped(db.getProfile(site.getDatabase()))) {
							String sql = StringUtils.concat(countStr, table, " t", result.getSql(), groupClause.toString());
							cq.addSql(site.getDatabase(), new BindSql(sql, result.getBind()));
						}
					}
				} else {
					for (PartitionResult site : sites) {
						for (String table : site.getTablesEscaped(db.getProfile(site.getDatabase()))) {
							String sql = StringUtils.concat("select count(*) from ", table, " t", result.getSql(), groupClause.toString());
							cq.addSql(site.getDatabase(), new BindSql(sql, result.getBind()));
						}
					}
				}
				return cq;
			} else if (obj instanceof Join) {
				Join join = (Join) obj;
				SqlContext context = join.prepare();
				DatabaseDialect profile = getProfile();
				GroupClause groupClause = toGroupAndHavingClause(join, context, profile);

				CountClause cq = new CountClause();
				String countStr;
				if (context.isDistinct()) {
					countStr = toSelectCountSql(context.getSelectsImpl(), context, groupClause.isNotEmpty());
				} else {
					countStr = "select count(*) from ";
				}
				BindSql result = parent.toPrepareWhereSql(join, context, false, profile);
				result.setSql(countStr + join.toTableDefinitionSql(parent, context, profile) + result.getSql() + groupClause);
				cq.addSql(null, result);
				return cq;
			} else if (obj instanceof ComplexQuery) {
				ComplexQuery cq = (ComplexQuery) obj;
				SqlContext context = cq.prepare();
				return cq.toPrepareCountSql(this, context);
			} else {
				throw new IllegalArgumentException();
			}
		}

		private String toSelectCountSql(SelectsImpl selectsImpl, SqlContext context, boolean groupMode) {
			if (selectsImpl == null) {
				return "select count(distinct *) from ";
			}
			List<ISelectItemProvider> items = selectsImpl.getReference();
			if (items.isEmpty() || items.get(0).isAllTableColumns()) {
				return "select count(distinct *) from ";
			}
			StringBuilder sb = new StringBuilder("select count(distinct ");
			int distinctItemCount = 0;
			for (ISelectItemProvider item : items) {
				CommentEntry[] ces = item.getSelectColumns(getProfile(), groupMode, context);
				for (CommentEntry ce : ces) {
					if (distinctItemCount > 0) {
						sb.append(',');
					}
					sb.append(ce.getKey());
					distinctItemCount++;
				}
			}
			sb.append(") from ");
			return sb.toString();
		}

		@Override
		protected long processCount0(OperateTarget db, BindSql bsql,SqlLog sb) throws SQLException {
			long total = 0;
			PreparedStatement psmt = null;
			String sql = bsql.getSql();
			ResultSet rs = null;
			sb.append(sql).append(db);
			long currentCount = 0;
			try {
				psmt = db.prepareStatement(sql);

				psmt.setQueryTimeout(ORMConfig.getInstance().getSelectTimeout());
				BindVariableContext context = new BindVariableContext(psmt, db.getProfile(), sb);
				context.setVariables(null, null, bsql.getBind());
				rs = psmt.executeQuery();
				if (rs.next()) {
					currentCount = rs.getLong(1);
					sb.append("\tCount=").append(currentCount);
					total += currentCount;
				}
			} catch (SQLException e) {
				DbUtils.processError(e, sql, db);
				throw e;
			} finally {
				sb.output();
				DbUtils.close(rs);
				DbUtils.close(psmt);
				db.releaseConnection();
			}
			return total;
		}
	}

	public static OrderClause toOrderClause(ConditionQuery obj, SqlContext context, DatabaseDialect profile) {
		if (obj.getOrderBy() == null || obj.getOrderBy().size() == 0) {
			return OrderClause.DEFAULT;
		}
		List<Entry<String, Boolean>> rs = new ArrayList<Entry<String, Boolean>>();
		StringBuffer sb = new StringBuffer();
		for (OrderField e : obj.getOrderBy()) {
			if (sb.length() > 0) {
				sb.append(",");
			}
			String orderResult = e.toString(profile, context);
			sb.append(orderResult).append(' ');
			sb.append(e.isAsc() ? "ASC" : "DESC");
			rs.add(new Entry<String, Boolean>(orderResult, e.isAsc()));
		}
		return new OrderClause(" order by " + sb.toString(), rs);
	}

	// 转为group + having语句
	public static GroupClause toGroupAndHavingClause(JoinElement q, SqlContext context, DatabaseDialect profile) {
		GroupClause result = new GroupClause();
		for (ISelectItemProvider table : context.getReference()) {
			if (table.getReferenceCol() == null)
				continue;
			for (ISelectProvider field : table.getReferenceCol()) {
				if (field instanceof SingleColumnSelect) {
					SingleColumnSelect column = (SingleColumnSelect) field;
					if ((column.getProjection() & ISelectProvider.PROJECTION_GROUP) > 0) {
						result.addGroup(column.getSelectItem(profile, table.getSchema(), context));
					}
					if ((column.getProjection() & ISelectProvider.PROJECTION_HAVING) > 0) {
						result.addHaving(column.toHavingClause(profile, table.getSchema(), context));
					} else if ((column.getProjection() & ISelectProvider.PROJECTION_HAVING_NOT_SELECT) > 0) {
						result.addHaving(column.toHavingClause(profile, table.getSchema(), context));
					}
				}
			}
		}
		return result;
	}

	/**
	 * 返回SQL的Select列部分,本来JDBC规定接口ResultsetMetadata中可以getTableName(int columnIndex)来获得某个列的表名 但是大多数JDBC驱动都没有实现，返回的是""。 为此，需要对所有字段进行唯一化编码处理
	 */
	public static SelectPart toSelectSql(SqlContext context, GroupClause group, DatabaseDialect profile) {
		boolean groupMode = group.isNotEmpty();
		SelectPart rs = new SelectPart();
		rs.setDistinct(context.isDistinct());
		for (ISelectItemProvider rp : context.getReference()) {// 每个rp就是一张表
			rs.addAll(rp.getSelectColumns(profile, groupMode, context));
		}
		if (!groupMode && profile.has(Feature.SELECT_ROW_NUM) && context.size() == 1) {
			ISelectItemProvider ip = context.getReference().get(0);
			if (ip.isAllTableColumns()) {
				rs.add(new CommentEntry("t.rowid", "rowid_"));
			}
		}
		return rs;
	}

	void processSelect(final QueryClause sql, final Session session, final ConditionQuery queryObj, final ResultSetContainer rs, final QueryOption option, int mustTx) throws SQLException {
		if (sql.isMultiDatabase()) {
			if (sql.getTables().length >= ORMConfig.getInstance().getParallelSelect()) {// 启用并行查询
				List<DbTask> tasks = new ArrayList<DbTask>();
				for (final PartitionResult site : sql.getTables()) {
					final SqlLog debug = ORMConfig.getInstance().newLogger();
					tasks.add(new DbTask() {
						public void execute() throws SQLException {
							processSelect0(session.selectTarget(site.getDatabase()), sql, site, queryObj, rs, option,debug);
						}
					});
				}
				DbUtils.parallelExecute(tasks);
			} else {
				final SqlLog debug = ORMConfig.getInstance().newLogger();
				for (PartitionResult site : sql.getTables()) {
					processSelect0(session.selectTarget(site.getDatabase()), sql, site, queryObj, rs, option,debug);
				}
			}
			sql.parepareInMemoryProcess(null, rs);
		} else {
			// 如果是结果集持有的，那么必须在事务中
			PartitionResult site = sql.getTables()[0];
			OperateTarget target = session.wrapTarget(site.getDatabase(), mustTx);
			processSelect0(target, sql, site, queryObj, rs, option, ORMConfig.getInstance().newLogger());
		}
	}

	long processCount(Session session, CountClause sqls) throws SQLException {
		if (sqls.getSqls().size() >= ORMConfig.getInstance().getParallelSelect()) {
			final AtomicLong total = new AtomicLong();
			List<DbTask> tasks = new ArrayList<DbTask>();
			for (final Map.Entry<String, List<BindSql>> sql : sqls.getSqls().entrySet()) {
				final SqlLog debug = ORMConfig.getInstance().newLogger();
				final OperateTarget target = session.selectTarget(sql.getKey());
				tasks.add(new DbTask() {
					@Override
					public void execute() throws SQLException {
						for (BindSql bs : sql.getValue()) {
							total.addAndGet(processCount0(target, bs,debug));
						}
					}
				});
			}
			DbUtils.parallelExecute(tasks);
			return total.get();
		} else {
			long total = 0;
			final SqlLog debug = ORMConfig.getInstance().newLogger();
			for (Map.Entry<String, List<BindSql>> sql : sqls.getSqls().entrySet()) {
				OperateTarget target = session.selectTarget(sql.getKey());
				for (BindSql bs : sql.getValue()) {
					total += processCount0(target, bs, debug);
				}
			}
			debug.output();
			return total;
		}

	}
}

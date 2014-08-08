package jef.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import jef.common.Entry;
import jef.common.log.LogUtil;
import jef.common.wrapper.IntRange;
import jef.database.BindVariableTool.SqlType;
import jef.database.annotation.PartitionResult;
import jef.database.dialect.DatabaseDialect;
import jef.database.meta.Feature;
import jef.database.meta.ISelectProvider;
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
import jef.database.wrapper.clause.BindSql;
import jef.database.wrapper.clause.CountClause;
import jef.database.wrapper.clause.GroupClause;
import jef.database.wrapper.clause.IQueryClause;
import jef.database.wrapper.clause.OrderClause;
import jef.database.wrapper.clause.QueryClauseImpl;
import jef.database.wrapper.clause.QueryClauseSqlImpl;
import jef.database.wrapper.clause.SelectPart;
import jef.database.wrapper.result.MultipleResultSet;
import jef.http.client.support.CommentEntry;
import jef.tools.ArrayUtils;
import jef.tools.StringUtils;

public abstract class SelectProcessor {
	public abstract IQueryClause toQuerySql(ConditionQuery obj, IntRange range, String myTableName,boolean withOrder);

	/**
	 * 形成count的语句 可以返回多个count语句，意味着要执行上述全部语句，然后累加
	 * 
	 * @deprecated 目前看来，当设置了投影操作时，这种转换很不靠谱
	 */
	public abstract CountClause toCountSql(ConditionQuery obj, String tableName) throws SQLException;

	abstract void processSelect(OperateTarget db, IQueryClause sql, PartitionResult site,ConditionQuery queryObj, MultipleResultSet rs, QueryOption option) throws SQLException;

	abstract int processCount(OperateTarget db, List<BindSql> bindSqls) throws SQLException;

	protected DbClient db;
	protected SqlProcessor parent;
	protected DbOperateProcessor p;

	SelectProcessor(DbClient db, DbOperateProcessor p, SqlProcessor parent) {
		this.db = db;
		this.parent = parent;
		this.p = p;
	}

	public DatabaseDialect getProfile() {
		return parent.getProfile();
	}

	protected OrderClause toOrderClause(ConditionQuery obj, SqlContext context) {
		if (obj.getOrderBy() == null || obj.getOrderBy().size() == 0) {
			return OrderClause.DEFAULT;
		}
		List<Entry<String, Boolean>> rs = new ArrayList<Entry<String, Boolean>>();
		StringBuffer sb = new StringBuffer();
		for (OrderField e : obj.getOrderBy()) {
			if (sb.length() > 0) {
				sb.append(",");
			}
			String orderResult = e.toString(parent, context);
			sb.append(orderResult).append(' ');
			sb.append(e.isAsc() ? "ASC" : "DESC");
			rs.add(new Entry<String, Boolean>(orderResult, e.isAsc()));
		}
		return new OrderClause(" order by " + sb.toString(), rs);
	}

	// 转为group + having语句
	protected GroupClause toGroupAndHavingClause(JoinElement q, SqlContext context) {
		GroupClause result=new GroupClause();
		for (ISelectItemProvider table : context.getReference()) {
			if (table.getReferenceCol() == null)
				continue;
			for (ISelectProvider field : table.getReferenceCol()) {
				if (field instanceof SingleColumnSelect) {
					SingleColumnSelect column = (SingleColumnSelect) field;
					if ((column.getProjection() & ISelectProvider.PROJECTION_GROUP) > 0) {
						result.addGroup(column.getSelectItem(getProfile(), table.getSchema(), context));
					}
					if ((column.getProjection() & ISelectProvider.PROJECTION_HAVING) > 0) {
						result.addHaving(column.toHavingClause(getProfile(), table.getSchema(), context));
					} else if ((column.getProjection() & ISelectProvider.PROJECTION_HAVING_NOT_SELECT) > 0) {
						result.addHaving(column.toHavingClause(getProfile(), table.getSchema(), context));
					}
				}
			}
		}
		return result;
	}

	/**
	 * 返回SQL的Select列部分,本来JDBC规定接口ResultsetMetadata中可以getTableName(int
	 * columnIndex)来获得某个列的表名 但是大多数JDBC驱动都没有实现，返回的是""。 为此，需要对所有字段进行唯一化编码处理
	 */
	protected SelectPart toSelectSql(SqlContext context, GroupClause group) {
		boolean groupMode=group.isNotEmpty();
		SelectPart rs = new SelectPart();
		rs.setDistinct(context.isDistinct());
		for (ISelectItemProvider rp : context.getReference()) {// 每个rp就是一张表
			rs.addAll(rp.getSelectColumns(getProfile(), groupMode, context));
		}
		if (!groupMode && getProfile().has(Feature.SELECT_ROW_NUM) && context.size() == 1) {
			ISelectItemProvider ip = context.getReference().get(0);
			if (ip.isAllTableColumns()) {
				rs.add(new CommentEntry("t.rowid", "rowid_"));
			}
		}
		return rs;
	}

	final static class NormalImpl extends SelectProcessor {

		NormalImpl(DbClient db, DbOperateProcessor p, SqlProcessor parent) {
			super(db, p, parent);
		}

		// 非递归，直接外部调用
		public IQueryClause toQuerySql(ConditionQuery obj, IntRange range, String myTableName,boolean order) {
			QueryClauseImpl sb = new QueryClauseImpl(parent.getProfile());
			if (obj instanceof Query<?>) {
				Query<?> query = (Query<?>) obj;
				SqlContext context = query.prepare();
				GroupClause groupClause = toGroupAndHavingClause(query, context);
				sb.setGrouphavingPart(groupClause);
				sb.setSelectPart(toSelectSql(context, groupClause));
				sb.setTables(DbUtils.toTableNames(query.getInstance(), myTableName, query, db.getPool().getPartitionSupport()), query.getMeta().getName());
				sb.setWherePart(parent.toWhereClause(query, context, false));
				if(order)
					sb.setOrderbyPart(toOrderClause(obj, context));
			} else if (obj instanceof Join) {
				Join join = (Join) obj;
				SqlContext context = join.prepare();
				GroupClause groupClause = toGroupAndHavingClause(join, context);
				sb.setGrouphavingPart(groupClause);
				sb.setSelectPart(toSelectSql(context, groupClause));
				sb.setTableDefinition(join.toTableDefinitionSql(parent, context));
				sb.setWherePart(parent.toWhereClause(join, context, false));
				if(order)
					sb.setOrderbyPart(toOrderClause(join, context));
			} else if (obj instanceof ComplexQuery) {
				ComplexQuery cq = (ComplexQuery) obj;
				SqlContext context = cq.prepare();
				String s = cq.toQuerySql(this);
				QueryClauseSqlImpl result = new QueryClauseSqlImpl(getProfile(), true);
				result.setBody(s);
				if(order){
					result.setOrderbyPart(toOrderClause(cq, context));
				}
				result.setPageRange(range);
				return result;
			} else {
				throw new IllegalArgumentException();
			}
			sb.setPageRange(range);
			return sb;
		}

		public void processSelect(OperateTarget db, IQueryClause sql,PartitionResult site, ConditionQuery queryObj, MultipleResultSet rs2, QueryOption option) throws SQLException {
			Statement st = null;
			ResultSet rs = null;

			int rsType;
			int concurType;
			if (option.holdResult) {
				if (db.getProfile().has(Feature.TYPE_FORWARD_ONLY)) {
					throw new UnsupportedOperationException("The database " + db.getProfile() + " can not support your 'selectForUpdate' operation.");
				}
				rsType = ResultSet.TYPE_SCROLL_INSENSITIVE;
				concurType = ResultSet.CONCUR_UPDATABLE;
			} else {
				rsType = ResultSet.TYPE_FORWARD_ONLY;
				concurType = ResultSet.CONCUR_READ_ONLY;
			}
			try {
				st = db.createStatement(rsType, concurType);
				option.setSizeFor(st);
				rs = st.executeQuery(sql.getSql(site).toString());
				rs2.add(rs, st, db);
				// 提前将连接归还连接池，用于接下来的查询，但是标记这个连接上还有未完成的查询结果集，因此不允许关闭这个连接。
			} catch (SQLException e) {
				DbUtils.close(rs);
				DbUtils.close(st);
				p.processError(e, ArrayUtils.toString(sql.getTables(), true), db);
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
		public CountClause toCountSql(ConditionQuery obj, String customTableName) throws SQLException {
			CountClause result = new CountClause();
			if (obj instanceof Query<?>) {
				Query<?> query = (Query<?>) obj;
				PartitionResult[] tables = DbUtils.toTableNames(query.getInstance(), customTableName, query, db.getPool().getPartitionSupport());
				SqlContext context = query.prepare();
				for (PartitionResult site : tables) {
					List<String> tablenames = site.getTables();
					for (int i = 0; i < tablenames.size(); i++) {
						result.addSql(site.getDatabase(), StringUtils.concat("select count(*) from ", tablenames.get(i), " t", parent.toWhereClause(query, context, false)));
					}
				}
				return result;
			} else if (obj instanceof Join) {
				Join join = (Join) obj;
				SqlContext context = join.prepare();
				result.addSql(null, "select count(*) from " + join.toTableDefinitionSql(parent, context) + parent.toWhereClause(join, context, false));
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
		int processCount(OperateTarget db, List<BindSql> bindSqls) throws SQLException {
			Statement st = null;
			int total = 0;
			boolean debug = ORMConfig.getInstance().isDebugMode();
			try {
				st = db.createStatement();
				int selectTimeout = ORMConfig.getInstance().getSelectTimeout();
				if (selectTimeout > 0)
					st.setQueryTimeout(selectTimeout);
				for (BindSql sql : bindSqls) {
					ResultSet rs = null;
					try {
						rs = st.executeQuery(sql.getSql());
						rs.next();
						total += rs.getInt(1);
					} catch (SQLException e) {
						p.processError(e, sql.getSql(), db);
						throw e;
					} finally {
						if (rs != null)
							rs.close();
						if (debug) {
							LogUtil.show(sql.getSql() + " | " + db.getTransactionId());
						}
					}
				}
				if (debug)
					LogUtil.show("Count:" + total);
				return total;
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
		PreparedImpl(DbClient db, DbOperateProcessor p, SqlProcessor parent) {
			super(db, p, parent);
		}

		public IQueryClause toQuerySql(ConditionQuery obj, IntRange range, String myTableName,boolean order) {
			QueryClauseImpl sb = new QueryClauseImpl(getProfile());
			if (obj instanceof Query<?>) {
				Query<?> query = (Query<?>) obj;
				SqlContext context = query.prepare();
				GroupClause groupClause = toGroupAndHavingClause(query, context);
				BindSql whereResult = parent.toPrepareWhereSql(query, context, false);

				sb.setSelectPart(toSelectSql(context, groupClause));
				sb.setGrouphavingPart(groupClause);
				sb.setTables(DbUtils.toTableNames(query.getInstance(), myTableName, query, db.getPool().getPartitionSupport()), query.getMeta().getName());
				sb.setWherePart(whereResult.getSql());
				sb.setBind(whereResult.getBind());
				if(order)
					sb.setOrderbyPart(toOrderClause(obj, context));
			} else if (obj instanceof Join) {
				Join join = (Join) obj;
				SqlContext context = join.prepare();
				GroupClause groupClause = toGroupAndHavingClause(join, context);

				sb.setSelectPart(toSelectSql(context, groupClause));
				sb.setTableDefinition(join.toTableDefinitionSql(parent, context));
				BindSql whereResult = parent.toPrepareWhereSql(join, context, false);
				sb.setWherePart(whereResult.getSql());
				sb.setBind(whereResult.getBind());
				sb.setGrouphavingPart(groupClause);
				if(order)
					sb.setOrderbyPart(toOrderClause(join, context));
			} else if (obj instanceof ComplexQuery) {
				ComplexQuery cq = (ComplexQuery) obj;
				SqlContext context = cq.prepare();
				IQueryClause result = cq.toPrepareQuerySql(this, context);
				if(order){
					result.setOrderbyPart(toOrderClause(cq, context));
				}
				result.setPageRange(range);
				return result;
			} else {
				throw new IllegalArgumentException();
			}
			sb.setPageRange(range);
			return sb;
		}

		public void processSelect(OperateTarget db, IQueryClause sqlResult,PartitionResult site, ConditionQuery queryObj, MultipleResultSet rs2, QueryOption option) throws SQLException {
			// 计算查询结果集参数
			int rsType;
			int concurType;
			boolean debugMode = ORMConfig.getInstance().isDebugMode();
			if (option.holdResult) {
				if (db.getProfile().has(Feature.TYPE_FORWARD_ONLY)) {
					throw new UnsupportedOperationException("The database " + db.getProfile() + " can not support your 'selectForUpdate' operation.");
				}
				rsType = ResultSet.TYPE_SCROLL_INSENSITIVE;
				concurType = ResultSet.CONCUR_UPDATABLE;
			} else {
				rsType = ResultSet.TYPE_FORWARD_ONLY;
				concurType = ResultSet.CONCUR_READ_ONLY;
			}
			BindSql sql = sqlResult.getSql(site);
			StringBuilder sb = null;
			PreparedStatement psmt = null;
			ResultSet rs = null;
			if (debugMode)
				sb = new StringBuilder(sql.getSql().length() + 150).append(sql).append(" | ").append(db.getTransactionId());
			try {
				psmt = db.prepareStatement(sql.getSql(), rsType, concurType);
				BindVariableContext context = new BindVariableContext(psmt, db, sb);
				BindVariableTool.setVariables(queryObj, SqlType.SELECT, null, sql.getBind(), context);
				option.setSizeFor(psmt);
				rs = psmt.executeQuery();
				rs2.add(rs, psmt, db);
			} catch (SQLException e) {
				DbUtils.close(rs);
				DbUtils.close(psmt);
				p.processError(e, ArrayUtils.toString(sqlResult.getTables(), true), db);
				db.releaseConnection();
				throw e;
			} catch (RuntimeException e) {
				DbUtils.close(rs);
				DbUtils.close(psmt);
				db.releaseConnection();
				throw e;
			} finally {
				if (debugMode)
					LogUtil.show(sb);
				// db.releaseConnection();因为resultset还没用完，所以这里不释放连接
			}
		}

		@Override
		public CountClause toCountSql(ConditionQuery obj, String tableName) throws SQLException {
			if (obj instanceof Query<?>) {
				Query<?> query = (Query<?>) obj;
				CountClause cq = new CountClause();
				PartitionResult[] sites = DbUtils.toTableNames(query.getInstance(), tableName, query, db.getPool().getPartitionSupport());
				SqlContext context = query.prepare();
				if (context.isDistinct() && sites.length > 1) {// 多数据库下还要Distinct，没办法了
					throw new UnsupportedOperationException("Access multi-databases with distinct operator is unsupported.");
				}
				GroupClause groupClause = toGroupAndHavingClause(query, context);
				BindSql result = parent.toPrepareWhereSql(query, context, false);
				if (context.isDistinct()) {
					String countStr = toSelectCountSql(context.getSelectsImpl(), context, groupClause.isNotEmpty());
					for (PartitionResult site : sites) {
						for (String table : site.getTables()) {
							String sql = StringUtils.concat(countStr, table, " t", result.getSql(), groupClause.toString());
							cq.addSql(site.getDatabase(), new BindSql(sql, result.getBind()));
						}
					}
				} else {
					for (PartitionResult site : sites) {
						for (String table : site.getTables()) {
							String sql = StringUtils.concat("select count(*) from ", table, " t", result.getSql(), groupClause.toString());
							cq.addSql(site.getDatabase(), new BindSql(sql, result.getBind()));
						}
					}
				}
				return cq;
			} else if (obj instanceof Join) {
				Join join = (Join) obj;
				SqlContext context = join.prepare();
				GroupClause groupClause = toGroupAndHavingClause(join, context);

				CountClause cq = new CountClause();
				String countStr;
				if (context.isDistinct()) {
					countStr = toSelectCountSql(context.getSelectsImpl(), context, groupClause.isNotEmpty());
				}else{
					countStr = "select count(*) from ";
				}
				BindSql result = parent.toPrepareWhereSql(join, context, false);
				result.setSql(countStr + join.toTableDefinitionSql(parent, context) + result.getSql()+groupClause);
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
		int processCount(OperateTarget db, List<BindSql> bindSqls) throws SQLException {
			PreparedStatement psmt = null;
			int total = 0;
			boolean debug = ORMConfig.getInstance().isDebugMode();
			for (BindSql bsql : bindSqls) {
				String sql = bsql.getSql();
				ResultSet rs = null;
				StringBuilder sb = new StringBuilder(sql.length() + 150).append(sql).append(" | ").append(db.getTransactionId());
				int currentCount = 0;
				try {
					psmt = db.prepareStatement(sql);

					psmt.setQueryTimeout(ORMConfig.getInstance().getSelectTimeout());
					BindVariableContext context = new BindVariableContext(psmt, db, sb);
					BindVariableTool.setVariables(null, SqlType.SELECT, null, bsql.getBind(), context);
					rs = psmt.executeQuery();
					if(rs.next()){
						currentCount = rs.getInt(1);	
						total += currentCount;	
					}
				} catch (SQLException e) {
					p.processError(e, sql, db);
					throw e;
				} finally {
					if (debug)
						LogUtil.show(sb);
					DbUtils.close(rs);
					DbUtils.close(psmt);
					db.releaseConnection();
				}
			}
			if (debug)
				LogUtil.show("Count:" + total);
			return total;
		}
	}
}

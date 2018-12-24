package jef.database.routing.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import jef.common.log.LogUtil;
import jef.database.ORMConfig;
import jef.database.OperateTarget;
import jef.database.jdbc.GenerateKeyReturnOper;
import jef.database.jsqlparser.statement.select.Limit;
import jef.database.jsqlparser.statement.select.Select;
import jef.database.jsqlparser.statement.select.Union;
import jef.database.jsqlparser.visitor.SelectBody;
import jef.database.jsqlparser.visitor.Statement;
import jef.database.routing.PartitionResult;
import jef.database.routing.jdbc.UpdateReturn;
import jef.database.wrapper.clause.BindSql;
import jef.database.wrapper.populator.AbstractResultSetTransformer;
import jef.database.wrapper.populator.ResultSetExtractor;
import jef.tools.PageLimit;
import jef.tools.StringUtils;

public class SimpleExecutionPlan implements ExecuteablePlan, QueryablePlan {
	private Statement sql;
	private List<Object> params;
	private OperateTarget db;
	private String changeDataSource;

	public String isChangeDatasource() {
		return changeDataSource;
	}

	public SimpleExecutionPlan(Statement sql, List<Object> params, String bindDsName, OperateTarget db) {
		this.changeDataSource = bindDsName;
		this.sql = sql;
		this.params = params;
		this.db = db;
	}

	@Override
	public String getSql(String table) {
		return null;
	}

	@Override
	public UpdateReturn processUpdate(GenerateKeyReturnOper generateKeys) throws SQLException {
		OperateTarget db = this.db;
		if (changeDataSource != null) {
			db = db.getTarget(changeDataSource);
		}
		return db.innerExecuteUpdate(sql.toString(), params, generateKeys);
	}

	@Override
	public boolean isMultiDatabase() {
		return false;
	}

	@Override
	public boolean isSimple() {
		return true;
	}

	@Override
	public PartitionResult[] getSites() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ResultSet getResultSet(SqlAndParameter parse, int maxRows, int fetchSize) throws SQLException {
		OperateTarget db = this.db;
		if (changeDataSource != null) {
			// Scenario 2: 普通查询 (变更数据源，垂直拆分场景)
			db = db.getTarget(changeDataSource);
		}
		String s = processPage(parse, sql, sql.toString());
		return db.innerSelectBySql(s, AbstractResultSetTransformer.getRaw(fetchSize, maxRows), parse.params, parse);
	}

	@Override
	public long getCount(SqlAndParameter paramHolder, int maxSize, int fetchSize) throws SQLException {
		boolean debug = ORMConfig.getInstance().isDebugMode();
		OperateTarget db = this.db;
		if (changeDataSource != null) {
			db = db.getTarget(changeDataSource);
		}
		String sql = paramHolder.statement.toString();
		long start = System.currentTimeMillis();
		long total = db.innerSelectBySql(sql, ResultSetExtractor.COUNT_EXTRACTER, paramHolder.params, paramHolder);
		total = (maxSize > 0 && maxSize < total) ? maxSize : total;
		if (debug) {
			long dbAccess = System.currentTimeMillis();
			LogUtil.show(StringUtils.concat("Count:", String.valueOf(total), "\t ([DbAccess]:", String.valueOf(dbAccess - start), "ms) |",
					db.getTransactionId()));
		}
		return total;
	}

	public boolean mustGetAllResultsToCount() {
		return false;
	}

	/*
	 * @param parse
	 * 
	 * @param sql 如果传入null，则一定是union查询
	 * 
	 * @param rawSQL
	 * 
	 * @return
	 */
	private String processPage(SqlAndParameter parse, Statement sql, String rawSQL) {
		if (parse.getLimit() != null) {
			Limit limit = parse.getLimit();
			long offset = 0;
			int rowcount = 0;
			if (limit.getOffsetJdbcParameter() != null) {
				Object obj = parse.getParamsMap().get(limit.getOffsetJdbcParameter());
				if (obj instanceof Number) {
					offset = ((Number) obj).longValue();
				}
			} else {
				offset = limit.getOffset();
			}
			if (limit.getRowCountJdbcParameter() != null) {
				Object obj = parse.getParamsMap().get(limit.getRowCountJdbcParameter());
				if (obj instanceof Number) {
					rowcount = ((Number) obj).intValue();
				}

			} else {
				rowcount = (int) limit.getRowCount();
			}
			if (offset > 0 || rowcount > 0) {
				parse.setNewLimit(null);
				boolean isUnion = sql == null ? true : (((Select) sql).getSelectBody() instanceof Union);
				BindSql bs = db.getProfile().getLimitHandler().toPageSQL(rawSQL, new PageLimit(offset, rowcount), isUnion);
				parse.setReverseResultSet(bs.getRsLaterProcessor());
				return bs.getSql();
			}
		}
		return rawSQL;
	}

	private String toPageSql(SqlAndParameter context, String rawSQL, PageLimit range) {
		if (range == null && context.getLimit() == null) {
			return rawSQL;
		}
		Statement sql = context.statement;
		if (!(sql instanceof Select))
			return rawSQL;

		if (context.getLimit() != null) {
			Limit limit = context.getLimit();
			long offset = 0;
			int rowcount = 0;
			if (limit.getOffsetJdbcParameter() != null) {
				Object obj = context.getParamsMap().get(limit.getOffsetJdbcParameter());
				if (obj instanceof Number) {
					offset = ((Number) obj).longValue();
				}
			} else {
				offset = limit.getOffset();
			}
			if (limit.getRowCountJdbcParameter() != null) {
				Object obj = context.getParamsMap().get(limit.getRowCountJdbcParameter());
				if (obj instanceof Number) {
					rowcount = ((Number) obj).intValue();
				}

			} else {
				rowcount = (int) limit.getRowCount();
			}
			if (offset > 0 || rowcount > 0) {
				// 优先解析并执行SQL中指定的分页上的操作
				// Range如果null，相当于清空limit，Range如果非null，相当于变为后过滤
				context.setNewLimit(range);

				PageLimit range1 = new PageLimit(offset, rowcount);
				boolean isUnion = ((Select) sql).getSelectBody() instanceof Union;
				BindSql bs = this.db.getProfile().getLimitHandler().toPageSQL(rawSQL, range1, isUnion);
				context.setReverseResultSet(bs.getRsLaterProcessor());
				return bs.getSql();
			}
		}

		if (range != null) {// 再执行后设置的结果限制操作
			SelectBody sb = ((Select) sql).getSelectBody();
			if (sb.getLimit() != null) {// 如果SQL中已经有限制的情况下，Range变为后过滤。
				context.setNewLimit(range);
			} else {
				boolean isUnion = sb instanceof Union;
				BindSql bs = this.db.getProfile().getLimitHandler().toPageSQL(rawSQL, range, isUnion);
				context.setReverseResultSet(bs.getRsLaterProcessor());
				return bs.getSql();
			}
		}
		return rawSQL;
	}

	@Override
	public <T> T doQuery(SqlAndParameter sqlContext, ResultSetExtractor<T> extractor, boolean forCount, PageLimit range) throws SQLException {
		OperateTarget db = this.db;
		if (changeDataSource != null) {
			db = db.getTarget(changeDataSource);
		}
		String rawSQL = sqlContext.statement.toString();
		rawSQL = toPageSql(sqlContext, rawSQL, range);

		return db.innerSelectBySql(rawSQL, extractor, sqlContext.params, sqlContext);
	}
}

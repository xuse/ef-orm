package jef.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import jef.common.PairSO;
import jef.common.log.LogUtil;
import jef.database.annotation.PartitionResult;
import jef.database.jdbc.statement.ResultSetLaterProcess;
import jef.database.jsqlparser.RemovedDelayProcess;
import jef.database.jsqlparser.SqlFunctionlocalization;
import jef.database.jsqlparser.expression.Table;
import jef.database.jsqlparser.statement.select.Limit;
import jef.database.jsqlparser.statement.select.Select;
import jef.database.jsqlparser.statement.select.Union;
import jef.database.jsqlparser.visitor.Statement;
import jef.database.meta.AbstractMetadata;
import jef.database.query.DbTable;
import jef.database.query.ParameterProvider;
import jef.database.routing.jdbc.BatchReturn;
import jef.database.routing.jdbc.ParameterContext;
import jef.database.routing.jdbc.SQLExecutor;
import jef.database.routing.jdbc.UpdateReturn;
import jef.database.routing.sql.ExecutionPlan;
import jef.database.routing.sql.InMemoryOperateProvider;
import jef.database.routing.sql.SelectExecutionPlan;
import jef.database.routing.sql.SqlAnalyzer;
import jef.database.routing.sql.SqlAndParameter;
import jef.database.routing.sql.TableMetaCollector;
import jef.database.wrapper.clause.BindSql;
import jef.database.wrapper.processor.BindVariableContext;
import jef.database.wrapper.processor.BindVariableTool;
import jef.database.wrapper.result.IResultSet;
import jef.database.wrapper.result.MultipleResultSet;
import jef.tools.StringUtils;

import com.google.common.collect.Multimap;

public class RoutingSQLExecutor implements SQLExecutor {
	private OperateTarget db;
	private int fetchSize = ORMConfig.getInstance().getGlobalFetchSize();
	private int maxResult = 0;
	private Statement st;
	private SqlFunctionlocalization l;

	/**
	 * 从SQL语句加上返回类型构造
	 * 
	 * @param db
	 * @param sql
	 * @param resultClass
	 */
	public RoutingSQLExecutor(Transaction db, Statement sql) {
		if (StringUtils.isEmpty(sql)) {
			throw new IllegalArgumentException("Please don't input an empty SQL.");
		}

		this.db = db.asOperateTarget(null);
		this.st = sql;
		l = new SqlFunctionlocalization(db.getProfile(), this.db);
		sql.accept(l);
	}

	/**
	 * 返回fetchSize
	 * 
	 * @return 每次游标获取的缓存大小
	 */
	public int getFetchSize() {
		return fetchSize;
	}

	/**
	 * 设置fetchSize
	 * 
	 * @param size
	 *            设置每次获取的缓冲大小
	 */
	public void setFetchSize(int size) {
		this.fetchSize = size;
	}

	/**
	 * 以迭代器模式返回查询结果
	 * 
	 * @return
	 * @throws SQLException
	 */
	public ResultSet getResultSet(int type, int concurrency, int holder, List<ParameterContext> params) throws SQLException {
		SqlAndParameter parse = getSqlAndParams(db, this, params);
		Statement sql = parse.statement;

		SelectExecutionPlan plan = null;
		plan = (SelectExecutionPlan) SqlAnalyzer.getSelectExecutionPlan((Select) sql, parse.getParamsMap(), parse.params, db);

		if (plan == null) {
			//Scenario 1: Normal Select
			String s = processPage(parse, sql,sql.toString());
			return db.getRawResultSet(s, maxResult, fetchSize, parse.params, parse);
		} else if (plan.isChangeDatasource() != null) {
			//Scenario 2: 普通查询 (变更数据源，垂直拆分场景)
			//Scenario 2: Normal Select (Change datasource)
			OperateTarget db = this.db.getTarget(plan.isChangeDatasource());
			String s = processPage(parse, sql,sql.toString());
			return db.getRawResultSet(s, maxResult, fetchSize, parse.params, parse);
		} else if (plan.isMultiDatabase()) {
			//Scenario 3: 多库查询
			//Scenario 3: Multiple Databases.
			return doMultiDatabaseQuery(plan, parse);
		} else if (plan.isEmpty()) {
			//Scenario 4: 无任何匹配表。但是如果不查ResultSetMetadata无法生成。是否有更好的办法.
			//Scenario 4: No any table.
			return db.getRawResultSet(sql.toString(), maxResult, fetchSize, parse.params, parse);
		} else {
			//Scenario 5: 单库，(单表或多表)，基于Union的查询. 可以使用数据库分页
			//Scenario 5: Single Database(One table or more tables)
			PartitionResult site = plan.getSites()[0];
			PairSO<List<Object>> result = plan.getSql(plan.getSites()[0], false);
			boolean isMultiTable=site.tableSize()>1;
			String s = processPage(parse,isMultiTable?null:sql,result.first);
			return db.getTarget(site.getDatabase()).getRawResultSet(s, maxResult, fetchSize, result.second, parse);
		}
	}

	/*
	 * @param parse 
	 * @param sql 如果传入null，则一定是union查询
	 * @param rawSQL 
	 * @return
	 */
	private String processPage(SqlAndParameter parse, Statement sql,String rawSQL) {
		if(parse.getLimit()!=null){
			Limit limit = parse.getLimit();
			int offset = 0;
			int rowcount = 0;
			if (limit.getOffsetJdbcParameter() != null) {
				Object obj=parse.getParamsMap().get(limit.getOffsetJdbcParameter());
				if(obj instanceof Number){
					offset = ((Number) obj).intValue();
				}
			} else {
				offset = (int)limit.getOffset();
			}
			if (limit.getRowCountJdbcParameter() != null) {
				Object obj=parse.getParamsMap().get(limit.getRowCountJdbcParameter());
				if(obj instanceof Number){
					rowcount = ((Number) obj).intValue();						
				}

			} else {
				rowcount = (int)limit.getRowCount();
			}
			if(offset>0 || rowcount>0){
				parse.setNewLimit(null);
				boolean isUnion = sql==null?true:(((Select) sql).getSelectBody() instanceof Union);
				BindSql bs=db.getProfile().getLimitHandler().toPageSQL(rawSQL, new int[]{offset,rowcount}, isUnion);
				parse.setReverseResultSet(bs.isReverseResult());
				return bs.getSql();
			}
		}
		return rawSQL;
	}

	private ResultSet doMultiDatabaseQuery(SelectExecutionPlan plan, InMemoryOperateProvider parse) throws SQLException {
		ORMConfig config = ORMConfig.getInstance();
		MultipleResultSet mrs = new MultipleResultSet(config.isCacheResultset(), config.debugMode);
		for (PartitionResult site : plan.getSites()) {
			processQuery(db.getTarget(site.getDatabase()), plan.getSql(site, false), 0, mrs,parse.isReverseResult());
		}
		plan.parepareInMemoryProcess(null, mrs);
		if (parse.hasInMemoryOperate()) {
			parse.parepareInMemoryProcess(null, mrs);
		}
		IResultSet irs = mrs.toSimple(null);

		return irs;
	}

	private SqlAndParameter getSqlAndParams(OperateTarget db2, RoutingSQLExecutor jQuery, List<ParameterContext> params) {
		ContextProvider cp = new ContextProvider(params);
		SqlAndParameter sp = new SqlAndParameter(st, SqlAnalyzer.asValue(params), cp);
		if (l.delayLimit != null || l.delayStartWith != null) {
			sp.setInMemoryClause(new RemovedDelayProcess(l.delayLimit, l.delayStartWith));
		}
		return sp;
	}

	/*
	 * 执行查询动作，将查询结果放入mrs
	 */
	private void processQuery(OperateTarget db, PairSO<List<Object>> sql, int max, MultipleResultSet mrs,ResultSetLaterProcess isReverse) throws SQLException {
		StringBuilder sb = null;
		PreparedStatement psmt = null;
		ResultSet rs = null;
		if (mrs.isDebug())
			sb = new StringBuilder(sql.first.length() + 150).append(sql.first).append(" | ").append(db.getTransactionId());
		try {
			psmt = db.prepareStatement(sql.first,isReverse,false);
			BindVariableContext context = new BindVariableContext(psmt, db.getProfile(), sb);
			BindVariableTool.setVariables(context, sql.second);
			if (fetchSize > 0) {
				psmt.setFetchSize(fetchSize);
			}
			if (max > 0) {
				psmt.setMaxRows(max);
			}
			rs = psmt.executeQuery();
			mrs.add(rs, psmt, db);
		} finally {
			if (mrs.isDebug())
				LogUtil.show(sb);
		}
	}

	static class ContextProvider implements ParameterProvider {
		private List<ParameterContext> params;

		public ContextProvider(List<ParameterContext> params) {
			this.params = params;
		}

		@Override
		public Object getNamedParam(String name) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object getIndexedParam(int index) {
			return params.get(index).getValue();
		}

		@Override
		public boolean containsParam(Object key) {
			if (key instanceof Integer) {
				return ((Integer) key) < params.size();
			} else {
				throw new UnsupportedOperationException();
			}
		}
	}

	/**
	 * 对于各种DDL、insert、update、delete等语句，不需要返回结果的，调用此方法来执行
	 * 
	 * @return 返回影响到的记录条数（针对update\delete）语句
	 */
	public UpdateReturn executeUpdate(int generateKeys, int[] returnIndex, String[] returnColumns, List<ParameterContext> params) throws SQLException {
		SqlAndParameter parse = getSqlAndParams(db, this, params);
		Statement sql = parse.statement;

		ExecutionPlan plan = SqlAnalyzer.getExecutionPlan(sql, parse.getParamsMap(), parse.params, db);
		if (plan == null) {
			return db.innerExecuteUpdate(parse.statement.toString(), parse.params, generateKeys, returnIndex, returnColumns);
		} else if (plan.isChangeDatasource() != null) {
			return db.getTarget(plan.isChangeDatasource()).innerExecuteUpdate(parse.statement.toString(), parse.params, generateKeys, returnIndex, returnColumns);
		} else {
			long start = System.currentTimeMillis();
			int total = 0;
			for (PartitionResult site : plan.getSites()) {
				total += plan.processUpdate(site, db);
			}
			if (plan.isMultiDatabase() && ORMConfig.getInstance().debugMode) {
				LogUtil.show(StringUtils.concat("Total Executed:", String.valueOf(total), "\t Time cost([DbAccess]:", String.valueOf(System.currentTimeMillis() - start), "ms) |  @", String.valueOf(Thread.currentThread().getId())));
			}
			return new UpdateReturn(total);
		}
	}

	/**
	 * 限制返回的最大结果数
	 */
	public void setMaxResults(int maxResult) {
		this.maxResult = maxResult;
	}

	/**
	 * 得到查询所在的dbclient对象
	 * 
	 * @return
	 */
	public OperateTarget getDb() {
		return db;
	}

	@Override
	public String toString() {
		return this.st.toString();
	}

	@Override
	public void setQueryTimeout(int queryTimeout) {
	}

	// Batch的约束，每个语句必是单库单表查询
	@Override
	public BatchReturn executeBatch(int autoGeneratedKeys, int[] columnIndexes, String[] columnNames, List<List<ParameterContext>> params) throws SQLException {
		TableMetaCollector collector = SqlAnalyzer.getTableMeta(st);
		if (collector.get() == null) {// 无需路由
			return processBatch(null, null, params, collector, autoGeneratedKeys, columnIndexes, columnNames);
		}
		// 先按路由结果分组
		AbstractMetadata meta = collector.get();
		if (meta.getPartition() == null) {
			if (meta.getBindDsName() != null) {
				DbTable dbTable = meta.getBaseTable(db.getProfile());
				return processBatch(dbTable.getDbName(), null, params, collector, autoGeneratedKeys, columnIndexes, columnNames);
			} else {
				return processBatch(null, null, params, collector, autoGeneratedKeys, columnIndexes, columnNames);
			}
		}
		// 分库分表
		Multimap<String, List<ParameterContext>> result = SqlAnalyzer.doGroup(meta, params, this.st, this.db);
		BatchReturn ur = new BatchReturn();
		for (String s : result.keySet()) {
			int index = s.indexOf('-');
			String db = s.substring(0, index);
			String table = s.substring(index + 1);
			BatchReturn u = processBatch(db, table, params, collector, autoGeneratedKeys, columnIndexes, columnNames);
			ur.merge(u.getBatchResult(), u.getGeneratedKeys());
		}
		return ur;
	}

	private BatchReturn processBatch(String database, String table, Collection<List<ParameterContext>> params, TableMetaCollector collector, int autoGeneratedKeys, int[] columnIndexes, String[] columnNames) throws SQLException {
		OperateTarget db;
		if (database != null && !database.equals(this.db.getDbkey())) {
			db = this.db.getTarget(database);
		} else {
			db = this.db;
		}
		String sql = getSql(table, collector);
		PreparedStatement st = null;
		boolean withGeneratedKeys = false;
		try {
			if (autoGeneratedKeys != java.sql.Statement.NO_GENERATED_KEYS) {
				st = db.prepareStatement(sql, autoGeneratedKeys);
				withGeneratedKeys = true;
			} else if (columnIndexes != null) {
				st = db.prepareStatement(sql, columnIndexes);
				withGeneratedKeys = true;
			} else if (columnNames != null) {
				st = db.prepareStatement(sql, columnNames);
				withGeneratedKeys = true;
			} else {
				st = db.prepareStatement(sql);
			}
			for (List<ParameterContext> record : params) {
				for (ParameterContext context : record) {
					context.apply(st);
				}
				st.addBatch();
			}
			BatchReturn result = new BatchReturn(st.executeBatch());
			if (withGeneratedKeys) {
				result.cacheGeneratedKeys(st.getGeneratedKeys());
			}
			return result;
		} finally {
			DbUtils.close(st);
		}

	}

	private String getSql(String table, TableMetaCollector collector) {
		if (table == null)
			return this.st.toString();
		for (Table t : collector.getModificationPoints()) {
			t.setReplace(table);
		}
		String s = this.st.toString();
		for (Table t : collector.getModificationPoints()) {
			t.removeReplace();
		}
		return s;
	}

}

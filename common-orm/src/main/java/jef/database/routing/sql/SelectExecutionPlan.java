package jef.database.routing.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import jef.common.PairSO;
import jef.common.log.LogUtil;
import jef.common.wrapper.IntRange;
import jef.database.DbUtils;
import jef.database.ORMConfig;
import jef.database.OperateTarget.TransformerAdapter;
import jef.database.jdbc.JDBCTarget;
import jef.database.jdbc.result.IResultSet;
import jef.database.jdbc.result.ResultSetContainer;
import jef.database.jdbc.statement.ResultSetLaterProcess;
import jef.database.jsqlparser.SelectToCountWrapper;
import jef.database.jsqlparser.expression.Table;
import jef.database.jsqlparser.statement.select.Distinct;
import jef.database.jsqlparser.statement.select.Limit;
import jef.database.jsqlparser.statement.select.OrderBy;
import jef.database.jsqlparser.statement.select.OrderByElement;
import jef.database.jsqlparser.statement.select.PlainSelect;
import jef.database.jsqlparser.statement.select.Select;
import jef.database.jsqlparser.statement.select.SelectExpressionItem;
import jef.database.jsqlparser.statement.select.Union;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.SelectBody;
import jef.database.jsqlparser.visitor.SelectItem;
import jef.database.jsqlparser.visitor.Statement;
import jef.database.routing.PartitionResult;
import jef.database.support.SqlLog;
import jef.database.wrapper.clause.BindSql;
import jef.database.wrapper.clause.GroupByItem;
import jef.database.wrapper.clause.GroupFunctionType;
import jef.database.wrapper.clause.InMemoryDistinct;
import jef.database.wrapper.clause.InMemoryGroupByHaving;
import jef.database.wrapper.clause.InMemoryOrderBy;
import jef.database.wrapper.clause.InMemoryPaging;
import jef.database.wrapper.executor.DbTask;
import jef.database.wrapper.populator.AbstractResultSetTransformer;
import jef.database.wrapper.populator.ColumnDescription;
import jef.database.wrapper.populator.ColumnMeta;
import jef.database.wrapper.populator.ResultSetExtractor;
import jef.database.wrapper.processor.BindVariableContext;
import jef.database.wrapper.processor.BindVariableTool;
import jef.tools.StringUtils;

/**
 * 路由查询执行计划
 * 
 * @author jiyi
 * 
 */
public class SelectExecutionPlan extends AbstractExecutionPlan implements QueryablePlan, InMemoryOperateProvider {
	/**
	 * 输入的SQL上下文
	 */
	private StatementContext<PlainSelect> context;

	/*
	 * Select的路由处理是最复杂的—— //SQL操作(查询前) //表名改写 条件：全部 //noGroup
	 * ——SQL尾部以及Select部分中的聚合函数去除 条件:多表(不区分是否多库) //noHaving——位于延迟的SQL尾部 条件:多库()
	 * ——union部分 //Order——位于子查询的尾部 条件：多表(不区分) //limit延迟 条件：单库多表 //limit去除 条件：多库
	 * 
	 * //内存操作(查询后) //内存分页 条件：多库 //内存排序： 条件：多库 //内存排重 条件：多库 //内存分组 条件：多库
	 */
	public SelectExecutionPlan(PartitionResult[] results, StatementContext<PlainSelect> context) {
		super(results);
		this.context = context;
	}

	@Override
	public boolean hasInMemoryOperate() {
		if (isMultiDatabase()) {
			PlainSelect st = context.statement;
			if (st.getGroupByColumnReferences() != null && !st.getGroupByColumnReferences().isEmpty()) {
				return true;
			}
			if (st.getDistinct() != null) {
				return true;
			}
			if (st.getOrderBy() != null) {
				return true;
			}
			if (st.getLimit() != null && st.getLimit().isValid()) {
				return true;
			}
		}
		return false;
	}

	public void parepareInMemoryProcess(IntRange range, ResultSetContainer rs) {
		PlainSelect st = context.statement;
		ColumnMeta meta = rs.getColumns();
		if (st.getGroupByColumnReferences() != null && !st.getGroupByColumnReferences().isEmpty()) {
			rs.setInMemoryGroups(processGroupBy(meta));
		}
		if (st.getDistinct() != null) {
			rs.setInMemoryDistinct(processDistinct(st.getDistinct(), meta));
		}
		if (st.getOrderBy() != null) {
			rs.setInMemoryOrder(processOrder(st.getSelectItems(), st.getOrderBy(), meta));
		}
		if (range != null) {
			int[] ints = range.toStartLimitSpan();
			rs.setInMemoryPage(processPage(meta, ints[0], ints[1]));
		} else if (st.getLimit() != null && st.getLimit().isValid()) {// 此处容错产生效果
			Limit limit = st.getLimit();
			rs.setInMemoryPage(processPage(meta, (int) limit.getOffset(), (int) limit.getRowCount()));
		}
	}

	public String getSql(String table) {
		for (Table t : context.modifications) {
			t.setReplace(table);
		}
		String s = context.statement.toString();
		for (Table t : context.modifications) {
			t.removeReplace();
		}
		return s;
	}

	/**
	 * 多库下的分组查询，由于分组操作依赖最后的内存计算，因此不得不将所有结果都查出后才能计算得到总数
	 * 
	 * @return 如果不得不查出全部结果才能得到总数，返回true
	 */
	public boolean mustGetAllResultsToCount() {
		if (isMultiDatabase()) {
			PlainSelect select = context.statement;
			if (select instanceof SelectToCountWrapper) {
				SelectToCountWrapper wrapper = (SelectToCountWrapper) select;
				if (wrapper.isDistinct()) {
					return true;
				}
				SelectBody sb = (wrapper.getInnerSelectBody());
				if (sb instanceof PlainSelect) {
					PlainSelect ps = (PlainSelect) sb;
					return ps.isGroupBy();
				}
			}
		}
		return false;
	}

	@Override
	public ResultSetLaterProcess getRsLaterProcessor() {
		return null;
	}

	public ResultSet getResultSet(SqlAndParameter parse, int maxRows, int fetchSize) throws SQLException {
		// Scenario 3: 多库查询
		// Scenario 3: Multiple Databases.
		if (isMultiDatabase()) {
			return doMultiDatabaseQuery(parse, maxRows, fetchSize);
		} else if (isEmpty()) {
			// Scenario 4: 无任何匹配表。但是如果不查ResultSetMetadata无法生成。是否有更好的办法.
			// Scenario 4: No any table.
			String sql = context.statement.toString();
			return context.db.innerSelectBySql(sql, AbstractResultSetTransformer.getRaw(fetchSize, maxRows), parse.params, parse);
		} else {
			// Scenario 5: 单库，(单表或多表)，基于Union的查询. 可以使用数据库分页
			// Scenario 5: Single Database(One table or more tables)
			PartitionResult site = getSites()[0];
			PairSO<List<Object>> result = getSql(getSites()[0], false);
			boolean isMultiTable = site.tableSize() > 1;
			String s = processPage(parse, isMultiTable ? null : parse.statement, result.first);
			return context.db.getTarget(site.getDatabase()).innerSelectBySql(s, AbstractResultSetTransformer.getRaw(fetchSize, maxRows), result.second, parse);
		}

	}

	/**
	 * 为NativeQuery场景提供getCount()的实现
	 * 
	 * @param site
	 * @param session
	 * @return
	 * @throws SQLException
	 */
	@Override
	public long getCount(SqlAndParameter parse, int maxSize, int fetchSize) throws SQLException {
		// 多库多表查询场合
		long total = 0;
		long start = System.currentTimeMillis();
		if (sites.length >= ORMConfig.getInstance().getParallelSelect()) {
			final AtomicLong counter = new AtomicLong();
			List<DbTask> tasks = new ArrayList<DbTask>(sites.length);
			for (final PartitionResult site : getSites()) {
				final List<String> sqls = new ArrayList<String>(site.tableSize());
				for (String table : site.getTablesEscaped(context.db.getDialectOf(site.getDatabase()))) {
					sqls.add(getSql(table));
				}
				tasks.add(new DbTask() {
					public void execute() throws SQLException {
						counter.addAndGet(getCount0(site, sqls));
					}
				});
			}
			DbUtils.parallelExecute(tasks);
			total = counter.get();
		} else {
			for (PartitionResult site : getSites()) {
				final List<String> sqls = new ArrayList<String>(site.tableSize());
				for (String table : site.getTables()) {
					sqls.add(getSql(table));
				}
				total += getCount0(site, sqls);
			}
		}
		total = (maxSize > 0 && maxSize < total) ? maxSize : total;
		LogUtil.show(StringUtils.concat("Count:", String.valueOf(total), "\t [DbAccess]:", String.valueOf(System.currentTimeMillis() - start), "ms) |  @", String.valueOf(Thread.currentThread().getId())));
		return total;
	}

	@Override
	public <T> T doQuery(SqlAndParameter sqlContext, ResultSetExtractor<T> extractor, boolean forCount, IntRange range) throws SQLException {
		long start = System.currentTimeMillis();
		String rawSQL = sqlContext.statement.toString();
		T result;
		JDBCTarget db = context.db;
		if (isMultiDatabase()) {// 多库
			result = executeMultiQuery(forCount, extractor, sqlContext, range);
		} else { // 单库多表，基于Union的查询. 可以使用数据库分页
			PartitionResult pr = getSites()[0];
			PairSO<List<Object>> sql = getSql(pr, false);
			rawSQL = sql.first;
			rawSQL = toPageSql(sqlContext, rawSQL, range);
			db = db.getTarget(pr.getDatabase());
			result = db.innerSelectBySql(rawSQL, extractor, sql.second, sqlContext);
		}
		if (ORMConfig.getInstance().isDebugMode()) {
			if (extractor.autoClose() && extractor instanceof TransformerAdapter) {// 普通方式
				long dbAccess = ((TransformerAdapter<?>) extractor).dbAccess;
				List<?> l = (List<?>) result;
				LogUtil.show(StringUtils.concat("Result Count:", String.valueOf(l.size()), "\t Time cost([DbAccess]:", String.valueOf(dbAccess - start), "ms, [Populate]:", String.valueOf(System.currentTimeMillis() - dbAccess), "ms) |", db.getTransactionId()));
			} else {// Iterate方式
				LogUtil.show(StringUtils.concat("Result Iterator:", "\t Time cost([DbAccess]:", String.valueOf(System.currentTimeMillis() - start), "ms  |", db.getTransactionId()));
			}
		}
		return result;
	}

	/*
	 * 执行查询动作，将查询结果放入mrs
	 */
	@SuppressWarnings("rawtypes")
	private static void processQuery(JDBCTarget db, PairSO<List<Object>> sql, ResultSetExtractor rst, ResultSetContainer mrs, ResultSetLaterProcess lazyProcessor, SqlLog sb) throws SQLException {
		PreparedStatement psmt = null;
		ResultSet rs = null;
		sb.ensureCapacity(sql.first.length() + 150);
		sb.append(sql.first).append(db);
		try {
			psmt = db.prepareStatement(sql.first, lazyProcessor, false);
			BindVariableContext context = new BindVariableContext(psmt, db.getProfile(), sb);
			BindVariableTool.setVariables(context, sql.second);
			rst.apply(psmt);
			rs = psmt.executeQuery();
			mrs.add(rs, psmt, db);
		} finally {
			sb.output();
		}
	}

	private <T> T executeMultiQuery(boolean noOrder, final ResultSetExtractor<T> rst, final InMemoryOperateProvider sqlContext, IntRange range) throws SQLException {
		final ORMConfig config = ORMConfig.getInstance();
		final ResultSetContainer mrs = new ResultSetContainer(config.isCacheResultset());
		if (getSites().length >= config.getParallelSelect()) {
			// 并行查询
			List<DbTask> tasks = new ArrayList<DbTask>();
			for (final PartitionResult site : getSites()) {
				final PairSO<List<Object>> sql = getSql(site, noOrder);
				tasks.add(new DbTask() {
					@Override
					public void execute() throws SQLException {
						processQuery(context.db.getTarget(site.getDatabase()), sql, rst, mrs, sqlContext.getRsLaterProcessor(), config.newLogger());
					}
				});
			}
			DbUtils.parallelExecute(tasks);
		} else {
			SqlLog sb = config.newLogger();
			for (PartitionResult site : getSites()) {
				PairSO<List<Object>> sql = getSql(site, noOrder);
				processQuery(context.db.getTarget(site.getDatabase()), sql, rst, mrs, sqlContext.getRsLaterProcessor(), sb);
			}
		}
		IResultSet rsw = null;
		try {
			// 这里可能有问题，在非limit的多库上运行时，
			// limit已经被虚化，并且作为追加任务处理。
			// 此时虚化的Limit会产生何种影响
			parepareInMemoryProcess(range, mrs);
			if (noOrder) { // 去除内存排序
				mrs.setInMemoryOrder(null);
			}
			if (sqlContext.hasInMemoryOperate()) {
				sqlContext.parepareInMemoryProcess(null, mrs);
			}

			rsw = mrs.toProperResultSet(null, rst.getStrategy());
			return rst.transformer(rsw);
		} finally {
			if (rst.autoClose() && rsw != null)
				rsw.close();
		}
	}

	private String toPageSql(SqlAndParameter context, String rawSQL, IntRange range) {
		if (range == null && context.getLimit() == null) {
			return rawSQL;
		}
		Statement sql = context.statement;
		if (!(sql instanceof Select))
			return rawSQL;

		if (context.getLimit() != null) {
			Limit limit = context.getLimit();
			int offset = 0;
			int rowcount = 0;
			if (limit.getOffsetJdbcParameter() != null) {
				Object obj = context.getParamsMap().get(limit.getOffsetJdbcParameter());
				if (obj instanceof Number) {
					offset = ((Number) obj).intValue();
				}
			} else {
				offset = (int) limit.getOffset();
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

				IntRange range1 = new IntRange(offset + 1, offset + rowcount);
				boolean isUnion = ((Select) sql).getSelectBody() instanceof Union;
				BindSql bs = this.context.db.getProfile().getLimitHandler().toPageSQL(rawSQL, range1.toStartLimitSpan(), isUnion);
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
				BindSql bs = this.context.db.getProfile().getLimitHandler().toPageSQL(rawSQL, range.toStartLimitSpan(), isUnion);
				context.setReverseResultSet(bs.getRsLaterProcessor());
				return bs.getSql();
			}
		}
		return rawSQL;
	}

	// 多库Distinct
	private InMemoryDistinct processDistinct(Distinct distinct, ColumnMeta meta) {
		return InMemoryDistinct.instance;
	}

	// 多库排序
	private InMemoryOrderBy processOrder(List<SelectItem> selectItems, OrderBy orderBy, ColumnMeta columns) {
		List<OrderByElement> asSelect = orderBy.getOrderByElements();
		if (asSelect.isEmpty()) {
			return null;
		}
		int[] orders = new int[asSelect.size()];
		boolean[] orderAsc = new boolean[asSelect.size()];

		for (int i = 0; i < asSelect.size(); i++) {
			OrderByElement order = asSelect.get(i);
			String alias = findAlias(order.getExpression().toString(), selectItems);
			if (alias == null) {
				throw new IllegalArgumentException("The order field " + order + " does not selected in SQL!");
			}
			// 可能为null
			ColumnDescription selectedColumn = columns.getByUpperName(alias.toUpperCase());
			if (selectedColumn == null) {
				throw new IllegalArgumentException("The order field " + alias + " does not found in this Query!");
			}
			orders[i] = selectedColumn.getN();//
			orderAsc[i] = order.isAsc();
		}
		return new InMemoryOrderBy(orders, orderAsc);
	}

	private String findAlias(String key, List<SelectItem> selectItems) {
		String alias = null;
		for (SelectItem c : selectItems) {
			if (c.isAllColumns())
				continue;
			SelectExpressionItem item = c.getAsSelectExpression();
			if (key.equals(item.getExpression().toString())) {
				alias = item.getAlias();
				break;
			}
		}
		if (alias == null) {
			alias = StringUtils.substringAfterIfExist(key, ".");
		}
		return alias;
	}

	private InMemoryPaging processPage(ColumnMeta meta, int start, int rows) {
		return new InMemoryPaging(start, rows);
	}

	private InMemoryGroupByHaving processGroupBy(ColumnMeta meta) {
		PlainSelect select = context.statement;
		return generateGroupHavingProcess(select.getSelectItems(), select.getGroupByColumnReferences(), meta);
	}

	/*
	 * 只有当确定select语句中使用了groupBy后才走入当前分支，解析出当前的内存分组任务
	 * 
	 * @param selects
	 * 
	 * @return
	 */
	private InMemoryGroupByHaving generateGroupHavingProcess(List<SelectItem> selects, List<Expression> groupExps, ColumnMeta meta) {
		List<GroupByItem> keys = new ArrayList<GroupByItem>();
		List<GroupByItem> values = new ArrayList<GroupByItem>();

		// 解析出SQL修改句柄，当延迟操作group时，必然要将原先的分组函数去除，配合将groupBy去除

		Set<String> groups = new HashSet<String>();
		for (Expression exp : groupExps) {
			groups.add(exp.toString().toUpperCase());
		}
		for (int i = 0; i < selects.size(); i++) {
			SelectItem e = selects.get(i);
			if (e.isAllColumns())
				continue;
			SelectExpressionItem item = e.getAsSelectExpression();
			String alias = item.getAlias();
			String sql = item.getExpression().toString().toUpperCase();
			if (groups.contains(sql)) {
				keys.add(new GroupByItem(i, GroupFunctionType.GROUP, alias));
			} else {
				GroupFunctionType type;
				String exp = sql.toUpperCase();
				if (exp.startsWith("AVG(")) {
					type = GroupFunctionType.AVG;
				} else if (exp.startsWith("COUNT(")) {
					type = GroupFunctionType.COUNT;
				} else if (exp.startsWith("SUM(")) {
					type = GroupFunctionType.SUM;
				} else if (exp.startsWith("MIN(")) {
					type = GroupFunctionType.MIN;
				} else if (exp.startsWith("MAX(")) {
					type = GroupFunctionType.MAX;
				} else if (exp.startsWith("ARRAY_TO_STRING(")) {
					type = GroupFunctionType.ARRAY_TO_STRING;
				} else {
					type = GroupFunctionType.NORMAL;
				}
				values.add(new GroupByItem(i, type, alias));
			}
		}

		// 解析出having

		return new InMemoryGroupByHaving(keys, values);
	}

	/*
	 * 有没有Group/Having/Distinct/Order/Limit等会引起查询复杂的东西？
	 */
	private boolean hasAnyGroupDistinctOrderLimit() {
		PlainSelect st = context.statement;
		if (st.getDistinct() != null) {
			return true;
		}
		if (st.getLimit() != null) {
			return true;
		}
		if (st.getGroupByColumnReferences() != null && !st.getGroupByColumnReferences().isEmpty()) {
			return true;
		}
		if (st.getOrderBy() != null && !st.getOrderBy().getOrderByElements().isEmpty()) {
			return true;
		}
		return false;
	}

	private void appendSql(StringBuilder sb, String table, boolean noGroup, boolean noHaving, boolean noOrder, boolean noLimit, boolean noDistinct) {
		for (Table tb : context.modifications) {
			tb.setReplace(table);
		}
		context.statement.appendTo(sb, noGroup, noHaving, noOrder, noLimit, noDistinct);
		// 清理现场
		for (Table tb : context.modifications) {
			tb.removeReplace();
		}
	}

	private ResultSet doMultiDatabaseQuery(final InMemoryOperateProvider parse, final int maxRows, final int fetchSize) throws SQLException {
		final ResultSetContainer mrs = new ResultSetContainer(ORMConfig.getInstance().isCacheResultset());
		if (sites.length > ORMConfig.getInstance().getParallelSelect()) {
			List<DbTask> tasks = new ArrayList<DbTask>(sites.length);
			for (final PartitionResult site : getSites()) {
				final PairSO<List<Object>> sql = getSql(site, false);
				tasks.add(new DbTask() {
					public void execute() throws SQLException {
						processQuery(context.db.getTarget(site.getDatabase()), sql, maxRows, fetchSize, mrs, parse.getRsLaterProcessor(), ORMConfig.getInstance().newLogger());
					}
				});
			}
			DbUtils.parallelExecute(tasks);
		} else {
			SqlLog log = ORMConfig.getInstance().newLogger();
			for (PartitionResult site : getSites()) {
				processQuery(context.db.getTarget(site.getDatabase()), getSql(site, false), maxRows, fetchSize, mrs, parse.getRsLaterProcessor(), log);
			}
		}
		parepareInMemoryProcess(null, mrs);
		if (parse.hasInMemoryOperate()) {
			parse.parepareInMemoryProcess(null, mrs);
		}
		return mrs.toProperResultSet(null);
	}

	/*
	 * 执行查询动作，将查询结果放入mrs
	 */
	private void processQuery(JDBCTarget db, PairSO<List<Object>> sql, int max, int fetchSize, ResultSetContainer mrs, ResultSetLaterProcess isReverse, SqlLog sb) throws SQLException {
		PreparedStatement psmt = null;
		ResultSet rs = null;
		sb.ensureCapacity(sql.first.length() + 150);
		sb.append(sql.first).append(db);
		try {
			psmt = db.prepareStatement(sql.first, isReverse, false);
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
			sb.output();
		}
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
			int offset = 0;
			int rowcount = 0;
			if (limit.getOffsetJdbcParameter() != null) {
				Object obj = parse.getParamsMap().get(limit.getOffsetJdbcParameter());
				if (obj instanceof Number) {
					offset = ((Number) obj).intValue();
				}
			} else {
				offset = (int) limit.getOffset();
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
				BindSql bs = context.db.getProfile().getLimitHandler().toPageSQL(rawSQL, new int[] { offset, rowcount }, isUnion);
				parse.setReverseResultSet(bs.getRsLaterProcessor());
				return bs.getSql();
			}
		}
		return rawSQL;
	}

	private long getCount0(PartitionResult site, List<String> sqls) throws SQLException {
		JDBCTarget db = context.db.getTarget(site.getDatabase());
		long count = 0;
		for (String sql : sqls) {
			count += db.innerSelectBySql(sql, ResultSetExtractor.GET_FIRST_LONG, context.params, null);
		}
		// for (String table : site.getTables()) {
		// String sql = getSql(table);
		// count += db.innerSelectBySql(sql, ResultSetExtractor.GET_FIRST_LONG,
		// context.params, null);
		// }
		return count;
	}

	/*
	 * p 得到一个数据库上操作的SQL语句 //表名改写 条件：全部 //noGroup延迟——SQL尾部以及Select部分中的聚合函数去除
	 * 条件:多表(不区分) //延迟Group消除—— 当多库时(?) 延迟Group不添加 //延迟noHaving消除——位于延迟的SQL尾部
	 * 条件:多库() ——union部分
	 * 
	 * //Order——位于子查询的尾部 条件：多表(不区分) //limit延迟 条件：单库多表 //limit去除 条件：多库
	 */
	private PairSO<List<Object>> getSql(PartitionResult site, boolean noOrder) {
		List<String> tables = site.getTables();
		boolean moreTable = tables.size() > 1; // 是否为多表
		boolean moreDatabase = isMultiDatabase();// 是否为多库

		PlainSelect st = context.statement;
		if (moreTable) {
			String tableAlias = st.getFromItem().getAlias();
			StringBuilder sb = new StringBuilder(200);
			for (int i = 0; i < tables.size(); i++) {
				if (i > 0) {
					sb.append("\n union all \n");
				}
				String tableName = site.getTables().get(i);
				appendSql(sb, tableName, true, true, true, true, true); // union子查询
			}
			// 绑定变量参数翻倍
			List<Object> params = SqlAnalyzer.repeat(context.params, tables.size());

			// 聚合操作用Union外嵌查询实现
			if (hasAnyGroupDistinctOrderLimit()) {
				StringBuilder sb2 = new StringBuilder();
				st.appendSelect(sb2, false);
				sb2.append(" from \n(").append(sb).append(") ");
				if (tableAlias != null) {
					sb2.append(tableAlias);
				} else {
					sb2.append("t");
				}
				sb2.append(ORMConfig.getInstance().wrap);
				st.appendGroupHavingOrderLimit(sb2, moreDatabase, noOrder, moreDatabase);
				sb = sb2;
			}
			return new PairSO<List<Object>>(sb.toString(), params);
		} else {
			String table = site.getTables().get(0);
			StringBuilder sb = new StringBuilder();
			// 单表情况下
			if (moreDatabase) {// 多库单表
				appendSql(sb, table, false, false, false, true, false);
			} else {
				appendSql(sb, table, false, false, false, false, false);
			}
			return new PairSO<List<Object>>(sb.toString(), context.params);
		}
	}

}

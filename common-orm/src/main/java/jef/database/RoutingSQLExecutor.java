package jef.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import jef.common.PairSO;
import jef.common.log.LogUtil;
import jef.database.annotation.PartitionResult;
import jef.database.jsqlparser.expression.JpqlDataType;
import jef.database.jsqlparser.statement.select.Select;
import jef.database.jsqlparser.visitor.Statement;
import jef.database.query.ParameterProvider;
import jef.database.query.SqlExpression;
import jef.database.routing.jdbc.SQLExecutor;
import jef.database.routing.sql.ExecutionPlan;
import jef.database.routing.sql.SelectExecutionPlan;
import jef.database.routing.sql.SqlAnalyzer;
import jef.database.routing.sql.SqlExecutionParam;
import jef.database.wrapper.result.IResultSet;
import jef.database.wrapper.result.JdbcResultSetAdapter;
import jef.database.wrapper.result.MultipleResultSet;
import jef.tools.DateUtils;
import jef.tools.StringUtils;

public class RoutingSQLExecutor implements ParameterProvider, SQLExecutor {
	private OperateTarget db;
	private int fetchSize = ORMConfig.getInstance().getGlobalFetchSize();
	private int maxResult = 0;
	private Statement st;
	private final List<Object> nameParams = new ArrayList<Object>();// 按名参数

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
	public ResultSet getResultSet() throws SQLException {
		SqlExecutionParam parse = getSqlAndParams(db, this);
		Statement sql = parse.statement;
		String s = sql.toString();

		ORMConfig config = ORMConfig.getInstance();
		boolean debug = config.debugMode;

		SelectExecutionPlan plan = null;
		plan = (SelectExecutionPlan) SqlAnalyzer.getSelectExecutionPlan((Select) sql, parse.params, db);
		if (plan == null) {// 普通查询
			return new JdbcResultSetAdapter(db.getRawResultSet(s, maxResult, fetchSize, parse.params));
		} else if (plan.isChangeDatasource() != null) {// 垂直拆分查询
			OperateTarget db = this.db.getTarget(plan.isChangeDatasource());
			return new JdbcResultSetAdapter(db.getRawResultSet(s, maxResult, fetchSize, parse.params));
		} else {// 分表分库查询
			if (plan.isMultiDatabase()) {// 多库
				MultipleResultSet mrs = new MultipleResultSet(config.isCacheResultset(), debug);
				for (PartitionResult site : plan.getSites()) {
					processQuery(db.getTarget(site.getDatabase()), plan.getSql(site, false), 0, mrs);
				}
				plan.parepareInMemoryProcess(null, mrs);
				IResultSet irs = mrs.toSimple(null);
				return new JdbcResultSetAdapter(irs);
			} else { // 单库多表，基于Union的查询. 可以使用数据库分页
				PartitionResult site = plan.getSites()[0];
				PairSO<List<Object>> result = plan.getSql(plan.getSites()[0], false);
				s = result.first;
				return new JdbcResultSetAdapter(db.getTarget(site.getDatabase()).getRawResultSet(s, maxResult, fetchSize, result.second));
			}
		}
	}

	private SqlExecutionParam getSqlAndParams(OperateTarget db2, RoutingSQLExecutor jQuery) {
		return new SqlExecutionParam(st, nameParams, this);
	}

	/*
	 * 执行查询动作，将查询结果放入mrs
	 */
	private void processQuery(OperateTarget db, PairSO<List<Object>> sql, int max, MultipleResultSet mrs) throws SQLException {
		StringBuilder sb = null;
		PreparedStatement psmt = null;
		ResultSet rs = null;
		if (mrs.isDebug())
			sb = new StringBuilder(sql.first.length() + 150).append(sql.first).append(" | ").append(db.getTransactionId());
		try {
			psmt = db.prepareStatement(sql.first, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
			BindVariableContext context = new BindVariableContext(psmt, db, sb);
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

	/**
	 * 对于各种DDL、insert、update、delete等语句，不需要返回结果的，调用此方法来执行
	 * 
	 * @return 返回影响到的记录条数（针对update\delete）语句
	 */
	public int executeUpdate() throws SQLException {
		SqlExecutionParam parse = getSqlAndParams(db, this);
		Statement sql = parse.statement;
		ExecutionPlan plan = SqlAnalyzer.getExecutionPlan(sql, parse.params, db);
		if (plan == null) {
			return db.innerExecuteSql(parse.statement.toString(), parse.params);
		} else if (plan.isChangeDatasource() != null) {
			return db.getTarget(plan.isChangeDatasource()).innerExecuteSql(parse.statement.toString(), parse.params);
		} else {
			long start = System.currentTimeMillis();
			int total = 0;
			for (PartitionResult site : plan.getSites()) {
				total += plan.processUpdate(site, db);
			}
			if (plan.isMultiDatabase() && ORMConfig.getInstance().debugMode) {
				LogUtil.show(StringUtils.concat("Total Executed:", String.valueOf(total), "\t Time cost([DbAccess]:", String.valueOf(System.currentTimeMillis() - start), "ms) |  @", String.valueOf(Thread.currentThread().getId())));
			}
			return total;
		}
	}

	/**
	 * 限制返回的最大结果数
	 */
	public void setMaxResults(int maxResult) {
		this.maxResult = maxResult;
	}

	/**
	 * 获取当前设置的最大结果设置
	 */
	public int getMaxResults() {
		return maxResult;
	}

	/*
	 * 将参数按照命名查询中的类型提示转换为合适的类型
	 */
	private Object toProperType(JpqlDataType type, String[] value) {
		// 如果是动态SQL片段类型，则将数组转换成1个String值。
		if (JpqlDataType.SQL.equals(type)) {
			return new SqlExpression(StringUtils.join(value));
		}

		Object[] result = new Object[value.length];
		for (int i = 0; i < value.length; i++) {
			result[i] = toProperType(type, value[i]);
		}
		return result;
	}

	private Object toProperType(JpqlDataType type, String value) {
		switch (type) {
		case DATE:
			return DateUtils.toSqlDate(DateUtils.autoParse(value));
		case BOOLEAN:
			return StringUtils.toBoolean(value, null);
		case DOUBLE:
			return StringUtils.toDouble(value, 0.0);
		case FLOAT:
			return StringUtils.toFloat(value, 0.0f);
		case INT:
			return StringUtils.toInt(value, 0);
		case LONG:
			return StringUtils.toLong(value, 0L);
		case SHORT:
			return (short) StringUtils.toInt(value, 0);
		case TIMESTAMP:
			return DateUtils.toSqlTimeStamp(DateUtils.autoParse(value));
		case SQL:
			return new SqlExpression(value);
		case $STRING:
			return "%".concat(value);
		case STRING$:
			return value.concat("%");
		case $STRING$:
			StringBuilder sb = new StringBuilder(value.length() + 2);
			return sb.append('%').append(value).append('%').toString();
		default:
			return value;
		}
	}

	/**
	 * 得到参数的值
	 */
	public Object getParameterValue(int position) {
		return nameParams.get(position);
	}

	/**
	 * 对于以序号排列的参数，获取其第index个参数的值
	 */
	public Object getIndexedParam(int index) {
		if (this.nameParams == null)
			return null;
		return nameParams.get(index);
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

	/**
	 * 清除之前设置过的所有参数。 此方法当一个NativeQuery被重复使用时十分有用。
	 */
	public void clearParameters() {
		nameParams.clear();
	}

	/**
	 * 清除指定的参数
	 * 
	 * @param index
	 */
	public void clearParameter(int index) {
		nameParams.remove(index);
	}

	@Override
	public Object getNamedParam(String name) {
		Integer index = Integer.valueOf(name);
		return this.nameParams.get(index);
	}

	@Override
	public boolean containsParam(Object key) {
		if (key instanceof Integer) {
			return nameParams.size() > (Integer) key;
		}
		return false;
	}

	public void setParams(List<Object> params) {
		nameParams.clear();
		nameParams.addAll(params);
	}

	@Override
	public void setResultSetType(int resultSetType) {
	}

	@Override
	public void setResultSetConcurrency(int resultSetConcurrency) {
	}

	@Override
	public void setResultSetHoldability(int resultSetHoldability) {
	}

	@Override
	public void setQueryTimeout(int queryTimeout) {
	}
}

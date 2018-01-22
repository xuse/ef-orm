package jef.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import jef.common.log.LogUtil;
import jef.database.Session.PopulateStrategy;
import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.ResultSetImpl;
import jef.database.jsqlparser.SelectToCountWrapper;
import jef.database.jsqlparser.parser.ParseException;
import jef.database.jsqlparser.statement.select.PlainSelect;
import jef.database.jsqlparser.statement.select.Union;
import jef.database.jsqlparser.visitor.SelectBody;
import jef.database.wrapper.clause.BindSql;
import jef.database.wrapper.populator.Transformer;
import jef.tools.Assert;
import jef.tools.PageInfo;
import jef.tools.PageLimit;

final class PagingIteratorSqlImpl<T> extends PagingIterator<T> {
	private String querySql; // 2 使用对象查询的情况
	private OperateTarget db;

	/**
	 * 获得返回结果拼装策略
	 * 
	 * @return
	 */
	public PopulateStrategy[] getStrategies() {
		return transformer.getStrategy();
	}

	/**
	 * 设置返回结果拼装策略
	 * 
	 * @param strategies
	 */
	public void setStrategies(PopulateStrategy... strategies) {
		this.transformer.setStrategy(strategies);
	}

	/*
	 * 从SQL语句构造，不支持绑定变量的
	 */
	PagingIteratorSqlImpl(String sql, int pageSize, Transformer resultSample, OperateTarget db) {
		Assert.notNull(resultSample);
		this.querySql = sql;
		this.transformer = resultSample;
		this.db = db;
		page = new PageInfo();
		page.setRowsPerPage(pageSize);
	}

	@Override
	protected long doCount() throws SQLException {
		DatabaseDialect dialect = db.getProfile();
		return db.countBySql(toCountSql(querySql, dialect));
	}

	private static String toCountSql(String sql, DatabaseDialect dialect) throws SQLException {
		// 重新解析
		try {
			SelectBody select = DbUtils.parseNativeSelect(sql).getSelectBody();
			SelectToCountWrapper sw;
			if (select instanceof Union) {
				sw = new SelectToCountWrapper((Union) select);
			} else {
				sw = new SelectToCountWrapper((PlainSelect) select, dialect);
			}
			return sw.toString();
		} catch (ParseException e) {
			throw new SQLException("Parser error:" + sql);
		}
	}

	/*
	 * 处理由SQL作为查询条件的分页查询
	 */
	protected List<T> doQuery(boolean pageFlag) throws SQLException {
		DatabaseDialect profile = db.getProfile();
		calcPage();
		PageLimit range = page.getCurrentRecordRange();
		if (range.getOffset() == 0 && range.getEnd() >= page.getTotal()) {
			pageFlag = false;
		}
		BindSql sql = pageFlag ? profile.getLimitHandler().toPageSQL(this.querySql, range) : new BindSql(this.querySql);
		boolean debug = ORMConfig.getInstance().isDebugMode();
		if (debug)
			LogUtil.info(sql.getSql());
		Statement st = null;
		ResultSet rs = null;
		List<T> list;
		try {
			st = db.createStatement(sql.getRsLaterProcessor(), false);
			rs = st.executeQuery(sql.getSql());
			list = db.populateResultSet(new ResultSetImpl(rs, db.getProfile()), null, transformer);
		} finally {
			DbUtils.close(rs);
			DbUtils.close(st);
			db.releaseConnection();
		}
		if (debug)
			LogUtil.show("Result Count:" + list.size());
		if (list.isEmpty()) {
			recordEmpty();
		}
		return list;
	}
}

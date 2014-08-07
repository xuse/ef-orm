package jef.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import jef.common.log.LogUtil;
import jef.common.wrapper.IntRange;
import jef.database.Session.PopulateStrategy;
import jef.database.dialect.DatabaseDialect;
import jef.database.wrapper.Transformer;
import jef.database.wrapper.result.ResultSetImpl;
import jef.tools.Assert;
import jef.tools.PageInfo;

final class PagingIteratorSqlImpl<T> extends PagingIterator<T>{
	private String querySql; // 2 使用对象查询的情况
	private OperateTarget db;
	
	/**
	 * 获得返回结果拼装策略
	 * @return
	 */
	public PopulateStrategy[] getStrategies() {
		return transformer.getStrategy();
	}

	/**
	 * 设置返回结果拼装策略
	 * @param strategies
	 */
	public void setStrategies(PopulateStrategy... strategies) {
		this.transformer.setStrategy(strategies);
	}
	
	/*
	 * 从SQL语句构造
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
		return db.countBySql(db.getProcessor().toCountSql(querySql));
	}
	/*
	 * 处理由SQL作为查询条件的分页查询
	 */
	protected List<T> doQuery(boolean pageFlag) throws SQLException {
		DatabaseDialect profile = db.getProfile();
		calcPage();
		String sql = this.querySql;
		IntRange range=page.getCurrentRecordRange();
		if(range.getStart()==1 && range.getEnd().intValue()==page.getTotal()){
			pageFlag=false;
		}
		sql = pageFlag?profile.toPageSQL(sql, range):sql;
		boolean debug=ORMConfig.getInstance().isDebugMode();
		if (debug)
			LogUtil.show(sql);
		Statement st = null;
		ResultSet rs = null;
		List<T> list;
		try{
			st=db.createStatement();
			rs=st.executeQuery(sql);
			list = db.populateResultSet(new ResultSetImpl(rs,db.getProfile()),  null,transformer);
		}finally{
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

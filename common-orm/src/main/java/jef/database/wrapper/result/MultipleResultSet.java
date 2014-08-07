package jef.database.wrapper.result;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.sql.rowset.CachedRowSet;

import jef.common.Entry;
import jef.common.log.LogUtil;
import jef.database.Condition;
import jef.database.OperateTarget;
import jef.database.Session.PopulateStrategy;
import jef.database.dialect.DatabaseDialect;
import jef.database.meta.Reference;
import jef.database.wrapper.clause.GroupByEle;
import jef.database.wrapper.populator.ColumnDescription;
import jef.database.wrapper.populator.ColumnMeta;
import jef.http.client.support.CommentEntry;
import jef.tools.ArrayUtils;

/**
 * 查询时记录的结果集
 * 
 * @author Administrator
 * 
 */
public final class MultipleResultSet extends AbstractResultSet{
	
	private int current = -1;
	//重新排序部分
	private List<Entry<String, Boolean>> orderAsSelect;
	//查询部分
	private List<CommentEntry> selectItems;
	//所有列的元数据记录
	private ColumnMeta columns;

	protected final List<ResultSetHolder> results = new ArrayList<ResultSetHolder>(5);

	private boolean cache;

	private boolean debug;

	public MultipleResultSet(boolean cache, boolean debug) {
		this.cache = cache;
		this.debug = debug;
	}

	public int size() {
		return results.size();
	}

	public ColumnMeta getColumns() {
		return columns;
	}

	private void initMetadata(ResultSet wrapped) throws SQLException {
		ResultSetMetaData meta = wrapped.getMetaData();
		List<ColumnDescription> columnList = new ArrayList<ColumnDescription>();
		for (int i = 1; i <= meta.getColumnCount(); i++) {
			String name = meta.getColumnLabel(i); // 对于Oracle
													// getCOlumnName和getColumnLabel是一样的（非标准JDBC实现），MySQL正确地实现了JDBC的要求，getLabel得到别名，getColumnName得到表的列名
			int type = meta.getColumnType(i);
			columnList.add(new ColumnDescription(i, type, name));
		}
		this.columns = new ColumnMeta(columnList);
	}



	public boolean next() {
		try {
			boolean n = (current > -1) && results.get(current).rs.next();
			if (n == false) {
				current++;
				if (current < results.size()) {
					return next();
				} else {
					return false;
				}
			}
			return n;
		} catch (SQLException e) {
			LogUtil.exception(e);
			return false;
		}

	}

	public boolean previous() throws SQLException {
		boolean b = (current < results.size()) && results.get(current).rs.previous();
		if (b == false) {
			current--;
			if (current > -1) {
				return previous();
			} else {
				return false;
			}
		}
		return b;
	}
	
	public void beforeFirst() throws SQLException {
		for (ResultSetHolder rs : results) {
			rs.rs.beforeFirst();
		}
		current = -1;
	}

	public void first() throws SQLException {
		results.get(0).rs.first();
		for (int i = 1; i < results.size(); i++) {
			ResultSetHolder rs = results.get(i);
			if (!rs.rs.isBeforeFirst()) {
				rs.rs.beforeFirst();
			}
		}
		current = 0;
	}

	public void afterLast() throws SQLException {
		for (ResultSetHolder rs : results) {
			rs.rs.afterLast();
		}
		current = results.size();
	}
	
	/**
	 * 添加一个
	 * 
	 * @param rs
	 * @param statement
	 */
	public void add(ResultSet rs, Statement statement, OperateTarget tx) {
		if (columns == null) {
			try {
				initMetadata(rs);
			} catch (SQLException e) {
				throw new IllegalStateException(e);
			}
		}
		ResultSetHolder r = new ResultSetHolder(tx,statement,rs);
		results.add(r);
		if (cache) {
			try{
				r.rs = tryCache(rs, tx.getProfile());
				r.close(false);
				return;
			}catch(SQLException e){
				//缓存失败
				LogUtil.exception(e);
			}
		}
		r.rs = rs;
		
	}

	private ResultSet tryCache(ResultSet set, DatabaseDialect profile) throws SQLException {
		long start = System.currentTimeMillis();
		CachedRowSet rs = profile.newCacheRowSetInstance();
		rs.populate(set);
		if (debug) {
			LogUtil.debug("Caching Results from database. Cost {}ms.", System.currentTimeMillis() - start);
		}
		set.close();
		return rs;

	}

	/**
	 * 关闭全部连接和结果集
	 * 
	 * @throws SQLException
	 */
	public void close() throws SQLException {
		List<SQLException> ex = new ArrayList<SQLException>();
		for (ResultSetHolder rsx : results) {
			rsx.close(true);
		}
		results.clear();
		if (ex.size() > 0) {
			throw new SQLException("theres " + ex.size() + " resultSet close error!");
		}
	}


	/**
	 * 进行结果集退化
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public IResultSet toSimple(Map<Reference, List<Condition>> filters,PopulateStrategy... args) {
		if(filters==null){
			filters=Collections.EMPTY_MAP;
		}
		if(results.isEmpty()){
			return new ResultSetWrapper();
		}
		if (results.size() == 1) {
			ResultSetWrapper rsw = new ResultSetWrapper(results.get(0), columns);
			rsw.setFilters(filters);
			return rsw;
		}
		if (orderAsSelect != null && !ArrayUtils.fastContains(args, PopulateStrategy.NO_RESORT)) {
			ReorderMultipleResultSet rw = new ReorderMultipleResultSet(results, orderAsSelect, selectItems, columns);
			rw.filters=filters;
			return rw;
		}
		this.filters=filters;
		return this;
	}

	public void setOrderDesc(List<Entry<String, Boolean>> asSelect) {
		this.orderAsSelect = asSelect;
	}

	public void setSelectDesc(List<CommentEntry> entries) {
		this.selectItems = entries;
	}

	public DatabaseDialect getProfile() {
		return results.get(current).db.getProfile();
	}

	@Override
	protected ResultSet get() {
		return results.get(current).rs;
	}

	public void setRegroupByAction(List<GroupByEle> parseSelectFunction) {
		// TODO Auto-generated method stub
		
	}
}

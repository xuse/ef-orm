package jef.database.wrapper.result;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.sql.rowset.CachedRowSet;

import jef.common.Entry;
import jef.common.log.LogUtil;
import jef.database.Condition;
import jef.database.DbUtils;
import jef.database.OperateTarget;
import jef.database.ResultSetReleaseHandler;
import jef.database.Session.PopulateStrategy;
import jef.database.Transaction;
import jef.database.dialect.DatabaseDialect;
import jef.database.meta.Reference;
import jef.database.wrapper.ColumnDescription;
import jef.database.wrapper.ColumnMeta;
import jef.http.client.support.CommentEntry;
import jef.tools.ArrayUtils;

/**
 * 查询时记录的结果集
 * 
 * @author Administrator
 * 
 */
public final class MultipleResultSet implements IResultSet {
	private int current = -1;
	private List<Entry<String, Boolean>> orderAsSelect;
	private List<CommentEntry> selectItems;
	private ColumnMeta columns;
	private Map<Reference, List<Condition>> filters;
	
	public Map<Reference, List<Condition>> getFilters() {
		return filters;
	}

	public void setFilters(Map<Reference, List<Condition>> filters) {
		this.filters = filters;
	}

	static class R {
		ResultSet rs;
		OperateTarget from;
		ResultSetReleaseHandler releaser;

		private R(OperateTarget tx) {
			from=tx;
		}
		public void close() {
			DbUtils.close(rs);
			if (releaser != null) {
				releaser.release();
			}
		}
	}

	protected final List<R> results = new ArrayList<R>();
	// protected final List<ResultSetReleaseHandler> callbacks=new
	// ArrayList<ResultSetReleaseHandler>();
	// protected final List<OperateTarget> resultsProfile=new
	// ArrayList<OperateTarget>();
	private boolean cache;

	private boolean debug;
	/**
	 * 当这个结果集被长时间持有时，为了防止连接被挪作他用，因此我们用一个专门的事务对象独占这个连接。
	 * 这个专门的事务对象只有当结果集被关闭时才会释放连接。
	 */
	private Transaction specialTx;

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

	public RowId getRowId(int columnIndex) throws SQLException {
		return results.get(current).rs.getRowId(columnIndex);
	}

	public Object getObject(int columnIndex) throws SQLException {
		return results.get(current).rs.getObject(columnIndex);
	}

	/**
	 * 添加一个
	 * 
	 * @param set
	 * @param statement
	 */
	public void add(ResultSet set, Statement statement, OperateTarget tx) {
		if (columns == null) {
			try {
				initMetadata(set);
			} catch (SQLException e) {
				throw new IllegalStateException(e);
			}
		}
		R r = new R(tx);
		results.add(r);
		if (cache) {
			try {
				r.rs = tryCache(set, tx.getProfile());
				ResultSetReleaseHandler.release(tx, statement);
				return;
			} catch (SQLException e) {
				LogUtil.exception(e);
			}
		}
		r.rs = set;
		r.releaser = new ResultSetReleaseHandler(tx, statement);
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

	/**
	 * {@link #specialTx}
	 * 
	 * @param specialTx
	 */
	public void setSpecialTx(Transaction specialTx) {
		this.specialTx = specialTx;
	}

	/**
	 * 关闭全部连接和结果集
	 * 
	 * @throws SQLException
	 */
	public void close() throws SQLException {
		List<SQLException> ex = new ArrayList<SQLException>();
		for (R rsx : results) {
			rsx.close();
		}
		results.clear();
		// //
		if (specialTx != null) {
			specialTx.commit();
			specialTx = null;
		}
		if (ex.size() > 0) {
			throw new SQLException("theres " + ex.size() + " resultSet close error!");
		}
	}

	public void moveToInsertRow() throws SQLException {
		results.get(current).rs.moveToInsertRow();
	}

	public void deleteRow() throws SQLException {
		results.get(current).rs.deleteRow();
	}

	public void updateRow() throws SQLException {
		results.get(current).rs.updateRow();
	}

	public void updateNull(String columnName) throws SQLException {
		results.get(current).rs.updateNull(columnName);
	}

	public void updateObject(String columnName, Object value) throws SQLException {
		results.get(current).rs.updateObject(columnName, value);
	}

	public void beforeFirst() throws SQLException {
		for (R rs : results) {
			rs.rs.beforeFirst();
		}
		current = -1;
	}

	public void first() throws SQLException {
		results.get(0).rs.first();
		for (int i = 1; i < results.size(); i++) {
			R rs = results.get(i);
			if (!rs.rs.isBeforeFirst()) {
				rs.rs.beforeFirst();
			}
		}
		current = 0;
	}

	public void afterLast() throws SQLException {
		for (R rs : results) {
			rs.rs.afterLast();
		}
		current = results.size();
	}

	public void insertRow() throws SQLException {
		results.get(current).rs.insertRow();
	}

	/**
	 * 进行结果集退化
	 * 
	 * @return
	 */
	public IResultSet toSimple(Map<Reference, List<Condition>> filters,PopulateStrategy... args) {
		if(filters==null){
			filters=Collections.EMPTY_MAP;
		}
		if(results.isEmpty()){
			return new ResultSetWrapper(null,null,null);
		}
		if (results.size() == 1) {
			R r=results.get(0);
			ResultSetWrapper rsw = new ResultSetWrapper(r.rs, columns, r.from.getProfile());
			rsw.setHandler(r.releaser);
			rsw.setFilters(filters);
			return rsw;
		}
		if (orderAsSelect != null && !ArrayUtils.fastContains(args, PopulateStrategy.NO_RESORT)) {
			ReorderMultipleResultSet rw = new ReorderMultipleResultSet(results, orderAsSelect, selectItems, columns);
			rw.parent = this;
			rw.setFilters(filters);
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
		return results.get(current).from.getProfile();
	}

	public Object getObject(String columnName) throws SQLException {
		return results.get(current).rs.getObject(columnName);
	}

	public boolean getBoolean(int i) throws SQLException {
		return results.get(current).rs.getBoolean(i);
	}

	public double getDouble(int i) throws SQLException {
		return results.get(current).rs.getDouble(i);
	}

	public float getFloat(int i) throws SQLException {
		return results.get(current).rs.getFloat(i);
	}

	public long getLong(int i) throws SQLException {
		return results.get(current).rs.getLong(i);
	}

	public int getInt(int i) throws SQLException {
		return results.get(current).rs.getInt(i);
	}

	public String getString(int i) throws SQLException {
		return results.get(current).rs.getString(i);
	}

	public java.sql.Date getDate(int i) throws SQLException {
		return results.get(current).rs.getDate(i);
	}

	public Timestamp getTimestamp(int i) throws SQLException {
		return results.get(current).rs.getTimestamp(i);
	}

	public Time getTime(int i) throws SQLException {
		return results.get(current).rs.getTime(i);
	}

	public Clob getClob(int columnIndex) throws SQLException {
		return results.get(current).rs.getClob(columnIndex);
	}

	public Blob getBlob(int columnIndex) throws SQLException {
		return results.get(current).rs.getBlob(columnIndex);
	}

	public boolean getBoolean(String columnName) throws SQLException {
		return results.get(current).rs.getBoolean(columnName);
	}

	public double getDouble(String columnName) throws SQLException {
		return results.get(current).rs.getDouble(columnName);
	}

	public float getFloat(String columnName) throws SQLException {
		return results.get(current).rs.getFloat(columnName);
	}

	public long getLong(String columnName) throws SQLException {
		return results.get(current).rs.getLong(columnName);
	}

	public int getInt(String columnName) throws SQLException {
		return results.get(current).rs.getInt(columnName);
	}

	public Clob getClob(String columnName) throws SQLException {
		return results.get(current).rs.getClob(columnName);
	}

	public Blob getBlob(String columnName) throws SQLException {
		return results.get(current).rs.getBlob(columnName);
	}

	public String getString(String columnName) throws SQLException {
		return results.get(current).rs.getString(columnName);
	}

	public Timestamp getTimestamp(String columnName) throws SQLException {
		return results.get(current).rs.getTimestamp(columnName);
	}

	public Time getTime(String columnName) throws SQLException {
		return results.get(current).rs.getTime(columnName);
	}

	public Date getDate(String columnName) throws SQLException {
		return results.get(current).rs.getDate(columnName);
	}

	public byte[] getBytes(int columnIndex) throws SQLException {
		return results.get(current).rs.getBytes(columnIndex);
	}
}

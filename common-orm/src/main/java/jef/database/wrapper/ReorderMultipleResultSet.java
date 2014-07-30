package jef.database.wrapper;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jef.common.Entry;
import jef.common.log.LogUtil;
import jef.database.Condition;
import jef.database.dialect.DatabaseDialect;
import jef.database.meta.Reference;
import jef.database.wrapper.MultipleResultSet.R;
import jef.http.client.support.CommentEntry;
import jef.tools.StringUtils;

import org.apache.commons.lang.ArrayUtils;

/**
 * 在使用next方法的时候，进行按顺序获取的结果集
 * 
 * @author Administrator
 * 
 */
final class ReorderMultipleResultSet implements IResultSet {
	IResultSet parent;
	private ColumnMeta columns;

	private int[] orderFields;
	private boolean[] orderAsc;

	private final List<ResultSet> gettingResults = new ArrayList<ResultSet>();
	private DatabaseDialect[] profiles;
	private List<R> allResults;
	private int currentIndex = -1;// 当前选中ResultSet;

	public ReorderMultipleResultSet(List<R> r, List<Entry<String, Boolean>> orderAsSelect, List<CommentEntry> selectItems, ColumnMeta columns) {
		allResults=r;
		{
			int len = r.size();
			profiles = new DatabaseDialect[len];
			for (int i = 0; i < len; i++) {
				R re=r.get(i);
				profiles[i] = re.from.getProfile();
				this.gettingResults.add(re.rs);
			}
		}
		this.columns = columns;
		parseOrder(orderAsSelect, selectItems);
	}

	private void parseOrder(List<Entry<String, Boolean>> orderAsSelect, List<CommentEntry> selectItems) {
		// String[] orders=new String[orderAsSelect.size()];
		int[] orders = new int[orderAsSelect.size()];
		boolean[] orderAsc = new boolean[orderAsSelect.size()];

		for (int i = 0; i < orderAsSelect.size(); i++) {
			Entry<String, Boolean> order = orderAsSelect.get(i);
			String alias = findAlias(order.getKey(), selectItems);
			if (alias == null) {
				throw new IllegalArgumentException("The order field " + order.getKey() + " does not selected in SQL!");
			}
			// 可能为null
			ColumnDescription selectedColumn = columns.getByFullName(alias);
			if (selectedColumn == null) {
				throw new IllegalArgumentException("The order field " + alias + " does not found in this Query!");
			}
			orders[i] = selectedColumn.getN();//
			orderAsc[i] = order.getValue();
		}
		this.orderFields = orders;
		this.orderAsc = orderAsc;
	}

	private String findAlias(String key, List<CommentEntry> selectItems) {
		String alias = null;
		for (CommentEntry c : selectItems) {
			if (key.equals(c.getKey())) {
				alias = c.getValue();
				break;
			}
		}
		if (alias == null) {
			alias = StringUtils.substringAfterIfExist(key, ".");
		}
		return alias;
	}

	public int size() {
		return allResults.size();
	}

	public ColumnMeta getColumns() {
		return columns;
	}

	private Map<Reference, List<Condition>> filters;
	
	public Map<Reference, List<Condition>> getFilters() {
		return filters;
	}

	public void setFilters(Map<Reference, List<Condition>> filters) {
		this.filters = filters;
	}
	public boolean next() {
		if (currentIndex == -2)
			throw new IllegalStateException();
		try {
			if (currentIndex == -1) {
				moveAllCursor();
			} else {
				moveCursor(currentIndex);
			}
			if (gettingResults.isEmpty())
				return false;
			chooseMinResult();
		} catch (SQLException e) {
			LogUtil.exception(e);
			return false;
		}
		return true;
	}

	// 移动游标
	private void moveCursor(int index) throws SQLException {
		boolean b = gettingResults.get(index).next();
		if (!b) {
			gettingResults.remove(index);
			profiles = (DatabaseDialect[]) ArrayUtils.remove(profiles, index);
		}
	}

	// 全移动游标
	private void moveAllCursor() throws SQLException {
		for (int index = 0; index < gettingResults.size(); index++) {
			ResultSet rs = gettingResults.get(index);
			boolean b = rs.next();
			if (!b) {
				gettingResults.remove(index);
				profiles = (DatabaseDialect[]) ArrayUtils.remove(profiles, index);
				index--;
			}
		}
	}

	private void chooseMinResult() throws SQLException {
		currentIndex = 0;
		ResultSet value = gettingResults.get(0);
		for (int i = 1; i < gettingResults.size(); i++) {
			ResultSet value2 = gettingResults.get(i);
			if (isMin(value, value2)) {
				currentIndex = i;
				value = value2;
			}
		}
		// System.out.println("->" +
		// results.indexOf(gettingResults.get(currentIndex)));
	}

	/*
	 * 如果value2要排在value1前面，则返回true
	 * 
	 * @param value
	 * 
	 * @param value2
	 * 
	 * @return
	 * 
	 * @throws SQLException
	 */
	// 总是取最小的数值
	private boolean isMin(ResultSet value, ResultSet value2) throws SQLException {
		for (int i = 0; i < orderFields.length; i++) {
			int r = compare(value.getObject(orderFields[i]), value2.getObject(orderFields[i]));
			if (r > 0) {
				return orderAsc[i];
			} else if (r < 0) {
				return !orderAsc[i];
			}
			// 上面这个逻辑写了好半天啊……
			// if((r>0)^(!orderAsc[i])){
			// return true;
			// }
		}
		return false;// all equals
	}

	public void moveToInsertRow() throws SQLException {
		gettingResults.get(currentIndex).moveToInsertRow();
	}

	public void deleteRow() throws SQLException {
		gettingResults.get(currentIndex).deleteRow();
	}

	public void updateRow() throws SQLException {
		gettingResults.get(currentIndex).updateRow();
	}

	public void updateNull(String columnName) throws SQLException {
		gettingResults.get(currentIndex).updateNull(columnName);
	}

	public void updateObject(String columnName, Object value) throws SQLException {
		gettingResults.get(currentIndex).updateObject(columnName, value);
	}

	public void beforeFirst() throws SQLException {
		for (int i = 0; i < allResults.size(); i++) {
			ResultSet rs = allResults.get(i).rs;
			if (!rs.isBeforeFirst()) {
				rs.beforeFirst();
			}
		}
		//
		currentIndex = -1;
		gettingResults.clear();
		for(R r:allResults){
			gettingResults.add(r.rs);
		}
	}

	public void first() throws SQLException {
		beforeFirst();
		next();
	}

	public void afterLast() throws SQLException {
		for (R rs : allResults) {
			rs.rs.afterLast();
		}
		gettingResults.clear();
		currentIndex = -2;
	}

	public void insertRow() throws SQLException {
		gettingResults.get(currentIndex).insertRow();
	}

	public DatabaseDialect getProfile() {
		if (currentIndex < 0)
			throw new IllegalStateException();
		return profiles[currentIndex];
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private int compare(Object object, Object object2) {
		if (object == object2)
			return 0;
		if (object == null)
			return 1;
		if (object2 == null)
			return -1;
		return ((Comparable) object).compareTo(object2);
	}

	public boolean previous() throws SQLException {
		throw new UnsupportedOperationException();
	}

	/**
	 * 关闭全部连接和结果集
	 * 
	 * @throws SQLException
	 */
	public void close() throws SQLException {
		if (parent != null) {
			parent.close();
			parent = null;
			this.gettingResults.clear();
			this.profiles = null;
			this.columns = null;
		}
	}

	public Object getObject(int columnIndex) throws SQLException {
		return gettingResults.get(currentIndex).getObject(columnIndex);
	}

	public Object getObject(String columnName) throws SQLException {
		return gettingResults.get(currentIndex).getObject(columnName);
	}

	public boolean getBoolean(int i) throws SQLException {
		return gettingResults.get(currentIndex).getBoolean(i);
	}

	public double getDouble(int i) throws SQLException {
		return gettingResults.get(currentIndex).getDouble(i);
	}

	public float getFloat(int i) throws SQLException {
		return gettingResults.get(currentIndex).getFloat(i);
	}

	public long getLong(int i) throws SQLException {
		return gettingResults.get(currentIndex).getLong(i);
	}

	public int getInt(int i) throws SQLException {
		return gettingResults.get(currentIndex).getInt(i);
	}

	public String getString(int i) throws SQLException {
		return gettingResults.get(currentIndex).getString(i);
	}

	public Timestamp getTimestamp(int i) throws SQLException {
		return gettingResults.get(currentIndex).getTimestamp(i);
	}

	public Time getTime(int i) throws SQLException {
		return gettingResults.get(currentIndex).getTime(i);
	}

	public Date getDate(int i) throws SQLException {
		return gettingResults.get(currentIndex).getDate(i);
	}

	public RowId getRowId(int columnIndex) throws SQLException {
		return gettingResults.get(currentIndex).getRowId(columnIndex);
	}

	public Clob getClob(int columnIndex) throws SQLException {
		return gettingResults.get(currentIndex).getClob(columnIndex);
	}

	public Blob getBlob(int columnIndex) throws SQLException {
		return gettingResults.get(currentIndex).getBlob(columnIndex);
	}

	public boolean getBoolean(String columnName) throws SQLException {
		return gettingResults.get(currentIndex).getBoolean(columnName);
	}

	public double getDouble(String columnName) throws SQLException {
		return gettingResults.get(currentIndex).getDouble(columnName);
	}

	public float getFloat(String columnName) throws SQLException {
		return gettingResults.get(currentIndex).getFloat(columnName);
	}

	public long getLong(String columnName) throws SQLException {
		return gettingResults.get(currentIndex).getLong(columnName);
	}

	public int getInt(String columnName) throws SQLException {
		return gettingResults.get(currentIndex).getInt(columnName);
	}

	public Clob getClob(String columnName) throws SQLException {
		return gettingResults.get(currentIndex).getClob(columnName);
	}

	public Blob getBlob(String columnName) throws SQLException {
		return gettingResults.get(currentIndex).getBlob(columnName);
	}

	public String getString(String columnName) throws SQLException {
		return gettingResults.get(currentIndex).getString(columnName);
	}

	public Timestamp getTimestamp(String columnName) throws SQLException {
		return gettingResults.get(currentIndex).getTimestamp(columnName);
	}

	public Time getTime(String columnName) throws SQLException {
		return gettingResults.get(currentIndex).getTime(columnName);
	}

	public Date getDate(String columnName) throws SQLException {
		return gettingResults.get(currentIndex).getDate(columnName);
	}

	public byte[] getBytes(int columnIndex) throws SQLException {
		return gettingResults.get(currentIndex).getBytes(columnIndex);
	}
}

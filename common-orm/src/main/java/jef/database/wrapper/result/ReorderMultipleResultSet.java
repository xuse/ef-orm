package jef.database.wrapper.result;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import jef.common.Entry;
import jef.common.log.LogUtil;
import jef.database.dialect.DatabaseDialect;
import jef.database.wrapper.populator.ColumnDescription;
import jef.database.wrapper.populator.ColumnMeta;
import jef.http.client.support.CommentEntry;
import jef.tools.StringUtils;

import org.apache.commons.lang.ArrayUtils;

/**
 * 在使用next方法的时候，进行按顺序获取的结果集
 * 
 * @author Administrator
 * 
 */
final class ReorderMultipleResultSet extends AbstractResultSet {
	private ColumnMeta columns;

	private int[] orderFields;
	private boolean[] orderAsc;

	private final List<ResultSet> gettingResults = new ArrayList<ResultSet>();
	private DatabaseDialect[] profiles;
	private List<ResultSetHolder> allResults;
	private int currentIndex = -1;// 当前选中ResultSet;

	public ReorderMultipleResultSet(List<ResultSetHolder> r, List<Entry<String, Boolean>> orderAsSelect, List<CommentEntry> selectItems, ColumnMeta columns) {
		allResults=r;
		{
			int len = r.size();
			profiles = new DatabaseDialect[len];
			for (int i = 0; i < len; i++) {
				ResultSetHolder re=r.get(i);
				profiles[i] = re.db.getProfile();
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
		// System.out.println("->" + results.indexOf(gettingResults.get(currentIndex)));
	}

	/*
	 * 如果value2要排在value1前面，则返回true
	 * @param value
	 * @param value2
	 * @return
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
		for(ResultSetHolder r:allResults){
			gettingResults.add(r.rs);
		}
	}

	public void first() throws SQLException {
		beforeFirst();
		next();
	}

	public void afterLast() throws SQLException {
		for (ResultSetHolder rs : allResults) {
			rs.rs.afterLast();
		}
		gettingResults.clear();
		currentIndex = -2;
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
		for (ResultSetHolder rsx : allResults) {
			rsx.close(true);
		}
		allResults.clear();
		gettingResults.clear();
		profiles = null;
		columns = null;
	}

	@Override
	protected ResultSet get() {
		return gettingResults.get(currentIndex);
	}
}

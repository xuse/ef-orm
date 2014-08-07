/*
 * JEF - Copyright 2009-2010 Jiyi (mr.jiyi@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jef.database.wrapper.result;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import jef.common.log.LogUtil;
import jef.database.dialect.DatabaseDialect;
import jef.database.wrapper.clause.InMemoryOrderBy;
import jef.database.wrapper.populator.ColumnMeta;

import org.apache.commons.lang.ArrayUtils;

/**
 * The is a resort implementation for results with Low Memory consume.
 * 
 * @author Jiyi
 * 
 */
final class ReorderResultSet extends AbstractResultSet {
	private ColumnMeta columns;

	private int[] orderFields;
	private boolean[] orderAsc;
	private final List<ResultSet> gettingResults = new ArrayList<ResultSet>();
	private DatabaseDialect[] profiles;
	private List<ResultSetHolder> allResults;
	private int currentIndex = -1;// 当前选中ResultSet;

	public ReorderResultSet(List<ResultSetHolder> r,InMemoryOrderBy order, ColumnMeta columns) {
		allResults=r;
		this.orderFields=order.getOrderFields();
		this.orderAsc=order.getOrderAsc();
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

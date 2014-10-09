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
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.persistence.PersistenceException;

import jef.database.Condition;
import jef.database.dialect.DatabaseDialect;
import jef.database.meta.Reference;
import jef.database.wrapper.clause.InMemoryOrderBy;
import jef.database.wrapper.populator.ColumnMeta;

/**
 * The is a resort implementation for results with Low Memory consume.
 * 使用王义林提供的新算法优化
 * 
 * @author Jiyi
 * 
 */
final class ReorderResultSet2 extends AbstractResultSet {
	private ColumnMeta columns;

	private final TreeSet<ResultSetHolder> gettingResults;
	private List<ResultSetHolder> allResults;
	private ResultSetHolder activeRs;
	// 级联过滤条件
	protected Map<Reference, List<Condition>> filters;
	public Map<Reference, List<Condition>> getFilters() {
		return filters;
	}

	public ReorderResultSet2(List<ResultSetHolder> r, InMemoryOrderBy order, ColumnMeta columns) {
		this.allResults = r;
		this.columns = columns;
		ResultSetCompartor orders = new ResultSetCompartor(order);
		gettingResults = new TreeSet<ResultSetHolder>(orders);
		try {
			chain(r);
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
	}

	private void chain(List<ResultSetHolder> r) throws SQLException {
		int len = r.size();
		for (int i = 0; i < len; i++) {
			ResultSetHolder re = r.get(i);
			if (re.next()) {
				this.gettingResults.add(re);
			}
		}
	}

	public int size() {
		return allResults.size();
	}

	public ColumnMeta getColumns() {
		return columns;
	}

	public boolean next() {
		// 当结果只有一个时的优化
		if (this.activeRs != null) {
			// 加入后重新排序
			if (this.activeRs.next()) {
				gettingResults.add(activeRs);
			}
		}
		return (activeRs = gettingResults.pollFirst()) != null;
	}

	public void beforeFirst() throws SQLException {
		throw new UnsupportedOperationException("beforeFirst");
	}

	public boolean first() throws SQLException {
		throw new UnsupportedOperationException("first");
	}

	public void afterLast() throws SQLException {
		throw new UnsupportedOperationException("afterLast");
	}

	public DatabaseDialect getProfile() {
		if(activeRs!=null){
			return activeRs.getProfile();
		}else{
			return allResults.get(0).getProfile();
		}

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
		columns = null;
	}

	@Override
	protected ResultSet get() {
		return activeRs;
	}

	public boolean isClosed() throws SQLException {
		return columns == null;
	}

	@Override
	public ResultSetMetaData getMetaData() throws SQLException {
		return columns.getMeta();
	}

}

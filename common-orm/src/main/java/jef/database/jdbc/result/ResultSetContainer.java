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
package jef.database.jdbc.result;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.sql.rowset.CachedRowSet;

import jef.common.log.LogUtil;
import jef.database.Condition;
import jef.database.DbUtils;
import jef.database.OperateTarget;
import jef.database.Session.PopulateStrategy;
import jef.database.dialect.DatabaseDialect;
import jef.database.meta.Reference;
import jef.database.routing.sql.InMemoryOperateProvider;
import jef.database.wrapper.clause.InMemoryDistinct;
import jef.database.wrapper.clause.InMemoryGroupByHaving;
import jef.database.wrapper.clause.InMemoryOrderBy;
import jef.database.wrapper.clause.InMemoryPaging;
import jef.database.wrapper.clause.InMemoryProcessor;
import jef.database.wrapper.clause.InMemoryStartWithConnectBy;
import jef.database.wrapper.populator.ColumnMeta;

/**
 * 查询时记录的结果集
 * 
 * @author Administrator
 * 
 */
public final class ResultSetContainer extends AbstractResultSet implements IResultSet {

	private int current = -1;
	// 重新排序部分
	private InMemoryOrderBy inMemoryOrder;
	// 重新分页逻辑
	private InMemoryPaging inMemoryPage;
	// 重新分组处理逻辑
	private List<InMemoryProcessor> mustInMemoryProcessor;

	// 所有列的元数据记录
	private ColumnMeta columns;

	protected final List<ResultSetHolder> results = new ArrayList<ResultSetHolder>(5);
	// 是否缓存
	private boolean cache;
	// 是否调试
	private boolean debug;

	public ResultSetContainer(boolean cache, boolean debug) {
		this.cache = cache;
		this.debug = debug;
	}

	public int size() {
		return results.size();
	}

	public ColumnMeta getColumns() {
		return columns;
	}

	// 级联过滤条件
	protected Map<Reference, List<Condition>> filters;

	public Map<Reference, List<Condition>> getFilters() {
		return filters;
	}

	@Override
	public ResultSetMetaData getMetaData() throws SQLException {
		return columns.getMeta();
	}

	public static IResultSet toInMemoryProcessorResultSet(InMemoryOperateProvider context, ResultSetHolder... rs) {
		ResultSetContainer mrs = new ResultSetContainer(false, false);
		for (ResultSetHolder rsh : rs) {
			mrs.add(rsh);
		}
		context.parepareInMemoryProcess(null, mrs);
		return mrs.toProperResultSet(null);
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

	public boolean first() throws SQLException {
		results.get(0).rs.first();
		for (int i = 1; i < results.size(); i++) {
			ResultSetHolder rs = results.get(i);
			if (!rs.rs.isBeforeFirst()) {
				rs.rs.beforeFirst();
			}
		}
		current = 0;
		return true;
	}

	public void afterLast() throws SQLException {
		for (ResultSetHolder rs : results) {
			rs.rs.afterLast();
		}
		current = results.size();
	}

	public void add(ResultSetHolder rsh) {
		if (columns == null) {
			try {
				initMetadata(rsh.rs);
			} catch (SQLException e) {
				throw new IllegalStateException(e);
			}
		}
		synchronized (results) {
			results.add(rsh);
		}
		if (cache) {
			try {
				rsh.rs = tryCache(rsh.rs, rsh.getProfile());
				rsh.close(false);
				return;
			} catch (SQLException e) {
				// 缓存失败
				LogUtil.exception(e);
			}
		}
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
		ResultSetHolder rsh = new ResultSetHolder(tx, statement, rs);
		synchronized (results) {
			results.add(rsh);
		}
		if (cache) {
			try {
				rsh.rs = tryCache(rs, tx.getProfile());
				rsh.close(false);
				return;
			} catch (SQLException e) {
				// 缓存失败
				LogUtil.exception(e);
			}
		}
		// rsh.rs = rs;
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
	 * 转换为可以用于输出正确的结果集
	 * 
	 * 
	 * 1、a 有内存任务，使用内存处理并排序。
	 *    b 无内存任务，有多个结果集且有排序任务，使用混合排序
	 *    c 无内存任务，有多个结果集且无排序任务，使用当前对象作为结果集
	 *    d 无内存内务，无多个结果集，退化为简单结果集
	 * 2、有分页任务，包装为分页结果集
	 * 
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public IResultSet toProperResultSet(Map<Reference, List<Condition>> filters, PopulateStrategy... args) {
		if (filters == null) {
			filters = Collections.EMPTY_MAP;
		}
		if (results.isEmpty()) {
			return new ResultSetWrapper();
		}
		//最后将要包装的对象
		IResultSet result;
		
		// 除了Order、Page以外的内存处理任务
		if (mustInMemoryProcessor != null) {
			InMemoryProcessResultSet rw = new InMemoryProcessResultSet(results,columns,filters);
			rw.addProcessor(mustInMemoryProcessor);
			rw.addProcessor(inMemoryOrder);// 如果需要处理,排序是第一位的.
			try {
				rw.process();
			} catch (SQLException e) {
				throw DbUtils.toRuntimeException(e);
			}
			result=rw;
		}else if(results.size()==1){
			ResultSetWrapper rw = new ResultSetWrapper(results.get(0), columns);
			rw.setFilters(filters);
			result=rw;
		}else if(inMemoryOrder!=null){
			ReorderResultSet2 rw = new ReorderResultSet2(results, inMemoryOrder, columns, filters);
			result=rw;
		}else{
			this.filters=filters;
			result=this;
		}
		//分页处理器
		if(inMemoryPage!=null){
			result=new LimitOffsetResultSet(result, inMemoryPage.getOffset(), inMemoryPage.getLimit());
		}
		return result;
	}

	public DatabaseDialect getProfile() {
		return results.get(current).getProfile();
	}

	@Override
	protected ResultSet get() {
		return results.get(current).rs;
	}

	public void setInMemoryPage(InMemoryPaging inMemoryPaging) {
		this.inMemoryPage = inMemoryPaging;
	}

	public void setInMemoryOrder(InMemoryOrderBy inMemoryOrder) {
		this.inMemoryOrder = inMemoryOrder;
	}

	public void setInMemoryGroups(InMemoryGroupByHaving inMemoryGroups) {
		addToInMemprocessor(inMemoryGroups);
	}

	public void setInMemoryDistinct(InMemoryDistinct instance) {
		addToInMemprocessor(instance);
	}

	public void setInMemoryConnectBy(InMemoryStartWithConnectBy parseStartWith) {
		addToInMemprocessor(parseStartWith);
	}

	public boolean isClosed() throws SQLException {
		return results.isEmpty();
	}

	public boolean isDebug() {
		return debug;
	}

	@Override
	public boolean isFirst() throws SQLException {
		throw new UnsupportedOperationException("isFirst");
	}

	@Override
	public boolean isLast() throws SQLException {
		throw new UnsupportedOperationException("isLast");
	}

	@Override
	public boolean last() throws SQLException {
		throw new UnsupportedOperationException("last");
	}

	@Override
	public boolean isBeforeFirst() throws SQLException {
		throw new UnsupportedOperationException("isBeforeFirst");
	}

	@Override
	public boolean isAfterLast() throws SQLException {
		throw new UnsupportedOperationException("isAfterLast");
	}

	// //////////////////////////// 私有方法 ///////////////////////////////////
	private void addToInMemprocessor(InMemoryProcessor process) {
		if (process != null) {
			if (this.mustInMemoryProcessor == null) {
				mustInMemoryProcessor = new ArrayList<InMemoryProcessor>(4);
			}
			mustInMemoryProcessor.add(process);
		}
	}

	private void initMetadata(ResultSet wrapped) throws SQLException {
		ResultSetMetaData meta = wrapped.getMetaData();
		this.columns = new ColumnMeta(meta);
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
}

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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jef.common.log.LogUtil;
import jef.database.Condition;
import jef.database.DbUtils;
import jef.database.ORMConfig;
import jef.database.dialect.DatabaseDialect;
import jef.database.meta.Reference;
import jef.database.rowset.CachedRowSetImpl;
import jef.database.wrapper.clause.InMemoryPaging;
import jef.database.wrapper.clause.InMemoryProcessor;
import jef.database.wrapper.populator.ColumnMeta;

public class InMemoryProcessResultSet extends AbstractResultSet{
	private CachedRowSetImpl cache;
	private ColumnMeta columns;
	private DatabaseDialect dialect;
	private List<ResultSetHolder> results;
	// 级联过滤条件
	protected Map<Reference, List<Condition>> filters;
	public Map<Reference, List<Condition>> getFilters() {
		return filters;
	}

	//动作
	private final List<InMemoryProcessor> processors=new ArrayList<InMemoryProcessor>(4);
	
	public InMemoryProcessResultSet(List<ResultSetHolder> results, ColumnMeta columns) {
		this.results=results;
		this.columns=columns;
		this.dialect=results.get(0).getDb().getProfile();
	}

	public void process() throws SQLException {
		boolean debug=ORMConfig.getInstance().isDebugMode();
		cache=new CachedRowSetImpl(ORMConfig.getInstance().getPartitionInMemoryMaxRows());
		InMemoryProcessor paging=null;
		long start=System.currentTimeMillis();
		for(ResultSetHolder sh:results){
			cache.populate(sh.rs);
			sh.close(true);
		}
		results.clear();
		long loaded=System.currentTimeMillis();
		for(InMemoryProcessor processor:processors){
			if(processor instanceof InMemoryPaging){
				paging=processor;
				continue;
			}
			processor.process(cache);
		}
		if(paging!=null){
			paging.process(cache);
		}
		long end=System.currentTimeMillis();
		if(debug){
			StringBuilder sb=new StringBuilder("InMemory processed [LOAD:" );
			sb.append(loaded-start).append("ms] [");
			for(InMemoryProcessor p:processors){
				sb.append(p.getName());
				sb.append('/');
			}
			sb.setLength(sb.length()-1);
			sb.append(':').append(end-loaded).append("ms]");
			LogUtil.show(sb.toString());
		}
		cache.refresh();
	}
	/**
	 * 添加内存记录处理器
	 * @param processor 处理器
	 */
	public void addProcessor(List<InMemoryProcessor> processor){
		if(processor.isEmpty())return;
		this.processors.addAll(processor);
	}
	/**
	 * 添加一个内存记录处理器
	 * @param processor 处理器
	 */
	public void addProcessor(InMemoryProcessor processor){
		if(processor==null)return;
		this.processors.add(processor);
	}
	
	
	public ColumnMeta getColumns() {
		return columns;
	}

	public boolean next() {
		try{
			return cache.next();
		}catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	public void beforeFirst() throws SQLException {
		cache.beforeFirst();
	}

	public boolean first() throws SQLException {
		return cache.first();
	}

	public boolean previous() throws SQLException {
		return cache.previous();
	}

	public void afterLast() throws SQLException {
		cache.afterLast();
	}

	public void close() throws SQLException {
		for(ResultSetHolder sh:results){
			sh.close(true);
		}
		results.clear();
		cache.close();
	}

	public DatabaseDialect getProfile() {
		return dialect;
	}

	@Override
	protected ResultSet get() {
		return cache;
	}

	public boolean isClosed() throws SQLException {
		return cache.isClosed();
	}

	@Override
	public ResultSetMetaData getMetaData() throws SQLException {
		return columns.getMeta();
	}

	@Override
	public boolean isFirst() throws SQLException {
		return cache.isFirst();
	}

	@Override
	public boolean isLast() throws SQLException {
		return cache.isLast();
	}

	@Override
	public boolean last() throws SQLException {
		return cache.last();
	}

	@Override
	public void moveToCurrentRow() throws SQLException {
		cache.moveToCurrentRow();
	}
}

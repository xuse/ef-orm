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

import jef.database.DbUtils;
import jef.database.ORMConfig;
import jef.database.dialect.DatabaseDialect;
import jef.database.rowset.CachedRowSetImpl;
import jef.database.wrapper.clause.InMemoryProcessor;
import jef.database.wrapper.populator.ColumnMeta;

public class InMemoryProcessResultSet extends AbstractResultSet{
	private CachedRowSetImpl cache;
	private ColumnMeta columns;
	private DatabaseDialect dialect;
	private List<ResultSetHolder> results;
	
	//动作
	private final List<InMemoryProcessor> processors=new ArrayList<InMemoryProcessor>(4);
	
	public InMemoryProcessResultSet(List<ResultSetHolder> results, ColumnMeta columns) {
		this.results=results;
		this.columns=columns;
		this.dialect=results.get(0).getDb().getProfile();
	}

	public void process() throws SQLException {
		cache=new CachedRowSetImpl(ORMConfig.getInstance().getPartitionInMemoryMaxRows());
		for(ResultSetHolder sh:results){
			cache.populate(sh.rs);
			sh.close(true);
		}
		results.clear();
		for(InMemoryProcessor processor:processors){
			processor.process(cache);
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

	public void first() throws SQLException {
		cache.first();
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
}

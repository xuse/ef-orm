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
import java.util.Comparator;
import java.util.List;

import jef.database.DbUtils;
import jef.database.dialect.DatabaseDialect;
import jef.database.wrapper.clause.InMemoryGroupBy;
import jef.database.wrapper.clause.InMemoryOrderBy;
import jef.database.wrapper.populator.ColumnMeta;
import jef.rowset.CachedRowSetImpl;

public class InMemoryProcessResultSet extends AbstractResultSet{
	private CachedRowSetImpl cache;
	private ColumnMeta columns;
	private DatabaseDialect dialect;
	private List<ResultSetHolder> results;
	
	//动作
	private List<InMemoryGroupBy> inMemoryGroupBy;
	private InMemoryOrderBy inMemoryOrderBy;
	
	public InMemoryProcessResultSet(List<ResultSetHolder> results, ColumnMeta columns) {
		this.results=results;
		this.columns=columns;
		this.dialect=results.get(0).getDb().getProfile();
	}

	public void process() throws SQLException {
		cache=new CachedRowSetImpl();
		for(ResultSetHolder sh:results){
			cache.populate(sh.rs);
			sh.close(true);
		}
		results.clear();
		if(inMemoryGroupBy!=null){
			groupBy(cache,inMemoryGroupBy);
		}
		if(inMemoryOrderBy!=null){
			orderBy(cache,inMemoryOrderBy);
		}
	}
	
	private void orderBy(CachedRowSetImpl cache, InMemoryOrderBy inMemoryOrderBy) {
		
		
		
		
	}
	static class C implements Comparator<Object>{
		private InMemoryOrderBy imo;

		public int compare(Object o1, Object o2) {
			// TODO Auto-generated method stub
			return 0;
		}
		
	}
	private void groupBy(CachedRowSetImpl cache, List<InMemoryGroupBy> inMemoryGroupBy) {
		
		
		
		
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
	
	public void setInMemoryGroups(List<InMemoryGroupBy> inMemoryGroups) {
		this.inMemoryGroupBy=inMemoryGroups;
	}


	public void setInMemoryOrder(InMemoryOrderBy inMemoryOrder) {
		this.inMemoryOrderBy=inMemoryOrder;
	}
}

package jef.database.wrapper.populator;

import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.rowset.CachedRowSet;

import jef.database.ORMConfig;
import jef.database.Session.PopulateStrategy;
import jef.database.wrapper.result.IResultSet;

public abstract class AbstractResultSetTransformer<T> implements ResultSetExtractor<T> {
	private int fetchSize;
	private int queryTimeout;
	private int maxRows;
	private static final PopulateStrategy[] EMPTY=new PopulateStrategy[0];
	
	protected AbstractResultSetTransformer(){
		ORMConfig config=ORMConfig.getInstance();
		this.fetchSize=config.getGlobalFetchSize();
		this.queryTimeout=config.getSelectTimeout();
		this.maxRows=config.getGlobalMaxResults();
	}
	
	public int getFetchSize() {
		return fetchSize;
	}

	public AbstractResultSetTransformer<T> setFetchSize(int fetchSize) {
		this.fetchSize = fetchSize;
		return this;
	}

	public int getQueryTimeout() {
		return queryTimeout;
	}

	public AbstractResultSetTransformer<T> setQueryTimeout(int queryTimeout) {
		this.queryTimeout = queryTimeout;
		return this;
	}

	public int getMaxRows() {
		return maxRows;
	}

	public AbstractResultSetTransformer<T> setMaxRows(int maxRows) {
		this.maxRows = maxRows;
		return this;
	}

	public void apply(Statement st) throws SQLException{
		if(this.fetchSize>0){
			st.setFetchSize(fetchSize);
		}
		if(this.maxRows>0){
			st.setMaxRows(maxRows);
		}
		if(this.queryTimeout>0){
			st.setQueryTimeout(queryTimeout);
		}
	}

	@Override
	public PopulateStrategy[] getStrategy() {
		return EMPTY;
	}

	@Override
	public boolean autoClose() {
		return true;
	}
	
	
	private final static class CacheAction extends AbstractResultSetTransformer<CachedRowSet>{
		@Override
		public CachedRowSet transformer(IResultSet rs) throws SQLException {
			CachedRowSet cache = rs.getProfile().newCacheRowSetInstance();
			cache.populate(rs);
			return cache;
		}
	}
	
	private final static class CountAction extends AbstractResultSetTransformer<Long>{
		@Override
		public Long transformer(IResultSet rs) throws SQLException {
			long count=0;
			while(rs.next()){
				count++;
			}
			return count;
		}
	}
	
	private static final CacheAction DEFAULT=new CacheAction();
	
	public static ResultSetExtractor<CachedRowSet> cacheResultSet(int maxRows,int fetchSize){
		if(maxRows==0 && fetchSize==0){
			return DEFAULT; 
		}
		 return new CacheAction().setFetchSize(fetchSize).setMaxRows(maxRows);
	}
	
	public static ResultSetExtractor<Long> countResultSet(int fetchSize){
		 return new CountAction().setFetchSize(fetchSize);
	}
}

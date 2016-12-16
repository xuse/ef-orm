package jef.database;

import java.nio.channels.IllegalSelectorException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import jef.database.dialect.type.ResultSetAccessor;
import jef.database.jdbc.result.ResultSetContainer;
import jef.database.meta.Reference;
import jef.database.query.ConditionQuery;
import jef.database.query.JoinElement;

public final class QueryOption implements Cloneable {
	public static final QueryOption DEFAULT=new QueryOption();
	public static QueryOption DEFAULT_MAX1;
	static{
		ORMConfig bean=ORMConfig.getInstance();
		DEFAULT.fetchSize=bean.getGlobalFetchSize();
		DEFAULT.queryTimeout=bean.getSelectTimeout();
		DEFAULT.maxResult=bean.getGlobalMaxResults();
		DEFAULT.cacheResultset=bean.isCacheResultset();
		try {
			DEFAULT_MAX1=(QueryOption) DEFAULT.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		DEFAULT_MAX1.maxResult=2;
		DEFAULT_MAX1.fetchSize=0;
	}
	private Map<String,ResultSetAccessor> mapper;
	private int maxResult;
	private int fetchSize;
	private int queryTimeout;
	boolean holdResult;
	boolean cacheResultset;
	
	private transient ResultSetContainer rs;
	
	ResultSetContainer getRs() {
		return rs;
	}

	void setRs(ResultSetContainer rs) {
		this.rs = rs;
	}

	private QueryOption(){
	}
	/**
	 * As sometimes , make sure the result has one rows.
	 * @param queryObj
	 * @return
	 */
	public static QueryOption createMax1Option(ConditionQuery queryObj){
		int queryTimeout=queryObj.getQueryTimeout();
		String tableName=null;
		if(queryObj instanceof JoinElement){
			tableName=(String)((JoinElement)queryObj).getAttribute(JoinElement.CUSTOM_TABLE_NAME);	
		}
		if(queryTimeout>0 || tableName!=null){
			QueryOption op;
			try {
				op = (QueryOption) DEFAULT_MAX1.clone();
			} catch (CloneNotSupportedException e) {
				throw new IllegalSelectorException();//never happens
			}
			op.queryTimeout=queryTimeout;
			return op;
		}else{
			return DEFAULT_MAX1;
		}
	}
	
	public static QueryOption createFrom(ConditionQuery queryObj){
		QueryOption op=createOption();
		int maxResult=queryObj.getMaxResult();
		if(maxResult>0)
			op.maxResult=maxResult;
		int fetchSize=queryObj.getFetchSize();
		if(fetchSize>0)
			op.fetchSize=fetchSize;
		int queryTimeout=queryObj.getQueryTimeout();
		if(queryTimeout>0)
			op.queryTimeout=queryTimeout;
		return op;
	}

	public static QueryOption createOption(){
		try {
			return (QueryOption) DEFAULT.clone();
		} catch (CloneNotSupportedException e) {
			throw new IllegalSelectorException();//never happens
		}
	}
	
	public int getFetchSize() {
		return fetchSize;
	}
	public int getMaxResult() {
		return maxResult;
	}

	public int getQueryTimeout() {
		return queryTimeout;
	}

	public void setFetchSize(int fetchSize) {
		this.fetchSize = fetchSize;
	}

	public void setQueryTimeout(int queryTimeout) {
		this.queryTimeout = queryTimeout;
	}

	public void setMaxResult(int maxResult) {
		this.maxResult = maxResult;
	}

	public void setCacheResultset(boolean cacheResultset) {
		this.cacheResultset = cacheResultset;
	}

	/**
	 * 设置max,fetchSize, queryTimeout等参数
	 * @param psmt
	 * @throws SQLException
	 */
	void setSizeFor(Statement psmt) throws SQLException {
		int maxResult=this.maxResult;
		if (maxResult > 0)
			psmt.setMaxRows(maxResult);
		int fetchSize=this.fetchSize;
		if (fetchSize > 0) {
			psmt.setFetchSize(fetchSize);
		}
		psmt.setQueryTimeout(queryTimeout);
	}

	public Map<String, ResultSetAccessor> getMapper() {
		return mapper;
	}

	public void setMapper(Map<String, ResultSetAccessor> mapper) {
		this.mapper = mapper;
	}
	
	public String toString(){
		return maxResult+"/fetch:"+fetchSize+"/timeout:"+queryTimeout;
	}
	
	
	List<Reference> skipReference; 
}

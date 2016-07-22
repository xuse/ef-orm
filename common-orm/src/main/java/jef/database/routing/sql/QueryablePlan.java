package jef.database.routing.sql;

import java.sql.ResultSet;
import java.sql.SQLException;

import jef.database.wrapper.populator.ResultSetExtractor;
import jef.tools.PageLimit;

public interface QueryablePlan extends ExecutionPlan{
	public boolean mustGetAllResultsToCount();
	
	public ResultSet getResultSet(SqlAndParameter parer,int maxRows, int fetchSize) throws SQLException;

	public long getCount(SqlAndParameter parse,int maxSize,int fetchSize) throws SQLException;
	
	<T> T doQuery(SqlAndParameter parse,ResultSetExtractor<T> extractor, boolean forCount,PageLimit range) throws SQLException;
}

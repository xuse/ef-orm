package jef.database.routing.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public interface SQLExecutor {
	public int executeUpdate() throws SQLException;
	public ResultSet getResultSet() throws SQLException;
	public void setFetchSize(int fetchSize);
	public void setMaxResults(int maxRows);
	public void setResultSetType(int resultSetType);
	public void setResultSetConcurrency(int resultSetConcurrency);
	public void setResultSetHoldability(int resultSetHoldability);
	public void setQueryTimeout(int queryTimeout);
	public void setParams(List<Object> params);
}

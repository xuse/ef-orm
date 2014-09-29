package jef.database.support.executor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ExecutorImpl implements StatementExecutor{

	
	
	
	@Override
	public void executeSql(String... ddls) throws SQLException {
		
		
	}

	@Override
	public void executeSql(List<String> ddls) throws SQLException {
		
		
	}

	@Override
	public void close() {
		
		
	}

	@Override
	public void setQueryTimeout(int seconds) throws SQLException {
		
		
	}

	@Override
	public ResultSet executeQuery(String sql, Object... params) throws SQLException {
		
		return null;
	}

	@Override
	public int executeUpdate(String sql, Object... params) throws SQLException {
		
		return 0;
	}
}

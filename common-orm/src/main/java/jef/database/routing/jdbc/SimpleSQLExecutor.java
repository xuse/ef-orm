package jef.database.routing.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import jef.database.OperateTarget;
import jef.database.wrapper.result.JdbcResultSetAdapter;
import jef.database.wrapper.result.ResultSetWrapper;

public class SimpleSQLExecutor implements SQLExecutor {
	private String sql;
	private OperateTarget db;
	private final List<Object> params=new ArrayList<Object>();
	private int fetchSize;
	private int maxRows;
	private int queryTimeout;
	
	public SimpleSQLExecutor(OperateTarget target, String sql) {
		this.db=target;
		this.sql=sql;
	}

	@Override
	public int executeUpdate() throws SQLException {
		PreparedStatement st=db.prepareStatement(sql);
		try{
			for(int i=0;i<params.size();i++){
				st.setObject(i, params.get(i));
			}
			return st.executeUpdate();	
		}finally{
			st.close();
		}
	}

	@Override
	public ResultSet getResultSet() throws SQLException {
		PreparedStatement st=db.prepareStatement(sql);
		try{
			for(int i=0;i<params.size();i++){
				st.setObject(i, params.get(i));
			}
			if(fetchSize>0)
				st.setFetchSize(fetchSize);
			if(maxRows>0);
				st.setMaxRows(maxRows);
			if(queryTimeout>0)
				st.setQueryTimeout(queryTimeout);
			ResultSet rs=st.executeQuery();
			return new JdbcResultSetAdapter(new ResultSetWrapper(db,st,rs));	
		}finally{
			st.close();
		}
	}
	

	@Override
	public void setFetchSize(int fetchSize) {
		this.fetchSize=fetchSize;
	}

	@Override
	public void setMaxResults(int maxRows) {
		this.maxRows=maxRows;
	}

	@Override
	public void setResultSetType(int resultSetType) {
	}

	@Override
	public void setResultSetConcurrency(int resultSetConcurrency) {
	}

	@Override
	public void setResultSetHoldability(int resultSetHoldability) {
	}

	@Override
	public void setQueryTimeout(int queryTimeout) {
		this.queryTimeout=queryTimeout;
	}

	@Override
	public void setParams(List<Object> params) {
		this.params.clear();
		this.params.addAll(params);
	}
}

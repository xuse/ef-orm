package jef.database.routing.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import jef.database.OperateTarget;
import jef.database.innerpool.WrapableConnection;
import jef.database.jdbc.GenerateKeyReturnOper;

public class SimpleSQLExecutor implements SQLExecutor {
	private String sql;
	private OperateTarget db;
	private int fetchSize;
	private int maxRows;
	private int queryTimeout;

	public SimpleSQLExecutor(OperateTarget target, String sql) {
		this.db = target;
		this.sql = sql;
	}

	@Override
	public UpdateReturn executeUpdate(GenerateKeyReturnOper oper, List<ParameterContext> params) throws SQLException {
		PreparedStatement st=null;
		try (WrapableConnection conn=db.get()){
			st = oper.prepareStatement(conn, sql);
			for (ParameterContext context : params) {
				context.apply(st);
			}
			UpdateReturn result = new UpdateReturn(st.executeUpdate());
			oper.getGeneratedKey(result, st);
			return result;
		} finally {
			st.close();
		}
	}
//
//	@Override
//	public ResultSet getResultSet(int type, int concurrency, int holder, List<ParameterContext> params) throws SQLException {
//		if (type < 1) {
//			type = ResultSet.TYPE_FORWARD_ONLY;
//		}
//		if (concurrency < 1) {
//			concurrency = ResultSet.CONCUR_READ_ONLY;
//		}
//		if (holder < 1) {
//			holder = ResultSet.CLOSE_CURSORS_AT_COMMIT;
//		}
//		try (WrapableConnection conn=db.get()){
//			PreparedStatement st = conn.prepareStatement(sql, type, concurrency, holder);
//			try {
//				if (fetchSize > 0)
//					st.setFetchSize(fetchSize);
//				if (maxRows > 0)
//					;
//				st.setMaxRows(maxRows);
//				if (queryTimeout > 0)
//					st.setQueryTimeout(queryTimeout);
//				for (ParameterContext context : params) {
//					context.apply(st);
//				}
//				ResultSet rs = st.executeQuery();
//				return new ResultSetHolder(conn, st, rs);
//			} finally {
//				st.close();
//			}	
//		}
//	}

	@Override
	public void setFetchSize(int fetchSize) {
		this.fetchSize = fetchSize;
	}

	@Override
	public void setMaxResults(int maxRows) {
		this.maxRows = maxRows;
	}

	@Override
	public void setQueryTimeout(int queryTimeout) {
		this.queryTimeout = queryTimeout;
	}

	@Override
	public BatchReturn executeBatch(GenerateKeyReturnOper oper, List<List<ParameterContext>> params) throws SQLException {
		try(WrapableConnection conn=db.get()){
			PreparedStatement st = oper.prepareStatement(conn, sql);
			for (Collection<ParameterContext> param : params) {
				for (ParameterContext context : param) {
					context.apply(st);
				}
				st.addBatch();
			}
			try {
				int[] re = st.executeBatch();
				BatchReturn result = new BatchReturn(re);
				oper.getGeneratedKey(result, st);
				return result;
			} finally {
				st.close();
			}	
		}
	}
}

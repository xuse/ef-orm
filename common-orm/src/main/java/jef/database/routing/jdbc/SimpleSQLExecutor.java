package jef.database.routing.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import jef.database.jdbc.GenerateKeyReturnOper;
import jef.database.jdbc.JDBCTarget;
import jef.database.jdbc.result.ResultSetHolder;

public class SimpleSQLExecutor implements SQLExecutor {
	private String sql;
	private JDBCTarget db;
	private int fetchSize;
	private int maxRows;
	private int queryTimeout;

	public SimpleSQLExecutor(JDBCTarget target, String sql) {
		this.db = target;
		this.sql = sql;
	}

	@Override
	public UpdateReturn executeUpdate(GenerateKeyReturnOper oper, List<ParameterContext> params) throws SQLException {
		PreparedStatement st = oper.prepareStatement(db, sql);
		try {
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

	@Override
	public ResultSet getResultSet(int type, int concurrency, int holder, List<ParameterContext> params) throws SQLException {
		if (type < 1) {
			type = ResultSet.TYPE_FORWARD_ONLY;
		}
		if (concurrency < 1) {
			concurrency = ResultSet.CONCUR_READ_ONLY;
		}
		if (holder < 1) {
			holder = ResultSet.CLOSE_CURSORS_AT_COMMIT;
		}
		PreparedStatement st = db.prepareStatement(sql, type, concurrency, holder);
		try {
			if (fetchSize > 0)
				st.setFetchSize(fetchSize);
			if (maxRows > 0)
				;
			st.setMaxRows(maxRows);
			if (queryTimeout > 0)
				st.setQueryTimeout(queryTimeout);
			for (ParameterContext context : params) {
				context.apply(st);
			}
			ResultSet rs = st.executeQuery();
			return new ResultSetHolder(db, st, rs);
		} finally {
			st.close();
		}
	}

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
		PreparedStatement st = oper.prepareStatement(db, sql);
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

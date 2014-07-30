package jef.database.wrapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.rowset.CachedRowSet;

import jef.database.dialect.DatabaseDialect;

public interface ResultSetTransformer<T> {
	T transformer(ResultSet rs, DatabaseDialect profile) throws SQLException;

	public static final ResultSetTransformer<CachedRowSet> CACHED_RESULTSET = new ResultSetTransformer<CachedRowSet>() {
		public CachedRowSet transformer(ResultSet rs, DatabaseDialect db) throws SQLException {
			CachedRowSet cache = db.newCacheRowSetInstance();
			cache.populate(rs);
			return cache;
		}
	};

	public static final ResultSetTransformer<Long> GET_FIRST_LONG = new ResultSetTransformer<Long>() {
		public Long transformer(ResultSet rs, DatabaseDialect db) throws SQLException {
			if (rs.next()) {
				return rs.getLong(1);
			} else {
				throw new SQLException("Result incorrect.count result must not be empty.");
			}
		}
	};
	
	public static final ResultSetTransformer<Integer> GET_FIRST_INT = new ResultSetTransformer<Integer>() {
		public Integer transformer(ResultSet rs, DatabaseDialect db) throws SQLException {
			if (rs.next()) {
				return rs.getInt(1);
			} else {
				throw new SQLException("Result incorrect.count result must not be empty.");
			}
		}
	};
}

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
package jef.database.wrapper.populator;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

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
	
	public static final ResultSetTransformer<Date> GET_FIRST_TIMESTAMP = new ResultSetTransformer<Date>() {
		public Date transformer(ResultSet rs, DatabaseDialect db) throws SQLException {
			if (rs.next()) {
				java.sql.Timestamp ts=rs.getTimestamp(1);
				return new java.util.Date(ts.getTime());
			} else {
				throw new SQLException("Result incorrect.count result must not be empty.");
			}
		}
	};
}

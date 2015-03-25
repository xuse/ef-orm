package jef.database.dialect.type;

import java.sql.SQLException;

import jef.database.jdbc.result.IResultSet;

final class ResultRawAccessor implements ResultSetAccessor{

	public Object jdbcGet(IResultSet rs, int n) throws SQLException {
		return rs.getObject(n);
	}

	public boolean applyFor(int type) {
		return true;
	}

}

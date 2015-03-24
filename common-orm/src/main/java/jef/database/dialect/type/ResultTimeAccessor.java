package jef.database.dialect.type;

import java.sql.SQLException;
import java.sql.Types;

import jef.database.jdbc.result.IResultSet;

final class ResultTimeAccessor implements ResultSetAccessor{
	public Object jdbcGet(IResultSet rs,int n) throws SQLException {
		return rs.getTime(n);
	}
	public Class<?> getReturnType() {
		return java.sql.Time.class;
	}
	public boolean applyFor(int type) {
		return Types.DATE==type || Types.TIMESTAMP==type ||Types.TIME==type;
	}
	
	
}

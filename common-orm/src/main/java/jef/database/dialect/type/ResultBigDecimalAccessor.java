package jef.database.dialect.type;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Types;

import jef.database.jdbc.result.IResultSet;

final class ResultBigDecimalAccessor implements ResultSetAccessor{
	public Object jdbcGet(IResultSet rs,int n) throws SQLException {
		return rs.getBigDecimal(n);
	}

	public Class<?> getReturnType() {
		return BigDecimal.class;
	}

	public boolean applyFor(int type) {
		return Types.DECIMAL == type ||Types.NUMERIC==type;
	}
}

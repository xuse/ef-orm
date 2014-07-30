package jef.database.dialect.type;

import java.sql.SQLException;

import jef.database.wrapper.IResultSet;

final class ResultRawAccessor implements ResultSetAccessor{

	public Object getProperObject(IResultSet rs, int n) throws SQLException {
		return rs.getObject(n);
	}

	public boolean applyFor(int type) {
		return true;
	}

}

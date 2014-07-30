package jef.database.dialect.type;

import java.sql.SQLException;

import jef.database.wrapper.IResultSet;

final  class ResultStringAccessor implements ResultSetAccessor{
	public Object getProperObject(IResultSet rs,int n) throws SQLException {
		return rs.getString(n);
	}

	public Class<?> getReturnType() {
		return String.class;
	}

	public boolean applyFor(int type) {
		return true;
	}
}

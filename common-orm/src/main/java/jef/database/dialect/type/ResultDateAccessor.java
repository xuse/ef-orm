package jef.database.dialect.type;

import java.sql.SQLException;
import java.sql.Types;

import jef.database.jdbc.result.IResultSet;

final class ResultDateAccessor implements ResultSetAccessor{
	public Object getProperObject(IResultSet rs,int n) throws SQLException {
		java.sql.Date d=rs.getDate(n);
		return d==null?null:new java.util.Date(d.getTime());
	}

	public Class<?> getReturnType() {
		return java.util.Date.class;
	}

	public boolean applyFor(int type) {
		return Types.DATE == type ||Types.TIMESTAMP==type || type==-104;
	}
}

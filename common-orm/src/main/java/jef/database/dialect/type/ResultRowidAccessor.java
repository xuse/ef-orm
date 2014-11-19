package jef.database.dialect.type;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;

import jef.database.jdbc.result.IResultSet;

final  class ResultRowidAccessor implements ResultSetAccessor{
	public Object getProperObject(IResultSet rs,int n) throws SQLException {
		byte[] bytes=rs.getRowId(n).getBytes();
		try {
			return new String(bytes,"US-ASCII");
		} catch (UnsupportedEncodingException e) {
			//never happens..
			return null;
		}
	}

	public Class<?> getReturnType() {
		return String.class;
	}

	public boolean applyFor(int type) {
		return true;
	}
}

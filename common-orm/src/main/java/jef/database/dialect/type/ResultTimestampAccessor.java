package jef.database.dialect.type;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

import jef.database.wrapper.IResultSet;

final class ResultTimestampAccessor implements ResultSetAccessor {
	public Object getProperObject(IResultSet rs,int n) throws SQLException {
		Timestamp d= rs.getTimestamp(n);
		if(d!=null){
			return new java.util.Date(d.getTime());
		}else{
			return null;
		}
	}

	public Class<?> getReturnType() {
		return java.util.Date.class;
	}

	public boolean applyFor(int type) {
		return Types.DATE==type || Types.TIMESTAMP==type;
	}
	

}

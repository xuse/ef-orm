package jef.database.dialect.type;

import java.sql.SQLException;

import jef.database.jdbc.result.IResultSet;

public class ResultFloatAccessor implements ResultSetAccessor {

	public Object jdbcGet(IResultSet rs, int n) throws SQLException {
		Object value=rs.getObject(n);
		if(value==null)return null;
		if(value instanceof Float){
			return value;
		}else if(value instanceof Number){
			return ((Number) value).floatValue();
		}
		throw new IllegalArgumentException("The column "+n+" from database is type "+value.getClass()+" but expected is int.");
	}

	public Class<?> getReturnType() {
		return Float.class;
	}

	public boolean applyFor(int type) {
		return type>=2 && type<=8;
	}
}

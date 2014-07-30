package jef.database.dialect.type;

import java.sql.SQLException;
import java.sql.Types;

import jef.database.wrapper.IResultSet;

public final class ResultDoubleAccessor implements ResultSetAccessor {
	
	public Object getProperObject(IResultSet rs, int n) throws SQLException {
		Object value=rs.getObject(n);
		if(value==null)return null;
		if(value instanceof Double){
			return value;
		}else if(value instanceof Number){
			return ((Number) value).doubleValue();
		}
		throw new IllegalArgumentException("The column "+n+" from database is type "+value.getClass()+" but expected is double.");
	}
	public Class<?> getReturnType() {
		return Double.class;
	}
	public boolean applyFor(int type) {
		return Types.DOUBLE==type || Types.FLOAT==type || Types.NUMERIC==type || Types.INTEGER==type || Types.TINYINT==type ||Types.BIGINT==type;
	}

}

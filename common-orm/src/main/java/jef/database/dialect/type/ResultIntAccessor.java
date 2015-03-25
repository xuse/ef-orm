package jef.database.dialect.type;

import java.sql.SQLException;
import java.sql.Types;

import jef.database.jdbc.result.IResultSet;

final  class ResultIntAccessor implements ResultSetAccessor{
	public Object jdbcGet(IResultSet rs,int n) throws SQLException {
		Object value=rs.getObject(n);
		if(value==null)return null;
		if(value instanceof Integer){
			return value;
		}else if(value instanceof Number){
			return ((Number) value).intValue();
		}
		throw new IllegalArgumentException("The column "+n+" from database is type "+value.getClass()+" but expected is int.");
	}
	public Class<?> getReturnType() {
		return Integer.class;
	}
	public boolean applyFor(int type) {
		return Types.INTEGER==type || Types.TINYINT==type || Types.SMALLINT==type || Types.BIGINT==type || Types.NUMERIC==type;
	}

}

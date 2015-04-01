package jef.database.dialect.type;

import java.sql.SQLException;
import java.sql.Types;

import javax.persistence.PersistenceException;

import jef.database.jdbc.result.IResultSet;

final  class ResultLongAccessor implements ResultSetAccessor {
	public Object jdbcGet(IResultSet rs,int n) throws SQLException {
		Object value=rs.getObject(n);
		if(value==null)return null;
		if(value instanceof Long){
			return value;
		}else if(value instanceof Number){
			return ((Number) value).longValue();
		}
		throw new PersistenceException("The column "+n+" from database is type "+value.getClass()+" but expected is double.");
	}
	public Class<?> getReturnType() {
		return Long.class;
	}
	public boolean applyFor(int type) {
		return 	Types.BIGINT==type || Types.INTEGER==type || Types.TINYINT==type || Types.NUMERIC==type || Types.SMALLINT==type;
	}
	

}

package jef.database.dialect.type;

import java.sql.SQLException;

import javax.persistence.PersistenceException;

import jef.database.jdbc.result.IResultSet;

public final class ResultDoubleAccessor implements ResultSetAccessor {
	
	public Object jdbcGet(IResultSet rs, int n) throws SQLException {
		Object value=rs.getObject(n);
		if(value==null)return null;
		if(value instanceof Double){
			return value;
		}else if(value instanceof Number){
			return ((Number) value).doubleValue();
		}
		throw new PersistenceException("The column "+n+" from database is type "+value.getClass()+" but expected is double.");
	}
	public Class<?> getReturnType() {
		return Double.class;
	}
	public boolean applyFor(int type) {
		return type>=2 && type<=8;
	}

}

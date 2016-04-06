package jef.database.dialect.type;

import java.sql.SQLException;

import javax.persistence.PersistenceException;

import jef.database.jdbc.result.IResultSet;

public class ResultByteAccessor implements ResultSetAccessor {

	public Object jdbcGet(IResultSet rs, int n) throws SQLException {
		Object value=rs.getObject(n);
		if(value==null)return null;
		if(value instanceof Byte){
			return value;
		}else if(value instanceof Number){
			return ((Number) value).byteValue();
		}
		throw new PersistenceException("The column "+n+" from database is type "+value.getClass()+" but expected is int.");
	}

	public Class<?> getReturnType() {
		return Byte.class;
	}

	public boolean applyFor(int type) {
		return true;
	}
}

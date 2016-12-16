package jef.database.dialect.type;

import java.sql.SQLException;
import java.sql.Types;

import javax.persistence.PersistenceException;

import jef.database.jdbc.result.IResultSet;

public class ResultShortAccessor implements ResultSetAccessor {

	public Object jdbcGet(IResultSet rs, int n) throws SQLException {
		Object value=rs.getObject(n);
		if(value==null)return null;
		if(value instanceof Short){
			return value;
		}else if(value instanceof Number){
			return ((Number) value).shortValue();
		}
		throw new PersistenceException("The column "+n+" from database is type "+value.getClass()+" but expected is int.");
	}

	public Class<?> getReturnType() {
		return Short.class;
	}

	public boolean applyFor(int type) {
		return Types.INTEGER==type || Types.TINYINT==type || Types.SMALLINT==type || Types.NUMERIC==type;
	}
}

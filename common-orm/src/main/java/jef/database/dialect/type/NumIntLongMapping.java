package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.IResultSet;

public class NumIntLongMapping extends AColumnMapping{
	public Object jdbcSet(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		if(value==null){
			st.setNull(index, java.sql.Types.INTEGER);
		}else{
			st.setLong(index, ((Number)value).longValue());
		}
		return value;
	}

	public int getSqlType() {
		return java.sql.Types.INTEGER;
	}

	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		return value.toString();
	}
	
	public Object jdbcGet(IResultSet rs, int n) throws SQLException {
		Object obj=rs.getObject(n);
		if(obj==null)return null;
		if(obj instanceof Long)return obj;
		return ((Number)obj).longValue();
	}

	@Override
	protected Class<?> getDefaultJavaType() {
		return Long.class;
	}
}

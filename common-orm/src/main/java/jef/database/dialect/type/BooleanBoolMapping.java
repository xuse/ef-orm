package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.IResultSet;

public class BooleanBoolMapping extends AColumnMapping{
	public Object jdbcSet(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		if(value==null){
			st.setNull(index, java.sql.Types.BOOLEAN);
		}else{
			st.setBoolean(index, ((Boolean)value).booleanValue());
		}
		return value;
	}

	public int getSqlType() {
		return java.sql.Types.BOOLEAN;
	}
	
	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		return value.toString();
	}

	public Object jdbcGet(IResultSet rs, int n) throws SQLException {
		Object value=rs.getObject(n);
		return (Boolean)value;
	}

	@Override
	protected Class<?> getDefaultJavaType() {
		return Boolean.class;
	}

	@Override
	public void jdbcUpdate(ResultSet rs, String columnLabel, Object value, DatabaseDialect dialect) throws SQLException {
		rs.updateBoolean(columnLabel, ((Boolean)value).booleanValue());
	}
}

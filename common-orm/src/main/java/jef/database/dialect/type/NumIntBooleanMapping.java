package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.IResultSet;

public class NumIntBooleanMapping extends AColumnMapping{
	public Object jdbcSet(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		if(value==null){
			st.setNull(index, java.sql.Types.INTEGER);
		}else{
			st.setInt(index, ((Boolean)value)?1:0);
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
		return ((Number)obj).intValue()!=0;
	}

	@Override
	protected Class<?> getDefaultJavaType() {
		return Boolean.class;
	}
}

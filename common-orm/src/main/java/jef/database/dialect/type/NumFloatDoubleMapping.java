package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.IResultSet;

public class NumFloatDoubleMapping extends AColumnMapping{
	public Object jdbcSet(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		if(value==null){
			st.setNull(index, java.sql.Types.FLOAT);
		}else{
			st.setDouble(index, ((Number)value).doubleValue());
		}
		return value;
	}

	public int getSqlType() {
		return java.sql.Types.FLOAT;
	}

	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		return value.toString();
	}
	
	public Object jdbcGet(IResultSet rs, int n) throws SQLException {
		Object obj=rs.getObject(n);
		if(obj==null)return null;
		if(obj instanceof Double)return obj;
		return ((Number)obj).doubleValue();
	}

	@Override
	protected Class<?> getDefaultJavaType() {
		return Double.class;
	}
}

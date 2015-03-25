package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.IResultSet;
import jef.tools.StringUtils;

public class NumDoubleStringMapping extends AColumnMapping{

	public Object jdbcSet(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		if(StringUtils.isEmpty(value)){
			st.setNull(index, java.sql.Types.DOUBLE);
		}else{
			st.setDouble(index, Double.parseDouble((String)value));
		}
		return value;
	}

	public int getSqlType() {
		return java.sql.Types.DOUBLE;
	}

	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		return value.toString();
	}
	
	public Object jdbcGet(IResultSet rs, int n) throws SQLException {
		Object obj=rs.getObject(n);
		if(obj==null)return null;
		return obj.toString();
	}

	@Override
	protected Class<?> getDefaultJavaType() {
		return String.class;
	}
}

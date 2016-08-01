package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.IResultSet;

public class BitBooleanMapping extends AColumnMapping{
	@Override
	public Object jdbcSet(PreparedStatement st, Object value, int index, DatabaseDialect dialect) throws SQLException {
		if(value==null){
			st.setNull(index, java.sql.Types.BIT);
			return null;
		}else{
			st.setBoolean(index, (Boolean)value);
			return value;
		}
	}
	
	@Override
	public void jdbcUpdate(ResultSet rs,String column, Object value, DatabaseDialect dialect) throws SQLException {
		rs.updateBoolean(column, (Boolean)value);
	}

	@Override
	public int getSqlType() {
		return java.sql.Types.BIT;
	}

	@Override
	public Object jdbcGet(IResultSet rs, int n) throws SQLException {
		return rs.getBoolean(n);
	}

	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		if((Boolean)value){
			return "1";
		}else{
			return "0";
		}
	}

	@Override
	protected Class<?> getDefaultJavaType() {
		return Boolean.class;
	}

}

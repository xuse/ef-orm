package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.IResultSet;

public class CharIntMapping extends AColumnMapping{
	public Object jdbcSet(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		if(value==null){
			st.setNull(index, java.sql.Types.CHAR);
		}else{
			st.setString(index, value.toString());
		}
		return value;
	}

	public int getSqlType() {
		return java.sql.Types.CHAR;
	}

	public Object jdbcGet(IResultSet rs, int n) throws SQLException {
		String obj=rs.getString(n);
		if(obj==null || obj.length()==0)return null;
		return Integer.parseInt(obj);
	}

	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		return super.wrapSqlStr(String.valueOf(value));
	}

	@Override
	protected Class<?> getDefaultJavaType() {
		return Integer.class;
	}

	@Override
	public void jdbcUpdate(ResultSet rs, String columnIndex, Object value, DatabaseDialect dialect) throws SQLException {
		rs.updateString(columnIndex,value.toString());
	}
}

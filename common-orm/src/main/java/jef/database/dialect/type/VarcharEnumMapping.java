package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.IResultSet;

public class VarcharEnumMapping extends AColumnMapping{
	
	
	public Object jdbcSet(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		if(value==null){
			st.setNull(index, java.sql.Types.VARCHAR);
		}else{
			st.setString(index, value.toString());
		}
		return value;
	}

	public int getSqlType() {
		return java.sql.Types.VARCHAR;
	}

	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		return super.wrapSqlStr(value.toString());
	}
	
	@SuppressWarnings("unchecked")
	public Object jdbcGet(IResultSet rs, int n) throws SQLException {
		String s=rs.getString(n);
		if(s==null || s.length()==0)return null;
		return Enum.valueOf(clz.asSubclass(Enum.class), s);
	}

	@Override
	protected Class<?> getDefaultJavaType() {
		return Enum.class;
	}
}

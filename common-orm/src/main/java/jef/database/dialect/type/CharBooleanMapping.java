package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.IResultSet;

public class CharBooleanMapping extends AColumnMapping{

	public Object jdbcSet(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		if(value==null){
			st.setNull(index, java.sql.Types.CHAR);
			return null;
		}else{
			String str=((Boolean)value)?"1":"0";
			st.setString(index, str);
			return str;
		}
		
	}

	public int getSqlType() {
		return java.sql.Types.CHAR;
	}

	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		if((Boolean)value){
			return "'1'";
		}else{
			return "'0'";
		}
	}

	public Object jdbcGet(IResultSet rs, int n) throws SQLException {
		String s=rs.getString(n);
		if(s!=null && s.length()>0){
			char c=s.charAt(0);
			return Boolean.valueOf(c=='1' || c=='T');
		}
		return null;
	}

	@Override
	protected Class<?> getDefaultJavaType() {
		return Boolean.class;
	}
}

package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import jef.database.dialect.DatabaseDialect;
import jef.database.wrapper.result.IResultSet;

public class CharBooleanMapping extends ATypeMapping<Boolean>{

	public Object set(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
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

	public Object getProperObject(IResultSet rs, int n) throws SQLException {
		String s=rs.getString(n);
		if(s!=null && s.length()>0){
			char c=s.charAt(0);
			return Boolean.valueOf(c=='1' || c=='T');
		}
		return null;
	}
}

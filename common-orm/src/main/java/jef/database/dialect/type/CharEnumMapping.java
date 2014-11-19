package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.IResultSet;


public class CharEnumMapping<T extends Enum<T>> extends AColumnMapping<T> {
	public CharEnumMapping(Class<T> clz){
		this.clz=clz;
	}
	
	public int getSqlType() {
		return java.sql.Types.CHAR;
	}
	
	public Object set(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		if(value==null){
			st.setNull(index, java.sql.Types.CHAR);
		}else{
			st.setString(index, ((Enum<?>)value).name());
		}
		return value;
	}

	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		return super.wrapSqlStr(value.toString());
	}

	public Object getProperObject(IResultSet rs, int n) throws SQLException {
		String s=rs.getString(n);
		if(s==null || s.length()==0)return null;
		return Enum.valueOf(clz, s);
	}
}

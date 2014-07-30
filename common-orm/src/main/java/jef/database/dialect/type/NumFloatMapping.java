package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import jef.database.dialect.DatabaseDialect;
import jef.database.wrapper.IResultSet;

public class NumFloatMapping extends ATypeMapping<Float>{
	public Object set(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		if(value==null){
			st.setNull(index, java.sql.Types.FLOAT);
		}else{
			st.setFloat(index, ((Number)value).floatValue());
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
	
	public Object getProperObject(IResultSet rs, int n) throws SQLException {
		Object obj=rs.getObject(n);
		if(obj==null)return null;
		if(obj instanceof Float)return obj;
		return ((Number)obj).floatValue();
	}
}

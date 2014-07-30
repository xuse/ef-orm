package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import jef.database.dialect.DatabaseDialect;
import jef.database.wrapper.IResultSet;

public class BooleanBoolMapping extends ATypeMapping<Boolean>{
	public Object set(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		if(value==null){
			st.setNull(index, java.sql.Types.BOOLEAN);
		}else{
			st.setBoolean(index, ((Boolean)value).booleanValue());
		}
		return value;
	}

	public int getSqlType() {
		return java.sql.Types.BOOLEAN;
	}
	
	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		return value.toString();
	}

	public Object getProperObject(IResultSet rs, int n) throws SQLException {
		Object value=rs.getObject(n);
		return (Boolean)value;
	}
}

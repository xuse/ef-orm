package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.IResultSet;

public class BitBooleanMapping extends AColumnMapping<Boolean>{
	@Override
	public Object set(PreparedStatement st, Object value, int index, DatabaseDialect dialect) throws SQLException {
		if(value==null){
			st.setNull(index, java.sql.Types.BIT);
			return null;
		}else{
			st.setBoolean(index, (Boolean)value);
			return value;
		}
	}

	@Override
	public int getSqlType() {
		return java.sql.Types.BIT;
	}

	@Override
	public Object getProperObject(IResultSet rs, int n) throws SQLException {
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

}

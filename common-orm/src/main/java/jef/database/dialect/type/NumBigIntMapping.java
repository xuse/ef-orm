package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import jef.database.dialect.DatabaseDialect;
import jef.database.wrapper.result.IResultSet;

public class NumBigIntMapping extends ATypeMapping<Integer> {

	public Object set(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		if(value==null){
			st.setNull(index, java.sql.Types.BIGINT);
		}else{
			st.setInt(index, ((Number)value).intValue());
		}
		return value;
	}

	public int getSqlType() {
		return java.sql.Types.BIGINT;
	}

	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		return value.toString();
	}

	public Object getProperObject(IResultSet rs, int n) throws SQLException {
		Object obj=rs.getObject(n);
		if(obj==null)return null;
		if(obj instanceof Integer)return obj;
		return ((Number)obj).intValue();
	}
}

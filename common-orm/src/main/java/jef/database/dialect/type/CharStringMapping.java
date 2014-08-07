package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import jef.database.dialect.DatabaseDialect;
import jef.database.wrapper.result.IResultSet;

public class CharStringMapping extends ATypeMapping<String>{
	public int getSqlType() {
		return java.sql.Types.CHAR;
	}
	
	public Object set(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		st.setString(index, (String)value);
		return value;
	}

	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		return super.wrapSqlStr(value.toString());
	}

	public Object getProperObject(IResultSet rs, int n) throws SQLException {
		return rs.getString(n);
	}
}

package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.IResultSet;

public class VarcharStringMapping extends AColumnMapping{
	public Object jdbcSet(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		st.setString(index, value==null?null:String.valueOf(value));
		return value;
	}

	public int getSqlType() {
		return java.sql.Types.VARCHAR;
	}

	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		return super.wrapSqlStr(value.toString());
	}

	public Object jdbcGet(IResultSet rs, int n) throws SQLException {
		return rs.getString(n);
	}

	@Override
	protected Class<?> getDefaultJavaType() {
		return String.class;
	}
}

package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import jef.database.dialect.DatabaseDialect;
import jef.database.wrapper.IResultSet;

public class XmlStringMapping extends ATypeMapping<String>{
	public Object set(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		st.setString(index, value==null?null:String.valueOf(value));
		return value;
	}

	public int getSqlType() {
		return java.sql.Types.SQLXML;
	}

	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		return super.wrapSqlStr(value.toString());
	}

	public Object getProperObject(IResultSet rs, int n) throws SQLException {
		return rs.getString(n);
	}
}

package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;

import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.IResultSet;

public class SqlTimeTimeMapping extends AColumnMapping<java.sql.Time>{

	public Object set(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		st.setTime(index, (Time)value);
		return value;
	}

	public int getSqlType() {
		return java.sql.Types.TIME;
	}

	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		return profile.getSqlTimeExpression((java.util.Date)value);
	}

	public Object getProperObject(IResultSet rs, int n) throws SQLException {
		return rs.getTime(n);
	}

}

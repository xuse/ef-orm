package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.IResultSet;

/**
 * INT <-> Integer
 */
public class NumIntIntMapping extends AbstractVersionNumberMapping {

	public Object jdbcSet(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		if (value == null) {
			st.setNull(index, java.sql.Types.INTEGER);
		} else {
			st.setInt(index, ((Number) value).intValue());
		}
		return value;
	}

	public int getSqlType() {
		return java.sql.Types.INTEGER;
	}

	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		return value.toString();
	}

	public Object jdbcGet(IResultSet rs, int n) throws SQLException {
		Object obj = rs.getObject(n);
		if (obj == null)
			return null;
		if (obj instanceof Integer)
			return obj;
		return ((Number) obj).intValue();
	}

	@Override
	protected Class<?> getDefaultJavaType() {
		return Integer.class;
	}

	@Override
	Object increament(Object value) {
		if (value == null)
			return 1;
		int i = ((Number) value).intValue();
		return i + 1;
	}
}

package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import jef.database.Field;
import jef.database.dialect.ColumnType;
import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.IResultSet;
import jef.database.meta.ITableMetadata;

public final class AutoLongMapping extends AutoIncrementMapping {
	@Override
	public void init(Field field, String columnName, ColumnType type, ITableMetadata meta) {
		super.init(field, columnName, type, meta);
		accessor = super.fieldAccessor;
	}

	public Object jdbcSet(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		if (value == null) {
			st.setNull(index, getSqlType());
		} else {
			st.setLong(index, ((Number) value).longValue());
		}
		return value;
	}

	public Object jdbcGet(IResultSet rs, int n) throws SQLException {
		Object obj = rs.getObject(n);
		if (obj == null)
			return null;
		if (obj instanceof Long)
			return obj;
		return ((Number) obj).longValue();
	}

	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		return value.toString();
	}

	@Override
	protected Class<?> getDefaultJavaType() {
		return Long.class;
	}
}

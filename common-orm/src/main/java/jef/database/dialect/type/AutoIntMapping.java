package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import jef.accelerator.bean.AbstractFastProperty;
import jef.database.Field;
import jef.database.dialect.ColumnType;
import jef.database.dialect.DatabaseDialect;
import jef.database.meta.ITableMetadata;
import jef.database.wrapper.result.IResultSet;
import jef.tools.reflect.Property;

public final class AutoIntMapping extends AutoIncrementMapping<Integer> {
	@Override
	public void init(Field field, String columnName, ColumnType type, ITableMetadata meta) {
		super.init(field, columnName, type, meta);
		accessor = new J2IProperty(super.fieldAccessor);
	}

	public Object set(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		if (value == null) {
			st.setNull(index, getSqlType());
		} else {
			st.setInt(index, ((Number) value).intValue());
		}
		return value;
	}

	public Object getProperObject(IResultSet rs, int n) throws SQLException {
		Object obj = rs.getObject(n);
		if (obj == null)
			return null;
		if (obj instanceof Integer)
			return obj;
		return ((Number) obj).intValue();
	}

	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		return value.toString();
	}

	private static class J2IProperty extends AbstractFastProperty {
		private Property sProperty;

		J2IProperty(Property inner) {
			this.sProperty = inner;
		}

		public String getName() {
			return sProperty.getName();
		}

		public Object get(Object obj) {
			Integer s = (Integer) sProperty.get(obj);
			if (s == null)
				return null;
			return s.longValue();
		}

		public void set(Object obj, Object value) {
			if (value != null) {
				value = ((Number) value).intValue();
			}
			sProperty.set(obj, value);
		}
	}
}

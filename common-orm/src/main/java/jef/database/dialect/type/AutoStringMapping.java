package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import jef.accelerator.bean.AbstractFastProperty;
import jef.accelerator.bean.BeanAccessor;
import jef.accelerator.bean.FastBeanWrapperImpl;
import jef.database.Field;
import jef.database.dialect.ColumnType;
import jef.database.dialect.DatabaseDialect;
import jef.database.meta.EntityType;
import jef.database.meta.ITableMetadata;
import jef.database.meta.TupleMetadata;
import jef.database.wrapper.IResultSet;
import jef.tools.Assert;
import jef.tools.StringUtils;
import jef.tools.reflect.Property;

public final class AutoStringMapping extends AutoIncrementMapping<String> {
	@Override
	public void init(Field field, String columnName, ColumnType type, ITableMetadata meta) {
		super.init(field, columnName, type, meta);
		BeanAccessor ba = FastBeanWrapperImpl.getAccessorFor(meta.getContainerType());
		if(meta.getType()!=EntityType.TUPLE){
			Assert.isTrue(ba.getPropertyNames().contains(field.name()));
		}
		accessor = new J2SProperty(ba.getProperty(field.name()));
	}


	public Object set(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		if (value == null) {
			st.setNull(index, getSqlType());
		} else {
			Number n = Long.parseLong((String) value);
			st.setLong(index, n.longValue());
		}
		return value;
	}


	public Object getProperObject(IResultSet rs, int n) throws SQLException {
		Object obj = rs.getObject(n);
		if (obj == null)
			return null;
		return String.valueOf(obj);
	}

	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		return value.toString();
	}

	private static class J2SProperty extends AbstractFastProperty {
		private Property sProperty;

		J2SProperty(Property p) {
			this.sProperty = p;
		}

		public String getName() {
			return sProperty.getName();
		}

		public Object get(Object obj) {
			String s = (String) sProperty.get(obj);
			if (StringUtils.isEmpty(s))
				return null;
			return StringUtils.toLong(s, 0L);
		}

		public void set(Object obj, Object value) {
			if (value != null) {
				value = String.valueOf(value);
			}
			sProperty.set(obj, value);
		}
	}
}

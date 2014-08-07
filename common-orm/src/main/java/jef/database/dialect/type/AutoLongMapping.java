package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import jef.accelerator.bean.BeanAccessor;
import jef.accelerator.bean.FastBeanWrapperImpl;
import jef.database.Field;
import jef.database.dialect.ColumnType;
import jef.database.dialect.DatabaseDialect;
import jef.database.meta.EntityType;
import jef.database.meta.ITableMetadata;
import jef.database.wrapper.result.IResultSet;
import jef.tools.Assert;

public final class AutoLongMapping extends AutoIncrementMapping<Long> {
	@Override
	public void init(Field field, String columnName, ColumnType type, ITableMetadata meta) {
		super.init(field, columnName, type, meta);
		// 初始化访问器
		BeanAccessor ba = FastBeanWrapperImpl.getAccessorFor(meta.getContainerType());
		if(meta.getType()!=EntityType.TUPLE){
			Assert.isTrue(ba.getPropertyNames().contains(field.name()));
		}
		accessor = ba.getProperty(field.name());
//		Assert.isTrue(accessor.getType() == Long.TYPE);
	}

	public Object set(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		if (value == null) {
			st.setNull(index, getSqlType());
		} else {
			st.setLong(index, ((Number) value).longValue());
		}
		return value;
	}

	public Object getProperObject(IResultSet rs, int n) throws SQLException {
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
}

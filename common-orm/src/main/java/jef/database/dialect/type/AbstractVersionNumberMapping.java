package jef.database.dialect.type;

import java.sql.SQLException;
import java.util.List;

import jef.accelerator.bean.BeanAccessor;
import jef.accelerator.bean.FastBeanWrapperImpl;
import jef.database.Field;
import jef.database.IQueryableEntity;
import jef.database.dialect.ColumnType;
import jef.database.dialect.DatabaseDialect;
import jef.database.meta.ITableMetadata;
import jef.database.wrapper.clause.InsertSqlClause;
import jef.database.wrapper.clause.UpdateClause;
import jef.tools.reflect.Property;

public abstract class AbstractVersionNumberMapping extends AColumnMapping implements VersionSupportColumn {
	private boolean version;
	private Property accessor;

	@Override
	public void init(Field field, String columnName, ColumnType type, ITableMetadata meta) {
		super.init(field, columnName, type, meta);
		if (type instanceof ColumnType.Int) {
			this.version = ((ColumnType.Int) type).isVersion();
		}
		BeanAccessor ba = FastBeanWrapperImpl.getAccessorFor(meta.getContainerType());
		accessor = ba.getProperty(field.name());
	}

	@Override
	public void processInsert(Object value, InsertSqlClause result, List<String> cStr, List<String> vStr, boolean smart, IQueryableEntity obj) throws SQLException {
		if (!obj.isUsed(field) && version) {
			value = 1;
			accessor.set(obj, value);// 版本号总是从1开始
		}
		super.processInsert(value, result, cStr, vStr, smart, obj);
	}

	@Override
	public void processPreparedInsert(IQueryableEntity obj, List<String> cStr, List<String> vStr, InsertSqlClause result, boolean dynamic) throws SQLException {
		if (!obj.isUsed(field) && version) {
			accessor.set(obj, 1);// 版本号总是从1开始
		}
		super.processPreparedInsert(obj, cStr, vStr, result, dynamic);
	}

	@Override
	public void processAutoUpdate(DatabaseDialect profile, UpdateClause result) {
		String columnName = getColumnName(profile, true);
		result.addEntry(columnName, columnName + "+1");
	}

	@Override
	public boolean isVersion() {
		return version;
	}

	@Override
	public Object getAutoUpdateValue(DatabaseDialect profile, Object bean) {
		Object value = accessor.get(bean);
		value = increament(value);
		accessor.set(bean, value);
		return value;
	}

	abstract Object increament(Object value);

	@Override
	public boolean isUpdateAlways() {
		return version;
	}

}

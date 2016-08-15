package jef.database.wrapper.variable;

import java.sql.SQLException;
import java.util.Map;

import jef.common.log.LogUtil;
import jef.database.Field;
import jef.database.dialect.type.ColumnMapping;
import jef.database.meta.ITableMetadata;
import jef.database.query.Query;
import jef.tools.reflect.BeanWrapper;

public class UpdateVairable extends Variable {
	private Field field;

	public UpdateVairable(Field field) {
		this.field = field;
	}

	@Override
	String name() {
		return null;
	}

	@Override
	Object jdbcSet(BindVariableContext context, int index, BeanWrapper bean, Query<?> query) throws SQLException {
		ITableMetadata meta = query.getMeta();
		ColumnMapping cType = meta.getColumnDef(field);
		Map<Field, Object> updateMap = query.getInstance().getUpdateValueMap();
		if (updateMap.containsKey(field)) {
			Object value = updateMap.get(field);
			return context.setValueInPsmt(index, value, cType);
		} else {
			Object value = bean.getPropertyValue(field.name());
			return context.setValueInPsmt(index, value, cType);
		}
	}
}

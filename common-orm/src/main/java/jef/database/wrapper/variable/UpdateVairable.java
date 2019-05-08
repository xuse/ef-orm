package jef.database.wrapper.variable;

import java.sql.SQLException;
import java.util.Map;

import javax.persistence.PersistenceException;

import jef.database.Field;
import jef.database.dialect.type.ColumnMapping;
import jef.database.meta.ITableMetadata;
import jef.database.query.ConditionQuery;
import jef.database.query.Query;
import jef.tools.reflect.BeanWrapper;

public class UpdateVairable extends Variable {

	private Field field;

	public UpdateVairable(Field field) {
		this.field = field;
	}

	@Override
	String name() {
		return field.name();
	}

	@Override
	Object jdbcSet(BindVariableContext context, int index, ConditionQuery cq){
		Query<?> query = (Query<?>) cq;
		try {
			ITableMetadata meta = query.getMeta();
			ColumnMapping cType = meta.getColumnDef(field);
			Map<Field, Object> updateMap = query.getUpdateValueMap();
			if (updateMap.containsKey(field)) {
				Object value = updateMap.get(field);
				return context.setValueInPsmt(index, value, cType);
			} else {
				Object bean = query.getInstance();
				Object value;
				if (cType != null) {
					value = cType.getFieldAccessor().get(query.getInstance());
				} else {
					// 虽然每次创建一个BeanWrapper很浪费，不过这个分支应该是非常少走到的，大部分场合会走上面分支。
					BeanWrapper bw = BeanWrapper.wrap(bean, BeanWrapper.FAST);
					value = bw.getPropertyValue(field.name());
				}
				return context.setValueInPsmt(index, value, cType);
			}
		} catch (SQLException ex) {
			throw new PersistenceException("The query param type error, field=" + field.name() + " into bean " + query.getType(), ex);
		} catch (ClassCastException e) {
			throw new PersistenceException("The query param type error, field=" + field.name() + " into bean " + query.getType(), e);
		}
	}

	@Override
	public Object getConstantValue() {
		throw new UnsupportedOperationException();
	}
}

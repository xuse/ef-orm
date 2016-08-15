package jef.database.wrapper.variable;

import java.sql.SQLException;

import jef.database.query.Query;
import jef.tools.reflect.BeanWrapper;

public class ConstantVariable extends Variable {
	private Object value;

	public ConstantVariable(Object value) {
		this.value = value;
	}

	String name() {
		return "";
	}

	Object jdbcSet(BindVariableContext context, int index, BeanWrapper bean, Query<?> query) throws SQLException {
		return context.setValueInPsmt(index, value);
	}
}

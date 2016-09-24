package jef.database.wrapper.variable;

import java.sql.SQLException;

import javax.persistence.PersistenceException;

import jef.database.query.ConditionQuery;

public class ConstantVariable extends Variable {
	private String field;
	private Object value;

	public ConstantVariable(String fieldname, Object value) {
		this.field = fieldname;
		this.value = value;
	}

	public ConstantVariable(Object value) {
		this.value = value;
	}

	String name() {
		return field == null ? "" : field;
	}

	Object jdbcSet(BindVariableContext context, int index, ConditionQuery query) {
		try {
			return context.setValueInPsmt(index, value);
		} catch (SQLException ex) {
			throw new PersistenceException("The query param type error, value=" + value, ex);
		}
	}

	@Override
	public Object getConstantValue() {
		return value;
	}
}

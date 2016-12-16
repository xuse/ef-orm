package jef.database.wrapper.variable;

import java.sql.SQLException;

import javax.persistence.PersistenceException;

import jef.database.dialect.type.ColumnMapping;
import jef.database.query.ConditionQuery;

public class ConstantVariable extends Variable {
	private String field;
	private Object value;
	private ColumnMapping column;

	public ConstantVariable(String fieldname, Object value, ColumnMapping column) {
		this.field = fieldname;
		this.value = value;
		this.column=column;
	}

	public ConstantVariable(Object value) {
		this.value = value;
	}

	String name() {
		return field == null ? "" : field;
	}

	Object jdbcSet(BindVariableContext context, int index, ConditionQuery query) {
		try {
			return context.setValueInPsmt(index, value, column);
		} catch (SQLException ex) {
			throw new PersistenceException("The query param type error, value=" + value, ex);
		}
	}

	@Override
	public Object getConstantValue() {
		return value;
	}
}

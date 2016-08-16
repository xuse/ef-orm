package jef.database.wrapper.variable;

import java.sql.SQLException;
import java.util.Collection;

import javax.persistence.PersistenceException;

import jef.database.Condition;
import jef.database.Condition.Operator;
import jef.database.DbUtils;
import jef.database.Field;
import jef.database.IQueryableEntity;
import jef.database.VariableCallback;
import jef.database.dialect.type.ColumnMapping;
import jef.database.meta.FBIField;
import jef.database.meta.ITableMetadata;
import jef.database.meta.TupleField;
import jef.database.query.ConditionQuery;
import jef.database.query.JoinElement;
import jef.database.query.Query;
import jef.tools.Assert;
import jef.tools.reflect.BeanWrapper;

/*
 * 描述字段加条件用于匹配绑定变量参数
 * 每个实例对应一个SQL中的问号，通过List来确定其在绑定SQL中的序号
 */
public class BatchQueryBindVariable extends Variable {
	private Field field;// 这个Field可以描述一个实际的条件路径
	private Operator oper; // 操作符
	private ITableMetadata meta;
	private static final Object NOT_FOUND = new Object();
	private VariableCallback callback;

	public BatchQueryBindVariable(Field field, Operator oper, VariableCallback callback) {
		this(field, oper);
		this.callback = callback;
	}

	public BatchQueryBindVariable(Field field, Operator oper) {
		Class<?> clz = field.getClass();
		if (clz == TupleField.class) {
			this.meta = ((TupleField) field).getMeta();
			this.field = field;
			this.oper = oper;
			return;
		}
		Assert.isTrue(clz.isEnum() || clz == FBIField.class, "The Field in bind variable desc must be a metamodel field");
		this.field = field;
		this.oper = oper;
		this.meta = DbUtils.getTableMeta(field);
	}

	public ColumnMapping getColumnType() {
		return meta == null ? null : meta.getColumnDef(field);
	}

	public VariableCallback getCallback() {
		return callback;
	}

	public void setCallback(VariableCallback callback) {
		this.callback = callback;
	}

	public String toString() {
		return name();
	}

	public Field getField() {
		return field;
	}

	public Operator getOper() {
		return oper;
	}

	public String name() {
		return field.name().concat(" " + oper.getKey());
	}

	public void setInBatch(boolean b) {
	}

	@Override
	Object jdbcSet(BindVariableContext context, int index, ConditionQuery query) {
		try {
			return setWhereVariable(context, query, index);
		} catch (SQLException ex) {
			throw new PersistenceException("Error while setting [+field.name()+], error type=" + ex.getClass().getName(), ex);
		}
	}

	private Object setWhereVariable(BindVariableContext context, ConditionQuery query, int count) throws SQLException {
		Collection<Condition> conds = null;
		IQueryableEntity obj = null;
		if (query != null) {
			if (query instanceof JoinElement) {
				conds = ((JoinElement) query).getConditions();
				if (query instanceof Query<?>) {
					obj = ((Query<?>) query).getInstance();
				}
			}
		}
		Object value = getWhereVariable(conds, obj);
		try {
			value = context.setValueInPsmt(count, value, getColumnType());
		} catch (Exception e) {
			String field = getField().name();
			ColumnMapping colType = getColumnType();
			throw new SQLException("The query param type error, field=" + field + " type=" + (colType == null ? "" : colType.getClass().getSimpleName()) + "\n" + e.getClass().getName() + ":" + e.getMessage());
		}
		return value;
	}

	/**
	 * 从conditionList或者bean当中获取指定的field的绑定参数值
	 * 
	 * @param conds
	 *            条件列表
	 * @param bean
	 *            实例
	 * @param variableDesc
	 *            要获取的具体值的匹配条件标记
	 * @return
	 */
	private Object getWhereVariable(Collection<Condition> conds, IQueryableEntity obj) {
		Object result = NOT_FOUND;
		for (Condition c : conds) {
			if (c.getField() == getField() && c.getOperator() == getOper()) {
				result = c.getValue();
				break;
			}
		}
		if (result == NOT_FOUND && obj != null && getOper() == Operator.EQUALS) {
			BeanWrapper bean = BeanWrapper.wrap(obj, BeanWrapper.FAST);
			result = bean.getPropertyValue(getField().name());
		}
		if (result == NOT_FOUND) {
			// 因为发生了TupleField在批操作过程中发生变化，造成无法定位这一特殊BUG，此处针对这一特定场景进行检测，用来提供更为实用的信息。
			for (Condition c : conds) {
				if (c.getField().name().equals(getField().name()) && c.getOperator() == getOper()) {
					throw new IllegalArgumentException(this + " has a match condition but belongs to another table metadata." + c.getField() + " <> " + getField());
				}
			}
			throw new IllegalArgumentException(this + "'s value not found in a batch update query.");
		}
		return this.callback == null ? result : callback.process(result);
	}

	@Override
	public Object getConstantValue() {
		throw new UnsupportedOperationException();
	}
}

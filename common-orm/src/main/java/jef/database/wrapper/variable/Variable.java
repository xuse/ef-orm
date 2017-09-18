package jef.database.wrapper.variable;

import jef.database.query.ConditionQuery;

public abstract class Variable {
	/**
	 * 变量的名称
	 * @return the name of the variable.
	 */
	abstract String name();

	/**
	 * 设置SQL语句中的值
	 * @param context
	 * @param index
	 * @param query
	 * @return
	 */
	abstract Object jdbcSet(BindVariableContext context, int index, ConditionQuery query);

	/**
	 * if the variable is a constant variable then return the constant.
	 * else throw a UnsupportedOperationException.
	 * @return the constant value
	 * @throws UnsupportedOperationException
	 */
	public abstract Object getConstantValue();
}

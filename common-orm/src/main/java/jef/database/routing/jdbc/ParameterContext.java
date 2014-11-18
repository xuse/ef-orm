package jef.database.routing.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * 单个参数上下文
 * 
 */
public class ParameterContext {
	private ParameterMethod parameterMethod;

	private int index;

	private Object[] args;

	public ParameterContext() {
	}

	public ParameterContext(ParameterMethod parameterMethod, int index, Object... args) {
		this.parameterMethod = parameterMethod;
		this.index = index;
		this.args = args;
	}

	public ParameterMethod getParameterMethod() {
		return parameterMethod;
	}

	public void setParameterMethod(ParameterMethod parameterMethod) {
		this.parameterMethod = parameterMethod;
	}

	public Object[] getArgs() {
		return args;
	}

	public void setArgs(Object[] args) {
		this.args = args;
	}

	public String toString() {
		StringBuilder buffer = new StringBuilder();
		buffer.append(parameterMethod).append("(").append(index).append(',');
		for (int i = 0; i < args.length; ++i) {
			buffer.append(args[i]);
			if (i != args.length - 1) {
				buffer.append(", ");
			}
		}
		buffer.append(")");

		return buffer.toString();
	}

	public void apply(PreparedStatement st) throws SQLException {
		parameterMethod.setParameter(st, index, args);
	}

	public Object getValue() {
		return args[1];
	}
}

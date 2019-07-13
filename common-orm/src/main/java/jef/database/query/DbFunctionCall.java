package jef.database.query;

import jef.database.DbFunction;
import jef.database.dialect.DatabaseDialect;
import jef.tools.ArrayUtils;
import jef.tools.StringUtils;

final class DbFunctionCall {
	private DbFunction func;
	private String[] args;
	private String template;

	/**
	 * 记录当前列作为第n个参数。实际情况从传入的参数中的?占位符来进行计算。
	 */
	private int varIndex = -1;

	/**
	 * 构造一个查询请求中的函数表达式
	 * 
	 * @param template 表达式模板
	 */
	DbFunctionCall(String template) {
		this.template = template;
		this.args=ArrayUtils.EMPTY_STRING_ARRAY;
	}

	/**
	 * 构造一个查询请求中的函数表达式
	 * 
	 * @param func
	 * @param args
	 */
	DbFunctionCall(DbFunction func, String[] args) {
		this.func = func;
		initArgs(args);
	}

	private void initArgs(String[] args) {
		this.args = args;
		if (args.length > 0) {
			varIndex = ArrayUtils.indexOf(args, "?");
		}
	}

	public int getVarIndex() {
		return varIndex;
	}

	public DbFunction getFunc() {
		return func;
	}

	public void setFunc(DbFunction func) {
		this.func = func;
	}

	public String[] getArgs() {
		return args;
	}

	public void setArgs(String[] args) {
		this.args = args;
	}

	public String getName() {
		return template;
	}

	public String apply(String start, DatabaseDialect profile) {
		Object[] args;
		if (getVarIndex() == -1) {
			args = new String[] { start };
		} else {
			args = getArgs();
			args[getVarIndex()] = start;
		}
		if (this.func == null) {
			return StringUtils.replace(this.template, "?", start);
		} else {
			return profile.getFunction(this.func, args);
		}
	}
}

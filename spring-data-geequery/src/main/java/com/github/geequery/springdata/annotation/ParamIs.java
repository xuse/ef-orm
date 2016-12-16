package com.github.geequery.springdata.annotation;

/**
 * 配置@IgnoreIf 使用，用来描述一个表示无条件的参数值
 * @author Jiyi
 *
 */
public enum ParamIs {
	/**
	 * 为空字符串时，该参数不作为查询条件使用
	 */
	Empty,
	/**
	 * 为null时间，该参数不作为查询条件使用
	 */
	Null,
	/**
	 * 为负数时 (<0)
	 * 
	 */
	Negative,
	/**
	 * 为0时
	 */
	Zero,
	/**
	 * <=0时
	 */
	ZeroOrNagative
}

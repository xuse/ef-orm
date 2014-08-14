package jef.database.jsqlparser.visitor;

/**
 * 表达式的种类
 * @author jiyi
 *
 */
public enum ExpressionType {
	/**
	 * 数值运算(算术\数学\文本等运算)
	 */
	arithmetic,
	/**
	 * 逻辑运算:AND
	 */
	and,
	/**
	 * 逻辑运算:OR
	 */
	or,
	/**
	 * 条件运算[]
	 */
	between,
	/**
	 * 条件运算=
	 */
	eq,
	/**
	 * 条件运算!=
	 */
	ne,
	/**
	 * 条件运算>=
	 */
	ge,
	/**
	 * 条件运算>
	 */
	gt,
	/**
	 * 条件运算<=
	 */
	le,
	/**
	 * 条件运算<
	 */
	lt,
	isnull,
	/**
	 * 条件运算 in
	 */
	in,
	/**
	 * 条件运算：Like
	 */
	like,
	/**
	 * 常量
	 */
	value,
	/**
	 * 列
	 */
	column,
	/**
	 * 参数
	 */
	param,
	/**
	 * 括号
	 */
	parenthesis,
	/**
	 * 函数
	 */
	function,
	/**
	 * 多表复合等难以归类的复杂运算
	 */
	complex
}

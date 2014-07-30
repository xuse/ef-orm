package jef.database.query;

import jef.database.DbFunction;

/**
 * 将科学计算相关的函数
 * 普通的数据库函数参见{@link Func}
 * @author jiyi
 * 
 * @see Func
 *
 */
public enum Scientific implements DbFunction{
	/**
	 * 平方根
	 */
	sqrt,
	/**
	 * 正弦
	 */
	sin,
	/**
	 * 双曲正弦函数
	 * (sinh、cosh、tanh、coth、sech、csch共6个称为双曲函数)
	 */
	sinh,
	/**
	 * 余弦
	 */
	cos, 
	/**
	 * 双曲余弦函数
	 * (sinh、cosh、tanh、coth、sech、csch共6个称为双曲函数)
	 */
	cosh,
	/**
	 * 正切
	 */
	tan,
	/**
	 * 双曲正切函数
	 */
	tanh,
	/**
	 * 反余弦
	 */
	acos,
	/**
	 * 反正弦
	 */
	asin,
	/**
	 * 反正切
	 */
	atan,
	/**
	 * 自然对数
	 * 等同于log
	 */
	ln,
	/**
	 * 以10为底的对数
	 */
	log10,
	/**
	 * 返回自然对数的底(e)的XX次方。是ln的逆运算
	 */
	exp,
	/**
	 * 乘方
	 */
	power,
	/**
	 * 三角余切
	 */
	cot,
	/**
	 * 这个函数虽然不属于科学计算，但只对英语有用，对东亚地区几乎没有任何作用。
	 */
	soundex,
	/**
	 * 随机数，返回0..1之间的浮点随机数.
	 * MYSQL可以传随机种子也可以不传
	 * Derby有random不传种子，rand一定要种子
	 */
	rand,
	/**
	 * 角度转弧度 即value/180*3.1415926
	 */
	radians,
	/**
	 * 弧度转角度 即value/3.1415926*180
	 */
	degrees,
}

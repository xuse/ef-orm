package jef.database.query;

import jef.database.DbFunction;

/**
 * 标准函数
 * 
 * <p>
 * 什么是标准函数。
 * 即框架整理出来的，支持跨数据库的通用函数。使用这些通用函数可以实现数据库无关的编程。
 * 这里只列举绝大部分数据库都能支持的函数
 * 科学计算类的函数位于{@link jef.database.query.Scientific}
 * 
 * @see jef.database.query.Scientific
 */
public enum Func implements DbFunction{
	//数值计算
	/**
	 * 取模（求余）运算
	 * mod(integer_type, integer_type) returns the remainder (modulus) of arg1/arg2
	 */
	mod,
	/**
	 * 获取绝对值
	 */
	abs,
	/**
	 * 正数返回1，负数返回-1，零返回0
	 */
	sign,
	/**
	 * 按大数取整
	 */
	ceil,
	/**
	 * 按小数取整
	 */
	floor,
	/**
	 * 四舍五入取整
	 */
	round,
	//条件计算
	/**
	 * 可以在括号中写多个参数，返回参数中第一个非null值
	 */
	coalesce,			//n 第一个非空值，变参
	/**
	 * 双参数，nullif ( L, R )
	 * 相等返回null，否则返回第一个参数值
	 */
	nullif, 
	/**
	 * 双参数，如果两个参数相等返回null，否则返回参数1。（WHEN arg1=arg2 THEN NULL ELSE arg1 END）
	 */
	nvl,
	/**
	 * 类似于多个if分支。(参照Oracle)
	 * select decode('abc','a',1,'b',2,'abc',3,4) E3,decode('abc','a',1,'b',2,4) E4 from dual --应该返回3和4
	 */
	decode,				//条件分支（类似于 MYSQL中的IF）
	
	//字符串处理
	/**
	 * 连接多个字符串，虽然大部分数据库都用 || 表示两个字符串拼接，但是MYSQL是必须用concat函数的
	 */
	concat,
	/**
	 * 三参数，查找并替换字符
	 * replace(sourceStr, searchStr, replacement)
	 */
	replace,
	/**
	 * 针对单个字符的批量查找替换.(参照Oracle)
	 * <pre><code>
	 * translate('abcde','ac','12') = '1b2de'
	 * </code></pre>
	 */
	translate,			//字符替换
	/**
	 * 取字符的子串 (第一个字符串序号为1)<br>
	 * substring(source, startIndex, length)
	 * <pre><code>substring('abcde',2,3)='bcd'
	 * </code></pre>
	 */
	substring,
	/**
	 * 截去字符串两头的空格
	 */
	trim,
	/**
	 * 截去字符串右边的空格
	 */
	rtrim,
	/**
	 * 截去字符串左边的空格
	 */
	ltrim,
	/**
	 * 字符串转小写
	 */
	lower,
	/**
	 * 字符串转大写
	 */
	upper,
	/**
	 * 在字符串或数字左侧添加字符，拼到指定的长度。 lpad(source, length, paddingStr)
	 * <pre><code>lpad('abc', 7, '1') = '1111abc'</code></pre>
	 */
	lpad, 
	/**
	 * 在字符串或数字右侧添加字符，拼到指定的长度。 lpad(source, length, paddingStr)
	 * <pre><code>rpad('abc', 7, '1') = 'abc1111'</code></pre>
	 */
	rpad,
	/**
	 * 文字长度计算。无论中西文字符，都按1个字符计算。来自大部分数据库。顺便一提，这个函数在MYSQL上的对应是char_length()，不是length().
	 */
	length,
	
	/**
	 * 文字长度按实际存储大小计算，因此在UTF8编码的数据库中，汉字实际占3个字符。在GBK编码中，汉字占两个字符。
	 * (参考oracle函数lengthb) 
	 */
	lengthb,
	
	/**
	 * 对于<strong>数字</strong>，保留其小数点后的指定位数。
	 * <pre><code>
	 * trunc(123.456)=123
	 * trunc(123.456,2)=123.45
	 * trunc(123.456,-1)=120
	 * 
	 * 在Oracle中,trunc还能起到将日期时间截为仅日期部分。目前并未在其他数据库上模拟这部分功能。因此要确保你的程序
	 * 能在所有数据库上正常运行，<strong>请使用{@link #date}功能来截取日期。</strong>
	 * 对于其他类型的数据库，类型转换到date都会自然的截断时分秒
	 * 
	 * </code></pre>
	 */
	trunc, 
	/**
	 * 双参数,在参数2中查找参数1，返回匹配的序号（序号从1开始）。(类似Java的indexOf，区别是参数相反)
	 */
	locate, 

	
	//统计计算
	/**
	 * 返回分组中数据的平均值
	 */
	avg,
	/**
	 * 返回分组中的数据的总和
	 */
	sum,
	/**
	 * 返回分组中的最大值
	 */
	max,
	/**
	 * 返回分组中的最小值
	 */
	min,
	/**
	 * 计算行数
	 */
	count,
	/**
	 * 返回当前时间（不含日期）
	 */
	current_time,
	/**
	 * 返回当前日期
	 */
	current_date,
	/**
	 * 返回当前时间戳
	 */
	current_timestamp,
	/**
	 * 和CURRENT_TIMESTAMP是同义词
	 */
	now,
	
	//日期时间处理函数
	/**
	 * 获得年
	 */
	year,	
	/**
	 * 获得月
	 */
	month,		
	/**
	 * 获得日
	 */
	day,	
	/**
	 * 获得时
	 */
	hour,	
	/**
	 * 获得分
	 */
	minute,
	/**
	 * 获得秒
	 */
	second,	
	/**
	 * 截取年月日
	 */
	date,		
	/**
	 * 截取时分秒
	 */
	time, 		
	
	/**
	 * 返回第一个日期减去第二个日期的天数。（不足一天不计算）
	 */
	datediff,
	/**
	 * 时间差多少，单位需要指定，第一个参数为单位取值范围是{@link jef.database.support.SQL_TSI}的枚举，后两个参数是日期1和日期2，返回日期2减去日期1（注意和datediff刚好相反）
	 * (JDBC标准函数)
	 */
	timestampdiff,
	/**
	 * 时间调整
	 * 第一个参数是时间调整单位。参见{@link jef.database.support.SQL_TSI}
	 * (MYSQL原生实现了timestampadd,timestampdiff两个函数，并且允许时间单位简写，但如果你写作 SQL_TSI_DAY MYSQL也能识别。)
	 * 第二个参数是整形表达式，表示调整的数值（正数+，负数-）
	 * 第三个参数是时间timestamp表达式
	 * ( JDBC标准函数)
	 */
	timestampadd,
	/**
	 * 在日期上增加天数 adddate(current_date, 1)即返回明天<br>
	 * (来自于MYSQL,DATE_ADD与之同义)
	 */
	adddate,//DATE_ADD和它是同义词
	/**
	 * 在日期上减去天数
	 * (来自于MYSQL,DATE_SUB与之同义)
	 */
	subdate,
	/**
	 * 在日期上增加月数
	 * （参照Oracle的add_month函数）
	 */
	add_months,
	/**
	 * 类型转换函数，格式为 cast(arg as varchar) cast(arg as timestamp)等等
	 */
	cast,
	/**
	 * 框架的统一函数，转为字符串类型
	 * (来自hibernate HQL)
	 */
	str,
	//JPA规定的通用函数，第一个参数作为数据库的函数名，后面的参数作为数据库函数的参数
	//FUNC
	;
	private int maxArg;
	private int minArg;
}

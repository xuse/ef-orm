package jef.database.annotation;

import java.util.Collection;

/**
 * 描述一个分表字段的处理逻辑。
 * 例如：表 BILL结构如下：(
 *      BILL_ID 		NUMBER(10),
 *      BILL_CUSTOM 	NUMBER(10),
 *      BILL_AMOUNT 	NUMBER(10,2),
 *      BILL_START_DATE DATE,
 *      BILL_END_DATE   DATE
 *      }
 * 此时我们设置<code><pre>&#64;PartitionTable(key = { 
 *    &#64;PartitionKey(field = "billStartDate", length = 1,function=KeyFunction.WEEKDAY)
 *})</pre></code>
 *
 * 表示对billStartDate中的日期进行星期运算，然后将星期几作为表名的后缀来处理。
 * 我们还可以自行编写很多分表字段的逻辑处理函数，例如
 * <pre><code>&#64;PartitionTable(key = { 
 *    &#64;PartitionKey(field = "billId", length = 1,function=ModulusFunction.class,functionClassConstructorParams="50")
 *})</code></pre>
 *表示对billId使用ModulusFunction这个类进行处理（即取模运算），取模的参数值是50.
 *
 */
public interface PartitionFunction<T> {
	/**
	 * 计算分表的表名文本片段
	 * @param value DB操作对象的字段值
	 * @return 分表维度值
	 */
	String eval(T value);

	/**
	 * 由用户实现：
	 * 计算min到max之间的分布值
	 * 输入的四个参数构成一个区间，left,right分别表示左右区间是否为闭区间。
	 * 比如min=2 max=7,left=right=true，那么这个区间即为[2,7]，实际返回2,3,4,5,6,7。
	 * 而当min=2011-1-1,max=2011-3-10,那么有些实现（按月）返回2011-1到2011-3的三个值，
	 *     有些实现（按天）返回这期间的所有天。
	 * 
	 * 实现的时候要注意min和max都有可能为null，从而表示无边界
	 * @param min
	 * @param max
	 * @param left
	 * @param right
	 * @return
	 */
	Collection<T> iterator(T min,T max,boolean left,boolean right);
}

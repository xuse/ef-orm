package jef.database.query;

import java.util.Collection;

import jef.database.annotation.PartitionFunction;




/**
 * 描述一个查询分表的维度，进行各种区间运算
 * 目前已知的维度有三种
 * 1、Range. 即一个数学上的区间，左右边界都可以描述开闭。当左边界等于右边界并且为闭区间时，即收敛为一个点（因此也可以表示一个点）
 * 2、多个区间。表示同时存在多个Range.
 * @author Administrator
 *
 */
public interface Dimension{
	/**
	 * 区间是否有效
	 * @return
	 */
	boolean isValid();
	/**
	 * 和另一个区间进行AND运算（取交集）
	 * @param d
	 * @return
	 */
	Dimension mergeAnd(Dimension d);
	/**
	 * 和另一个区间进行OR运算（取并集）
	 * @param d
	 * @return
	 */
	Dimension mergeOr(Dimension d);
	/**
	 * 取反（取补集）
	 * @return
	 */
	Dimension mergeNot();
	/**
	 * 根据指定的枚举算法，将区间采样为有限个数的点。
	 * @param function
	 * @return
	 */
	Collection<?> toEnumationValue(@SuppressWarnings("rawtypes") Collection<PartitionFunction> function);
}

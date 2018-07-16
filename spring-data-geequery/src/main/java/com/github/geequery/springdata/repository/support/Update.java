package com.github.geequery.springdata.repository.support;

/**
 * 描述一个数据更新的动作 用于实现数据的loadAndUpdate操作。
 *
 * @param <T>
 */
@FunctionalInterface
public interface Update<T> {
	/**
	 * Consumer, to update fields of the value object
	 * @param value
	 */
	void setValue(T value);

}

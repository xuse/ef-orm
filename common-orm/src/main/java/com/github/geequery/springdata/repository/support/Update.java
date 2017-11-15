package com.github.geequery.springdata.repository.support;

/**
 * 描述一个数据更新的动作
 * 用于实现数据的loadAndUpdate操作。
 *
 * @param <T>
 */
public interface Update<T> {
	void setValue(T value);

}

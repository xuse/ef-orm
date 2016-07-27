package com.github.geequery.springdata.repository.support;

import java.io.Serializable;

/**
 * FIXME 这个类正式版本中要换回去不能使用
 * 
 * @author jiyi
 *
 */
public class IdValues implements Serializable {
	private static final long serialVersionUID = -6204574121448352142L;

	public IdValues(Object o) {
		value = new Object[] { o };
	}

	private Object[] value;

	public Object[] getValue() {
		return value;
	}

	public void setValue(Object[] value) {
		this.value = value;
	}

	public IdValues(Object[] value) {
		this.value = value;
	}
}

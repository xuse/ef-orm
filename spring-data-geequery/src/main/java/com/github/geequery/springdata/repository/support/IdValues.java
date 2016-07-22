package com.github.geequery.springdata.repository.support;

import java.io.Serializable;

public class IdValues implements Serializable {
	private static final long serialVersionUID = -6204574121448352142L;
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

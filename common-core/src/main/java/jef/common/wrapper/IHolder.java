package jef.common.wrapper;

import java.io.Serializable;

public interface IHolder<T> extends Serializable{
	T get();
	void set(T obj);
}

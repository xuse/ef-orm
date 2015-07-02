package jef.tools.reflect.convert;

import com.google.common.base.Function;

public abstract class Converter<T> implements Function<Object, T> {
	abstract public boolean accept(Object v);
}

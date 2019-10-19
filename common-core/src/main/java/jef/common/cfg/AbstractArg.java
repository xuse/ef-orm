package jef.common.cfg;

import org.apache.commons.lang3.StringUtils;

public abstract class AbstractArg<T> {
//	String rawValue;

	protected String argName;

	abstract void set(String value);

	public abstract T argName(String argName);

	protected String getName() {
		return StringUtils.isEmpty(argName)? "The value": argName;
	}

}

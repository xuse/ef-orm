package jef.common.cfg;

import org.apache.commons.lang3.StringUtils;

public class BooleanValue extends AbstractArg<BooleanValue> {

	private Boolean b;

	public BooleanValue defaultIs(boolean value) {
		if (b == null) {
			this.b = value;
		}
		return this;
	}

	public BooleanValue assertNotNull() {
		if (b == null) {
			throw new IllegalArgumentException(getName() + " is null!");
		}
		return this;
	}

	@Override
	void set(String value) {
		if (StringUtils.isNotEmpty(value)) {
			b = "true".equalsIgnoreCase(value);
		}
	}

	/**
	 * 设置参数名称，一般用在断言之前
	 * 
	 * @param argName
	 * @return
	 */
	public BooleanValue argName(String argName) {
		this.argName = argName;
		return this;
	}
}
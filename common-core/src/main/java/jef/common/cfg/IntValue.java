package jef.common.cfg;

import org.apache.commons.lang3.StringUtils;

public class IntValue extends AbstractArg<IntValue> {
	private Integer value;

	/**
	 * 设置缺省值
	 * 
	 * @param value
	 * @return
	 */
	public IntValue defaultIs(int value) {
		if (this.value == null) {
			this.value = value;
		}
		return this;
	}

	/**
	 * 断言配置值是正数
	 * 
	 * @return
	 */
	public IntValue assertPositive() {
		if (value == null || value <= 0) {
			throw new IllegalArgumentException(getName() + " is not positive!" + value);
		}
		return this;
	}

	/**
	 * 断言配置值是负数
	 * 
	 * @return
	 */
	public IntValue assertNegative() {
		if (value == null || value >= 0) {
			throw new IllegalArgumentException(getName() + " is not negative!" + value);
		}
		return this;
	}

	/**
	 * 断言配置值大于或等于指定数值
	 * 
	 * @param num
	 * @return
	 * @throws IllegalArgumentException 如果配置值不大于等于指定数值
	 */

	public IntValue assertGoE(int num) {
		if (value == null || value < num) {
			throw new IllegalArgumentException(getName() + " [" + value + "] is great or equal to!" + num);
		}
		return this;
	}

	/**
	 * 断言配置值大于指定数值
	 * 
	 * @param num
	 * @return
	 * @throws IllegalArgumentException 如果配置值不大于指定数值
	 */
	public IntValue assertGt(int num) {
		if (value == null || value <= num) {
			throw new IllegalArgumentException(getName() + " [" + value + "] is not great  to!" + num);
		}
		return this;
	}

	/**
	 * 断言配置值小于等于指定数值
	 * 
	 * @param num
	 * @return
	 * @throws IllegalArgumentException 如果配置值不小于等于指定数值
	 */
	public IntValue assertLoE(int num) {
		if (value == null || value > num) {
			throw new IllegalArgumentException(getName() + " [" + value + "] is not less or equal to!" + num);
		}
		return this;
	}

	/**
	 * 断言配置值小于指定数值
	 * 
	 * @param num
	 * @return
	 * @throws IllegalArgumentException 如果配置值不小于指定数值
	 */
	public IntValue assertLt(int num) {
		if (value == null || value >= num) {
			throw new IllegalArgumentException(getName() + " [" + value + "] is not less to!" + num);
		}
		return this;
	}

	/**
	 * 断言配置值介于之间
	 * 
	 * @param from
	 * @param end
	 * @return
	 * @throws IllegalArgumentException 如果配置值不介于条件之间
	 */
	public IntValue assertBetween(int from, int end) {
		if (value == null || value < from || value > end) {
			throw new IllegalArgumentException(getName() + " [" + value + "] is not between " + from + " and " + end);
		}
		return this;
	}

	/**
	 * 配置值不可大于
	 * 
	 * @param num
	 * @return
	 */
	public IntValue noGreatThan(int num) {
		if (value != null && value > num) {
			value = num;
		}
		return this;
	}

	/**
	 * 配置值不可小于
	 * 
	 * @param value
	 * @return
	 */
	public IntValue noLessThan(int num) {
		if (value != null && value < num) {
			value = num;
		}
		return this;
	}

	/**
	 * 设置参数名称，一般用在断言之前
	 * 
	 * @param argName
	 * @return
	 */
	public IntValue argName(String argName) {
		this.argName = argName;
		return this;
	}

	/**
	 * 获得配置值
	 * 
	 * @return
	 */
	public int get() {
		if (value == null) {
			throw new IllegalArgumentException(getName() + " was not set.");
		}
		return value;
	}

	@Override
	void set(String value) {
		if (StringUtils.isNotEmpty(value)) {
			try {
				this.value = Integer.valueOf(value);
			} catch (NumberFormatException e) {
			}
		}
	}
}
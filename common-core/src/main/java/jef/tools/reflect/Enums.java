package jef.tools.reflect;

import jef.tools.Exceptions;

/**
 * 自己写的Enum工具，Guava的实现也看了，总觉他搞复杂了，0.3us~0.8us 的一次转换操作被他多搞出一个Optional对象来，
 * <p>
 * Guava：
 * <code>Item i=Enums.getIfPresent(Item.class, "HTTP_TIMEOUT").orNull();</code>
 * Consume 962ns
 * <p> 
 * EF:
 * <code>Item i=jef.tools.Enums.valueOf(Item.class, "HTTP_TIMEOUT", null);</code>
 * Consume 321ns
 * <p>
 * 感觉Guava为了让编程符合自然语言习惯已经有点走火入魔了。 
 * @author jiyi
 *
 */
public final class Enums {
	private Enums(){}
	
	/**
	 * get the enum value. or return the defaultValue if absent.
	 * 
	 * @param              <T>
	 * @param clz
	 * @param value
	 * @param defaultValue
	 * @return the enum value. or return the defaultValue if absent.
	 */
	public static <T extends Enum<T>> T valueOf(Class<T> clz, String value, T defaultValue) {
		try {
			return Enum.valueOf(clz, value);
		} catch (IllegalArgumentException e) {
			return defaultValue;
		}
	}

	/**
	 * 根据序号获取枚举、
	 * 本方法主要是为了提供一个合适的异常信息。
	 * @param clz
	 * @param ordinal
	 * @return
	 */
	public static <T extends Enum<T>> T valueOf(Class<T> clz, int ordinal) {
		try {
			return clz.getEnumConstants()[ordinal];
		} catch (ArrayIndexOutOfBoundsException e) {
			throw Exceptions.illegalArgument("The enum[{}] has no element of ordinal {}", clz.getSimpleName(), ordinal,
					e);
		}
	}
	
	/**
	 * 根据序号转换枚举
	 * 
	 * @param clz
	 * @param ordinal
	 * @param defaultValue
	 * @return
	 */
	public static <T extends Enum<T>> T valueOf(Class<T> clz, Integer ordinal, T defaultValue) {
		try {
			return ordinal == null ? defaultValue : clz.getEnumConstants()[ordinal.intValue()];
		} catch (ArrayIndexOutOfBoundsException e) {
			return defaultValue;
		}
	}

	/**
	 * get the enum value. or throw exception if the name not exist.
	 * 
	 * @param                  <T>
	 * @param clz
	 * @param value
	 * @param exceptionMessage 异常消息模板，用 {}来标记传入的value（类似slf4j日志格式）
	 * @return
	 */
	public static <T extends Enum<T>> T valueOf(Class<T> clz, String value, String exceptionMessage, Object... params) {
		try {
			return Enum.valueOf(clz, value);
		} catch (IllegalArgumentException e) {
			throw Exceptions.illegalArgument(exceptionMessage, params);
		}
	}
}

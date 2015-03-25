package jef.tools;

import java.lang.reflect.InvocationTargetException;

/**
 * 常见异常操作类
 * @author jiyi
 *
 */
public class Exceptions {

	/**
	 * 【异常转封装】：转换为IllegalArgumentException并抛出。
	 * 
	 * @param t
	 *            异常
	 */
	public static void thorwAsIllegalArgument(Throwable t) {
		thorwAsIllegalArgument(t, true);
	}

	/**
	 * 【异常转封装】：转换为IllegalStateException并抛出。
	 * 
	 * @param t
	 */
	public static void thorwAsIllegalState(Throwable t) {
		thorwAsIllegalState(t, true);
	}

	/**
	 * 【异常转封装】：转换为IllegalArgumentException并抛出。
	 * 
	 * @param t
	 *            异常
	 * @param allowOtherRuntime
	 *            true则允许抛出其他RuntimeException.
	 */
	public static void thorwAsIllegalArgument(Throwable t, boolean allowOtherRuntime) {
		if (t instanceof Error) {
			throw (Error) t;
		}
		if (t instanceof IllegalArgumentException) {
			throw (IllegalArgumentException) t;
		} else if (t instanceof InvocationTargetException) {
			thorwAsIllegalArgument(t.getCause(), allowOtherRuntime);
		} else if (allowOtherRuntime && (t instanceof RuntimeException)) {
			throw (RuntimeException) t;
		}
		throw new IllegalArgumentException(t);
	}

	/**
	 * 【异常转封装】：转换为IllegalStateException并抛出。
	 * 
	 * @param t
	 *            异常
	 * @param allowOtherRuntime
	 *            true则允许抛出其他RuntimeException.
	 */
	public static void thorwAsIllegalState(Throwable t, boolean allowOtherRuntime) {
		if (t instanceof Error) {
			throw (Error) t;
		}
		if (t instanceof IllegalStateException) {
			throw (IllegalStateException) t;
		} else if (t instanceof InvocationTargetException) {
			thorwAsIllegalState(t.getCause(), allowOtherRuntime);
		} else if (allowOtherRuntime && (t instanceof RuntimeException)) {
			throw (RuntimeException) t;
		}
		throw new IllegalStateException(t);
	}
}

package jef.tools;

import java.lang.reflect.InvocationTargetException;

/**
 * 常见异常操作类
 * 
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
		if (t instanceof Error) {
			throw (Error) t;
		}
		throw asIllegalArgument(t, true);
	}

	/**
	 * 【异常转封装】：转换为IllegalStateException并抛出。
	 * 
	 * @param t
	 */
	public static void thorwAsIllegalState(Throwable t) {
		if (t instanceof Error) {
			throw (Error) t;
		}
		throw asIllegalState(t, true);
	}


	/**
	 * 将指定的异常封装为IllegalArgumentException
	 * 
	 * @param t
	 * @return
	 */
	public static IllegalArgumentException asIllegalArgument(Throwable t) {
		return asIllegalArgument(t, true);
	}

	/**
	 * 转封装为IllegalArgumentException
	 * 
	 * @param t
	 *            异常
	 * @param allowOtherRuntime
	 *            true则允许抛出其他RuntimeException.
	 * @return IllegalArgumentException
	 */
	public static IllegalArgumentException asIllegalArgument(Throwable t, boolean allowOtherRuntime) {
		if (t instanceof IllegalArgumentException) {
			return (IllegalArgumentException) t;
		} else if (t instanceof InvocationTargetException) {
			return asIllegalArgument(t.getCause(), allowOtherRuntime);
		} else if (allowOtherRuntime && (t instanceof RuntimeException)) {
			throw (RuntimeException) t;
		}
		return new IllegalArgumentException(t);
	}
	
	/**
	 * 转封装为IllegalStateException
	 * @param t 异常
	 * @return IllegalStateException
	 */
	public static IllegalStateException asIllegalState(Throwable t) {
		return asIllegalState(t,true);
	}

	/**
	 * 转封装为IllegalStateException
	 * @param t 异常
	 * @param allowOtherRuntime true则允许抛出其他RuntimeException.
	 * @return IllegalStateException
	 */
	public static IllegalStateException asIllegalState(Throwable t, boolean allowOtherRuntime) {
		if (t instanceof IllegalStateException) {
			return (IllegalStateException) t;
		} else if (t instanceof InvocationTargetException) {
			return asIllegalState(t.getCause(), allowOtherRuntime);
		} else if (allowOtherRuntime && (t instanceof RuntimeException)) {
			throw (RuntimeException) t;
		}
		return new IllegalStateException(t);
	}
}

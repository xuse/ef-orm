package jef.tools;

import java.lang.reflect.InvocationTargetException;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 常见异常操作类
 * 
 * @author jiyi
 *
 */
public class Exceptions {
	private static final Logger log = LoggerFactory.getLogger(Exceptions.class);

	/**
	 * 【异常转封装】：转换为IllegalArgumentException并抛出。
	 * 
	 * @param t 异常
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
	 * @param t                 异常
	 * @param allowOtherRuntime true则允许抛出其他RuntimeException.
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
	 * 
	 * @param t 异常
	 * @return IllegalStateException
	 */
	public static IllegalStateException asIllegalState(Throwable t) {
		return asIllegalState(t, true);
	}

	/**
	 * 转封装为IllegalStateException
	 * 
	 * @param t                 异常
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

	/**
	 * 重试执行指定的函数（出现异常后继续重试）
	 * 
	 * @param invokeCount 重试次数
	 * @param input       入参
	 * @param retryInvoke 重试函数
	 * @return 成功与否
	 */
	public static <T> boolean retryIgnoreException(int invokeCount, T input, Predicate<T> retryInvoke) {
		for (int i = 0; i < invokeCount; i++) {
			try {
				if (retryInvoke.test(input)) {
					return true;
				}
			} catch (Exception e) {
				log.error("Retrys {} error,", retryInvoke, e);
			}
		}
		return false;
	}

	/**
	 * 重试执行指定的函数（出现异常后继续重试）
	 * 
	 * @param invokeCount 重试次数
	 * @param retryInvoke 重试函数
	 * @return 成功与否
	 */
	public static <T> boolean retryIgnoreException(int invokeCount, BooleanSupplier retryInvoke) {
		for (int i = 0; i < invokeCount; i++) {
			try {
				if (retryInvoke.getAsBoolean()) {
					return true;
				}
			} catch (Exception e) {
				log.error("Retrys {} error,", retryInvoke, e);
			}
		}
		return false;
	}

	/**
	 * 重试执行指定的函数（会被异常所打断并抛出异常）
	 * 
	 * @param invokeCount 重试次数
	 * @param input       入参
	 * @param retryInvoke 重试函数
	 * @return 成功与否
	 */
	public static <T> boolean retry(int invokeCount, T input, Predicate<T> retryInvoke) {
		for (int i = 0; i < invokeCount; i++) {
			if (retryInvoke.test(input)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 重试执行指定的函数（会被异常所打断并抛出异常）
	 * 
	 * @param invokeCount 重试次数
	 * @param retryInvoke 重试函数
	 * @return 成功与否
	 */
	public static <T> boolean retry(int invokeCount, BooleanSupplier retryInvoke) {
		for (int i = 0; i < invokeCount; i++) {
			if (retryInvoke.getAsBoolean()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 执行指定的函数，当出现异常时返回默认值
	 * 
	 * @param function     函数
	 * @param s            参数
	 * @param defaultValue 默认值
	 * @return 函数结果，或默认值
	 */
	public static <S, T> T apply(Function<S, T> function, S s, T defaultValue) {
		try {
			return function.apply(s);
		} catch (Exception e) {
			log.error("apply {} error,", function, e);
			return defaultValue;
		}
	}

	/**
	 * 执行指定的函数，当出现异常时返回默认值
	 * 
	 * @param function     函数
	 * @param p1           参数1
	 * @param p2           参数2
	 * @param defaultValue 默认值
	 * @return 函数结果，或默认值
	 */
	public static <P1, P2, T> T apply(BiFunction<P1, P2, T> function, P1 p1, P2 p2, T defaultValue) {
		try {
			return function.apply(p1, p2);
		} catch (Exception e) {
			log.error("apply {} error,", function, e);
			return defaultValue;
		}
	}

	/**
	 * 执行指定的函数，当出现异常时返回默认值
	 * 
	 * @param function     函数
	 * @param defaultValue 默认值
	 * @return 函数结果，或默认值
	 */
	public static <T> T apply(Supplier<T> function, T defaultValue) {
		try {
			return function.get();
		} catch (Exception e) {
			log.error("apply {} error,", function, e);
			return defaultValue;
		}
	}

	/**
	 * 执行指定的函数，当出现异常或执行结果为null时返回默认值
	 * 
	 * @param function     函数
	 * @param s            参数
	 * @param defaultValue 默认值
	 * @return 函数结果，或默认值
	 */
	public static <S, T> T applyNotNull(Function<S, T> function, S s, T defaultValue) {
		try {
			T t = function.apply(s);
			return t == null ? defaultValue : t;
		} catch (Exception e) {
			log.error("apply {} error,", function, e);
			return defaultValue;
		}
	}

	/**
	 * 执行指定的函数，当出现异常或执行结果为null时返回默认值
	 * 
	 * @param function     函数
	 * @param p1           参数1
	 * @param p2           参数2
	 * @param defaultValue 默认值
	 * @return 函数结果，或默认值
	 */
	public static <P1, P2, T> T applyNotNull(BiFunction<P1, P2, T> function, P1 p1, P2 p2, T defaultValue) {
		try {
			T t = function.apply(p1, p2);
			return t == null ? defaultValue : t;
		} catch (Exception e) {
			log.error("apply {} error,", function, e);
			return defaultValue;
		}
	}

	/**
	 * 执行指定的函数，当出现异常或执行结果为null时返回默认值
	 * 
	 * @param function     函数
	 * @param defaultValue 默认值
	 * @return 函数结果，或默认值
	 */
	public static <T> T applyNotNull(Supplier<T> function, T defaultValue) {
		try {
			T t =  function.get();
			return t == null ? defaultValue : t;
		} catch (Exception e) {
			log.error("apply {} error,", function, e);
			return defaultValue;
		}
	}
}

package jef.tools;

import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 异常处理工具
 * 
 * @author jiyi
 *
 */
public class Exceptions extends Exceptions7 {

	/**
	 * 重试执行指定的函数（出现异常后继续重试）
	 * 
	 * @param invokeCount
	 *            重试次数
	 * @param input
	 *            入参
	 * @param retryInvoke
	 *            重试函数
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
	 * @param invokeCount
	 *            重试次数
	 * @param retryInvoke
	 *            重试函数
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
	 * @param invokeCount
	 *            重试次数
	 * @param input
	 *            入参
	 * @param retryInvoke
	 *            重试函数
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
	 * @param invokeCount
	 *            重试次数
	 * @param retryInvoke
	 *            重试函数
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
	 * @param function
	 *            函数
	 * @param s
	 *            参数
	 * @param defaultValue
	 *            默认值
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
	 * @param function
	 *            函数
	 * @param p1
	 *            参数1
	 * @param p2
	 *            参数2
	 * @param defaultValue
	 *            默认值
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
	 * @param function
	 *            函数
	 * @param defaultValue
	 *            默认值
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
	 * @param function
	 *            函数
	 * @param s
	 *            参数
	 * @param defaultValue
	 *            默认值
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
	 * @param function
	 *            函数
	 * @param p1
	 *            参数1
	 * @param p2
	 *            参数2
	 * @param defaultValue
	 *            默认值
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
	 * @param function
	 *            函数
	 * @param defaultValue
	 *            默认值
	 * @return 函数结果，或默认值
	 */
	public static <T> T applyNotNull(Supplier<T> function, T defaultValue) {
		try {
			T t = function.get();
			return t == null ? defaultValue : t;
		} catch (Exception e) {
			log.error("apply {} error,", function, e);
			return defaultValue;
		}
	}
}

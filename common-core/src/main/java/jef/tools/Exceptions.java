package jef.tools;

import java.lang.reflect.InvocationTargetException;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;

/**
 * 常见异常包装类
 * 
 * @author jiyi
 *
 */
public class Exceptions {
	private static final Logger log = LoggerFactory.getLogger(Exceptions.class);
	
	private Exceptions() {
	}

	/**
	 * 将指定的异常封装为IllegalArgumentException
	 * 
	 * @param t
	 * @return
	 */
	public static IllegalArgumentException asIllegalArgument(Throwable t) {
		return illegalArgument(t, true);
	}

	/**
	 * 使用slf4j的机制来生成异常信息
	 * 
	 * @param message
	 * @param objects
	 * @return
	 */
	public static IllegalArgumentException illegalArgument(String message, Object... objects) {
		FormattingTuple f = MessageFormatter.arrayFormat(message, objects);
		return f.getThrowable() == null ? new IllegalArgumentException(f.getMessage()) : new IllegalArgumentException(f.getMessage(), f.getThrowable());
	};

	/**
	 * 转封装为IllegalArgumentException
	 * 
	 * @param t                 异常
	 * @param allowOtherRuntime true则允许抛出其他RuntimeException.
	 * @return IllegalArgumentException
	 */
	public static IllegalArgumentException illegalArgument(Throwable t, boolean allowOtherRuntime) {
		if (t instanceof IllegalArgumentException) {
			return (IllegalArgumentException) t;
		} else if (t instanceof InvocationTargetException) {
			return illegalArgument(t.getCause(), allowOtherRuntime);
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
	public static IllegalStateException illegalState(Throwable t) {
		return illegalState(t, true);
	}

	/**
	 * 转封装为IllegalStateException
	 * 
	 * @param t                 异常
	 * @param allowOtherRuntime true则允许抛出其他RuntimeException.
	 * @return IllegalStateException
	 */
	public static IllegalStateException illegalState(Throwable t, boolean allowOtherRuntime) {
		if (t instanceof IllegalStateException) {
			return (IllegalStateException) t;
		} else if (t instanceof InvocationTargetException) {
			return illegalState(t.getCause(), allowOtherRuntime);
		} else if (allowOtherRuntime && (t instanceof RuntimeException)) {
			throw (RuntimeException) t;
		}
		return new IllegalStateException(t);
	}

	/**
	 * 使用slf4j的机制来生成异常信息
	 * 
	 * @param message
	 * @param objects
	 * @return
	 */
	public static IllegalStateException illegalState(String message, Object... objects) {
		FormattingTuple f = MessageFormatter.arrayFormat(message, objects);
		return f.getThrowable() == null ? new IllegalStateException(f.getMessage()) : new IllegalStateException(f.getMessage(), f.getThrowable());
	};

	/**
	 * 使用slf4j的机制来生成异常信息
	 * 
	 * @param message
	 * @param objects
	 * @return NoSuchElementException
	 */
	public static NoSuchElementException noSuchElement(String message, Object... objects) {
		FormattingTuple f = MessageFormatter.arrayFormat(message, objects);
		return new NoSuchElementException(f.getMessage());
	}

	/**
	 * 使用slf4j的机制来生成异常信息
	 * 
	 * @param message
	 * @param objects
	 * @return IndexOutOfBoundsException
	 */
	public static IndexOutOfBoundsException indexOutOfBounds(String message, Object... objects) {
		FormattingTuple f = MessageFormatter.arrayFormat(message, objects);
		return new IndexOutOfBoundsException(f.getMessage());
	}

	/**
	 * 使用slf4j的机制来生成异常信息
	 * 
	 * @param message
	 * @param objects
	 * @return UnsupportedOperationException
	 */
	public static UnsupportedOperationException unsupportedOperation(String message, Object... objects) {
		FormattingTuple f = MessageFormatter.arrayFormat(message, objects);
		return new UnsupportedOperationException(f.getMessage());
	}

	/**
	 * 进行消息格式化
	 * 
	 * @param message
	 * @param objects
	 * @return
	 */
	public static String format(String message, Object... objects) {
		FormattingTuple f = MessageFormatter.arrayFormat(message, objects);
		return f.getMessage();
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
				if (retryInvoke.apply(input)) {
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
	public static <T> boolean retryIgnoreException(int invokeCount, Supplier<Boolean> retryInvoke) {
		for (int i = 0; i < invokeCount; i++) {
			try {
				Boolean b=retryInvoke.get();
				if (b!=null && b) {
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
			if (retryInvoke.apply(input)) {
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
	public static <T> boolean retry(int invokeCount,Supplier<Boolean> retryInvoke) {
		for (int i = 0; i < invokeCount; i++) {
			Boolean b=retryInvoke.get();
			if (b!=null && b) {
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
	 * @param defaultValue 默认值
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

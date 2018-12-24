package jef.tools;

import java.lang.reflect.InvocationTargetException;
import java.util.NoSuchElementException;

import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

/**
 * 常见异常包装类
 * 
 * @author jiyi
 *
 */
public class Exceptions {
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
	 * @param t
	 *            异常
	 * @param allowOtherRuntime
	 *            true则允许抛出其他RuntimeException.
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
	 * @param t
	 *            异常
	 * @return IllegalStateException
	 */
	public static IllegalStateException illegalState(Throwable t) {
		return illegalState(t, true);
	}

	/**
	 * 转封装为IllegalStateException
	 * 
	 * @param t
	 *            异常
	 * @param allowOtherRuntime
	 *            true则允许抛出其他RuntimeException.
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
	 *  使用slf4j的机制来生成异常信息
	 * @param message
	 * @param objects
	 * @return NoSuchElementException
	 */
	public static NoSuchElementException noSuchElement(String message, Object... objects) {
		FormattingTuple f = MessageFormatter.arrayFormat(message, objects);
		return new NoSuchElementException(f.getMessage());
	}
	/**
	 *  使用slf4j的机制来生成异常信息
	 * @param message
	 * @param objects
	 * @return IndexOutOfBoundsException
	 */
	public static IndexOutOfBoundsException indexOutOfBounds(String message, Object... objects) {
		FormattingTuple f = MessageFormatter.arrayFormat(message, objects);
		return new IndexOutOfBoundsException(f.getMessage());
	}

	/**
	 *  使用slf4j的机制来生成异常信息
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
	 * @param message
	 * @param objects
	 * @return
	 */
	public static String format(String message, Object... objects) {
		FormattingTuple f = MessageFormatter.arrayFormat(message, objects);
		return f.getMessage();
	}
}

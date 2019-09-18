package jef.tools;

import java.lang.reflect.InvocationTargetException;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

/**
 * Java7兼容的异常处理类
 * 
 * @author jiyi
 *
 */
public class Exceptions7 {
	protected static final Logger log = LoggerFactory.getLogger(Exceptions7.class);
	
	public static final class WrapException extends RuntimeException{
		private static final long serialVersionUID = -9058355728108119655L;
		WrapException(String message){
			super(message);
		}
		WrapException(String message,Throwable t){
			super(message,(t instanceof InvocationTargetException)? t.getCause():t);
		}
		WrapException(Throwable t){
			super((t instanceof InvocationTargetException)? t.getCause():t);
		}
		@Override
		public synchronized Throwable fillInStackTrace() {
			return this;
		}
		
	}
	
	protected Exceptions7() {
	}

	/**
	 * 将指定的异常封装为IllegalArgumentException
	 * @deprecated use {@link #toIllegalArgument(Throwable)} please.
	 * @param t
	 * @return IllegalArgumentException
	 */
	public static IllegalArgumentException asIllegalArgument(Throwable t) {
		return toIllegalArgument(t);
	}
	
	/**
	 * 将指定的异常封装为IllegalArgumentException
	 * 
	 * @param t
	 * @return IllegalArgumentException
	 */
	public static IllegalArgumentException toIllegalArgument(Throwable t) {
		return toIllegalArgument(t, true);
	}
	
	/**
	 * 将异常转换为RuntimeException
	 * @param t
	 * @return RuntimeException
	 */
	public static RuntimeException toRuntime(Throwable t) {
		if (t instanceof RuntimeException) {
			return (RuntimeException) t;
		} else if (t instanceof InvocationTargetException) {
			return toRuntime(t.getCause());
		}else {
			return new WrapException(t);
		}
	}
	
	/**
	 * 转封装为IllegalArgumentException
	 * 
	 * @param t                 异常
	 * @param allowOtherRuntime true则允许抛出其他RuntimeException.
	 * @return IllegalArgumentException
	 */
	public static IllegalArgumentException toIllegalArgument(Throwable t, boolean allowOtherRuntime) {
		if (t instanceof IllegalArgumentException) {
			return (IllegalArgumentException) t;
		} else if (t instanceof InvocationTargetException) {
			return toIllegalArgument(t.getCause(), allowOtherRuntime);
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
	public static IllegalStateException toIllegalState(Throwable t) {
		return toIllegalState(t, true);
	}

	/**
	 * 转封装为IllegalStateException
	 * 
	 * @param t                 异常
	 * @param allowOtherRuntime true则允许抛出其他RuntimeException.
	 * @return IllegalStateException
	 */
	public static IllegalStateException toIllegalState(Throwable t, boolean allowOtherRuntime) {
		if (t instanceof IllegalStateException) {
			return (IllegalStateException) t;
		} else if (t instanceof InvocationTargetException) {
			return toIllegalState(t.getCause(), allowOtherRuntime);
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
	 * @return IllegalArgumentException
	 */
	public static IllegalArgumentException illegalArgument(String message, Object... objects) {
		FormattingTuple f = MessageFormatter.arrayFormat(message, objects);
		return f.getThrowable() == null ? new IllegalArgumentException(f.getMessage()) : new IllegalArgumentException(f.getMessage(), f.getThrowable());
	};

	
	/**
	 * 使用slf4j的机制来生成异常信息
	 * 
	 * @param message
	 * @param objects
	 * @return IllegalStateException
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
		return f.getThrowable()==null? new UnsupportedOperationException(f.getMessage()):new UnsupportedOperationException(f.getMessage(),f.getThrowable());
	}
	
	/**
	 * 使用slf4j的机制来生成异常信息
	 * @param message
	 * @param objects
	 * @return RuntimeException
	 */
	public static RuntimeException runtime(String message, Object... objects) {
		FormattingTuple f = MessageFormatter.arrayFormat(message, objects);
		return f.getThrowable()==null? new WrapException(f.getMessage()):new WrapException(f.getMessage(),f.getThrowable());
		
	}
	

	/**
	 * 进行消息格式化
	 * 
	 * @param message
	 * @param objects
	 * @return formated string
	 */
	public static String format(String message, Object... objects) {
		FormattingTuple f = MessageFormatter.arrayFormat(message, objects);
		return f.getMessage();
	}

	/**
	 * 记录异常日志
	 * @param t
	 */
	public static void  log(Throwable e) {
		if (e instanceof InvocationTargetException) {
			e = e.getCause();
		}
		log.error("", e);
	}
}

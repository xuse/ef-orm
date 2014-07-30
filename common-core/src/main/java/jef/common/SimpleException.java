package jef.common;

/**
 * 没有堆栈的异常，用于不影响性能的中断处理流程
 *
 */
public class SimpleException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	/**
	 * 阻止填充异常堆栈。减少日志处理信息
	 */
	@Override
	public synchronized Throwable fillInStackTrace() {
		return this;
	}
	
	public SimpleException(String message){
		super(message);
	}
	
	public SimpleException(Exception e){
		super(e);
	}
	
}

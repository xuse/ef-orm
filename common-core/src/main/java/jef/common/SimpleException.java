package jef.common;

/**
 * 没有堆栈的异常，用于不影响性能的中断处理流程
 *
 */
public class SimpleException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	private Object[] args = null;
	
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
	
	public SimpleException(String message,Object... args){
		super(format(message,args));
		this.args=args;
	}

	/**
	 * 格式化文本
	 * @param message
	 * @param args
	 * @return
	 */
	private static String format(String message, Object[] args) {
		if(args.length==0)return message;
		
		int start=0;
		int index=message.indexOf("{}",start);
		int order=0;
		StringBuilder sb=new StringBuilder(message.length()+8);
		while(index>-1) {
			sb.append(message,start,index);//前部分
			Object arg;
			if(order<args.length) {
				arg=args[order];
			}else {
				arg=args[args.length-1];
			}
			sb.append(String.valueOf(arg));
			order++;
			start=index+2;
			index=message.indexOf("{}",start);	
		}
		sb.append(message,start,message.length());
		return sb.toString();
	}
}

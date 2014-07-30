package jef.database.pooltest;

import java.lang.reflect.Method;

/**
 * 判断是否在服务器上的工具类
 * 	    在服务器上用log输出
 * 	    否则直接输出
 * @author zhaolong
 *
 */
public class ConnPrintOutUtil {
	
	//判断是否在服务器上
	private static boolean isOnServer=false;
	//case的执行结果
	public static final int SUCCESS=0;
	public static final int FAILURE=1;
	
	public static final String WARN="warn";
	public static final String INFO="info";
	public static final String ERROR="error";
	public static final String DEBUG="debug";
	public static final String TRACE="trace";
	
	
	/**
	 * 打印信息
	 * @param log
	 * @param level
	 * @param msg
	 */
	public static void print(org.slf4j.Logger log,String level,String msg){
		try{
			Method method=org.slf4j.Logger.class.getMethod(level, String.class);
			if(isOnServer){
				method.invoke(log, msg);
			}else{
				System.out.println(msg);
			}
			
		}catch (Exception e) {
			if(isOnServer){
				log.error(e.getMessage());
			}else{
				e.printStackTrace();
			}
		}
	}
	
	public static void printSuccess(org.slf4j.Logger log){
		print(log,INFO,"case exit successfully..................");
		System.exit(SUCCESS);
	}
	
	public static void printFailure(org.slf4j.Logger log){
		print(log,INFO,"case exit failure........................");
		System.exit(FAILURE);
	}

	public static boolean isOnServer() {
		return isOnServer;
	}


	

}

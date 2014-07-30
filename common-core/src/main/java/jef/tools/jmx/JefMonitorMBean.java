package jef.tools.jmx;

import java.util.Map;

public interface JefMonitorMBean {
	/**
	 * 给出一个class名，然后返回其class文件所在路径
	 * @param className
	 * @return
	 */
	public String codeSource(String className);
	
	/**
	 * 返回系统参数
	 * @return
	 */
	public Map<String,String> getSystemProperties();
	
	/**
	 * 获得当前服务器名称
	 * @return
	 */
	public String getServerName();
}

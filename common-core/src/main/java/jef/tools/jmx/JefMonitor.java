package jef.tools.jmx;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.NotificationBroadcasterSupport;

import jef.tools.StringUtils;
import jef.tools.reflect.ClassLoaderUtil;

public final class JefMonitor extends NotificationBroadcasterSupport implements JefMonitorMBean {
	static JefMonitor monitor;
	private AtomicLong sequence=new AtomicLong();
	private MemoryMXBean m= ManagementFactory.getMemoryMXBean();

	private static String toStr(MemoryUsage m){
		StringBuilder sb=new StringBuilder(64);
		sb.append("-Xms").append(StringUtils.formatSize(m.getInit())).append(' ');
		sb.append("-Xmx").append(StringUtils.formatSize(m.getMax())).append(' ');
		sb.append("Committed:").append(StringUtils.formatSize(m.getCommitted())).append(' ');
		sb.append("Used:").append(StringUtils.formatSize(m.getUsed()));
		return sb.toString();
	}
	
	public String codeSource(String className) {
		Class<?> c;
		try {
			c = Class.forName(className);
		} catch (ClassNotFoundException t) {
			t.printStackTrace();
			return "Not Found.";
		}
		try {
			URL source = ClassLoaderUtil.getCodeSource(c);
			if (source == null) {
				return "No Source. May be in JRE.";
			}
			return source.toString();
		} catch (Throwable t) {
			t.printStackTrace();
			String msg = t.getClass().getSimpleName() + ":" + t.getMessage();
			return msg;
		}
	}

	public Map<String,String> getSystemProperties(){
		Map<String,String> map=new HashMap<String,String>();
		for(Entry<?,?> e:System.getProperties().entrySet()){
			map.put(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
		}
		return map;
	}
	
	private String serverName=null;
	
	public String getServerName() {
		if (serverName==null){
			Properties p=System.getProperties();
			if (p.containsKey("com.bes.instanceName")){
				serverName=p.getProperty("com.bes.instanceName"); 
			}else if (p.containsKey("weblogic.Name")){
				serverName=p.getProperty("weblogic.Name"); 
			}else if (p.containsKey("jef.hostName")){
				serverName=p.getProperty("jef.hostName"); 
			}
		}
		return serverName;
	}
	
	public static JefMonitor getInstance() {
		if(monitor==null)monitor=new JefMonitor();
		return monitor;
	}
}

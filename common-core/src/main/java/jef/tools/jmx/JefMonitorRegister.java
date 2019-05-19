package jef.tools.jmx;

import java.lang.management.ManagementFactory;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class JefMonitorRegister {
	private static final Logger log=LoggerFactory.getLogger(JefMonitorRegister.class);
	
//	private static HtmlAdaptorServer server; 
	public static boolean isJmxEnable(){
		return !"false".equals(System.getProperty("enable.jmx"));
	}
	
	public static void registeJefDefault() {
		if(isJmxEnable() && System.getProperty("jef.jmx.registed")==null){
			JefMonitor mm = JefMonitor.getInstance();
			registe(null,mm);	
		}
	}
	
	public static synchronized void registe(String path,Object mxBean){
		System.setProperty("jef.jmx.registed","true");
		try{
			List<MBeanServer> servers = MBeanServerFactory.findMBeanServer(null);
			MBeanServer s;
			if (servers.isEmpty()) {
				s = ManagementFactory.getPlatformMBeanServer();
			} else {
				s = servers.get(0);
			}
			String clsName=mxBean.getClass().getSimpleName();
			String name=path==null?clsName+":name=default":path+",objectname="+clsName;
			ObjectName objName = new ObjectName(name);
			s.registerMBean(mxBean, objName);
//			try{
//				if(Class.forName("com.sun.jdmk.comm.HtmlAdaptorServer")!=null){
//					processHtmlAdaptor(s);
//				}
//			}catch(ClassNotFoundException e){
//			}
		}catch(Throwable t){
			System.out.println("MBean Regist error!");
			t.printStackTrace();
		}
	}
	public static void unregiste(String path,Object mxBean){
		try{
			List<MBeanServer> servers = MBeanServerFactory.findMBeanServer(null);
			MBeanServer s;
			if (servers.isEmpty()) {
				s = ManagementFactory.getPlatformMBeanServer();
			} else {
				s = servers.get(0);
			}
			if(mxBean==null){
				return;
			}
			String clsName=mxBean.getClass().getSimpleName();
			String name=path==null?clsName+":name=default":path+",objectname="+clsName;
			ObjectName objName = new ObjectName(name);
			s.unregisterMBean(objName);
		}catch(Throwable t){
			log.error("MBean Regist error!", t);
		}
	}
}

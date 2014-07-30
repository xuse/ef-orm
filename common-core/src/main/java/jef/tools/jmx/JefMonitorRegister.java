package jef.tools.jmx;

import java.lang.management.ManagementFactory;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import jef.common.log.LogUtil;

public class JefMonitorRegister {
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
	
	/*
	private static void processHtmlAdaptor(MBeanServer s) throws Exception {
		//如果环境变量禁止了，那么就禁用
		//如果是在复杂的启动环境里，（认为是在某个Java容器中，那么也禁用）
		if(JefMonitorRegister.class.getClassLoader()!=ClassLoader.getSystemClassLoader() && !"1".equals(System.getenv("USE_JMX_HTTP"))){
			return;	
		}
		if(server==null){
			int port=ProcessUtil.getFreePort();
			server = new HtmlAdaptorServer();
			ObjectName adapterName = new ObjectName("HttpAgent:name=jef-jmx,port="+port);
			server.setPort(port);
			String password=RandomStringUtils.random(5);
			AuthInfo authInfo=new AuthInfo("admin",password);
			server.addUserAuthenticationInfo(authInfo);
			s.registerMBean(server, adapterName);
			ThreadUtils.doTask(server);
			TripleDES t = new TripleDES();
			String passwordText = t.cipher2("781296-5e32-89122".getBytes("US-ASCII"), password);
			//p1就是端口， p2就是密码用3DES加密后的密文，这里故意用p1和p2的名称，目的是让人看不太懂。防止有人破解后用JMX搞破坏
			LogUtil.info("The JMX Agent was started at p1="+port+" p2="+passwordText);
		}
	}
*/
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
			LogUtil.exception("MBean Regist error!", t);
		}
	}
}

package jef.jre5support;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import jef.common.log.LogUtil;
import jef.tools.Exceptions;
import jef.tools.IOUtils;
import jef.tools.StringUtils;
import jef.tools.management.OperatingSystemMXBean;


/**
 * 跟进程相关的以及环境相关的若干工具方法
 * 
 * 这个工具类中还有若干和网络地址相关方法，比如获取本机的网络信息，空闲端口等
 * @author Administrator
 * 部分功能使用了JDK 1.6中的方法，因此本类的使用范围要求JDK 1.6
 * @since 1.6 
 */
public class ProcessUtil {
	private static final int processId;
	private static final String hostname;
	private static final long startTime;
	private static String mac;
	
	static {
		RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
		String name = runtime.getName(); // format: "pid@hostname"
		processId = Integer.parseInt(name.substring(0, name.indexOf('@')));
		hostname = name.substring(name.indexOf('@') + 1);
		startTime=runtime.getStartTime();
	}

	/**
	 * 获取进程号
	 * @return
	 */
	public static int getPid() {		
		return processId;
	}
	/**
	 * 网络适配器信息
	 * @author Administrator
	 *
	 */
	public static class NetworkInfo{
		NetworkInterface net;
		InetAddress addr;
		NetworkInfo(NetworkInterface t) {
			this.net=t;
			Enumeration<InetAddress> addrs=net.getInetAddresses();
			addr=addrs.nextElement();
			if(addrs.hasMoreElements()){
				LogUtil.warn("The network " + net.getDisplayName()+" has more than one address");
			}
		}
		/**
		 * 获得广播地址
		 * @return
		 */
		public String getBroadcastAddress(){
			InterfaceAddress addr=net.getInterfaceAddresses().get(0);
			return addr.getBroadcast().getHostAddress();
		}
		/**
		 * 获得子网掩码
		 * @return
		 */
		public String getMaskAddress(){
			InterfaceAddress addr=net.getInterfaceAddresses().get(0);
			int add=addr.getNetworkPrefixLength();
			String[] s=new String[]{"0","128","192","224","240","248","252","254","255"};
			if(add>24){
				return "255.255.255."+s[add-24];
			}else if(add>16){
				return "255.255."+s[add-16]+".0";
			}else if(add>8){
				return "255."+s[add-8]+".0.0";
			}else{
				return s[add]+".0.0.0";
			}
		}
		/**
		 * 获得IP地址
		 * @return
		 */
		public String getIpAddress(){
			return addr.getHostAddress();
		}
		/**
		 * 获得主机名
		 * @return
		 */
		public String getHostName(){
			return addr.getHostName();
		}
		/**
		 * 获得缩写主机名
		 * @return
		 */
		public String getCanonicalHostName(){
			return addr.getCanonicalHostName();
		}
		/**
		 * 获得Mac地址
		 * 注意JDK有BUG，无线网卡的MAC地址不能获得，会返回有线网卡的MAC地址
		 * @return
		 */
		public String getMac(){
			try {
				return StringUtils.join(net.getHardwareAddress(),'-');
			} catch (SocketException e) {
				Exceptions.log(e);
				return null;
			}
		}
		/**
		 * 获得最大传输单元
		 * @return
		 */
		public int getMTU(){
			try {
				return net.getMTU();
			} catch (SocketException e) {
				Exceptions.log(e);
				return 0;
			}
		}
		/**
		 * 获得名称
		 * @return
		 */
		public String getName(){
			return net.getName();
		}
		/**
		 * 获得适配器名称
		 * @return
		 */
		public String getDisplayName(){
			return net.getDisplayName();
		}
		@Override
		public String toString() {
			StringBuilder sb=new StringBuilder();
			sb.append(getName()).append(' ');
			sb.append(getIpAddress()).append('|').append(getHostName());
			return sb.toString();
		}
	}
	
	/**
	 * 获得现有活动的网络连接信息。未连接的不会在这里出现
	 * lo 127.0.0.1也不会在这里出现
	 * @return
	 */
	public static NetworkInfo[] getActiveNetwork(){
		Enumeration<NetworkInterface> nets;
		try{
			nets=NetworkInterface.getNetworkInterfaces();
		}catch(IOException e){
			throw Exceptions.asIllegalArgument(e);
		}
		List<NetworkInfo> n=new ArrayList<NetworkInfo>();
		while(nets.hasMoreElements()){
			NetworkInterface t=nets.nextElement();
			try{
				byte[] mac=t.getHardwareAddress();
				if(mac!=null && mac.length==6 && t.getInetAddresses().hasMoreElements()){
					n.add(new NetworkInfo(t));
				}	
			}catch(IOException e){
				throw Exceptions.illegalState(e);
			}
		}	
		return n.toArray(new NetworkInfo[n.size()]);
	}
	
	/**
	 * 获取进程号
	 * @return
	 * @deprecated
	 * 进程号应该是int类型，请使用getPid();
	 */
	public static long getProcessId() {		
		return processId;
	}

	/**
	 * 获取JVM启动时间。（毫秒数）
	 * @return
	 */
	public static long getStarttime() {
		return startTime;
	}
	/**
	 * 获取一个空闲端口
	 * @return
	 */
	public static int getFreePort(){
		ServerSocket serverSocket=null;
		try{
			serverSocket=  new ServerSocket(0); //读取空闲的可用端口
			int port = serverSocket.getLocalPort();
			return port;
		}catch(Exception e){
			return 0;
		}finally{
			if(serverSocket!=null){
				try{
					serverSocket.close();
				}catch(Exception e){
				}
			}
		}
	}
	/**
	 * 获取当前主机名
	 * @return
	 */
	public static String getHostname() {
		return hostname;
	}
	/**
	 * 判断当前的操作系统是不是64位的操作系统
	 * @return
	 */
	public static boolean is64BitOs(){
		return System.getProperty("java.vm.name").indexOf("64")>=0;
	}
	
	/**
	 * 获取当前操作系统名称. return 操作系统名称 例如:windows xp,linux 等.
	 */
	public static String getOSName() {
		return System.getProperty("os.name").toLowerCase();
	}

	/**
	 * <p>
	 * 方法 getLocalIp
	 * </p>
	 * 获取本机ip地址
	 * 
	 * @return
	 */
	public static String getLocalIp() {
		String localIp = null;
		try {
			localIp = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			Exceptions.log(e);
		}
		return localIp;
	}
	
	public static String getRemoteIp(String hostName) throws UnknownHostException{
		InetAddress addr=InetAddress.getByName(hostName);
		return addr.getHostAddress();
	}
	
	
	/**
	 * <p>
	 * 方法 getLocalMac
	 * </p>
	 * 获取本机MAC地址
	 * 
	 * @return
	 */
	public static String getLocalMac() {
		if(mac==null){
			if (OperatingSystemMXBean.isWindows()) {
				mac = getWindowsMACAddress();
			} else {
				mac = getUnixMACAddress();
			}	
		}
		return mac;
	}

	/**
	 * 获取unix网卡的mac地址. 非windows的系统默认调用本方法获取.如果有特殊系统请继续扩充新的取mac地址方法.
	 * 
	 * @return mac地址
	 */
	private static String getUnixMACAddress() {
		BufferedReader bufferedReader = null;
		Process process = null;
		try {
			process = Runtime.getRuntime().exec("ifconfig eth0");// linux下的命令，一般取eth0作为本地主网卡显示信息中包含有mac地址信息
			bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line = null;
			int index = -1;
			while ((line = bufferedReader.readLine()) != null) {
				index = line.toLowerCase().indexOf("hwaddr");// 寻找标示字符串[hwaddr]
				if (index >= 0) {// 找到了
					String mac = line.substring(index + "hwaddr".length() + 1).trim();// 取出mac地址并去除2边空格
					return mac;
				}
			}
		} catch (IOException e) {
			Exceptions.log(e);
		} finally {
			if(process!=null){
				process.destroy();
			}
			IOUtils.closeQuietly(bufferedReader);
		}
		return "";
	}

	/**
	 * 获取widnows网卡的mac地址.
	 * 
	 * @return mac地址
	 */
	private static String getWindowsMACAddress() {
		BufferedReader reader = null;
		Process process = null;
		try {
			process = Runtime.getRuntime().exec("ipconfig /all");// windows下的命令，显示信息中包含有mac地址信息
			reader = new BufferedReader(new InputStreamReader(process.getInputStream(),"GB18030"));
			String line;
			while ((line = reader.readLine()) != null) {
				int index = line.toLowerCase().indexOf("physical address");// 寻找标示字符串[physical address]
				if(index==-1){
					index=line.indexOf("物理地址");
				}
				if (index >= 0) {// 找到了
					index = line.indexOf(":");// 寻找":"的位置
					if (index >= 0) {
						String mac = line.substring(index + 1).trim();// 取出mac地址并去除2边空格
						return mac;
					}
				}
			}
		} catch (IOException ex) {
			Exceptions.log(ex);
		} finally {
			if(process!=null)process.destroy();
			IOUtils.closeQuietly(reader);
		}
		return "";
	}
	
	/**
	 * 获取当前系统的实例信息
	 * @return
	 */
	public static String getServerName(){
		Properties p=System.getProperties();
		String serverName=null;
		if (p.containsKey("com.bes.instanceName")){
			serverName=p.getProperty("com.bes.instanceName"); 
		}else if (p.containsKey("weblogic.Name")){
			serverName=p.getProperty("weblogic.Name"); 
		}else if (p.containsKey("jef.hostName")){
			serverName=p.getProperty("jef.hostName"); 
		}
		return serverName;
	}

}

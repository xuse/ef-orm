package jef.tools.management;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import jef.common.log.LogUtil;
import jef.jre5support.ProcessUtil;
import jef.tools.IOUtils;
import jef.tools.StringUtils;
import jef.tools.collection.CollectionUtil;
import jef.tools.reflect.ClassEx;

/**
 * 这个Bean封装了JDK的OperatingSystemMXBean,可以根据SUN JDK(及其兼容)，IBM JDK等 提供与JDK平台无关的扩展接口
 * 
 * @author Administrator
 * 
 */
public class OperatingSystemMXBean implements java.lang.management.OperatingSystemMXBean {
	private static OperatingSystemMXBean instance;
	final static boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
	final static boolean isHp = System.getProperty("os.name").toLowerCase().indexOf("hp") > -1;
	private static String linuxVersion = System.getProperty("os.version");
	private MemoryMXBean memMx;
	private long maxPerm;
	private long minPerm;
	private long maxHeap;
	private long minHeap;
	
	private OperatingSystemMXBean() {
		bean = ManagementFactory.getOperatingSystemMXBean();
		ClassEx cw = new ClassEx(bean.getClass());
		List<String> result = CollectionUtil.getPropertyValues(cw.getAllInterfaces(), "getName", String.class);
		if (result.contains("com.sun.management.UnixOperatingSystemMXBean")) {
			initSunJDKBean(cw);
		} else if (result.contains("com.sun.management.OperatingSystemMXBean")) {
			initSunJDKBean(cw);
		} else if (result.contains("com.ibm.lang.management.OperatingSystemMXBean")) {
			initIBMJDKBean(cw);
		}
		initMethodInJDK6(cw);
		for (Method e : propertyMap.values()) {
			e.setAccessible(true);
		}
		memMx=ManagementFactory.getMemoryMXBean();
		MemoryUsage mu=memMx.getHeapMemoryUsage();
		minHeap=mu.getInit();
		maxHeap=mu.getMax();
		
		mu=memMx.getNonHeapMemoryUsage();
		minPerm=mu.getInit();
		maxPerm=mu.getMax();
	}

	
	public static OperatingSystemMXBean getInstance() {
		if (instance == null) {
			instance = new OperatingSystemMXBean();
		}
		return instance;
	}

	private java.lang.management.OperatingSystemMXBean bean;
	private Map<Attribute, Method> propertyMap = new HashMap<Attribute, Method>();

	enum Attribute {
		CommittedVirtualMemorySize, TotalSwapSpaceSize, FreeSwapSpaceSize, ProcessCpuTime, FreePhysicalMemorySize, TotalPhysicalMemorySize, SystemLoadAverage
	}

	/**
	 * 获取当前进程的pid
	 * 
	 * @return
	 */
	public int getPid() {
		return ProcessUtil.getPid();
	}

	/**
	 * 获取虚拟机的启动时间
	 * 
	 * @return
	 */
	public Date getStartTime() {
		return new Date(ProcessUtil.getStarttime());
	}

	
	/*
	 * 
	 */
	private void initSunJDKBean(ClassEx cw) {
		try {
			propertyMap.put(Attribute.CommittedVirtualMemorySize, cw.getMethod("getCommittedVirtualMemorySize").getJavaMethod());
			propertyMap.put(Attribute.FreePhysicalMemorySize, cw.getMethod("getFreePhysicalMemorySize").getJavaMethod());
			propertyMap.put(Attribute.FreeSwapSpaceSize, cw.getMethod("getFreeSwapSpaceSize").getJavaMethod());
			propertyMap.put(Attribute.ProcessCpuTime, cw.getMethod("getProcessCpuTime").getJavaMethod());
			propertyMap.put(Attribute.TotalPhysicalMemorySize, cw.getMethod("getTotalPhysicalMemorySize").getJavaMethod());
			propertyMap.put(Attribute.TotalSwapSpaceSize, cw.getMethod("getTotalSwapSpaceSize").getJavaMethod());
		} catch (NoSuchMethodException e) {
			LogUtil.exception(e);
		}
	}

	private void initIBMJDKBean(ClassEx cw) {
		try {
			propertyMap.put(Attribute.TotalPhysicalMemorySize, cw.getMethod("getTotalPhysicalMemory").getJavaMethod());
		} catch (NoSuchMethodException e) {
			LogUtil.exception(e);
		}
	}

	private void initMethodInJDK6(ClassEx cw) {
		try {
			propertyMap.put(Attribute.SystemLoadAverage, cw.getMethod("getSystemLoadAverage").getJavaMethod());
		} catch (NoSuchMethodException e) {
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.management.OperatingSystemMXBean#getName()
	 */
	public String getName() {
		return bean.getName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.management.OperatingSystemMXBean#getArch()
	 */
	public String getArch() {
		return bean.getArch();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.management.OperatingSystemMXBean#getVersion()
	 */
	public String getVersion() {
		return bean.getVersion();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.management.OperatingSystemMXBean#getAvailableProcessors()
	 */
	public int getAvailableProcessors() {
		return bean.getAvailableProcessors();
	}

	/*
	 * 这个是JDK6才有的方法 (non-Javadoc)
	 * 
	 * @see java.lang.management.OperatingSystemMXBean#getSystemLoadAverage()
	 */
	public double getSystemLoadAverage() {
		if (!isWindows && propertyMap.containsKey(Attribute.SystemLoadAverage)) {
			return invokeGet(propertyMap.get(Attribute.SystemLoadAverage), Double.class);
		} else {
			return getCpuRatio();
		}
	}
	
	public long getHeapCommittedMemorySize(){
		return memMx.getHeapMemoryUsage().getCommitted();
	}
	
	/**
	 * 获取最大的heap大小。一般由Xmx参数指定
	 * @return
	 */
	public long getMaxHeapSize(){
		return maxHeap;
	}
	/**
	 * 获得初始的heap大小。一般由Xms参数指定
	 * @return
	 */
	public long getInitHeapSize(){
		return minHeap;
	}
	/**
	 * 获取最大的Perm大小
	 * @return
	 */
	public long getMaxPermSize(){
		return maxPerm;
	}
	/**
	 * 获的初始的Perm内存区域大小
	 * @return
	 */
	public long getInitPermSize(){
		return minPerm;
	}
	/**
	 * 获取当前Heap内存的占用摘要信息
	 * @return
	 */
	public String getHeapMemoryUsage(){
		return memMx.getHeapMemoryUsage().toString();
	}
	/**
	 * 获取当前Perm内存的占用摘要信息
	 * @return
	 */
	public String getPermMemoryUsage(){
		return memMx.getNonHeapMemoryUsage().toString();
	}

	/**
	 * 获取目前操作系统已分配的内存数
	 */
	public long getCommittedVirtualMemorySize() {
		return invokeGetLong(propertyMap.get(Attribute.CommittedVirtualMemorySize));
	}

	/**
	 * 当前系统总内存交换区
	 * 
	 * @return
	 */
	public long getTotalSwapSpaceSize() {
		return invokeGetLong(propertyMap.get(Attribute.TotalSwapSpaceSize));
	}

	/**
	 * 当前系统空闲交换区
	 * 
	 * @return
	 */
	public long getFreeSwapSpaceSize() {
		return invokeGetLong(propertyMap.get(Attribute.FreeSwapSpaceSize));
	}

	/**
	 * 当前进程的CPU时间
	 * 
	 * @return
	 */
	public long getProcessCpuTime() {
		return invokeGetLong(propertyMap.get(Attribute.ProcessCpuTime));
	}

	/**
	 * 当前系统的空闲物理内存
	 * 
	 * @return
	 */
	public long getFreePhysicalMemorySize() {
		return invokeGetLong(propertyMap.get(Attribute.FreePhysicalMemorySize));
	}

	/**
	 * 当前系统的物理内存
	 * 
	 * @return
	 */
	public long getTotalPhysicalMemorySize() {
		return invokeGetLong(propertyMap.get(Attribute.TotalPhysicalMemorySize));
	}

	@SuppressWarnings("unchecked")
	private <T> T invokeGet(Method method, Class<T> clz) {
		if (method == null) {
			throw new UnsupportedOperationException();
		}
		try {
			return (T) method.invoke(bean);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e.getTargetException());
		}
	}

	private long invokeGetLong(Method method) {
		return invokeGet(method, Long.class);
	}

	/**
	 * 取得CPU使用比例
	 * 
	 * @return
	 * @deprecated 尽量不要用，使用命令方式不稳定，而却效率不高
	 */
	@Deprecated
	private double getCpuRatio() {
		double cpuRatio = 0;
		if (isWindows) {
			cpuRatio = getCpuRatioForWindows();
		} else if (isHp) {
			cpuRatio = getCpuRateForHP();
		} else {
			cpuRatio = getCpuRateForLinux();
		}
		return cpuRatio;
	}

	/**
	 * 当前系统是否windows
	 * 
	 * @return
	 */
	public static boolean isWindows() {
		return isWindows;
	}

	/**
	 * 获得当前的线程总数.
	 *
	 * 完成的线程 状态访问功能还是请使用
	 * ManagementFactory.getThreadMXBean();
	 * @return 返回构造好的监控对象
	 */
	public int getThreadCount(){
		ThreadMXBean threadMx=ManagementFactory.getThreadMXBean();
		return threadMx.getThreadCount();
	}

	private static double getCpuRateForLinux() {
		BufferedReader brStat = null;
		Process process = null;
		try {
			process = Runtime.getRuntime().exec("top -b -n 1");
			brStat = IOUtils.getReader(process.getInputStream(), "US-ASCII");
			StringTokenizer tokenStat = null;
			if (linuxVersion.startsWith("2.4")) {
				brStat.readLine();
				brStat.readLine();
				brStat.readLine();
				brStat.readLine();

				tokenStat = new StringTokenizer(brStat.readLine());
				tokenStat.nextToken();
				tokenStat.nextToken();
				String user = tokenStat.nextToken();
				tokenStat.nextToken();
				String system = tokenStat.nextToken();
				tokenStat.nextToken();
				String nice = tokenStat.nextToken();
				// System.out.println(user + " , " + system + " , " + nice);
				user = user.substring(0, user.indexOf("%"));
				system = system.substring(0, system.indexOf("%"));
				nice = nice.substring(0, nice.indexOf("%"));

				float userUsage = new Float(user).floatValue();
				float systemUsage = new Float(system).floatValue();
				float niceUsage = new Float(nice).floatValue();
				return (userUsage + systemUsage + niceUsage) / 100;
			} else {
				brStat.readLine();
				brStat.readLine();
				tokenStat = new StringTokenizer(brStat.readLine());
				tokenStat.nextToken();
				tokenStat.nextToken();
				tokenStat.nextToken();
				tokenStat.nextToken();
				tokenStat.nextToken();
				tokenStat.nextToken();
				tokenStat.nextToken();
				String cpuUsage = tokenStat.nextToken();
				// System.out.println("CPU idle : " + cpuUsage);
				Float usage = new Float(cpuUsage.substring(0, cpuUsage.indexOf("%")));
				return (1 - usage.floatValue() / 100);
			}
		} catch (IOException e) {
			LogUtil.exception(e);
			return -1.0;
		} finally {
			if (process != null)
				process.destroy();
			IOUtils.closeQuietly(brStat);
		}
	}

	/**
	 * 返回HP机器的CPU使用率
	 * 
	 * @return
	 */
	private static double getCpuRateForHP() {
		BufferedReader reader = null;
		Process process = null;
		try {
			process = Runtime.getRuntime().exec("top -n 1");
			reader = IOUtils.getReader(process.getInputStream(), "US-ASCII");
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.startsWith("System Page Size") || line.length() == 0)
					break;
				if (line.startsWith("avg")) {
					StringTokenizer st = new StringTokenizer(line);
					for (int i = 0; i < 5; i++) {
						st.nextToken();
					}
					String idle = st.nextToken().trim().replace("%", "");
					return (100 - Double.parseDouble(idle)) / 100;
				}
			}
			return -1.0;
		} catch (IOException ioe) {
			LogUtil.exception(ioe);
			return -1.0;
		} finally {
			if (process != null)
				process.destroy();
			IOUtils.closeQuietly(reader);
		}
	}

	/**
	 * 获得CPU使用率.
	 * 
	 * @return 返回cpu使用率
	 */
	private static double getCpuRatioForWindows() {
		try {
			String procCmd = System.getenv("windir") + "\\system32\\wbem\\wmic.exe process get Caption,CommandLine," + "KernelModeTime,ReadOperationCount,ThreadCount,UserModeTime,WriteOperationCount";
			// 取进程信息
			long[] c0 = readCpu(Runtime.getRuntime().exec(procCmd));
			Thread.sleep(CPUTIME);
			long[] c1 = readCpu(Runtime.getRuntime().exec(procCmd));
			if (c0 != null && c1 != null) {
				long idletime = c1[0] - c0[0];
				long busytime = c1[1] - c0[1];
				return Double.valueOf(PERCENT * (busytime) / (busytime + idletime)).doubleValue();
			} else {
				return 0.0;
			}
		} catch (Exception ex) {
			LogUtil.error(ex.getMessage());
			return 0.0;
		}
	}

	private static final int CPUTIME = 30;
	private static final int PERCENT = 100;
	private static final int FAULTLENGTH = 10;

	/**
	 * 
	 * 读取CPU信息.
	 * 
	 * @param proc
	 */
	private static long[] readCpu(final Process proc) {
		long[] retn = new long[2];
		try {
			proc.getOutputStream().close();
			InputStreamReader ir = new InputStreamReader(proc.getInputStream());
			LineNumberReader input = new LineNumberReader(ir);
			String line = input.readLine();
			if (line == null || line.length() < FAULTLENGTH) {
				return null;
			}
			int capidx = line.indexOf("Caption");
			int cmdidx = line.indexOf("CommandLine");
			int rocidx = line.indexOf("ReadOperationCount");
			int umtidx = line.indexOf("UserModeTime");
			int kmtidx = line.indexOf("KernelModeTime");
			int wocidx = line.indexOf("WriteOperationCount");
			long idletime = 0;
			long kneltime = 0;
			long usertime = 0;
			while ((line = input.readLine()) != null) {
				if (line.length() < wocidx) {
					continue;
				}
				String caption = line.substring(capidx, cmdidx - 1).trim();
				String cmd = line.substring(cmdidx, kmtidx - 1).trim();
				if (cmd.indexOf("wmic.exe") >= 0) {
					continue;
				}
				if (caption.equals("System Idle Process") || caption.equals("System")) {
					idletime += Long.valueOf(line.substring(kmtidx, rocidx - 1).trim()).longValue();
					idletime += Long.valueOf(line.substring(umtidx, wocidx - 1).trim()).longValue();
					continue;
				}

				kneltime += Long.valueOf(line.substring(kmtidx, rocidx - 1).trim()).longValue();
				usertime += Long.valueOf(line.substring(umtidx, wocidx - 1).trim()).longValue();
			}
			retn[0] = idletime;
			retn[1] = kneltime + usertime;
			return retn;
		} catch (Exception ex) {
			LogUtil.exception(ex);
		} finally {
			IOUtils.closeQuietly(proc.getErrorStream());
		}
		return null;
	}

	/**
	 * 测试代码，由于需要在不同的平台上试运行。所以暂时都写在这个类里。
	 * @param args
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	public static void main(String[] args) throws Exception {
		OperatingSystemMXBean bean=OperatingSystemMXBean.getInstance();
		for(Method m:bean.getClass().getMethods()){
			if(m.getParameterTypes().length==0 && m.getDeclaringClass()==OperatingSystemMXBean.class){
				Object value=m.invoke(bean);
				if(value instanceof Long){
					if(m.getName().endsWith("Size")){
						System.out.println(StringUtils.rightPad(m.getName(), 28)+"\t"+StringUtils.formatSize((Long)value));		
					}else if(m.getName().endsWith("getStartTime")){
						System.out.println(StringUtils.rightPad(m.getName(), 28)+"\t"+new Date((Long)value));
					}else{
						System.out.println(StringUtils.rightPad(m.getName(), 28)+"\t"+value);
					}
				}else{
					System.out.println(StringUtils.rightPad(m.getName(), 28)+"\t"+value);
				}
				
			}
		}
	}


	public ObjectName getObjectName() {
		try {
			return ObjectName.getInstance("java.lang:type=OperatingSystem");
		} catch (MalformedObjectNameException e) {
			throw new RuntimeException(); 
		} catch (NullPointerException e) {
			throw new RuntimeException();
		}
	}
	
}

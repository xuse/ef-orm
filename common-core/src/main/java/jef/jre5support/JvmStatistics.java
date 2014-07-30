package jef.jre5support;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;


public class JvmStatistics implements JvmStatisticsMBean{
	
	private MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
	private ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
	private List<GarbageCollectorMXBean> gcList=ManagementFactory.getGarbageCollectorMXBeans();
	private List<String> youngGcNameList = new ArrayList<String>();
    private List<String> fullGcNameList = new ArrayList<String>();

    public JvmStatistics(){
    	// Oracle (Sun) HotSpot
    	youngGcNameList.add("Copy");
    	youngGcNameList.add("ParNew");
    	youngGcNameList.add("PS Scavenge");
    	fullGcNameList.add("MarkSweepCompact");
    	fullGcNameList.add("PS MarkSweep");
    	fullGcNameList.add("ConcurrentMarkSweep");
    	
    	// JRocket
    	youngGcNameList.add("Garbage collection optimized for short pausetimes Young Collector");
    	youngGcNameList.add("Garbage collection optimized for throughput Young Collector");
    	youngGcNameList.add("Garbage collection optimized for deterministic pausetimes Young Collector");
    	fullGcNameList.add("Garbage collection optimized for short pausetimes Old Collector");
    	fullGcNameList.add("Garbage collection optimized for throughput Old Collector");
    	fullGcNameList.add("Garbage collection optimized for deterministic pausetimes Old Collector");
    	
    }
    
	public long getHeapMemoryUse() {
		return memoryMXBean.getHeapMemoryUsage().getUsed();
	}
	public long getHeapMemoryMax() {
		return memoryMXBean.getHeapMemoryUsage().getMax();
	}
	public long getDaemonThreadCount() {
		return threadMXBean.getDaemonThreadCount();
	}
	public long getPeakThreadCount() {
		return threadMXBean.getPeakThreadCount();
	}
	public long getYoungGcCount() {
		int gcSize=gcList.size();
		long result=0;
		for (int i=0;i<gcSize;i++){
			GarbageCollectorMXBean gcMBean=gcList.get(i);
			String name=gcMBean.getName();
			if (youngGcNameList.contains(name)){
				long tmp=gcMBean.getCollectionCount();
				result= tmp>0 ? result+tmp : result;
			}
		}
		return result;
	}
	
	public long getYoungGcTotalTime() {
		int gcSize=gcList.size();
		long result=0;
		for (int i=0;i<gcSize;i++){
			GarbageCollectorMXBean gcMBean=gcList.get(i);
			String name=gcMBean.getName();
			if (youngGcNameList.contains(name)){
				long tmp=gcMBean.getCollectionTime();
				result= tmp>0 ? result+tmp : result;
			}
		}
		return result;
	}
	public long getFullGcCount() {
		int gcSize=gcList.size();
		long result=0;
		for (int i=0;i<gcSize;i++){
			GarbageCollectorMXBean gcMBean=gcList.get(i);
			String name=gcMBean.getName();
			if (fullGcNameList.contains(name)){
				long tmp=gcMBean.getCollectionCount();
				result= tmp>0 ? result+tmp : result;
			}
		}
		return result;
	}
	
	public long getFullGcTotalTime() {
		int gcSize=gcList.size();
		long result=0;
		for (int i=0;i<gcSize;i++){
			GarbageCollectorMXBean gcMBean=gcList.get(i);
			String name=gcMBean.getName();
			if (fullGcNameList.contains(name)){
				long tmp=gcMBean.getCollectionTime();
				result= tmp>0 ? result+tmp : result;
			}
		}
		return result;
	}

	public long getCurrentThreadCount() {
		return threadMXBean.getThreadCount();
	}
	
}

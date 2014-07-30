package jef.jre5support;

public interface JvmStatisticsMBean {

	public long getHeapMemoryUse();
	public long getHeapMemoryMax();
	public long getDaemonThreadCount();
	public long getPeakThreadCount();
	public long getCurrentThreadCount();
	
	public long getYoungGcCount();
	public long getYoungGcTotalTime();
	public long getFullGcCount();
	public long getFullGcTotalTime();
	
	
	
}

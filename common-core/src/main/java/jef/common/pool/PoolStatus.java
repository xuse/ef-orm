package jef.common.pool;


/**
 * 描述对象池内的大小数量
 * 
 * @author jiyi
 * 
 */
public final class PoolStatus {
	
	private int maxSize;
	private int minSize;
	private int currentSize;
	private int used;
	private int free;
	private long pollCount;
	private long offerCount;
	
	public PoolStatus(int max,int min,int current,int used,int free){
		this.maxSize=max;
		this.minSize=min;
		this.currentSize=current;
		this.used=used;
		this.free=free;
	}
	
	/**
	 * 得到连接池的最大空间
	 * 
	 * @return
	 */
	int getMaxSize(){
		return maxSize;
	}

	/**
	 * 得到连接池的当前大小
	 * 
	 * @return
	 */
	int getCurrentSize(){
		return currentSize;
	}
	
	/**
	 * 得到连接池的最小数量
	 * @return
	 */
	int getMinSize(){
		return minSize;
	}

	/**
	 * 得到连接池的连接使用数量
	 * 
	 * @return
	 */
	int getUsedCount(){
		return used;
	}

	/**
	 * 得到连接池的空闲连接数量
	 * 
	 * @return
	 */
	int getFreeCount(){
		return free;
	}

	public long getPollCount() {
		return pollCount;
	}

	public void setPollCount(long pollCount) {
		this.pollCount = pollCount;
	}

	public long getOfferCount() {
		return offerCount;
	}

	public void setOfferCount(long offerCount) {
		this.offerCount = offerCount;
	}

	@Override
	public String toString() {
		StringBuilder sb=new StringBuilder(64);
		sb.append("{max=").append(maxSize).append(',');
		sb.append("min=").append(minSize).append(',');
		sb.append("current=").append(currentSize).append(',');
		sb.append("used=").append(used).append(',');
		sb.append("free=").append(free).append(',');
		sb.append("poll=").append(pollCount).append(',');
		sb.append("offer=").append(offerCount).append('}');
		return sb.toString();
	}
}

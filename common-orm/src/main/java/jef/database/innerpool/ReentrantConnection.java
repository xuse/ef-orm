package jef.database.innerpool;

import jef.database.innerpool.PoolService.CheckableConnection;

/**
 * 描述连接可被一个对象多次重入使用的特性
 * @author jiyi
 *
 */
public interface ReentrantConnection extends IConnection,CheckableConnection{
	/**
	 * 设置表示连接被user对象所使用。
	 * 占用计数器+1
	 * @param flag
	 */
	public void setUsedByObject(Object user);
	
	/**
	 * 连接被重复取用，占用计数器+1
	 */
	public void addUsedByObject();
	
	/**
	 * 连接被归还时调用，占用计数器-1
	 * 如果占用计数器到0，那么返回连接的占用对象。 如果不为0，那么返回null
	 * @return
	 */
	public Object popUsedByObject();
}

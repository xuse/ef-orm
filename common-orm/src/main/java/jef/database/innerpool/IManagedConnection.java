package jef.database.innerpool;


/**
 * 受管理的连接
 * @author jiyi
 *
 */
public interface IManagedConnection extends CheckableConnection,ReentrantConnection{
	/**
	 * 当各种具体的SQL操作在执行中发现IO错误时，就会通知出现问题的连接，让这个连接失效丢弃。 同时还能触发连接池的检查等功能。
	 * 
	 * 通知连接失效
	 */
	public void notifyDisconnect();
}

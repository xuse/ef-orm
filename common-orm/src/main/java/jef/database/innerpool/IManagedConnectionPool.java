package jef.database.innerpool;


/**
 * 受管理的连接池
 * @author jiyi
 *
 */
public interface IManagedConnectionPool<T extends IConnection> extends IUserManagedPool<T>{
	/**
	 * 当部分连接使用中发现问题时，可以通过这个接口来提示连接失效
	 */
	void notifyDbDisconnect();
}

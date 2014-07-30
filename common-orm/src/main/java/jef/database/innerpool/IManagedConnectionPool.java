package jef.database.innerpool;

import jef.database.dialect.DatabaseDialect;

/**
 * 受管理的连接池
 * @author jiyi
 *
 */
public interface IManagedConnectionPool extends IUserManagedPool,CheckablePool{
	/**
	 * 得到数据库方言，这是为了自我检测和重连而设计的接口
	 * 
	 * @return
	 */
	DatabaseDialect getProfile();
	

	/**
	 * 当部分连接使用中发现问题时，可以通过这个接口来提示连接失效
	 */
	void notifyDbDisconnect();
}

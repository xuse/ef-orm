package jef.database.innerpool;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 这是真正在JEF内部所使用的数据库连接对象。 IConnection对象 当使用事务处理时，每个事务对象都被分配到一个IConnection。
 * 当处理非事务任务时，每个线程都得到一个IConnection。 无论哪种情况ConnectionInPool都由
 * {@link IConnectionPool}对象给出。
 * <p>
 * 
 * @author Administrator
 */
public interface IConnection extends Connection {
	/**
	 * (物理上)关闭连接
	 * 
	 * 
	 * 正常情况下,连接有close方法。但对于连接池中的连接，close方法意味着释放而不是关闭。
	 * 
	 * 
	 * 而出现一个问题，就是连接池在管理连接的时候，需要关闭连接怎么办？所以增加此方法。 此方法只用于连接池管理连接时使用，不允许在其他场合使用。
	 */
	void closePhysical();

	/**
	 * 确认连接有效，在每次获取连接时执行，因此不可能执行开销太大的方法
	 * 
	 * @throws SQLException
	 */
	void ensureOpen() throws SQLException;

	/**
	 * 设置连接要从哪个数据源获取，当多数据源时，连接是有状态的。通过这个方法设置连接的状态
	 * 
	 * @param key
	 * @return
	 */
	void setKey(String key);

	/**
	 * 释放回连接池
	 */
	void close();
}
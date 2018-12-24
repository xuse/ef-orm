package jef.database.innerpool;

import java.sql.Connection;

/**
 * 聚合连接。
 * 带多数据源的连接。用于将多个连接对象模拟成单个连接对象，从而方便框架编写符合JDBC API的代码，但能在不同的数据源之间进行切换。
 * 同时，也方便复用Spring等事务管理机制，进行简单的事务管理。
 * <p>
 * <strong>Not  thread-safe.</strong>
 * 对于大部分JDBC实现来说，Connection是线程安全的，然而本类中，由于IConnection对象中记录了一个key的状态，因此不再是线程安全的了。
 * 明确禁止跨线程共用IConnection对象。
 * 
 * @author Administrator
 */
public interface IConnection extends Connection {
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
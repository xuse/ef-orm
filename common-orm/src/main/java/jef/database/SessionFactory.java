package jef.database;

public interface SessionFactory {

	
	/**
	 * 得到数据库会话Session。
	 * 使用Spring事务托管模式下，可以得到当前的事务Session。
	 * 如果未托管事务，相当于在一个AutoCommit=true的连接上进行操作。
	 * 
	 * 此外：如果处于未开启托管模式，那么在进行级联操作时为了保障数据一致性，框架会自动将级联操作嵌入到一个内部事务当中。
	 * @return
	 */
	Session getSession();
	
	/**
	 * 当使用编程式事务的时候，调用这个接口获得一个事务。
	 * 如果使用事务托管模式，调用此方法将会抛出异常。
	 * @return
	 */
	Transaction startTransaction();

	/**
	 * 为了兼容旧版本增加的转换接口。
	 * @return
	 */
	DbClient asDbClient();
	
	/**
	 * 关闭所有，施放资源。
	 */
	void shutdown();
	
	
	/**
	 * 获得数据元数据操作。
	 * @return
	 */
	DbMetaData getDefaultDatabaseMetadata();
}

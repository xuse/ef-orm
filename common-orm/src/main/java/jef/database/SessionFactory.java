package jef.database;

public interface SessionFactory {
	public abstract TransactionalSession startTransaction();

	public abstract void shutdown();
	
	/**
	 * 得到无事务状态的Session
	 * @return
	 */
	public abstract Session getSession();
	
}

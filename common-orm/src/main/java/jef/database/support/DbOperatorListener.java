package jef.database.support;

import jef.database.Session;
import jef.database.Transaction;

/**
 * 数据库操作的监听器
 * @author Administrator
 *
 */
public interface DbOperatorListener {
	/**
	 * 当一个对象将被删除时执行
	 * @param obj 请求
	 * @param db  所操作的事务或db
	 */
	void beforeDelete(Object obj,Session db);

	/**
	 * 当一个对象被删除后执行
	 * @param obj 请求
	 * @param n  操作记录条数
	 * @param db 所操作的事务或db
	 */
	void afterDelete(Object obj, int n,Session db);

	/**
	 * 当对象更新前执行
	 * @param obj 请求
	 * @param db
	 */
	void beforeUpdate(Object obj,Session db);

	/**
	 * 当对象更新后执行
	 * @param obj 请求
	 * @param n   更新条数
	 * @param db
	 */
	void afterUpdate(Object obj, int n,Session db);

	/**
	 * 当一个新的事务开启时执行
	 * @param transaction
	 */
	void newTransaction(Transaction transaction);

	/**
	 * 当事务回滚前执行
	 * @param transaction
	 */
	void beforeRollback(Transaction transaction);
	
	/**
	 * 当一个事务回滚后执行
	 * @param transaction
	 */
	void postRollback(Transaction transaction);

	/**
	 * 当事务提交时执行
	 * @param transaction
	 */
	void beforeCommit(Transaction transaction);
	
	/**
	 * 当事务提交后
	 * @param transaction
	 */
	void postCommit(Transaction transaction);
	
	/**
	 * 当事务关闭时执行
	 * @param transaction
	 */
	void tracsactionClose(Transaction transaction);

	/**
	 * 当对象插入数据库前执行
	 * @param obj
	 * @param abstractDbClient
	 */
	void beforeInseret(Object obj, Session abstractDbClient);
	
	/**
	 * 当对象插入数据库后执行
	 * @param obj
	 * @param abstractDbClient
	 */
	void afterInsert(Object obj, Session abstractDbClient);

	/**
	 * 在SQL语句执行之前
	 * @param sql
	 * @param params
	 */
	void beforeSqlExecute(String sql, Object... params);

	/**
	 * 在SQL语句执行之后
	 * @param sql
	 * @param n
	 * @param params
	 */
	void afterSqlExecuted(String sql, int n,Object... params);
	
	/**
	 * Before select sql
	 * @param sql
	 * @param params
	 */
	void beforeSelect(String sql,Object... params);
	
	/**
	 * 当整个DbClient关闭时执行
	 */
	void onDbClientClose();
}

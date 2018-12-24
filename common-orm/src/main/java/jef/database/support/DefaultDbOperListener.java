package jef.database.support;

import jef.database.Session;
import jef.database.Transaction;
import jef.database.query.Query;

/**
 * 默认的，空实现
 * 
 * @author Administrator
 *
 */
public class DefaultDbOperListener implements DbOperatorListener {
	private static DbOperatorListener instance = new DefaultDbOperListener();

	public static DbOperatorListener getInstance() {
		return instance;
	}

	@Override
	public void beforeDelete(Query<?> obj, Session db) {
	}

	@Override
	public void afterDelete(Query<?> obj, int n, Session db) {
	}

	@Override
	public void beforeUpdate(Query<?> obj, Session db) {
	}

	@Override
	public void afterUpdate(Query<?> obj, int n, Session db) {
	}

	@Override
	public void beforeInseret(Object obj, Session abstractDbClient) {
	}

	@Override
	public void afterInsert(Object obj, Session abstractDbClient) {
	}

	public void newTransaction(Transaction transaction) {
	}

	public void tracsactionClose(Transaction transaction) {
	}

	public void beforeSqlExecute(String sql, Object... params) {
	}

	public void afterSqlExecuted(String sql, int n, Object... params) {
	}

	public void beforeRollback(Transaction transaction) {
	}

	public void postRollback(Transaction transaction) {
	}

	public void beforeCommit(Transaction transaction) {
	}

	public void postCommit(Transaction transaction) {
	}

	public void beforeSelect(String sql, Object... params) {
	}

	public void onDbClientClose() {
	}
}

package jef.database.support;

import jef.database.Session;
import jef.database.IQueryableEntity;
import jef.database.Transaction;

/**
 * 默认的，空实现
 * @author Administrator
 *
 */
public class DefaultDbOperListener implements DbOperatorListener{
	private static DbOperatorListener instance=new DefaultDbOperListener();
	
	public static DbOperatorListener getInstance(){
		return instance;
	}
	
	public void beforeDelete(IQueryableEntity obj, Session db) {
	}

	public void afterDelete(IQueryableEntity obj, int n, Session db) {
	}

	public void beforeUpdate(IQueryableEntity obj, Session db) {
	}

	public void afterUpdate(IQueryableEntity obj, int n, Session db) {
	}

	public void newTransaction(Transaction transaction) {
	}

	public void tracsactionClose(Transaction transaction){
	}

	public void afterInsert(IQueryableEntity obj, Session abstractDbClient) {
	}

	public void beforeInseret(IQueryableEntity obj, Session abstractDbClient) {
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

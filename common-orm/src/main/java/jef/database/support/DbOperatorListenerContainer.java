package jef.database.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jef.database.Session;
import jef.database.IQueryableEntity;
import jef.database.Transaction;

public final class DbOperatorListenerContainer implements DbOperatorListener{
	private List<DbOperatorListener> listeners=new ArrayList<DbOperatorListener>();
	
	public DbOperatorListenerContainer(DbOperatorListener... listeners){
		this.listeners.addAll(Arrays.asList(listeners));
	}
	
	public void add(DbOperatorListener lis){
		this.listeners.add(lis);
	}
	
	public void beforeDelete(IQueryableEntity obj, Session db) {
		for(DbOperatorListener l:listeners){
			l.beforeDelete(obj, db);
		}
	}

	public void afterDelete(IQueryableEntity obj, int n, Session db) {
		for(DbOperatorListener l:listeners){
			l.afterDelete(obj, n, db);
		}
		
	}

	public void beforeUpdate(IQueryableEntity obj, Session db) {
		for(DbOperatorListener l:listeners){
			l.beforeUpdate(obj, db);
		}		
	}

	public void afterUpdate(IQueryableEntity obj, int n, Session db) {
		for(DbOperatorListener l:listeners){
			l.afterUpdate(obj, n, db);
		}		
	}

	public void newTransaction(Transaction transaction) {
		for(DbOperatorListener l:listeners){
			l.newTransaction(transaction);
		}		
	}

	public void beforeRollback(Transaction transaction) {
		for(DbOperatorListener l:listeners){
			l.beforeRollback(transaction);
		}		
	}

	public void postRollback(Transaction transaction) {
		for(DbOperatorListener l:listeners){
			l.postRollback(transaction);
		}		
	}

	public void beforeCommit(Transaction transaction) {
		for(DbOperatorListener l:listeners){
			l.beforeCommit(transaction);
		}		
	}

	public void postCommit(Transaction transaction) {
		for(DbOperatorListener l:listeners){
			l.postCommit(transaction);
		}		
	}

	public void tracsactionClose(Transaction transaction) {
		for(DbOperatorListener l:listeners){
			l.tracsactionClose(transaction);
		}		
	}

	public void beforeInseret(IQueryableEntity obj, Session abstractDbClient) {
		for(DbOperatorListener l:listeners){
			l.beforeInseret(obj, abstractDbClient);
		}		
	}

	public void afterInsert(IQueryableEntity obj, Session abstractDbClient) {
		for(DbOperatorListener l:listeners){
			l.afterInsert(obj, abstractDbClient);
		}		
	}

	public void beforeSqlExecute(String sql, Object... params) {
		for(DbOperatorListener l:listeners){
			l.beforeSqlExecute(sql, params);
		}		
	}

	public void afterSqlExecuted(String sql, int n, Object... params) {
		for(DbOperatorListener l:listeners){
			l.afterSqlExecuted(sql, n, params);
		}		
	}

	public void beforeSelect(String sql, Object... params) {
		for(DbOperatorListener l:listeners){
			l.beforeSelect(sql, params);
		}	
	}

	public void onDbClientClose() {
		for(DbOperatorListener l:listeners){
			l.onDbClientClose();
		}	
	}
}

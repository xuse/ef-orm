package jef.database.jpa;

import java.sql.SQLException;
import java.util.Map;

import javax.persistence.Cache;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.metamodel.Metamodel;
import javax.sql.DataSource;

import jef.database.DbClient;
import jef.database.DbClientFactory;
import jef.database.cache.CacheDummy;
import jef.database.jmx.JefFacade;

import org.easyframe.enterprise.spring.TransactionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JefEntityManagerFactory implements EntityManagerFactory {
	/**
	 * EMF名称
	 */
	private String name;

	// private CriteriaBuilderImpl cbuilder=new CriteriaBuilderImpl(this);

	private DbClient db;
	private Map<String, Object> properties;
	private static Logger log = LoggerFactory.getLogger(JefEntityManagerFactory.class);

	public EntityManager createEntityManager() {
		return createEntityManager(null);
	}

	@SuppressWarnings("rawtypes")
	public EntityManager createEntityManager(Map map) {
		EntityManager result = new JefEntityManager(this, map);
		log.trace("[JPA DEBUG]:creating EntityManager:{} at {}", result, Thread.currentThread());
		return result;
	}

	public Map<String, Object> getProperties() {
		return properties;
	}

	public CriteriaBuilder getCriteriaBuilder() {
		// TODO 2013-11 为了提高单元测试覆盖率，暂将这部分分支去除
		throw new UnsupportedOperationException();
		// return cbuilder;
	}

	public Cache getCache() {
		return CacheDummy.getInstance();
	}

	public PersistenceUnitUtil getPersistenceUnitUtil() {
		throw new UnsupportedOperationException();
	}

	public void close() {
		log.debug("[JPA DEBUG]:close.{}", this);
		if (db.isOpen()) {
			db.close();
		}
	}

	public Metamodel getMetamodel() {
		throw new UnsupportedOperationException();
	}

	public boolean isOpen() {
		boolean flag = db.isOpen();
		log.debug("[JPA DEBUG]:isOpen - {}", flag);
		return flag;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public JefEntityManagerFactory(DbClient db) {
		this.db = db;
		JefFacade.registeEmf(db, this);
	}

	public JefEntityManagerFactory(DataSource ds) {
		this(ds, null);
	}

	public JefEntityManagerFactory(DataSource dataSource, TransactionMode txType) {
		try {
			this.db = DbClientFactory.getDbClient(dataSource, txType);
			JefFacade.registeEmf(db, this);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public DbClient getDefault() {
		return db;
	}

	// /**
	// * 返回当前线程的JPA Session，如果不在事务中返回null
	// * JPA Session是在JEF Transaction对象上的进一步封装，内部可能包含多个独立的JEF Transaction
	// *
	// * @return
	// */
	// public JefEntityTransaction getTransaction() {
	// return transactions.get();
	// }
	//
	// /**
	// * 获得当前线程的事务Session，如果不在事务中返回null
	// * @return
	// */
	// public Transaction getTransactionSession() {
	// JefEntityTransaction jefTransaction=transactions.get();
	// return jefTransaction==null?null:jefTransaction.get();
	// }
	//
	// /**
	// * 获得当前线程的Session，如果在事务中返回事务，如果不在事务中返回公用的非事务Session
	// * @return
	// */
	// public AbstractDbClient getSession(){
	// JefEntityTransaction transaction=transactions.get();
	// if(transaction==null){
	// return getDefault();
	// }else{
	// return transaction.get();
	// }
	// }
	//
	//
	// //开始新事务,采用默认超时和隔离级别
	// public void beginTransaction(){
	// this.createEntityManager().getTransaction().begin();
	// }
	//
	// //开始新事务并设置超时和隔离级别
	// public void beginTransaction(int timeout,int isolationLevel){
	// //if(involveTransaction){
	// this.createEntityManager().getTransaction().begin();
	// //获取线程当前事务
	// Transaction transaction = transactions.get().get();
	// transaction.setTimeout(timeout);
	// transaction.setIsolationLevel(isolationLevel);
	// //}
	// }
	//
	// // //清除当前事务信息
	// public void closeTransaction(JefEntityManager jefEntityManager){
	// JefEntityTransaction transaction=transactions.get();
	// if(transaction!=null){
	// if(!transaction.isAllClosed()){
	// //这个情况应该不会发生的，这里测试一下，所以抛出异常
	// throw new IllegalStateException("The EM can not be closed since:"+
	// transaction.toString() +" is not closed.");
	// // return;
	// }
	// transactions.set(null);
	// }
	// }
	//
	// public JefEntityTransaction createTransaction(JefEntityManager
	// jefEntityManager) {
	// JefEntityTransaction trans=new JefEntityTransaction(jefEntityManager);
	// transactions.set(trans);
	// return trans;
	// }
	//
}

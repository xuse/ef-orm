package org.easyframe.enterprise.spring;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import jef.database.ConnectionManagedSession;
import jef.database.Session;
import jef.database.jpa.JefEntityManager;
import jef.database.jpa.JefEntityManagerFactory;
import jef.tools.Assert;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 所有DAO的基类
 * @author jiyi
 *
 */
public class BaseDao {
	@Autowired
	private EntityManagerFactory entityManagerFactory;

	private JefEntityManagerFactory jefEmf;
	
	@PostConstruct
	public void init(){
		Assert.notNull(entityManagerFactory);
		Assert.notNull(jefEmf);
	}
	
	/**
	 * 获得EntityManager
	 * @return
	 */
	protected final EntityManager getEntityManager(){
		TransactionType tx=jefEmf.getDefault().getTxType();
		EntityManager em;
		switch (tx) {
		case JPA:
			em=EntityManagerFactoryUtils.doGetTransactionalEntityManager(entityManagerFactory,null);
			if(em==null){ //当无事务时。Spring返回null
				em=entityManagerFactory.createEntityManager(null);
			}	
			break;
		case DATASORUCE:
			ConnectionHolder conn=(ConnectionHolder)TransactionSynchronizationManager.getResource(jefEmf.getDefault().getDataSource());
			if(conn==null){//基于数据源的Spring事务
				em=entityManagerFactory.createEntityManager(null);
			}else{
				ConnectionManagedSession session=new ConnectionManagedSession(jefEmf.getDefault(),conn.getConnection());
				em= new JefEntityManager(entityManagerFactory,null,session);
			}
		default:
			throw new UnsupportedOperationException(tx.name());
		}
		return em;
	}
	
	/**
	 * 获得JEF的操作Session
	 * @return
	 */
	protected final Session getSession() {
		JefEntityManager em=(JefEntityManager)getEntityManager();
		Session session= em.getSession();
		Assert.notNull(session);
		return session;
	}
	
	/**
	 * 获得JEF的操作Session
	 * @deprecated use method {@link #getSession()} instead.
	 * @return
	 */
	protected final Session getDbClient(){
		return getSession();
	}

	public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
		this.jefEmf=(JefEntityManagerFactory)entityManagerFactory;
	}
}

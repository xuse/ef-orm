package org.easyframe.enterprise.spring;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import jef.database.Session;
import jef.database.jpa.JefEntityManager;
import jef.tools.Assert;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;

/**
 * Repo
 * @author jiyi
 *
 */
@SuppressWarnings("restriction")
public class BaseDao {
	@Autowired
	private EntityManagerFactory entityManagerFactory;
	
	@SuppressWarnings("restriction")
	@PostConstruct
	public void init(){
		Assert.notNull(entityManagerFactory);
	}
	
	/**
	 * 获得EntityManager
	 * @return
	 */
	protected final EntityManager getEntityManager(){
		EntityManager em=EntityManagerFactoryUtils.doGetTransactionalEntityManager(entityManagerFactory,null);
		if(em==null){ //当无事务时。Spring返回null
			em=entityManagerFactory.createEntityManager(null);
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
}

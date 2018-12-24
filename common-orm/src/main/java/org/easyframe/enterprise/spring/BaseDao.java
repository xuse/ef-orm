package org.easyframe.enterprise.spring;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;

import jef.database.Session;
import jef.database.SessionFactory;
import jef.tools.Assert;

/**
 * 所有DAO的基类
 * 
 * @author jiyi
 *
 */
public class BaseDao {
    private SessionFactory sessionFactory;

    /**
     * 获得EntityManager
     * 
     * @return
     */
    public final Session getSession() {
    	return sessionFactory.getSession();
    }

    /**
     * 获得JEF的操作Session
     * 
     * @return
     */
    public Session getNonTransactionalSession() {
        return sessionFactory.asDbClient();
    }

    @PostConstruct
    public void init() {
       Assert.notNull(sessionFactory);
    }

    @Autowired(required = false)
    public void setEntityManagerFactory(SessionFactory entityManagerFactory) {
        if (this.sessionFactory == null) {
            Assert.notNull(entityManagerFactory);
            this.sessionFactory = entityManagerFactory;
        }
    }
}

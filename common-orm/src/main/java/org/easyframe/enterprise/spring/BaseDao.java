package org.easyframe.enterprise.spring;

import java.util.Collections;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import jef.database.ManagedTransactionImpl;
import jef.database.Session;
import jef.database.jpa.JefEntityManager;
import jef.database.jpa.JefEntityManagerFactory;
import jef.tools.Assert;

/**
 * 所有DAO的基类
 * 
 * @author jiyi
 *
 */
public class BaseDao {
    private EntityManagerFactory entityManagerFactory;

    private JefEntityManagerFactory jefEmf;

    /**
     * 获得EntityManager
     * 
     * @return
     */
    protected final EntityManager getEntityManager() {
        TransactionMode tx = jefEmf.getDefault().getTxType();
        EntityManager em;
        switch (tx) {
        case JPA:
        case JTA:
            em = EntityManagerFactoryUtils.doGetTransactionalEntityManager(entityManagerFactory, null);
            if (em == null) { // 当无事务时。Spring返回null
                em = entityManagerFactory.createEntityManager(null, Collections.EMPTY_MAP);
            }
            break;
        case JDBC:
            ConnectionHolder conn = (ConnectionHolder) TransactionSynchronizationManager.getResource(jefEmf.getDefault().getDataSource());
            if (conn == null) {// 基于数据源的Spring事务
                em = entityManagerFactory.createEntityManager(null, Collections.EMPTY_MAP);
            } else {
                ManagedTransactionImpl session = new ManagedTransactionImpl(jefEmf.getDefault(), conn.getConnection());
                em = new JefEntityManager(entityManagerFactory, null, session);
            }
            break;
        default:
            throw new UnsupportedOperationException(tx.name());
        }
        return em;
    }

    /**
     * 获得JEF的操作Session
     * 
     * @return
     */
    public Session getSession() {
        JefEntityManager em = (JefEntityManager) getEntityManager();
        Session session = em.getSession();
        Assert.notNull(session);
        return session;
    }

    /**
     * 获得JEF的操作Session
     * 
     * @return
     */
    public Session getNonTransactionalSession() {
        return jefEmf.getDefault();
    }

    /**
     * 获得JEF的操作Session
     * 
     * @deprecated use method {@link #getSession()} instead.
     * @return
     */
    protected final Session getDbClient() {
        return getSession();
    }

    @PostConstruct
    public void init() {
        Assert.notNull(entityManagerFactory);
        if (jefEmf == null) {
            jefEmf = (JefEntityManagerFactory) entityManagerFactory;
        }
    }

    @Autowired(required = false)
    public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
        if (this.entityManagerFactory == null) {
            Assert.notNull(entityManagerFactory);
            this.entityManagerFactory = entityManagerFactory;
            this.jefEmf = (JefEntityManagerFactory) entityManagerFactory;

        }
    }
}

package org.easyframe.enterprise.spring;

import java.sql.SQLException;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

import jef.database.jpa.JefEntityManager;
import jef.database.support.SavepointNotSupportedException;

import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.orm.jpa.DefaultJpaDialect;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.NestedTransactionNotSupportedException;
import org.springframework.transaction.SavepointManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;

/**
 * 标准的JPA API中因为没有关于savepoint的特性，因此Spring是不支持使用nested这个事务传播级别的，
 * 这个dialect可以使Spring的7个传播级别全部得到支持
 * 在Spring中配置如下
 * <pre>
 * &lt;bean id="transactionManager" class="org.springframework.orm.jpa.JpaTransactionManager" lazy-init="true"&gt;
 * 	&lt;property name="entityManagerFactory" ref="entityManagerFactory" /&gt;
 * 	&lt;property name="jpaDialect"&gt;
 * 		&lt;bean class="org.jef.enterprise.spring.JefJpaDialect"/&gt;
 * 	&lt;/property&gt;		
 * &lt;/bean&gt;
 * </pre>
 * 
 * @author jiyi
 * 
 */
public class JefJpaDialect extends DefaultJpaDialect {
	private static final long serialVersionUID = 1L;

	public Object beginTransaction(EntityManager entityManager, TransactionDefinition definition) throws PersistenceException, SQLException, TransactionException {
		JefEntityManager em = getJefEntityManager(entityManager);
		return new JefJpaTransactionData(em,definition);
	}

	private JefEntityManager getJefEntityManager(EntityManager entityManager) {
		if (entityManager instanceof JefEntityManager)
			return (JefEntityManager) entityManager;
		throw new IllegalStateException("You should use jef.database.DbClientFacotry for EntityManagerFactory!");
	}

	/**
	 * Transaction data Object exposed from <code>beginTransaction</code>,
	 * implementing the SavepointManager interface.
	 */
	private static class JefJpaTransactionData implements SavepointManager {
		private final JefEntityManager entityManager;
		private int savepointCounter = 0;

		public JefJpaTransactionData(JefEntityManager entityManager,TransactionDefinition def) {
			this.entityManager = entityManager;
			entityManager.getTransaction().begin(def.getTimeout(),def.getIsolationLevel(),def.isReadOnly());
		}

		public Object createSavepoint() throws TransactionException {
			this.savepointCounter++;
			String savepointName = ConnectionHolder.SAVEPOINT_NAME_PREFIX + this.savepointCounter;
			try {
				this.entityManager.setSavepoint(savepointName);
			} catch (SavepointNotSupportedException e) {
				throw new NestedTransactionNotSupportedException("Cannot create a nested transaction because savepoints are not supported.");
			} catch (Throwable ex) {
				throw new CannotCreateTransactionException("Could not create JDBC savepoint", ex);
			}
			return savepointName;
		}

		public void rollbackToSavepoint(Object savepoint) throws TransactionException {
			this.entityManager.rollbackToSavepoint((String) savepoint);
		}

		public void releaseSavepoint(Object savepoint) throws TransactionException {
			this.entityManager.releaseSavepoint((String) savepoint);
		}
	}

}

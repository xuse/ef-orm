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
}

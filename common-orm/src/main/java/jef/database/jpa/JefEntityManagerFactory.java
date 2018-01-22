package jef.database.jpa;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Cache;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.Query;
import javax.persistence.SynchronizationType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.metamodel.Metamodel;
import javax.sql.DataSource;

import jef.database.DbCfg;
import jef.database.DbClient;
import jef.database.cache.CacheDummy;
import jef.database.jmx.JefFacade;
import jef.database.meta.AbstractMetadata;
import jef.database.meta.MetaHolder;
import jef.tools.JefConfiguration;

import org.easyframe.enterprise.spring.TransactionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JefEntityManagerFactory implements EntityManagerFactory,MetaProvider {
	/**
	 * EMF名称
	 */
	private String name;

	// private CriteriaBuilderImpl cbuilder=new CriteriaBuilderImpl(this);

	private DbClient db;
	private final Map<String, Object> properties = new HashMap<String,Object>();
	private static Logger log = LoggerFactory.getLogger(JefEntityManagerFactory.class);

	public EntityManager createEntityManager() {
		return createEntityManager(null, Collections.EMPTY_MAP);
	}

	@SuppressWarnings("rawtypes")
	public EntityManager createEntityManager(Map map) {
		return createEntityManager(SynchronizationType.SYNCHRONIZED, map);
	}

	@Override
	public EntityManager createEntityManager(SynchronizationType synchronizationType) {
		return createEntityManager(synchronizationType, Collections.EMPTY_MAP);
	}

	@Override
	public EntityManager createEntityManager(SynchronizationType synchronizationType, @SuppressWarnings("rawtypes") Map map) {
		EntityManager result = new JefEntityManager(this, map);
		log.debug("[JPA DEBUG]:creating EntityManager:{} at {}", result, Thread.currentThread());
		return result;
	}

	public Map<String, Object> getProperties() {
		return properties;
	}

	public CriteriaBuilder getCriteriaBuilder() {
		throw new UnsupportedOperationException();
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
		return null;
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
		properties.put("javax.persistence.nonJtaDataSource",db.getDataSource());
		JefFacade.registeEmf(db, this);
	}

	public JefEntityManagerFactory(DataSource datasource) {
		this(datasource, JefConfiguration.getInt(DbCfg.DB_CONNECTION_POOL, 3), JefConfiguration.getInt(DbCfg.DB_CONNECTION_POOL_MAX, 50), null);
		properties.put("javax.persistence.nonJtaDataSource",datasource);
	}

	public JefEntityManagerFactory(DataSource dataSource, int min, int max, TransactionMode txMode) {
		this.db = new DbClient(dataSource, min, max, txMode);
		properties.put("javax.persistence.nonJtaDataSource",dataSource);
	}

	public DbClient getDefault() {
		return db;
	}

	@Override
	public void addNamedQuery(String name, Query query) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T unwrap(Class<T> cls) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> void addNamedEntityGraph(String graphName, EntityGraph<T> entityGraph) {
		throw new UnsupportedOperationException();
	}

	public Collection<AbstractMetadata> getEntityTypes() {
		return MetaHolder.getCachedModels();
	}

	public AbstractMetadata managedType(Class<?> type) {
		return MetaHolder.getMeta(type);
	}
}

/*
 * JEF - Copyright 2009-2010 Jiyi (mr.jiyi@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jef.database.jpa;

import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.metamodel.Metamodel;

import jef.common.log.LogUtil;
import jef.database.DbClient;
import jef.database.DbUtils;
import jef.database.Field;
import jef.database.IQueryableEntity;
import jef.database.PojoWrapper;
import jef.database.Session;
import jef.database.TransactionalSession;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.database.support.SavepointNotSupportedException;
import jef.database.support.TransactionTimedOutException;
import jef.tools.reflect.BeanUtils;

/**
 * JPA接口 EntityManager 的JEF实现类
 * 
 * @author Administrator
 * 
 */
@SuppressWarnings("rawtypes")
public class JefEntityManager implements EntityManager {
	private boolean close;
	JefEntityManagerFactory parent;
	JefEntityTransaction tx;
	private Map properties;
	private FlushModeType mode = FlushModeType.AUTO;

	/**
	 * 构造
	 * 
	 * @param parent
	 * @param properties
	 */
	public JefEntityManager(EntityManagerFactory parent, Map properties) {
		this.parent = (JefEntityManagerFactory) parent;
		this.properties = properties;
	}

	public JefEntityManager(EntityManagerFactory parent, Map properties, TransactionalSession session) {
		this.parent = (JefEntityManagerFactory) parent;
		this.properties = properties;
		this.tx = new JefEntityTransaction(this, session);

	}

	public void persist(Object entity) {
		if (entity instanceof IQueryableEntity) {
			doMerge((IQueryableEntity) entity, false);
		} else {
			ITableMetadata meta = MetaHolder.getMeta(entity.getClass());
			PojoWrapper wrapper = meta.transfer(entity, false);
			doMerge(wrapper, false);
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T merge(T entity) {
		if (entity instanceof IQueryableEntity) {
			return (T) doMerge((IQueryableEntity) entity, false);
		} else {
			ITableMetadata meta = MetaHolder.getMeta(entity.getClass());
			PojoWrapper wrapper = meta.transfer(entity, false);
			wrapper = doMerge(wrapper, false);
			return (T) wrapper.get();
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T mergeCascade(T entity) {
		if (entity instanceof IQueryableEntity) {
			return (T) doMerge((IQueryableEntity) entity, true);
		} else {
			ITableMetadata meta = MetaHolder.getMeta(entity.getClass());
			PojoWrapper wrapper = meta.transfer(entity, false);
			wrapper = doMerge(wrapper, true);
			return (T) wrapper.get();
		}
	}

	public void remove(Object entity) {
		try {
			if (entity instanceof IQueryableEntity) {
				IQueryableEntity data = (IQueryableEntity) entity;
				getSession().deleteCascade(data);
			} else {
				ITableMetadata meta = MetaHolder.getMeta(entity.getClass());
				PojoWrapper wrapper = meta.transfer(entity, true);
				getSession().deleteCascade(wrapper);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage() + " " + e.getSQLState(), e);
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T find(Class<T> entityClass, Object primaryKey) {
		if (primaryKey == null)
			return null;
		try {
			Object obj = entityClass.newInstance();
			if (obj instanceof IQueryableEntity) {
				IQueryableEntity data = (IQueryableEntity) obj;
				DbUtils.setPrimaryKeyValue(data, primaryKey);
				return (T) getSession().load(data,true);
			} else {
				ITableMetadata meta = MetaHolder.getMeta(entityClass);
				PojoWrapper data = meta.transfer(obj, false);
				DbUtils.setPrimaryKeyValue(data, primaryKey);
				data = getSession().load(data,true);
				return data == null ? null : (T) data.get();
			}
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage() + " " + e.getSQLState(), e);
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 根据主键和其他属性查找
	 */
	@SuppressWarnings("unchecked")
	public <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties) {
		try {
			IQueryableEntity data = (IQueryableEntity) entityClass.newInstance();
			if (primaryKey != null) {
				DbUtils.setPrimaryKeyValue(data, primaryKey);
			} else if (properties != null && properties.size() > 0) {
				for (String s : properties.keySet()) {
					Field f = MetaHolder.getMeta(data).getField(s);
					if (f == null) {
						LogUtil.error("No Field named " + s + " in bean " + data.getClass().getName());
						continue;
					}
					data.getQuery().addCondition(f, properties.get(s));
				}
			} else {
				data.getQuery().setAllRecordsCondition();
			}
			return (T) getSession().load(data,true);
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage() + " " + e.getSQLState(), e);
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode) {
		return find(entityClass, primaryKey);
	}

	/**
	 * 根据主键和其他属性查找
	 */
	public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode, Map<String, Object> properties) {
		return find(entityClass, primaryKey, properties);
	}

	@SuppressWarnings("unchecked")
	public <T> T getReference(Class<T> entityClass, Object primaryKey) {
		try {
			IQueryableEntity data = (IQueryableEntity) entityClass.newInstance();
			DbUtils.setPrimaryKeyValue(data, primaryKey);
			List<IQueryableEntity> list = getSession().select(data);
			if (list.isEmpty())
				return null;
			return (T) list.get(0);
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage() + " " + e.getSQLState(), e);
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	public void flush() {
	}

	public void setFlushMode(FlushModeType flushMode) {
		this.mode = flushMode;
	}

	public FlushModeType getFlushMode() {
		return mode;
	}

	public void lock(Object entity, LockModeType lockMode) {
	}

	public void lock(Object entity, LockModeType lockMode, Map<String, Object> properties) {
	}

	public void refresh(Object entity) {
		if (entity instanceof IQueryableEntity) {
			try {
				IQueryableEntity newObj = this.getSession().load((IQueryableEntity) entity,true);
				newObj.stopUpdate();
				BeanUtils.copyProperties(newObj, entity);
			} catch (SQLException e) {
				throw new PersistenceException(e);
			}
		}
	}

	public void refresh(Object entity, Map<String, Object> properties) {
		if (entity instanceof IQueryableEntity) {
			try {
				IQueryableEntity newObj = this.getSession().load((IQueryableEntity) entity,true);
				newObj.stopUpdate();
				BeanUtils.copyProperties(newObj, entity);
			} catch (SQLException e) {
				throw new PersistenceException(e);
			}
		}
	}

	public void refresh(Object entity, LockModeType lockMode) {
		refresh(entity);
	}

	public void refresh(Object entity, LockModeType lockMode, Map<String, Object> properties) {
		refresh(entity);
	}

	public void clear() {
		getSession().evictAll();
	}

	public void detach(Object entity) {
		getSession().evict((IQueryableEntity) entity);
	}

	public boolean contains(Object entity) {
		if (entity instanceof IQueryableEntity) {
			try {
				IQueryableEntity newObj = this.getSession().load((IQueryableEntity) entity,true);
				return newObj != null;
			} catch (SQLException e) {
				throw new PersistenceException(e);
			}
		}
		return false;
	}

	public LockModeType getLockMode(Object entity) {
		return LockModeType.NONE;
	}

	@SuppressWarnings("unchecked")
	public void setProperty(String propertyName, Object value) {
		properties.put(propertyName, value);
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> getProperties() {
		return properties;
	}

	public Query createQuery(String qlString) {
		return getSession().createQuery(qlString);
	}

	public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery) {
		throw new UnsupportedOperationException();
	}

	public <T> TypedQuery<T> createQuery(String qlString, Class<T> resultClass) {
		return getSession().createQuery(qlString, resultClass);
	}

	public Query createNamedQuery(String name) {
		return getSession().createNamedQuery(name);
	}

	public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass) {
		return getSession().createNamedQuery(name, resultClass);
	}

	public Query createNativeQuery(String sqlString) {
		return getSession().createNativeQuery(sqlString);
	}

	@SuppressWarnings("unchecked")
	public Query createNativeQuery(String sqlString, Class resultClass) {
		return getSession().createNativeQuery(sqlString, resultClass);
	}

	/*
	 */
	public Query createNativeQuery(String sqlString, String resultSetMapping) {
		throw new UnsupportedOperationException();
	}

	/*
	 */
	public void joinTransaction() {
		if (tx == null || !tx.isActive()) {
			throw new TransactionTimedOutException("No transaction to join.");
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> cls) {
		return (T) getSession();
	}

	public Object getDelegate() {
		return this;
	}

	public void close() {
		if (tx != null && tx.isActive()) {
			throw new TransactionTimedOutException("Tx was not closed while em closing");
		}
		tx = null;
		close = true;
		parent = null;
	}

	public boolean isOpen() {
		return !close;
	}

	public EntityManagerFactory getEntityManagerFactory() {
		return parent;
	}

	public CriteriaBuilder getCriteriaBuilder() {
		return parent.getCriteriaBuilder();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.persistence.EntityManager#getMetamodel()
	 */
	public Metamodel getMetamodel() {
		throw new UnsupportedOperationException();
	}

	/*
	 * JPA标准接口(non-Javadoc)
	 * 
	 * @see javax.persistence.EntityManager#getTransaction()
	 */
	public JefEntityTransaction getTransaction() {
		if (tx == null) {
			tx = new JefEntityTransaction(this);
		}
		return tx;
	}

	/**
	 * 设置保存点
	 * 
	 * @param savepointName
	 * @throws SavepointNotSupportedException
	 */
	public Savepoint setSavepoint(String savepointName) throws SavepointNotSupportedException {
		if (tx != null) {
			try {
				return tx.get().setSavepoint(savepointName);
			} catch (SQLException e) {
				throw new PersistenceException(e.getMessage() + " " + e.getSQLState(), e);
			}
		}
		return null;
	}

	/**
	 * 回滚到保存点
	 * 
	 * @param savepoint
	 * @throws SavepointNotSupportedException
	 */
	public void rollbackToSavepoint(Savepoint savepoint) throws SavepointNotSupportedException {
		if (tx != null) {
			try {
				tx.get().rollbackToSavepoint(savepoint);
			} catch (SQLException e) {
				throw new PersistenceException(e.getMessage() + " " + e.getSQLState(), e);
			}
		}
	}

	/**
	 * 释放保存点
	 * 
	 * @param savepoint
	 */
	public void releaseSavepoint(Savepoint savepoint) {
		if (tx != null) {
			try {
				tx.get().releaseSavepoint(savepoint);
			} catch (SQLException e) {
				throw DbUtils.toRuntimeException(e);
			}
		}
	}

	private <T extends IQueryableEntity> T doMerge(T entity, boolean flag) {
		try {
			if (flag) {
				return getSession().mergeCascade(entity);
			} else {
				return getSession().merge(entity);
			}
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	/**
	 * get the current databa sesession.
	 * 
	 * @return
	 */
	public Session getSession() {
		if (close)
			throw new RuntimeException("the " + this.toString() + " has been closed!");
		if (tx != null && tx.isActive()) {
			return (Session) tx.get();
		} else {
			return parent.getDefault();
		}
	}

	@Override
	public Query createQuery(CriteriaUpdate updateQuery) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Query createQuery(CriteriaDelete deleteQuery) {
		throw new UnsupportedOperationException();
	}

	@Override
	public StoredProcedureQuery createNamedStoredProcedureQuery(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public StoredProcedureQuery createStoredProcedureQuery(String procedureName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public StoredProcedureQuery createStoredProcedureQuery(String procedureName, Class... resultClasses) {
		throw new UnsupportedOperationException();
	}

	@Override
	public StoredProcedureQuery createStoredProcedureQuery(String procedureName, String... resultSetMappings) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isJoinedToTransaction() {
		return false;
	}

	@Override
	public <T> EntityGraph<T> createEntityGraph(Class<T> rootType) {
		throw new UnsupportedOperationException();
	}

	@Override
	public EntityGraph<?> createEntityGraph(String graphName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public EntityGraph<?> getEntityGraph(String graphName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) {
		throw new UnsupportedOperationException();
	}
	
	public MetaProvider getMetadataModel(){
		return parent;
	}
	
	public DbClient getDbClient(){
		return parent==null? null:parent.getDefault();
	}
}

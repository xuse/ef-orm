package org.easyframe.enterprise.spring;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

import jef.common.wrapper.Page;
import jef.database.DbUtils;
import jef.database.Field;
import jef.database.IQueryableEntity;
import jef.database.NativeQuery;
import jef.database.PagingIterator;
import jef.database.PagingIteratorObjImpl;
import jef.database.meta.AbstractMetadata;
import jef.database.meta.MetaHolder;
import jef.database.query.PKQuery;
import jef.tools.reflect.ClassEx;
import jef.tools.reflect.GenericUtils;

import org.springframework.transaction.annotation.Transactional;

/**
 * 框架提供的基本数据库操作实现<br>
 * 
 * 考虑到一般项目命名习惯等因素，建议有需要的同学自行继承BaseDao来编写通用的DAO，本类可当做是参考实现。
 * @param <T>
 */
@Transactional
public abstract class GenericDaoSupport<T extends IQueryableEntity> extends BaseDao implements GenericDao<T> {
	protected Class<T> entityClass;
	protected AbstractMetadata meta;

	/**
	 * 根据泛型参数构造
	 */
	@SuppressWarnings("unchecked")
	public GenericDaoSupport() {
		Class<?> c = getClass();
		c = ClassEx.getRealClass(c);
		Type[] t = DbUtils.getTypeParameters(c, GenericDao.class);
		Type type = t[0];
		if (type instanceof Class<?>) {
		} else if (type instanceof ParameterizedType) {
			type = GenericUtils.getRawClass(type);
		} else {
			throw new IllegalArgumentException("The class " + this.getClass().getName() + " must assign the generic type T.");
		}
		this.entityClass = (Class<T>) type;
		this.meta = MetaHolder.getMeta(entityClass);
	}

	/**
	 * 指定Metadata的构造
	 * 
	 * @param meta
	 */
	@SuppressWarnings("unchecked")
	public GenericDaoSupport(AbstractMetadata meta) {
		this.meta = meta;
		this.entityClass = (Class<T>) meta.getThisType();
	}

	/**
	 * 通过JPQL进行更新，删除等操作
	 * 
	 * @Title: updateByJPQL
	 */
	public int updateByJPQL(String jpql, Map<String, Object> params) {
		EntityManager em = getEntityManager();
		Query q = em.createQuery("jpql");
		for (String key : params.keySet()) {
			q.setParameter(key, params.get(key));
		}
		return q.executeUpdate();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ailk.easyframe.web.common.dal.IDaoCrudSupport#insert(jef.database
	 * .IQueryableEntity)
	 */
	public T insert(T entity) {
		try {
			getSession().insert(entity);
			return entity;
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage() + " " + e.getSQLState(), e);
		}
	}

	/**
	 * 
	 */
	public T insertCascade(T entity) {
		try {
			getSession().insertCascade(entity);
			return entity;
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage() + " " + e.getSQLState(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ailk.easyframe.web.common.dal.IDaoCrudSupport#merge(jef.database.
	 * IQueryableEntity)
	 */
	public T merge(T entity) {
		return getEntityManager().merge(entity);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ailk.easyframe.web.common.dal.IDaoCrudSupport#update(jef.database
	 * .IQueryableEntity)
	 */
	public int update(T entity) {
		try {
			return getSession().update(entity);
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage() + " " + e.getSQLState(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ailk.easyframe.web.common.dal.IDaoCrudSupport#updateWithoutCascade
	 * (jef.database.IQueryableEntity)
	 */
	public int updateCascade(T entity) {
		try {
			return getSession().updateCascade(entity);
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage() + " " + e.getSQLState(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ailk.easyframe.web.common.dal.IDaoCrudSupport#remove(jef.database
	 * .IQueryableEntity)
	 */
	public int remove(T entity) {
		try {
			return getSession().delete(entity);
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage() + " " + e.getSQLState(), e);
		}
	}

	public int removeByExample(T entity) {
		try {
			return getSession().delete(DbUtils.populateExampleConditions(entity));
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ailk.easyframe.web.common.dal.IDaoCrudSupport#removeWithCascade(jef
	 * .database.IQueryableEntity)
	 */
	public int removeCascade(T entity) {
		try {
			return getSession().deleteCascade(entity);
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage() + " " + e.getSQLState(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ailk.easyframe.web.common.dal.IDaoCrudSupport#load(jef.database.
	 * IQueryableEntity)
	 */
	public T load(T entity) {
		try {
			return getSession().load(entity);
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage() + " " + e.getSQLState(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.easyframe.enterprise.spring.GenericDao#get(java.io.Serializable)
	 */
	public T get(Serializable key) {
		PKQuery<T> query = new PKQuery<T>(meta, key);
		try {
			return getSession().load(query);
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage() + " " + e.getSQLState(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.easyframe.enterprise.spring.GenericDao#getAll()
	 */
	@SuppressWarnings("unchecked")
	public List<T> getAll() {
		T t = (T) meta.newInstance();
		t.getQuery().setAllRecordsCondition();
		return find(t);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ailk.easyframe.web.common.dal.IDaoCrudSupport#loadWithoutCascade(
	 * jef.database.IQueryableEntity)
	 */
	public T loadCascade(T entity) {
		try {
			entity.getQuery().setCascade(true);
			return getSession().load(entity);
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage() + " " + e.getSQLState(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ailk.easyframe.web.common.dal.IDaoCrudSupport#findByExample(jef.database
	 * .IQueryableEntity)
	 */
	public List<T> findByExample(T entity) {
		try {
			return getSession().select(DbUtils.populateExampleConditions(entity), null);
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage() + " " + e.getSQLState(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ailk.easyframe.web.common.dal.IDaoCrudSupport#find(jef.database.
	 * IQueryableEntity)
	 */
	public List<T> find(T entity) {
		try {
			return getSession().select(entity);
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage() + " " + e.getSQLState(), e);
		}
	}

	/*
	 * 
	 */
	public List<T> find(jef.database.query.Query<T> entity) {
		try {
			return getSession().select(entity);
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage() + " " + e.getSQLState(), e);
		}
	}

	public List<T> findCascade(T entity) {
		try {
			entity.getQuery().setCascade(true);
			return getSession().select(entity);
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage() + " " + e.getSQLState(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ailk.easyframe.web.common.dal.IDaoCrudSupport#findAndPage(jef.database
	 * .IQueryableEntity, int, int)
	 */
	public Page<T> findAndPage(T entity, int start, int limit) {
		Page<T> p = new Page<T>();
		try {
			PagingIteratorObjImpl<T> i = (PagingIteratorObjImpl<T>) getSession().pageSelect(entity, limit);
			// i.setRefQuery(false);
			// i.setFillVsNField(false);
			i.setOffset(start);
			p.setTotalCount(i.getTotal());
			List<T> data = i.next();
			p.setList(data);
			return p;
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage() + " " + e.getSQLState(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ailk.easyframe.web.common.dal.IDaoCrudSupport#findAndPageWithoutCascade
	 * (jef.database.IQueryableEntity, int, int)
	 */
	public Page<T> findAndPageCascade(T entity, int start, int limit) {
		Page<T> p = new Page<T>();
		try {
			PagingIteratorObjImpl<T> i = (PagingIteratorObjImpl<T>) getSession().pageSelect(entity, limit);
			i.setRefQuery(true);
			i.setFillVsNField(true);
			i.setOffset(start);
			p.setTotalCount(i.getTotal());
			List<T> data = i.next();
			p.setList(data);
			return p;
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage() + " " + e.getSQLState(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ailk.easyframe.web.common.dal.IDaoCrudSupport#find(java.lang.Class,
	 * java.lang.String, java.lang.Object[])
	 */
	public List<T> find(Class<T> entity, String query, Object... params) {
		NativeQuery<T> q = getSession().createNativeQuery(query, entity);
		for (int i = 0; i < params.length; i++) {
			q.setParameter(i, params[i]);
		}
		return q.getResultList();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ailk.easyframe.web.common.dal.IDaoCrudSupport#findAndPageByNq(java
	 * .lang.String, java.util.Map, int, int)
	 */
	public Page<T> findAndPageByNq(String nqName, Map<String, Object> params, int start, int limit) {
		Page<T> p = new Page<T>();
		try {
			NativeQuery<T> query = this.getSession().createNamedQuery(nqName, meta);
			query.setParameterMap(params);

			PagingIterator<T> i = getSession().pageSelect(query, limit);
			i.setOffset(start);
			p.setTotalCount(i.getTotal());
			List<T> data = i.next();
			p.setList(data);

			return p;
		} catch (SQLException ex) {
			throw new PersistenceException(ex.getMessage() + " " + ex.getSQLState(), ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ailk.easyframe.web.common.dal.IDaoCrudSupport#findByNq(java.lang.
	 * String, java.util.Map)
	 */
	public List<T> findByNq(String nqName, Map<String, Object> params) {
		NativeQuery<T> nQuery = getSession().createNamedQuery(nqName, meta);
		nQuery.setParameterMap(params);
		return nQuery.getResultList();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ailk.easyframe.web.common.dal.IDaoCrudSupport#executeNq(java.lang
	 * .String, java.util.Map)
	 */
	public int executeNq(String nqName, Map<String, Object> param) {
		NativeQuery<?> query = getSession().createNamedQuery(nqName);
		query.setParameterMap(param);
		return query.executeUpdate();
	}

	public int batchInsert(List<T> entities) {
		try {
			getSession().batchInsert(entities);
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage() + " " + e.getSQLState(), e);
		}
		return entities.size();
	}

	public int batchInsert(List<T> entities, boolean group) {
		try {
			getSession().batchInsert(entities, group);
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage() + " " + e.getSQLState(), e);
		}
		return entities.size();
	}

	public int batchRemove(List<T> entities) {
		try {
			getSession().executeBatchDeletion(entities);
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage() + " " + e.getSQLState(), e);
		}
		return entities.size();
	}

	public int batchRemove(List<T> entities, boolean group) {
		try {
			getSession().executeBatchDeletion(entities, group);
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage() + " " + e.getSQLState(), e);
		}
		return entities.size();
	}

	public int batchUpdate(List<T> entities) {
		try {
			getSession().batchUpdate(entities);
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage() + " " + e.getSQLState(), e);
		}
		return entities.size();
	}

	public int batchUpdate(List<T> entities, boolean group) {
		try {
			getSession().batchUpdate(entities, group);
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage() + " " + e.getSQLState(), e);
		}
		return entities.size();
	}

	public int executeQuery(String sql, Map<String, Object> param) {
		NativeQuery<?> query = getSession().createNativeQuery(sql);
		query.setParameterMap(param);
		return query.executeUpdate();
	}

	public List<T> findByQuery(String sql, Map<String, Object> param) {
		NativeQuery<T> query = getSession().createNativeQuery(sql, meta);
		query.setParameterMap(param);
		return query.getResultList();
	}

	public T loadByField(String fieldname, Serializable id) {
		Field field = meta.getField(fieldname);
		if (field == null) {
			throw new IllegalArgumentException("There's no property named " + fieldname + " in type of " + meta.getName());
		}
		@SuppressWarnings("unchecked")
		T q = (T) meta.newInstance();
		q.getQuery().addCondition(field, id);
		try {
			return getSession().load(q);
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
	}

	public List<T> findByField(String fieldname, Serializable id) {
		Field field = meta.getField(fieldname);
		if (field == null) {
			throw new IllegalArgumentException("There's no property named " + fieldname + " in type of " + meta.getName());
		}
		try {
			return getSession().loadByField(field, id);
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
	}

	public int deleteByField(String fieldname, Serializable key) {
		Field field = meta.getField(fieldname);
		if (field == null) {
			throw new IllegalArgumentException("There's no property named " + fieldname + " in type of " + meta.getName());
		}
		try {
			return getSession().deleteByField(field, key);
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
	}

	@Override
	public List<T> batchLoad(List<? extends Serializable> pkValues) {
		try {
			return getSession().batchLoad(meta, pkValues);
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
	}

	@Override
	public int batchDelete(List<? extends Serializable> pkValues) {
		try {
			return getSession().batchDelete(meta, pkValues);
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
	}

	@Override
	public List<T> batchLoadByField(String fieldname, List<? extends Serializable> values) {
		Field field = meta.getField(fieldname);
		if (field == null) {
			throw new IllegalArgumentException("There's no property named " + fieldname + " in type of " + meta.getName());
		}
		try {
			return getSession().batchLoadByField(field, values);
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
	}

	@Override
	public int batchDeleteByField(String fieldname, List<? extends Serializable> values) {
		Field field = meta.getField(fieldname);
		if (field == null) {
			throw new IllegalArgumentException("There's no property named " + fieldname + " in type of " + meta.getName());
		}
		try {
			return getSession().batchDeleteByField(field, values);
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
	}

}

package org.easyframe.enterprise.spring;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import javax.persistence.PersistenceException;

import jef.common.log.LogUtil;
import jef.database.DbUtils;
import jef.database.IQueryableEntity;
import jef.database.PojoWrapper;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.database.query.Query;
import jef.tools.reflect.ClassWrapper;
import jef.tools.reflect.GenericUtils;

public class POJODaoSupport<T> extends BaseDao{
	protected Class<T> entityClass;

	/**
	 * 根据泛型参数构造
	 */
	@SuppressWarnings("unchecked")
	public POJODaoSupport() {
		Class<?> c = getClass();
		c = ClassWrapper.getRealClass(c);
		Type[] t = GenericUtils.getTypeParameters(c, POJODaoSupport.class);
		Type type = t[0];
		if (type instanceof Class<?>) {
		} else if (type instanceof ParameterizedType) {
			type = GenericUtils.getRawClass(type);
		} else {
			throw new IllegalArgumentException("The class " + this.getClass().getName() + " must assign the generic type T.");
		}
		this.entityClass = (Class<T>) type;
	}
	
	public T insert(T entity) {
		if (entity == null)
			return null;
		try {
			if (entity instanceof IQueryableEntity) {
				getSession().insertCascade((IQueryableEntity) entity);
			} else {
				ITableMetadata meta = MetaHolder.getMeta(entity.getClass());
				getSession().insertCascade(meta.transfer(entity, false));
			}
			return entity;
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage() + " " + e.getSQLState(), e);
		}
	}

	public int update(T entity) {
		if (entity == null)
			return 0;
		try {
			if (entity instanceof IQueryableEntity) {
				return getSession().update((IQueryableEntity) entity);
			} else {
				ITableMetadata meta = MetaHolder.getMeta(entity);
				PojoWrapper pojo = meta.transfer(entity, true);
				return getSession().update(pojo);
			}
		} catch (SQLException e) {
			LogUtil.exception(e);
			throw new PersistenceException(e.getMessage() + " " + e.getSQLState(), e);
		}
	}

	public void remove(T entity) {
		super.getEntityManager().remove(entity);
	}


	public T merge(T entity) {
		super.getEntityManager().remove(entity);
		return entity;
	}

	public T load(T entity) {
		if (entity == null)
			return null;
		try {
			if (entity instanceof IQueryableEntity) {
				return (T) getSession().load((IQueryableEntity) entity);
			} else {
				ITableMetadata meta = MetaHolder.getMeta(entity.getClass());
				PojoWrapper vw = getSession().load(meta.transfer(entity, true));
				return vw == null ? null : (T) vw.get();
			}
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage() + " " + e.getSQLState(), e);
		}
	}

	@SuppressWarnings("unchecked")
	public int removeByExample(T entity,String... properties) {
		try {
			if (entity instanceof IQueryableEntity) {
				return getSession().delete(DbUtils.populateExampleConditions((IQueryableEntity) entity, properties));
			} else {
				ITableMetadata meta = MetaHolder.getMeta(entity.getClass());
				Query<PojoWrapper> q;
				if (properties.length == 0) {
					q = (Query<PojoWrapper>) meta.transfer(entity, true).getQuery();
				} else {
					q = DbUtils.populateExampleConditions(meta.transfer(entity, false), properties);
				}
				return getSession().delete(q);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
	}

	public List<T> find(T obj) {
		if (obj == null)
			return Collections.emptyList();
		try {
			if (obj instanceof IQueryableEntity) {
				return (List<T>) getSession().select((IQueryableEntity) obj);
			} else {
				ITableMetadata meta = MetaHolder.getMeta(obj);
				PojoWrapper pojo = meta.transfer(obj, true);
				if (!pojo.hasQuery() && pojo.getUpdateValueMap().isEmpty()) {
					pojo.getQuery().setAllRecordsCondition();
				}
				List<PojoWrapper> result = getSession().select(pojo);
				return PojoWrapper.unwrapList(result);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage() + " " + e.getSQLState(), e);
		}
	}

	public List<T> getAll() {
		T t;
		try {
			t = (T) entityClass.newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
			throw new PersistenceException(e);
		} catch (IllegalAccessException e) {
			throw new PersistenceException(e);
		}
		return find(t);
	}

	public int batchInsert(List<T> entities) {
		if (entities == null || entities.isEmpty())
			return 0;

		try {
			T t = entities.get(0);
			if (t instanceof IQueryableEntity) {
				getSession().batchInsert((List<IQueryableEntity>) entities, null);
			} else {
				List<PojoWrapper> list = PojoWrapper.wrap(entities, false);
				getSession().batchInsert(list, null);
			}
			return entities.size();
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage() + " " + e.getSQLState(), e);
		}
	}

	public int batchRemove(List<T> entities) {
		if (entities == null || entities.isEmpty())
			return 0;

		try {
			T t = entities.get(0);
			if (t instanceof IQueryableEntity) {
				getSession().executeBatchDeletion((List<IQueryableEntity>) entities, null);
			} else {
				List<PojoWrapper> list = PojoWrapper.wrap(entities, false);
				getSession().executeBatchDeletion(list, null);
			}
			return entities.size();
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage() + " " + e.getSQLState(), e);
		}
	}

	public int batchUpdate(List<T> entities) {
		if (entities == null || entities.isEmpty())
			return 0;

		try {
			T t = entities.get(0);
			if (t instanceof IQueryableEntity) {
				getSession().batchUpdate((List<IQueryableEntity>) entities, null);
			} else {
				List<PojoWrapper> list = PojoWrapper.wrap(entities, false);
				getSession().batchUpdate(list, null);
			}
			return entities.size();
		} catch (SQLException e) {
			throw new PersistenceException(e.getMessage() + " " + e.getSQLState(), e);
		}
	}

}

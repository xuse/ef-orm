package org.easyframe.enterprise.spring;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.EntityManagerFactory;

import jef.common.log.LogUtil;
import jef.common.wrapper.Page;
import jef.database.DbClient;
import jef.database.DbUtils;
import jef.database.Field;
import jef.database.IQueryableEntity;
import jef.database.NativeQuery;
import jef.database.PagingIterator;
import jef.database.PojoWrapper;
import jef.database.QB;
import jef.database.RecordHolder;
import jef.database.RecordsHolder;
import jef.database.dialect.type.ColumnMapping;
import jef.database.jpa.JefEntityManagerFactory;
import jef.database.meta.EntityType;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.database.query.Query;
import jef.database.wrapper.ResultIterator;
import jef.tools.Assert;
import jef.tools.reflect.BeanWrapper;

import com.querydsl.sql.SQLQuery;

/**
 * 一个通用的DAO实现，涵盖了Session()对象中大部分数据库操作的方法。。<br>
 * 考虑到一般项目命名习惯等因素，建议有需要的同学自行继承BaseDao来编写通用的DAO，本类可当做是参考实现。
 * 
 * @see CommonDao
 * @author Administrator
 * 
 */
public class CommonDaoImpl extends BaseDao implements CommonDao {
	public void persist(Object entity) {
		super.getEntityManager().persist(entity);
	}

	public <T> T merge(T entity) {
		return super.getEntityManager().merge(entity);
	}

	public int remove(Object entity) {
		try {
			return getSession().delete(entity);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	public int removeCascade(Object entity) {
		try {
			return getSession().deleteCascade(entity);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	/**
	 * 空构造，一般用于在SpringFramework中初始化此Bean
	 */
	public CommonDaoImpl() {
	}

	public CommonDaoImpl(EntityManagerFactory emf) {
		this.setEntityManagerFactory(emf);
	}

	/**
	 * 构造
	 * 
	 * @param db
	 */
	public CommonDaoImpl(DbClient db) {
		this.setEntityManagerFactory(new JefEntityManagerFactory(db));
	}

	@SuppressWarnings("unchecked")
	public <T> int removeByExample(T entity, String... properties) {
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
			throw DbUtils.toRuntimeException(e);
		}
	}

	public <T> T loadByPrimaryKey(Class<T> entityClass, Serializable primaryKey) {
		try {
			return getSession().load(entityClass, primaryKey);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	@Override
	public <T> List<T> loadByPrimaryKeys(Class<T> entityClass, List<? extends Serializable> primaryKey) {
		try {
			return getSession().batchLoad(entityClass, primaryKey);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> find(T obj) {
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
			throw DbUtils.toRuntimeException(e);
		}
	}

	public <T> int updateCascade(T entity) {
		if (entity == null)
			return 0;
		try {
			return getSession().updateCascade(entity);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.easyframe.enterprise.spring.CommonDao#update(java.lang.Object)
	 */
	public <T> int update(T entity) {
		if (entity == null)
			return 0;
		try {
			return getSession().update(entity);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.easyframe.enterprise.spring.CommonDao#updateByProperty(java.lang.Object, java.lang.String[])
	 */
	public <T> int updateByProperty(T entity, String... property) {
		if (entity == null)
			return 0;
		if (property.length == 0) {
			return update(entity);
		}
		try {
			IQueryableEntity ent;
			if (!(entity instanceof IQueryableEntity)) {
				ITableMetadata meta = MetaHolder.getMeta(entity);
				ent = meta.transfer(entity, true);

			} else {
				ent = (IQueryableEntity) entity;
			}
			if (ent.getUpdateValueMap() == null || ent.getUpdateValueMap().isEmpty()) {
				DbUtils.fillUpdateMap(ent);
			}
			BeanWrapper bw = BeanWrapper.wrap(ent);
			Query<?> qq = ent.getQuery();
			ITableMetadata meta = qq.getMeta();
			for (String s : property) {
				ColumnMapping field = meta.findField(s);
				if (field == null) {
					throw new IllegalArgumentException(s + " not found database field in entity " + bw.getClassName());
				}
				ent.getUpdateValueMap().remove(field.field());
				qq.addCondition(field.field(), bw.getPropertyValue(s));
			}
			return getSession().update(qq.getInstance());
		} catch (SQLException e) {
			LogUtil.exception(e);
			throw DbUtils.toRuntimeException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.easyframe.enterprise.spring.CommonDao#update(java.lang.Object, java.util.Map, java.lang.String[])
	 */
	public <T> int update(T entity, Map<String, Object> setValues, String... property) {
		try {
			IQueryableEntity ent;
			if (!(entity instanceof IQueryableEntity)) {
				ITableMetadata meta = MetaHolder.getMeta(entity);
				ent = meta.transfer(entity, true);

			} else {
				ent = (IQueryableEntity) entity;
			}

			Query<?> qq = ent.getQuery();
			ITableMetadata meta = qq.getMeta();
			ent.clearUpdate();
			for (Entry<String, Object> entry : setValues.entrySet()) {
				ColumnMapping field = meta.findField(entry.getKey());
				if (field == null) {
					throw new IllegalArgumentException(entry.getKey() + " not found database field in entity " + meta.getName());
				}
				ent.prepareUpdate(field.field(), entry.getValue());
			}
			if (property.length == 0) {
				return update(entity);
			}
			// 准备where条件
			BeanWrapper bw = BeanWrapper.wrap(ent);
			for (String s : property) {
				ColumnMapping field = meta.findField(s);
				if (field == null) {
					throw new IllegalArgumentException(s + " not found database field in entity " + bw.getClassName());
				}
				qq.addCondition(field.field(), bw.getPropertyValue(s));
			}
			return getSession().update(qq.getInstance());
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	public <T> T insert(T entity) {
		try {
			getSession().insert(entity);
			return entity;
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	public <T> T insertCascade(T entity) {
		try {
			getSession().insertCascade(entity);
			return entity;
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	public <T> T load(T entity) {
		return load(entity, true);
	}

	public <T> T load(T entity, boolean unique) {
		if (entity == null)
			return null;
		try {
			return getSession().load(entity, unique);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	public <T> Page<T> findAndPage(T entity, int start, int limit) {
		if (entity == null)
			return null;
		try {
			return getSession().selectPage(entity, start, limit);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	public <T> Page<T> findAndPageByQuery(String sql, Class<T> retutnType, Map<String, Object> params, int start, int limit) {
		try {
			NativeQuery<T> query = this.getSession().createNativeQuery(sql, retutnType);
			query.setParameterMap(params);
			return getSession().pageSelect(query, limit).setOffset(start).getPageData();
		} catch (Exception ex) {
			throw DbUtils.toRuntimeException(ex);
		}
	}

	public <T> Page<T> findAndPageByQuery(String sql, ITableMetadata retutnType, Map<String, Object> params, int start, int limit) {
		try {
			NativeQuery<T> query = this.getSession().createNativeQuery(sql, retutnType);
			query.setParameterMap(params);
			return getSession().pageSelect(query, limit).setOffset(start).getPageData();
		} catch (Exception ex) {
			throw DbUtils.toRuntimeException(ex);
		}
	}

	public <T> List<T> findByNq(String nqName, Class<T> type, Map<String, Object> params) {
		try {
			NativeQuery<T> nQuery = getSession().createNamedQuery(nqName, type);
			nQuery.setParameterMap(params);

			return nQuery.getResultList();
		} catch (Exception ex) {
			throw DbUtils.toRuntimeException(ex);
		}
	}

	public <T> Page<T> findAndPageByNq(String nqName, Class<T> type, Map<String, Object> params, int start, int limit) {
		try {
			NativeQuery<T> query = this.getSession().createNamedQuery(nqName, type);
			query.setParameterMap(params);

			PagingIterator<T> i = getSession().pageSelect(query, limit);
			return i.setOffset(start).getPageData();
		} catch (Exception ex) {
			throw DbUtils.toRuntimeException(ex);
		}
	}

	public <T> List<T> findByNq(String nqName, ITableMetadata meta, Map<String, Object> params) {
		try {
			@SuppressWarnings("unchecked")
			NativeQuery<T> query = (NativeQuery<T>) getSession().createNamedQuery(nqName, meta);
			query.setParameterMap(params);
			return query.getResultList();
		} catch (Exception ex) {
			throw DbUtils.toRuntimeException(ex);
		}
	}

	public <T> Page<T> findAndPageByNq(String nqName, ITableMetadata meta, Map<String, Object> params, int start, int limit) {
		try {
			@SuppressWarnings("unchecked")
			NativeQuery<T> query = (NativeQuery<T>) getSession().createNamedQuery(nqName, meta);
			query.setParameterMap(params);
			PagingIterator<T> i = getSession().pageSelect(query, limit);
			return i.setOffset(start).getPageData();
		} catch (Exception ex) {
			throw DbUtils.toRuntimeException(ex);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.easyframe.enterprise.spring.CommonDao#findByExample(java.lang.Object,
	 * java.lang.String[])
	 */
	public <T> List<T> findByExample(T entity, String... propertyName) {
		if (entity == null) {
			return Collections.emptyList();
		}
		try {
			return getSession().selectByExample(entity, propertyName);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
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
		return query.executeUpdate();
	}

	public int executeQuery(String sql, Map<String, Object> param) {
		if (sql == null) {
			return 0;
		}
		NativeQuery<?> query = getSession().createNativeQuery(sql);
		query.setParameterMap(param);
		return query.executeUpdate();
	}

	public <T> List<T> findByQuery(String sql, Class<T> type, Map<String, Object> params) {
		if (sql == null || type == null) {
			return Collections.emptyList();
		}
		NativeQuery<T> query = getSession().createNativeQuery(sql, type);
		query.setParameterMap(params);
		return query.getResultList();
	}

	public <T> List<T> findByQuery(String sql, ITableMetadata retutnType, Map<String, Object> params) {
		if (sql == null || retutnType == null) {
			return Collections.emptyList();
		}
		NativeQuery<T> query = getSession().createNativeQuery(sql, retutnType);
		query.setParameterMap(params);
		return query.getResultList();
	}

	public void removeByProperty(ITableMetadata meta, String propertyName, List<?> values) {
		if (meta == null || propertyName == null || values == null || values.isEmpty())
			return;
		Assert.notNull(meta);
		List<IQueryableEntity> objs = new ArrayList<IQueryableEntity>();
		for (Object o : values) {
			IQueryableEntity t;
			try {
				t = meta.newInstance();
				t.startUpdate();
			} catch (Exception e) {
				throw new IllegalArgumentException(e);
			}
			BeanWrapper bw = BeanWrapper.wrap(t);
			bw.setPropertyValue(propertyName, o);
			objs.add(t);
		}
		try {
			getSession().executeBatchDeletion(objs);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	public void removeAll(ITableMetadata meta) {
		if (meta == null)
			return;
		try {
			this.getSession().delete(QB.create(meta));
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T loadByPrimaryKey(ITableMetadata meta, Object id) {
		if (meta == null || id == null)
			return null;
		try {
			if (meta.getType() == EntityType.POJO) {
				IQueryableEntity bean = meta.newInstance();
				DbUtils.setPrimaryKeyValue(bean, id);
				PojoWrapper wrapper = (PojoWrapper) getSession().load(bean, true);
				return (T) wrapper.get();
			} else {
				return (T) getSession().load(meta, (Serializable) id);
			}

		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	public List<?> findByField(ITableMetadata meta, String propertyName, Object value) {
		try {
			return getSession().selectByField(meta, propertyName, value);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	public <T> List<T> findByField(Class<T> meta, String propertyName, Object value) {
		try {
			return getSession().selectByField(meta, propertyName, value);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public <T> ResultIterator<T> iterate(T obj) {
		if (obj == null)
			return null;
		try {
			if (obj instanceof IQueryableEntity) {
				return (ResultIterator<T>) getSession().iteratedSelect((IQueryableEntity) obj, null);
			} else {
				ITableMetadata meta = MetaHolder.getMeta(obj);
				PojoWrapper pojo = meta.transfer(obj, true);
				if (!pojo.hasQuery() && pojo.getUpdateValueMap().isEmpty()) {
					pojo.getQuery().setAllRecordsCondition();
				}
				ResultIterator<PojoWrapper> result = getSession().iteratedSelect(pojo, null);
				return PojoWrapper.unwrapIterator(result);
			}
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	public <T> ResultIterator<T> iterateByQuery(String sql, Class<T> returnType, Map<String, Object> params) {
		if (sql == null || returnType == null)
			return null;
		NativeQuery<T> query = getSession().createNativeQuery(sql, returnType);
		query.setParameterMap(params);
		return query.getResultIterator();
	}

	public <T> ResultIterator<T> iterateByQuery(String sql, ITableMetadata returnType, Map<String, Object> params) {
		if (sql == null || returnType == null)
			return null;
		NativeQuery<T> query = getSession().createNativeQuery(sql, returnType);
		query.setParameterMap(params);
		return query.getResultIterator();
	}

	public DbClient getNoTransactionSession() {
		return getSession().getNoTransactionSession();
	}

	public <T> int batchInsert(List<T> entities) {
		return batchInsert(entities, null);
	}

	public <T> int extremeInsert(List<T> entities) {
		try {
			getSession().extremeInsert(entities, null);
			return entities.size();
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	public <T> int batchInsert(List<T> entities, Boolean doGroup) {
		try {
			getSession().batchInsert(entities, doGroup);
			return entities.size();
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	public <T> int batchDelete(List<T> entities) {
		return batchDelete(entities, null);
	}

	public <T> int batchDelete(List<T> entities, Boolean doGroup) {
		try {
			return getSession().batchDelete(entities, doGroup == null ? false : doGroup);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	public <T> int batchUpdate(List<T> entities) {
		return batchUpdate(entities, null);
	}

	public <T> int batchUpdate(List<T> entities, Boolean doGroup) {
		try {
			return getSession().batchUpdate(entities, doGroup);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	@Override
	public List<?> findByField(ITableMetadata meta, String propertyName, List<? extends Serializable> value) {
		Field field = meta.getField(propertyName);
		if (field == null) {
			throw new IllegalArgumentException("There's no property named " + propertyName + " in type of " + meta.getName());
		}
		try {
			return (List<?>) getSession().batchLoadByField(field, value);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	@Override
	public <T> T load(Class<T> entityClass, Serializable... id) {
		try {
			return getSession().load(entityClass, id);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	@Override
	public <T extends IQueryableEntity> List<T> find(Query<T> data) {
		try {
			return getSession().select(data);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T loadByField(ITableMetadata meta, String field, Serializable value, boolean unique) {
		try {
			return (T) getSession().loadByField(meta, field, value, unique);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends IQueryableEntity> T loadByField(Field field, Object value) {
		try {
			return (T) getSession().loadByField(field, value);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends IQueryableEntity> T loadByField(Field field, Object value, boolean unique) {
		try {
			return (T) getSession().loadByField(field, value, unique);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T loadByField(Class<T> clz, String field, Serializable value, boolean unique) {
		if (clz == null || field == null) {
			return null;
		}
		ITableMetadata meta = MetaHolder.getMeta(clz);
		ColumnMapping def = meta.findField(field);
		if (def == null) {
			throw new IllegalArgumentException("There's no field [" + field + "] in " + clz.getName());
		}
		try {
			return (T) getSession().loadByField(def.field(), value, unique);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	@Override
	public <T> int removeByField(Class<T> clz, String field, Serializable value) {
		if (clz == null || field == null) {
			return 0;
		}
		ITableMetadata meta = MetaHolder.getMeta(clz);
		ColumnMapping def = meta.findField(field);
		if (def == null) {
			throw new IllegalArgumentException("There's no field [" + field + "] in " + clz.getName());
		}
		try {
			return getSession().deleteByField(def.field(), value);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	public int removeByKey(ITableMetadata meta, String fieldname, Serializable key) {
		if (meta == null || fieldname == null)
			return 0;
		Field field = meta.getField(fieldname);
		if (field == null) {
			throw new IllegalArgumentException("There's no property named " + fieldname + " in type of " + meta.getName());
		}
		Query<?> query = QB.create(meta);
		query.addCondition(field, key);
		try {
			return getSession().delete(query);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	@Override
	public int removeByField(ITableMetadata meta, String field, Serializable value) {
		if (meta == null || field == null) {
			return 0;
		}
		ColumnMapping def = meta.findField(field);
		if (def == null) {
			throw new IllegalArgumentException("There's no field [" + field + "] in " + meta.getName());
		}
		try {
			return getSession().deleteByField(def.field(), value);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	@Override
	public <T extends IQueryableEntity> RecordsHolder<T> selectForUpdate(Query<T> query) {
		try {
			return getSession().selectForUpdate(query.getInstance());
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	@Override
	public <T extends IQueryableEntity> RecordHolder<T> loadForUpdate(Query<T> query) {
		try {
			return getSession().loadForUpdate(query.getInstance());
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}
	
	public <T extends IQueryableEntity> RecordHolder<T> loadForUpdate(T query) {
		try {
			return getSession().loadForUpdate(query);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}
	

    @Override
    public SQLQuery sql() {
        return getSession().sql();
    }
}

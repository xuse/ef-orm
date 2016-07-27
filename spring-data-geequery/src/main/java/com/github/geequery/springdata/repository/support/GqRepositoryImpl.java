/*
 * Copyright 2008-2016 the original author or authors.
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
package com.github.geequery.springdata.repository.support;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;

import jef.common.wrapper.IntRange;
import jef.database.DbClient;
import jef.database.DbUtils;
import jef.database.Field;
import jef.database.QB;
import jef.database.Session;
import jef.database.dialect.type.ColumnMapping;
import jef.database.jpa.JefEntityManager;
import jef.database.query.ConditionQuery;
import jef.database.query.Query;
import jef.database.query.SqlExpression;
import jef.tools.ArrayUtils;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.orm.jpa.EntityManagerProxy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.github.geequery.springdata.repository.GqRepository;
import com.github.geequery.springdata.repository.JpaSpecificationExecutor;

/**
 * Default implementation of the
 * {@link org.springframework.data.repository.CrudRepository} interface. This
 * will offer you a more sophisticated interface than the plain
 * {@link EntityManager} .
 * 
 * @author Jiyi
 * @param <T>
 *            the type of the entity to handle
 * @param <ID>
 *            the type of the entity's identifier
 */
@Repository
@Transactional(readOnly = true)
public class GqRepositoryImpl<T, ID extends Serializable> implements GqRepository<T, ID>, JpaSpecificationExecutor<T> {

	private MetamodelInformation<T> meta;
	// 这是Spring的SharedEntityManager的代理，只可从中提取EMF，不可直接转换，因此这个EM上携带了基于线程的事务上下文
	private EntityManagerProxy em;

	private final Query<?> q_all;

	public GqRepositoryImpl(MetamodelInformation<T> meta, EntityManagerProxy emf) {
		this.meta = meta;
		this.em = emf;
		q_all = QB.create(meta.getMetadata());
	}

	@Override
	public Page<T> findAll(Pageable pageable) {
		Session s = getSession();
		try {
			long total = s.count(q_all);
			List<T> result = s.select(q_all, toRange(pageable));
			return new PageImpl<T>(result, pageable, total);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	@Override
	public long count() {
		Session s = getSession();
		try {
			long total = s.count(q_all);
			return total;
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <S extends T> S findOne(Example<S> example) {
		return (S) findOne(toQuery(example));
	}

	@SuppressWarnings("unchecked")
	@Override
	public <S extends T> Page<S> findAll(Example<S> example, Pageable pageable) {
		return (Page<S>) findAll(toQuery(example), pageable);
	}

	@Override
	public <S extends T> long count(Example<S> example) {
		Session s = getSession();
		try {
			return s.count(toQuery(example));
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	@Override
	public <S extends T> boolean exists(Example<S> example) {
		Session s = getSession();
		try {
			return s.count(toQuery(example)) > 0;
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	@Override
	public T findOne(ConditionQuery spec) {
		Session s = getSession();
		try {
			return s.load(spec);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	@Override
	public List<T> findAll(ConditionQuery spec) {
		Session s = getSession();
		try {
			return s.select(spec, null);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	@Override
	public Page<T> findAll(ConditionQuery spec, Pageable pageable) {
		Session s = getSession();
		try {
			long count = s.countLong(spec);
			setSortToSpec(spec, pageable.getSort());
			List<T> result = s.select(spec, toRange(pageable));
			return new PageImpl<T>(result, pageable, count);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	@Override
	public List<T> findAll(ConditionQuery spec, Sort sort) {
		Session s = getSession();
		try {
			setSortToSpec(spec, sort);
			return s.selectAs(spec, meta.getJavaType());
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	@Override
	public long count(ConditionQuery spec) {
		Session s = getSession();
		try {
			return s.countLong(spec);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<T> findAll() {
		Session s = getSession();
		try {
			return (List<T>) s.select(q_all);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<T> findAll(Sort sort) {
		Session s = getSession();
		try {
			@SuppressWarnings("rawtypes")
			Query q_all = QB.create(meta.getMetadata());
			setSortToSpec(q_all, sort);
			return s.select(q_all);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	@Override
	public T getOne(ID id) {
		if (id == null)
			return null;
		Session s = getSession();
		try {
			Serializable[] ids = toId(id);
			return s.load(meta.getMetadata(), ids);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <S extends T> List<S> findAll(Example<S> example) {
		Session s = getSession();
		try {
			return (List<S>) s.select(toQuery(example));
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <S extends T> List<S> findAll(Example<S> example, Sort sort) {
		Session s = getSession();
		try {
			Query<?> query = toQuery(example);
			setSortToSpec(query, sort);
			return (List<S>) s.select(query);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	@Override
	public T findOne(ID id) {
		return getOne(id);
	}

	@Override
	public boolean exists(ID id) {
		return getOne(id) != null;
	}

	@Override
	public Iterable<T> findAll(Iterable<ID> ids) {
		Session s = getSession();
		try {
			return s.batchLoad(meta.getMetadata(), asList(ids));
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	@Override
	public void flush() {
	}

	// /////////////////////////////以此为界，读方法在上，写方法在下///////////////////////////////////

	@Override
	@Transactional
	public <S extends T> S save(S entity) {
		return getEntityManager().merge(entity);
	}

	@Override
	@Transactional
	public void delete(T entity) {
		Session s = getSession();
		try {
			s.delete(entity);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	@Override
	@Transactional
	public void delete(Iterable<? extends T> entities) {
		Session s = getSession();
		try {
			for (T t : entities) {
				s.delete(t);
			}
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	@Override
	@Transactional
	public void deleteAll() {
		Session s = getSession();
		try {
			s.delete(q_all);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	@Override
	@Transactional
	public <S extends T> List<S> save(Iterable<S> entities) {
		Session s = getSession();
		try {
			List<S> result = asList(entities);
			s.batchInsert(result);
			return result;
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	@Override
	@Transactional
	public <S extends T> S saveAndFlush(S entity) {
		return save(entity);
	}

	@Override
	@Transactional
	public void deleteInBatch(Iterable<T> entities) {
		Session s = getSession();
		try {
			s.batchDelete(asList(entities));
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	@Override
	@Transactional
	public void deleteAllInBatch() {
		DbClient db = getNoTransactionSession();
		try {
			db.truncate(meta.getMetadata());
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	@Override
	@Transactional
	public void delete(ID id) {
		Session s = getSession();
		try {
			s.delete(meta.getMetadata(), id);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	/////////////////////////////////////////////////////
	
	private <S> List<S> asList(Iterable<S> entities) {
		List<S> list = new ArrayList<S>();
		for (Iterator<S> iter = entities.iterator(); iter.hasNext();) {
			list.add(iter.next());
		}
		return list;
	}

	@SuppressWarnings("unchecked")
	private Serializable[] toId(ID id) {
		Serializable[] ids;
		if (id.getClass().isArray()) {
			ids = ArrayUtils.toObject(id, Serializable.class);
		} else if (Collection.class.isAssignableFrom(id.getClass())) {

			Collection<? extends Serializable> cs = (Collection<? extends Serializable>) id;
			ids = ((Collection<? extends Serializable>) id).toArray(new Serializable[cs.size()]);
		} else {
			ids = new Serializable[] { id };
		}
		return ids;
	}

	private void setSortToSpec(ConditionQuery spec, Sort sort) {
		for (Order order : sort) {
			Field field;
			ColumnMapping column = this.meta.getMetadata().findField(order.getProperty());
			if (column == null) {
				field = new SqlExpression(order.getProperty());
			} else {
				field = column.field();
			}
			spec.addOrderBy(order.isAscending(), field);
		}
	}

	private <S extends T> Query<?> toQuery(Example<S> example) {
		return QueryByExamplePredicateBuilder.getPredicate(meta.getMetadata(), example);
	}

	private Session getSession() {
		JefEntityManager jem = (JefEntityManager) em.getTargetEntityManager();
		return jem.getSession();
	}

	private DbClient getNoTransactionSession() {
		JefEntityManager jem = (JefEntityManager) em.getTargetEntityManager();
		return jem.getDbClient();
	}

	private EntityManager getEntityManager() {
		EntityManager jem = em.getTargetEntityManager();
		return jem;
	}

	private IntRange toRange(Pageable pageable) {
		return new IntRange(pageable.getOffset() + 1, pageable.getOffset() + pageable.getPageSize());
	}
}

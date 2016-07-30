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
package com.github.geequery.springdata.repository.query;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import jef.common.wrapper.IntRange;
import jef.database.DbUtils;
import jef.database.Field;
import jef.database.IConditionField.And;
import jef.database.IConditionField.Or;
import jef.database.QB;
import jef.database.Session;
import jef.database.dialect.type.ColumnMapping;
import jef.database.jpa.JefEntityManager;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.database.query.ConditionQuery;
import jef.database.query.Query;
import jef.database.query.SqlExpression;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.repository.query.AbstractJpaQuery;
import org.springframework.data.jpa.repository.query.PartTreeJpaQuery;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.data.repository.query.parser.PartTree.OrPart;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.EntityManagerProxy;
import org.springframework.util.Assert;

import com.github.geequery.springdata.provider.PersistenceProvider;
import com.github.geequery.springdata.repository.query.GqParameters.JpaParameter;
import com.github.geequery.springdata.repository.query.GqQueryExecution.DeleteExecution;

/**
 * A {@link AbstractJpaQuery} implementation based on a {@link PartTree}.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public class GqPartTreeQuery extends AbstractGqQuery {

	private final ITableMetadata metadata;
	private final PartTree tree;
	private final GqParameters parameters;
	private final EntityManagerProxy em;

	/**
	 * Creates a new {@link PartTreeJpaQuery}.
	 * 
	 * @param method
	 *            must not be {@literal null}.
	 * @param factory
	 *            must not be {@literal null}.
	 * @param em
	 *            must not be {@literal null}.
	 */
	public GqPartTreeQuery(GqQueryMethod method, EntityManagerProxy em, PersistenceProvider persistenceProvider) {
		super(method, em);
		this.em = em;
		this.metadata = MetaHolder.getMeta(method.getEntityInformation().getJavaType());
		this.tree = new PartTree(method.getName(), metadata.getThisType());
		this.parameters = method.getParameters();
		// boolean recreationRequired = parameters.hasDynamicProjection() ||
		// parameters.potentiallySortsDynamically();
		// boolean isCount = tree.isCountProjection();
	}

	@Override
	protected GqQueryExecution getExecution() {
		return this.tree.isDelete() ? new DeleteExecution(em) : super.getExecution();
	}

	private Query<?> createQuery(Object[] values, boolean withPageSort) {
		Query<?> q = QB.create(metadata);
		ParametersParameterAccessor accessor = new ParametersParameterAccessor(parameters, values);
		Or or = new Or();
		int index = 0;
		for (OrPart node : tree) {
			And and = new And();
			for (Part part : node) {
				PropertyPath path = part.getProperty();
				if (path.getOwningType().getType() != metadata.getThisType()) {
					throw new IllegalArgumentException("PathType:" + path.getOwningType().getType() + "  metadata:" + metadata.getThisType());
				}
				String fieldName = path.getSegment();
				ColumnMapping field = metadata.findField(fieldName);

				Object obj = accessor.getBindableValue(getBindParamIndex(index++, fieldName));
				boolean required = part.getParameterRequired();
				if (required) {
					Assert.notNull(obj);
				}

				if (field != null) {
					add(and, part, field.field(), obj);
				}
			}
			or.addCondition(and);
		}
		q.addCondition(or);

		if (withPageSort) {
			Sort sort = tree.getSort();
			if (accessor.getSort() != null) {
				sort = accessor.getSort();
			}
			Pageable page = accessor.getPageable();
			if (page != null && page.getSort() != null) {
				sort = page.getSort();
			}
			if (sort != null)
				setSortToSpec(q, sort, metadata);
		}
		return q;
	}

	//FIXME It can be a Parameter binder to cache..
	private int getBindParamIndex(int index,String fieldName) {
		int i=0;
		for(JpaParameter param: this.parameters){
			if(param.getName()==null){
				if(index==param.getIndex()){
					return i;
				}
			}else{
				if(fieldName.equals(param.getName())){
					return i;
				}
			}
			i++;
		}
		throw new NoSuchElementException("Can not found bind parameter '"+fieldName+"' in method "+this.getQueryMethod().getName());
	}

	private void add(And and, Part part, Field field, Object value) {

		switch (part.getType()) {
		case SIMPLE_PROPERTY:
			and.addCondition(QB.eq(field, value));
			break;
		case BETWEEN:
			// and.addCondition(QB.between(field, begin, end));
			throw new UnsupportedOperationException();
		case ENDING_WITH:
			and.addCondition(QB.matchEnd(field, String.valueOf(value)));
			break;
		case STARTING_WITH:
			and.addCondition(QB.not(QB.matchStart(field, String.valueOf(value))));
			break;
		case CONTAINING:
			and.addCondition(QB.matchAny(field, String.valueOf(value)));
			break;
		case GREATER_THAN:
			and.addCondition(QB.gt(field, value));
			break;
		case GREATER_THAN_EQUAL:
			and.addCondition(QB.ge(field, value));
			break;
		case IN:
			and.addCondition(QB.in(field, (Object[]) value));
			break;
		case IS_NOT_NULL:
			and.addCondition(QB.notNull(field));
			break;
		case IS_NULL:
			and.addCondition(QB.isNull(field));
			break;
		case LESS_THAN:
			and.addCondition(QB.lt(field, value));
			break;
		case LESS_THAN_EQUAL:
			and.addCondition(QB.le(field, value));
			break;
		case LIKE:
			and.addCondition(QB.like(field, String.valueOf(value)));
			break;
		case NOT_CONTAINING:
			and.addCondition(QB.not(QB.matchAny(field, String.valueOf(value))));
			break;
		case NOT_IN:
			and.addCondition(QB.not(QB.notin(field, (Object[]) value)));
			break;
		case NOT_LIKE:
			and.addCondition(QB.not(QB.like(field, String.valueOf(value))));
			break;
		case NEAR:
			throw new UnsupportedOperationException();
		case NEGATING_SIMPLE_PROPERTY:
			throw new UnsupportedOperationException();
		case AFTER:
			throw new UnsupportedOperationException();
		case BEFORE:
			throw new UnsupportedOperationException();
		case TRUE:
			throw new UnsupportedOperationException();
		case WITHIN:
			throw new UnsupportedOperationException();
		case REGEX:
			throw new UnsupportedOperationException();

		case EXISTS:
			throw new UnsupportedOperationException();
		case FALSE:
			throw new UnsupportedOperationException();
		}
	}

	@Override
	protected List<?> getResultList(Object[] values, Pageable page) {
		Query<?> q = createQuery(values, true);
		IntRange range = (page == null) ? null : toRange(page);
		try {
			return getSession().select(q, range);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	private IntRange toRange(Pageable pageable) {
		return new IntRange(pageable.getOffset() + 1, pageable.getOffset() + pageable.getPageSize());
	}

	private void setSortToSpec(ConditionQuery spec, Sort sort, ITableMetadata meta) {
		for (Order order : sort) {
			Field field;
			ColumnMapping column = meta.findField(order.getProperty());
			if (column == null) {
				field = new SqlExpression(order.getProperty());
			} else {
				field = column.field();
			}
			spec.addOrderBy(order.isAscending(), field);
		}
	}

	@Override
	protected Object getSingleResult(Object[] values) {
		Query<?> q = createQuery(values, true);
		try {
			return getSession().load(q, false);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	@Override
	protected int executeUpdate(Object[] values) {
		Query<?> q = createQuery(values, false);
		try {
			return getSession().update(q.getInstance());
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	@Override
	protected int executeDelete(Object[] values) {
		Query<?> q = createQuery(values, false);
		try {
			return getSession().delete(q);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	@Override
	protected long getResultCount(Object[] values) {
		Query<?> q = createQuery(values, false);
		try {
			return getSession().countLong(q);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	private Session getSession() {
		EntityManagerFactory emf = em.getEntityManagerFactory();
		EntityManager em = EntityManagerFactoryUtils.doGetTransactionalEntityManager(emf, null);
		if (em == null) { // 当无事务时。Spring返回null
			em = emf.createEntityManager(null, Collections.EMPTY_MAP);
		}
		if (em instanceof JefEntityManager) {
			return ((JefEntityManager) em).getSession();
		}
		throw new IllegalArgumentException(em.getClass().getName());
	}

}

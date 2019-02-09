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
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

import javax.persistence.EntityManagerFactory;

import jef.common.PairIO;
import jef.common.wrapper.IntRange;
import jef.database.Condition;
import jef.database.Condition.Operator;
import jef.database.DbUtils;
import jef.database.Field;
import jef.database.IConditionField.And;
import jef.database.IConditionField.Or;
import jef.database.QB;
import jef.database.dialect.type.ColumnMapping;
import jef.database.jpa.JefEntityManagerFactory;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.database.query.ConditionQuery;
import jef.database.query.Query;
import jef.database.query.SqlExpression;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.parser.PartTree;

import com.github.geequery.springdata.annotation.FindBy;
import com.github.geequery.springdata.annotation.IgnoreIf;
import com.github.geequery.springdata.repository.query.GqParameters.GqParameter;
import com.github.geequery.springdata.repository.query.GqQueryExecution.CountExecution;
import com.github.geequery.springdata.repository.query.GqQueryExecution.DeleteExecution;

/**
 * A {@link AbstractJpaQuery} implementation based on a {@link PartTree}.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public class GqPartTreeQuery extends AbstractGqQuery {

	private final ITableMetadata metadata;
	private final GqPartTree tree;
	private final GqParameters parameters;
	private final EntityManagerFactory em;

	/**
	 * Creates a new {@link PartTreeJpaQuery}.
	 * 
	 * @param method
	 *            must not be {@literal null}.
	 * @param factory
	 *            must not be {@literal null}.
	 * @param emf
	 *            must not be {@literal null}.
	 */
	public GqPartTreeQuery(GqQueryMethod method, EntityManagerFactory emf) {
		super(method, emf);
		this.em = emf;
		this.metadata = MetaHolder.getMeta(method.getEntityInformation()
				.getJavaType());
		this.tree = new GqPartTree(method.getName(), metadata.getThisType());
		this.parameters = method.getParameters();
		// boolean recreationRequired = parameters.hasDynamicProjection() ||
		// parameters.potentiallySortsDynamically();
	}

	public GqPartTreeQuery(GqQueryMethod method, JefEntityManagerFactory emf,FindBy findBy) {
        super(method, emf);
        this.em = emf;
        this.metadata = MetaHolder.getMeta(method.getEntityInformation()
                .getJavaType());
        this.tree = new GqPartTree(findBy, metadata.getThisType());
        this.parameters = method.getParameters();
    }

    @Override
	protected GqQueryExecution getExecution() {
		if(tree.isDelete()){
			return new DeleteExecution();
		}
		if(tree.isCountProjection()){
			return new CountExecution();
		}
		return super.getExecution();
	}

	private Query<?> createQuery(Object[] values, boolean withPageSort) {
		Query<?> q = QB.create(metadata);
		ParametersParameterAccessor accessor = new ParametersParameterAccessor(
				parameters, values);
		Or or = new Or();
		int index = 0;
		for (GqOrPart node : tree) {
			And and = new And();
			for (GqPart part : node) {
				PropertyPath path = part.getProperty();
				if (path.getOwningType().getType() != metadata.getThisType()) {
					throw new IllegalArgumentException("PathType:"
							+ path.getOwningType().getType() + "  metadata:"
							+ metadata.getThisType());
				}
				String fieldName = path.getSegment();
				ColumnMapping field = metadata.findField(fieldName);
				PairIO<GqParameter> paramInfo = getBindParamIndex(index++,
						fieldName);
				Object obj = accessor.getBindableValue(paramInfo.first);
				if (field != null) {
					IgnoreIf ignore = paramInfo.second.getIgnoreIf();
					if (ignore == null || !QueryUtils.isIgnore(ignore, obj)) {
						add(and, part, field.field(), obj);
					}
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

	// FIXME use Binder to optmize.
	private PairIO<GqParameter> getBindParamIndex(int index, String fieldName) {
		int i = 0;
		for (GqParameter param : this.parameters) {
			if (param.getName() == null) {
				if (index == param.getIndex()) {
					return new PairIO<GqParameter>(i, param);
				}
			} else {
				if (fieldName.equals(param.getName())) {
					return new PairIO<GqParameter>(i, param);
				}
			}
			i++;
		}
		throw new NoSuchElementException("Can not found bind parameter '"
				+ fieldName + "' in method " + this.getQueryMethod().getName());
	}

	private void add(And and, GqPart part, Field field, Object value) {

		switch (part.getType()) {
		case SIMPLE_PROPERTY:
			and.addCondition(QB.eq(field, value));
			break;
		case BETWEEN:
			if (value instanceof Collection<?>) {
				Object[] objs = ((Collection<?>) value).toArray();
				assertTwObjects(objs.length);
				and.addCondition(Condition.get(field, Operator.BETWEEN_L_L,
						objs));
			} else if (value instanceof int[]) {
				int[] objs = (int[]) value;
				assertTwObjects(objs.length);
				and.addCondition(Condition.get(field, Operator.BETWEEN_L_L,
						new Object[] { objs[0], objs[1] }));
			} else if (value instanceof long[]) {
				long[] objs = (long[]) value;
				assertTwObjects(objs.length);
				and.addCondition(Condition.get(field, Operator.BETWEEN_L_L,
						new Object[] { objs[0], objs[1] }));
			} else if (value instanceof Object[]) {
				Object[] objs = (Object[]) value;
				assertTwObjects(objs.length);
				and.addCondition(Condition.get(field, Operator.BETWEEN_L_L,
						objs));
			} else {
				throw new IllegalArgumentException(
						"The condition value of IN must be a 'Collection' or 'Object[]'");
			}
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
			if (value instanceof Collection<?>) {
				and.addCondition(QB.in(field, (Collection<?>) value));
			} else if (value instanceof int[]) {
				and.addCondition(QB.in(field, (int[]) value));
			} else if (value instanceof long[]) {
				and.addCondition(QB.in(field, (long[]) value));
			} else if (value instanceof Object[]) {
				and.addCondition(QB.in(field, (Object[]) value));
			} else {
				throw new IllegalArgumentException(
						"The condition value of IN must be a 'Collection' or 'Object[]'");
			}
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
			and.addCondition(QB.not(QB.eq(field, value)));
			break;
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

	private void assertTwObjects(int length) {
		if (length != 2) {
			throw new IllegalArgumentException(
					"The condition of BETWEEN must be 2 elements. but ["
							+ length + "]");
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
		return new IntRange(pageable.getOffset() + 1, pageable.getOffset()
				+ pageable.getPageSize());
	}

	private void setSortToSpec(ConditionQuery spec, Sort sort,
			ITableMetadata meta) {
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
}

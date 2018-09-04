/*
 * Copyright 2012-2017 the original author or authors.
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

import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.querydsl.QSort;
import org.springframework.util.Assert;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.OrderSpecifier.NullHandling;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.sql.AbstractSQLQuery;
import com.querydsl.sql.SQLQuery;

/**
 * Helper instance to ease access to Querydsl JPA query API.
 */
public class Querydsl {

	private final PathBuilder<?> builder;

	/**
	 * Creates a new {@link Querydsl} for the given {@link EntityManager} and {@link PathBuilder}.
	 * 
	 * @param em must not be {@literal null}.
	 * @param builder must not be {@literal null}.
	 */
	public Querydsl(PathBuilder<?> builder) {
		Assert.notNull(builder, "PathBuilder must not be null!");
		this.builder = builder;
	}


	/**
	 * Creates the {@link SQLQuery} instance based on the configured {@link EntityManager}.
	 * 
	 * @return
	 */
	public AbstractSQLQuery<Object, SQLQuery<Object>> createQuery(EntityPath<?>... paths) {
		return createQuery().from(paths);
	}

	/**
	 * Applies the given {@link Pageable} to the given {@link SQLQuery}.
	 * 
	 * @param pageable
	 * @param query must not be {@literal null}.
	 * @return the Querydsl {@link SQLQuery}.
	 */
	public <T> SQLQuery<T> applyPagination(Pageable pageable, SQLQuery<T> query) {

		if (pageable.isUnpaged()) {
			return query;
		}

		query.offset(pageable.getOffset());
		query.limit(pageable.getPageSize());

		return applySorting(pageable.getSort(), query);
	}

	/**
	 * Applies sorting to the given {@link SQLQuery}.
	 * 
	 * @param sort
	 * @param query must not be {@literal null}.
	 * @return the Querydsl {@link SQLQuery}
	 */
	public <T> SQLQuery<T> applySorting(Sort sort, SQLQuery<T> query) {

		if (sort.isUnsorted()) {
			return query;
		}

		if (sort instanceof QSort) {
			return addOrderByFrom((QSort) sort, query);
		}

		return addOrderByFrom(sort, query);
	}

	/**
	 * Applies the given {@link OrderSpecifier}s to the given {@link SQLQuery}. Potentially transforms the given
	 * {@code OrderSpecifier}s to be able to injection potentially necessary left-joins.
	 * 
	 * @param qsort must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 */
	private <T> SQLQuery<T> addOrderByFrom(QSort qsort, SQLQuery<T> query) {

		List<OrderSpecifier<?>> orderSpecifiers = qsort.getOrderSpecifiers();
		return query.orderBy(orderSpecifiers.toArray(new OrderSpecifier[orderSpecifiers.size()]));
	}

	/**
	 * Converts the {@link Order} items of the given {@link Sort} into {@link OrderSpecifier} and attaches those to the
	 * given {@link SQLQuery}.
	 * 
	 * @param sort must not be {@literal null}.
	 * @param query must not be {@literal null}.
	 * @return
	 */
	private <T> SQLQuery<T> addOrderByFrom(Sort sort, SQLQuery<T> query) {

		Assert.notNull(sort, "Sort must not be null!");
		Assert.notNull(query, "Query must not be null!");

		for (Order order : sort) {
			query.orderBy(toOrderSpecifier(order));
		}

		return query;
	}

	/**
	 * Transforms a plain {@link Order} into a QueryDsl specific {@link OrderSpecifier}.
	 * 
	 * @param order must not be {@literal null}.
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private OrderSpecifier<?> toOrderSpecifier(Order order) {

		return new OrderSpecifier(
				order.isAscending() ? com.querydsl.core.types.Order.ASC : com.querydsl.core.types.Order.DESC,
				buildOrderPropertyPathFrom(order), toQueryDslNullHandling(order.getNullHandling()));
	}

	/**
	 * Converts the given {@link org.springframework.data.domain.Sort.NullHandling} to the appropriate Querydsl
	 * {@link NullHandling}.
	 * 
	 * @param nullHandling must not be {@literal null}.
	 * @return
	 * @since 1.6
	 */
	private NullHandling toQueryDslNullHandling(org.springframework.data.domain.Sort.NullHandling nullHandling) {

		Assert.notNull(nullHandling, "NullHandling must not be null!");

		switch (nullHandling) {

			case NULLS_FIRST:
				return NullHandling.NullsFirst;

			case NULLS_LAST:
				return NullHandling.NullsLast;

			case NATIVE:
			default:
				return NullHandling.Default;
		}
	}

	/**
	 * Creates an {@link Expression} for the given {@link Order} property.
	 * 
	 * @param order must not be {@literal null}.
	 * @return
	 */
	private Expression<?> buildOrderPropertyPathFrom(Order order) {

		Assert.notNull(order, "Order must not be null!");

		PropertyPath path = PropertyPath.from(order.getProperty(), builder.getType());
		Expression<?> sortPropertyExpression = builder;

		while (path != null) {

			if (!path.hasNext() && order.isIgnoreCase()) {
				// if order is ignore-case we have to treat the last path segment as a String.
				sortPropertyExpression = Expressions.stringPath((Path<?>) sortPropertyExpression, path.getSegment()).lower();
			} else {
				sortPropertyExpression = Expressions.path(path.getType(), (Path<?>) sortPropertyExpression, path.getSegment());
			}

			path = path.next();
		}

		return sortPropertyExpression;
	}
}

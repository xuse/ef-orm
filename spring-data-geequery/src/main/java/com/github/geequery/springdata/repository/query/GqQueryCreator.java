/*
 * Copyright 2008-2015 the original author or authors.
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

import java.util.Iterator;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import jef.database.QB;
import jef.database.meta.ITableMetadata;
import jef.database.query.Query;

import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;

public class GqQueryCreator extends AbstractQueryCreator<Query<?>, Void> {

	private final Query<?> query;
//	private final ParameterMetadataProvider provider;
//	private final ReturnedType returnedType;

	/**
	 * Create a new {@link GqQueryCreator}.
	 * 
	 * @param tree
	 * @param domainClass
	 * @param accessor
	 * @param em
	 */
	public GqQueryCreator(PartTree tree,ITableMetadata meta) {
		super(tree);
		query=QB.create(meta);
//		this.provider = provider;
//		this.returnedType = type;
//		query = createCriteriaQuery(builder, type);
//
//		this.query = criteriaQuery.distinct(tree.isDistinct());
//		this.root = query.from(type.getDomainType());
		
	}

	/**
	 * Creates the {@link CriteriaQuery} to apply predicates on.
	 * 
	 * @param builder
	 *            will never be {@literal null}.
	 * @param type
	 *            will never be {@literal null}.
	 * @return must not be {@literal null}.
	 */
	protected CriteriaQuery<? extends Object> createCriteriaQuery(CriteriaBuilder builder, ReturnedType type) {

		Class<?> typeToRead = type.getTypeToRead();

		return typeToRead == null ? builder.createTupleQuery() : builder.createQuery(typeToRead);
	}

//	/**
//	 * Returns all {@link javax.persistence.criteria.ParameterExpression}
//	 * created when creating the query.
//	 * 
//	 * @return the parameterExpressions
//	 */
//	public List<ParameterMetadata<?>> getParameterExpressions() {
//		return provider.getExpressions();
//	}


	/**
	 * Template method to finalize the given {@link Predicate} using the given
	 * {@link CriteriaQuery} and {@link CriteriaBuilder}.
	 * 
	 * @param predicate
	 * @param sort
	 * @param query
	 * @param builder
	 * @return
	 */
	protected CriteriaQuery<? extends Object> complete(Predicate predicate, Sort sort, CriteriaQuery<? extends Object> query, CriteriaBuilder builder, Root<?> root) {

////		if (returnedType.needsCustomConstruction()) {
////
////			List<Selection<?>> selections = new ArrayList<Selection<?>>();
////
////			for (String property : returnedType.getInputProperties()) {
////				selections.add(root.get(property).alias(property));
////			}
////
////			query = query.multiselect(selections);
////		} else {
////			query = query.select((Root) root);
////		}
//
//		CriteriaQuery<? extends Object> select = query.orderBy(QueryUtils.toOrders(sort, root, builder));
//		return predicate == null ? select : select.where(predicate);
		return null;
	}


	/**
	 * Simple builder to contain logic to create JPA {@link Predicate}s from
	 * {@link Part}s.
	 * 
	 * @author Phil Webb
	 * @author Oliver Gierke
	 */
//	@SuppressWarnings({ "unchecked", "rawtypes" })
//	private class PredicateBuilder {
//		private final Part part;
//		private final Query<?> root;
//
//		/**
//		 * Creates a new {@link PredicateBuilder} for the given {@link Part} and
//		 * {@link Root}.
//		 * 
//		 * @param part
//		 *            must not be {@literal null}.
//		 * @param root
//		 *            must not be {@literal null}.
//		 */
//		public PredicateBuilder(Part part, Query<?> root) {
//			Assert.notNull(part);
//			Assert.notNull(root);
//			this.part = part;
//			this.root = root;
//		}
//
//		/**
//		 * Builds a JPA {@link Predicate} from the underlying {@link Part}.
//		 * 
//		 * @return
//		 */
//		public Predicate build() {
//
//			PropertyPath property = part.getProperty();
//			Type type = part.getType();
//
//			switch (type) {
//			case BETWEEN:
//				ParameterMetadata<Comparable> first = provider.next(part);
//				ParameterMetadata<Comparable> second = provider.next(part);
//				return builder.between(getComparablePath(root, part), first.getExpression(), second.getExpression());
//			case AFTER:
//			case GREATER_THAN:
//				return builder.greaterThan(getComparablePath(root, part), provider.next(part, Comparable.class).getExpression());
//			case GREATER_THAN_EQUAL:
//				return builder.greaterThanOrEqualTo(getComparablePath(root, part), provider.next(part, Comparable.class).getExpression());
//			case BEFORE:
//			case LESS_THAN:
//				return builder.lessThan(getComparablePath(root, part), provider.next(part, Comparable.class).getExpression());
//			case LESS_THAN_EQUAL:
//				return builder.lessThanOrEqualTo(getComparablePath(root, part), provider.next(part, Comparable.class).getExpression());
//			case IS_NULL:
//				return getTypedPath(root, part).isNull();
//			case IS_NOT_NULL:
//				return getTypedPath(root, part).isNotNull();
//			case NOT_IN:
//				return getTypedPath(root, part).in(provider.next(part, Collection.class).getExpression()).not();
//			case IN:
//				return getTypedPath(root, part).in(provider.next(part, Collection.class).getExpression());
//			case STARTING_WITH:
//			case ENDING_WITH:
//			case CONTAINING:
//			case NOT_CONTAINING:
//
//				if (property.getLeafProperty().isCollection()) {
//
//					Expression<Collection<Object>> propertyExpression = traversePath(root, property);
//					Expression<Object> parameterExpression = provider.next(part).getExpression();
//
//					// Can't just call .not() in case of negation as EclipseLink
//					// chokes on that.
//					return type.equals(NOT_CONTAINING) ? builder.isNotMember(parameterExpression, propertyExpression) : builder.isMember(parameterExpression, propertyExpression);
//				}
//
//			case LIKE:
//			case NOT_LIKE:
//				Expression<String> stringPath = getTypedPath(root, part);
//				Expression<String> propertyExpression = upperIfIgnoreCase(stringPath);
//				Expression<String> parameterExpression = upperIfIgnoreCase(provider.next(part, String.class).getExpression());
//				Predicate like = builder.like(propertyExpression, parameterExpression);
//				return type.equals(NOT_LIKE) || type.equals(NOT_CONTAINING) ? like.not() : like;
//			case TRUE:
//				Expression<Boolean> truePath = getTypedPath(root, part);
//				return builder.isTrue(truePath);
//			case FALSE:
//				Expression<Boolean> falsePath = getTypedPath(root, part);
//				return builder.isFalse(falsePath);
//			case SIMPLE_PROPERTY:
//				ParameterMetadata<Object> expression = provider.next(part);
//				Expression<Object> path = getTypedPath(root, part);
//				return expression.isIsNullParameter() ? path.isNull() : builder.equal(upperIfIgnoreCase(path), upperIfIgnoreCase(expression.getExpression()));
//			case NEGATING_SIMPLE_PROPERTY:
//				return builder.notEqual(upperIfIgnoreCase(getTypedPath(root, part)), upperIfIgnoreCase(provider.next(part).getExpression()));
//			default:
//				throw new IllegalArgumentException("Unsupported keyword " + type);
//			}
//		}
//
//		/**
//		 * Applies an {@code UPPERCASE} conversion to the given
//		 * {@link Expression} in case the underlying {@link Part} requires
//		 * ignoring case.
//		 * 
//		 * @param expression
//		 *            must not be {@literal null}.
//		 * @return
//		 */
//		private <T> Expression<T> upperIfIgnoreCase(Expression<? extends T> expression) {
//
//			switch (part.shouldIgnoreCase()) {
//
//			case ALWAYS:
//
//				Assert.state(canUpperCase(expression), "Unable to ignore case of " + expression.getJavaType().getName() + " types, the property '"
//						+ part.getProperty().getSegment() + "' must reference a String");
//				return (Expression<T>) builder.upper((Expression<String>) expression);
//
//			case WHEN_POSSIBLE:
//
//				if (canUpperCase(expression)) {
//					return (Expression<T>) builder.upper((Expression<String>) expression);
//				}
//
//			case NEVER:
//			default:
//
//				return (Expression<T>) expression;
//			}
//		}
//
//		private boolean canUpperCase(Expression<?> expression) {
//			return String.class.equals(expression.getJavaType());
//		}
//
//		/**
//		 * Returns a path to a {@link Comparable}.
//		 * 
//		 * @param root
//		 * @param part
//		 * @return
//		 */
//		private Expression<? extends Comparable> getComparablePath(Root<?> root, Part part) {
//			return getTypedPath(root, part);
//		}
//
//		private <T> Expression<T> getTypedPath(Root<?> root, Part part) {
//			return toExpressionRecursively(root, part.getProperty());
//			return null;
//		}
//
//		private <T> Expression<T> traversePath(Path<?> root, PropertyPath path) {
//
//			Path<Object> result = root.get(path.getSegment());
//			return (Expression<T>) (path.hasNext() ? traversePath(result, path.next()) : result);
//		}
//	}
	
	@Override
	protected Void create(Part part, Iterator<Object> iterator) {
	
		return null;
	}

	@Override
	protected Void and(Part part, Void base, Iterator<Object> iterator) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Void or(Void base, Void criteria) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Query<?> complete(Void criteria, Sort sort) {
		// TODO Auto-generated method stub
		return null;
	}

	
}

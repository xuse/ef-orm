/*
 * Copyright 2016 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.metamodel.Attribute.PersistentAttributeType;
import javax.persistence.metamodel.SingularAttribute;

import jef.database.Field;
import jef.database.QB;
import jef.database.dialect.type.ColumnMapping;
import jef.database.meta.FBIField;
import jef.database.meta.ITableMetadata;
import jef.database.query.Query;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.repository.core.support.ExampleMatcherAccessor;
import org.springframework.data.util.DirectFieldAccessFallbackBeanWrapper;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@link QueryByExamplePredicateBuilder} creates a single
 * {@link CriteriaBuilder#and(Predicate...)} combined {@link Predicate} for a
 * given {@link Example}. <br />
 * The builder includes any {@link SingularAttribute} of the
 * {@link Example#getProbe()} applying {@link String} and {@literal null}
 * matching strategies configured on the {@link Example}. Ignored paths are no
 * matter of their actual value not considered. <br />
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 1.10
 */
public class QueryByExamplePredicateBuilder {

	private static final Set<PersistentAttributeType> ASSOCIATION_TYPES;

	static {
		ASSOCIATION_TYPES = new HashSet<PersistentAttributeType>(Arrays.asList(PersistentAttributeType.MANY_TO_MANY, PersistentAttributeType.MANY_TO_ONE,
				PersistentAttributeType.ONE_TO_MANY, PersistentAttributeType.ONE_TO_ONE));
	}

	/**
	 * Extract the {@link Predicate} representing the {@link Example}.
	 *
	 * @param root
	 *            must not be {@literal null}.
	 * @param cb
	 *            must not be {@literal null}.
	 * @param example
	 *            must not be {@literal null}.
	 * @return never {@literal null}.
	 */
	public static Query<?> getPredicate(ITableMetadata meta, Example<?> example) {
		Assert.notNull(meta, "meta must not be null!");
		Assert.notNull(example, "Example must not be null!");

		Query<?> q = QB.create(meta);
		getPredicates(q, "", meta, example.getProbe(), example.getProbeType(), new ExampleMatcherAccessor(example.getMatcher()), new PathNode("root", null, example.getProbe()));
		return q;
	}

	static void getPredicates(Query<?> q, String path, ITableMetadata type, Object value, Class<?> probeType, ExampleMatcherAccessor exampleAccessor, PathNode currentNode) {

		// List<Predicate> predicates = new ArrayList<Predicate>();
		DirectFieldAccessFallbackBeanWrapper beanWrapper = new DirectFieldAccessFallbackBeanWrapper(value);

		for (ColumnMapping attribute : type.getColumns()) {
			String fieldName = attribute.fieldName();
			String currentPath = !StringUtils.hasText(path) ? fieldName : path + "." + fieldName;
			if (exampleAccessor.isIgnoredPath(currentPath)) {
				continue;
			}
			Object attributeValue = exampleAccessor.getValueTransformerForPath(currentPath).convert(beanWrapper.getPropertyValue(fieldName));

			if (attributeValue == null) {
				if (exampleAccessor.getNullHandler().equals(ExampleMatcher.NullHandler.INCLUDE)) {
					q.addCondition(QB.isNull(type.getField(fieldName)));
				}
				continue;
			}
			// 这种情况不存在
			// if
			// (attribute.getPersistentAttributeType().equals(PersistentAttributeType.EMBEDDED))
			// {
			//
			// predicates.addAll(getPredicates(currentPath, cb,
			// from.get(attribute.getName()),
			// (ManagedType<?>) attribute.getType(), attributeValue, probeType,
			// exampleAccessor, currentNode));
			// continue;
			// }
			Field expression = attribute.field();
			if (attribute.getFieldType().equals(String.class)) {
				// 全转小写处理
				if (exampleAccessor.isIgnoreCaseForPath(currentPath)) {
					expression = new FBIField("lower(" + expression.name() + ")", q);
					attributeValue = attributeValue.toString().toLowerCase();
				}
				// 根据运算符计算String的运算符
				switch (exampleAccessor.getStringMatcherForPath(currentPath)) {
				case DEFAULT:
				case EXACT:
					q.addCondition(QB.eq(expression, attributeValue));
					break;
				case CONTAINING:
					q.addCondition(QB.matchAny(expression, String.valueOf(attributeValue)));
					break;
				case STARTING:
					q.addCondition(QB.matchStart(expression, String.valueOf(attributeValue)));
					break;
				case ENDING:
					q.addCondition(QB.matchEnd(expression, String.valueOf(attributeValue)));
					break;
				default:
					throw new IllegalArgumentException("Unsupported StringMatcher " + exampleAccessor.getStringMatcherForPath(currentPath));
				}
			} else {
				q.addCondition(QB.eq(expression, attributeValue));
			}
		}
		// 处理位于引用字段（关系对象）上的条件，先不支持，后续再加
		// if (isAssociation(attribute)) {
		//
		// if (!(from instanceof From)) {
		// throw new JpaSystemException(new IllegalArgumentException(
		// String.format("Unexpected path type for %s. Found % where From.class was expected.",
		// currentPath, from)));
		// }
		//
		// PathNode node = currentNode.add(attribute.getName(), attributeValue);
		// if (node.spansCycle()) {
		// throw new InvalidDataAccessApiUsageException(
		// String.format("Path '%s' from root %s must not span a cyclic property reference!\r\n%s",
		// currentPath,
		// ClassUtils.getShortName(probeType), node));
		// }
		//
		// predicates.addAll(getPredicates(currentPath, cb, ((From<?, ?>)
		// from).join(attribute.getName()),
		// (ManagedType<?>) attribute.getType(), attributeValue, probeType,
		// exampleAccessor, node));
	}

	/**
	 * {@link PathNode} is used to dynamically grow a directed graph structure
	 * that allows to detect cycles within its direct predecessor nodes by
	 * comparing parent node values using
	 * {@link System#identityHashCode(Object)}.
	 *
	 * @author Christoph Strobl
	 */
	private static class PathNode {

		String name;
		PathNode parent;
		List<PathNode> siblings = new ArrayList<PathNode>();;
		Object value;

		public PathNode(String edge, PathNode parent, Object value) {

			this.name = edge;
			this.parent = parent;
			this.value = value;
		}

		PathNode add(String attribute, Object value) {

			PathNode node = new PathNode(attribute, this, value);
			siblings.add(node);
			return node;
		}

		boolean spansCycle() {

			if (value == null) {
				return false;
			}

			String identityHex = ObjectUtils.getIdentityHexString(value);
			PathNode tmp = parent;

			while (tmp != null) {

				if (ObjectUtils.getIdentityHexString(tmp.value).equals(identityHex)) {
					return true;
				}
				tmp = tmp.parent;
			}

			return false;
		}

		@Override
		public String toString() {

			StringBuilder sb = new StringBuilder();
			if (parent != null) {
				sb.append(parent.toString());
				sb.append(" -");
				sb.append(name);
				sb.append("-> ");
			}

			sb.append("[{ ");
			sb.append(ObjectUtils.nullSafeToString(value));
			sb.append(" }]");
			return sb.toString();
		}
	}
}

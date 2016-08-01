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

import static java.util.regex.Pattern.CASE_INSENSITIVE;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import javax.persistence.NamedQuery;

import jef.database.jpa.JefEntityManagerFactory;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.jpa.repository.query.JpaQueryMethod;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.github.geequery.springdata.annotation.Modifying;
import com.github.geequery.springdata.annotation.Procedure;
import com.github.geequery.springdata.annotation.Query;
import com.github.geequery.springdata.repository.support.MetamodelInformation;

/**
 * GQ specific extension of {@link QueryMethod}.
 * 
 * supports more Annotations on method..
 * 
 * @see Query
 * @see Modifying
 * @see Procedure
 * 
 */
public class GqQueryMethod extends QueryMethod {

	// @see JPA 2.0 Specification 2.2 Persistent Fields and Properties Page 23 -
	// Top paragraph.
	private static final Set<Class<?>> NATIVE_ARRAY_TYPES;
	// private static final StoredProcedureAttributeSource
	// storedProcedureAttributeSource = StoredProcedureAttributeSource.INSTANCE;

	static {

		Set<Class<?>> types = new HashSet<Class<?>>();
		types.add(byte[].class);
		types.add(Byte[].class);
		types.add(char[].class);
		types.add(Character[].class);

		NATIVE_ARRAY_TYPES = Collections.unmodifiableSet(types);
	}

	private final Method method;

	private final JefEntityManagerFactory emf;

	/**
	 * Creates a {@link JpaQueryMethod}.
	 * 
	 * @param method
	 *            must not be {@literal null}
	 * @param extractor
	 *            must not be {@literal null}
	 * @param metadata
	 *            must not be {@literal null}
	 */
	public GqQueryMethod(Method method, RepositoryMetadata metadata, ProjectionFactory factory, JefEntityManagerFactory emf) {

		super(method, metadata, factory);

		Assert.notNull(method, "Method must not be null!");

		this.method = method;
		this.emf = emf;
		Assert.isTrue(!(isModifyingQuery() && getParameters().hasSpecialParameter()), String.format("Modifying method must not contain %s!", Parameters.TYPES));
		assertParameterNamesInAnnotatedQuery();
	}

	private void assertParameterNamesInAnnotatedQuery() {
		String annotatedQuery = getAnnotatedQuery();
		if (!hasNamedParameter(annotatedQuery)) {
			return;
		}
		for (Parameter parameter : getParameters()) {
			if (!parameter.isNamedParameter()) {
				continue;
			}
			if (!annotatedQuery.contains(String.format(":%s", parameter.getName())) && !annotatedQuery.contains(String.format("#%s", parameter.getName()))) {
				throw new IllegalStateException(String.format("Using named parameters for method %s but parameter '%s' not found in annotated query '%s'!", method, parameter.getName(), annotatedQuery));
			}
		}
	}
	public static boolean hasNamedParameter(String query) {
		return StringUtils.hasText(query) && NAMED_PARAMETER.matcher(query).find();
	}
	
	private static final String IDENTIFIER = "[\\p{Lu}\\P{InBASIC_LATIN}\\p{Alnum}._$]+";
	private static final Pattern NAMED_PARAMETER = Pattern.compile(":" + IDENTIFIER + "|\\#" + IDENTIFIER,
			CASE_INSENSITIVE);

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.data.repository.query.QueryMethod#getEntityInformation
	 * ()
	 */
	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public GqEntityMetadata<?> getEntityInformation() {
		return new MetamodelInformation(getDomainClass(), emf);
	}

	/**
	 * Returns whether the finder is a modifying one.
	 * 
	 * @return
	 */
	@Override
	public boolean isModifyingQuery() {

		return null != AnnotationUtils.findAnnotation(method, Modifying.class);
	}

	/**
	 * Returns the actual return type of the method.
	 * 
	 * @return
	 */
	Class<?> getReturnType() {
		return method.getReturnType();
	}

	/**
	 * Returns the query string declared in a {@link Query} annotation or
	 * {@literal null} if neither the annotation found nor the attribute was
	 * specified.
	 * 
	 * @return
	 */
	String getAnnotatedQuery() {
		String query = getAnnotationValue("value", Query.class, String.class);
		return StringUtils.hasText(query) ? query : null;
	}

	/**
	 * Returns the countQuery string declared in a {@link Query} annotation or
	 * {@literal null} if neither the annotation found nor the attribute was
	 * specified.
	 * 
	 * @return
	 */
	String getCountQuery() {
		String countQuery = getAnnotationValue("countQuery", Query.class, String.class);
		return StringUtils.hasText(countQuery) ? countQuery : null;
	}

	/**
	 * Returns the count query projection string declared in a {@link Query}
	 * annotation or {@literal null} if neither the annotation found nor the
	 * attribute was specified.
	 * 
	 * @return
	 * @since 1.6
	 */
	String getCountQueryProjection() {

		String countProjection = getAnnotationValue("countProjection", Query.class, String.class);
		return StringUtils.hasText(countProjection) ? countProjection : null;
	}

	/**
	 * Returns whether the backing query is a native one.
	 * 
	 * @return
	 */
	boolean isNativeQuery() {
		return getAnnotationValue("nativeQuery", Query.class, Boolean.class).booleanValue();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.data.repository.query.QueryMethod#getNamedQueryName()
	 */
	@Override
	public String getNamedQueryName() {
		String annotatedName = getAnnotationValue("name", Query.class, String.class);
		return StringUtils.hasText(annotatedName) ? annotatedName : super.getNamedQueryName();
	}

	/**
	 * Returns the name of the {@link NamedQuery} that shall be used for count
	 * queries.
	 * 
	 * @return
	 */
	String getNamedCountQueryName() {
		String annotatedName = getAnnotationValue("countName", Query.class, String.class);
		return StringUtils.hasText(annotatedName) ? annotatedName : getNamedQueryName() + ".count";
	}

	/**
	 * 获得指定指定注解的字段值
	 * 
	 * @param attribute
	 *            需要获得的注解字段名
	 * @param annotationType
	 *            注解类型
	 * @param targetType
	 *            返回值类型
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <T> T getAnnotationValue(String attribute, Class annotationType, Class<T> targetType) {
		Annotation annotation = AnnotatedElementUtils.findMergedAnnotation(method, annotationType);
		if (annotation == null) {
			return targetType.cast(AnnotationUtils.getDefaultValue(annotationType, attribute));
		}

		return targetType.cast(AnnotationUtils.getValue(annotation, attribute));
	}

	/**
	 * 获得方法上的有效注解
	 * 
	 * @param annotationType
	 * @return
	 */
	public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
		return AnnotatedElementUtils.findMergedAnnotation(method, annotationType);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.data.repository.query.QueryMethod#createParameters
	 * (java.lang.reflect.Method)
	 */
	@Override
	protected GqParameters createParameters(Method method) {
		return new GqParameters(method);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.data.repository.query.QueryMethod#getParameters()
	 */
	@Override
	public GqParameters getParameters() {
		return (GqParameters) super.getParameters();
	}

	@Override
	public boolean isCollectionQuery() {
		return super.isCollectionQuery() && !NATIVE_ARRAY_TYPES.contains(method.getReturnType());
	}

	/**
	 * 通过@Procedure可以指定方法运行存储过程
	 * 
	 * @return
	 */
	public boolean isProcedureQuery() {
		return AnnotationUtils.findAnnotation(method, Procedure.class) != null;
	}

	/**
	 * Returns whether we should clear automatically for modifying queries.
	 * 
	 * @return
	 */
	boolean getClearAutomatically() {
		return getAnnotationValue("clearAutomatically", Modifying.class, Boolean.class);
	}
}

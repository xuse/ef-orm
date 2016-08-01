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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.util.Assert;

public abstract class GqQueryExecution {

	private static final ConversionService CONVERSION_SERVICE;

	static {
		ConfigurableConversionService conversionService = new DefaultConversionService();
		conversionService.removeConvertible(Collection.class, Object.class);
//		conversionService.addConverter(JpaResultConverters.BlobToByteArrayConverter.INSTANCE);
		CONVERSION_SERVICE = conversionService;
	}

	/**
	 * Executes the given {@link AbstractStringBasedJpaQuery} with the given {@link ParameterBinder}.
	 * 
	 * @param query must not be {@literal null}.
	 * @param binder must not be {@literal null}.
	 * @return
	 */
	public Object execute(AbstractGqQuery query, Object[] values) {
		Assert.notNull(query);
		Assert.notNull(values);

		Object result;

		try {
			result = doExecute(query, values);
		} catch (NoResultException e) {
			return null;
		}

		if (result == null) {
			return null;
		}
		GqQueryMethod queryMethod = query.getQueryMethod();
		Class<?> requiredType = queryMethod.getReturnType();
		if (void.class.equals(requiredType) || requiredType.isAssignableFrom(result.getClass())) {
			return result;
		}
		return CONVERSION_SERVICE.canConvert(result.getClass(), requiredType)
				? CONVERSION_SERVICE.convert(result, requiredType) : result;
	}

	/**
	 * Method to implement {@link AbstractStringBasedJpaQuery} executions by single enum values.
	 * 
	 * @param query
	 * @param binder
	 * @return
	 */
	protected abstract Object doExecute(AbstractGqQuery query, Object[] values);

	/**
	 * Executes the {@link AbstractStringBasedJpaQuery} to return a {@link org.springframework.data.domain.Page} of
	 * entities.
	 */
	static class PagedExecution extends GqQueryExecution {
		private final Parameters<?, ?> parameters;

		public PagedExecution(Parameters<?, ?> parameters) {
			this.parameters = parameters;
		}

		@Override
		protected Object doExecute(AbstractGqQuery repositoryQuery, Object[] values) {
			long total = repositoryQuery.getResultCount(values);
			ParameterAccessor accessor = new ParametersParameterAccessor(parameters, values);
			Pageable pageable = accessor.getPageable();
			if (pageable.getOffset()>=total) {
				return new PageImpl<Object>(Collections.emptyList(), pageable, total);
			}
			List<?> content=repositoryQuery.getResultList(values,pageable);
			return new PageImpl<>(content, pageable, total);
		}
	}

	static class CollectionExecution extends GqQueryExecution {
		@Override
		protected Object doExecute(AbstractGqQuery repositoryQuery, Object[] values) {
			return repositoryQuery.getResultList(values,null);
		}
	}

	
	/**
	 * Executes a {@link AbstractStringBasedJpaQuery} to return a single entity.
	 */
	static class SingleEntityExecution extends GqQueryExecution {
		@Override
		protected Object doExecute(AbstractGqQuery query, Object[] values) {
			return query.getSingleResult(values);
		}
	}

	/**
	 * Executes a modifying query such as an update, insert or delete.
	 */
	static class ModifyingExecution extends GqQueryExecution {
		private final EntityManager em;
		/**
		 * Creates an execution that automatically clears the given {@link EntityManager} after execution if the given
		 * {@link EntityManager} is not {@literal null}.
		 * 
		 * @param em
		 */
		public ModifyingExecution(GqQueryMethod method, EntityManager em) {
			Class<?> returnType = method.getReturnType();

			boolean isVoid = void.class.equals(returnType) || Void.class.equals(returnType);
			boolean isInt = int.class.equals(returnType) || Integer.class.equals(returnType);
			Assert.isTrue(isInt || isVoid, "Modifying queries can only use void or int/Integer as return type!");
			this.em = em;
		}

		@Override
		protected Object doExecute(AbstractGqQuery query, Object[] values) {
			int result = query.executeUpdate(values);
			return result;
		}
	}

	/**
	 * {@link Execution} removing entities matching the query.
	 * @since 1.6
	 */
	static class DeleteExecution extends GqQueryExecution {
		private final EntityManager em;
		public DeleteExecution(EntityManager em) {
			this.em = em;
		}
		@Override
		protected Object doExecute(AbstractGqQuery jpaQuery, Object[] values) {
			return jpaQuery.executeDelete(values);
//			List<?> resultList = jpaQuery.getResultList(values, null);
//			return jpaQuery.getQueryMethod().isCollectionQuery() ? resultList : resultList.size();
		}
	}
}

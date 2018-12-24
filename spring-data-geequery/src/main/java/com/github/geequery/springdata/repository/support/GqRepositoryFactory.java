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
package com.github.geequery.springdata.repository.support;

import java.io.Serializable;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.util.Assert;

import com.github.geequery.springdata.repository.GqRepository;
import com.github.geequery.springdata.repository.query.GqQueryLookupStrategy;

import jef.database.SessionFactory;

/**
 * GeeQuery specific generic repository factory.
 * 
 * @author Jiyi
 */
public class GqRepositoryFactory extends RepositoryFactorySupport {

	private final SessionFactory emf;
	private final CrudMethodMetadataPostProcessor crudMethodMetadataPostProcessor;

	/**
	 * Creates a new {@link GqRepositoryFactory}.
	 * 
	 * @param entityManager
	 *            must not be {@literal null}
	 */
	public GqRepositoryFactory(SessionFactory entityManager) {
		Assert.notNull(entityManager);
		this.emf = entityManager;
		this.crudMethodMetadataPostProcessor = new CrudMethodMetadataPostProcessor();

		addRepositoryProxyPostProcessor(crudMethodMetadataPostProcessor);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.data.repository.core.support.RepositoryFactorySupport
	 * #setBeanClassLoader(java.lang.ClassLoader)
	 */
	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		super.setBeanClassLoader(classLoader);
		this.crudMethodMetadataPostProcessor.setBeanClassLoader(classLoader);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.data.repository.core.support.RepositoryFactorySupport
	 * #getTargetRepository(org.springframework.data.repository.core.
	 * RepositoryMetadata)
	 */
	@Override
	protected Object getTargetRepository(RepositoryInformation information) {
		GqRepository<?, ?> repository = getTargetRepository(information, emf);
		// repository.setRepositoryMethodMetadata(crudMethodMetadataPostProcessor.getCrudMethodMetadata());
		return repository;
	}

	/**
	 * Callback to create a {@link JpaRepository} instance with the given
	 * {@link EntityManager}
	 * 
	 * @param <T>
	 * @param <ID>
	 * @param entityManager
	 * @see #getTargetRepository(RepositoryMetadata)
	 * @return
	 */
	protected <T, ID extends Serializable> GqRepository<T, ID> getTargetRepository(RepositoryInformation information, SessionFactory entityManager) {
		EntityInformation<?, Serializable> entityInformation = getEntityInformation(information.getDomainType());
		return getTargetRepositoryViaReflection(information, entityInformation, entityManager);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.data.repository.support.RepositoryFactorySupport#
	 * getRepositoryBaseClass()
	 */
	@Override
	protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
		return GqRepositoryImpl.class;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.data.repository.core.support.RepositoryFactorySupport
	 * #getQueryLookupStrategy(org.springframework.data.repository.query.
	 * QueryLookupStrategy.Key,
	 * org.springframework.data.repository.query.EvaluationContextProvider)
	 */
	@Override
	protected Optional<QueryLookupStrategy> getQueryLookupStrategy(Key key, EvaluationContextProvider evaluationContextProvider) {
		return Optional.<QueryLookupStrategy> of(new GqQueryLookupStrategy(emf));
	}

	@Override
	public <T, ID> EntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {
		return new MetamodelInformation<T, ID>(domainClass, emf.asDbClient());
	}

	// /*
	// * (non-Javadoc)
	// *
	// * @see
	// * org.springframework.data.repository.support.RepositoryFactorySupport#
	// * getEntityInformation(java.lang.Class)
	// */
	// @Override
	// public <T, ID extends Serializable> GQEntityInformation<T>
	// getEntityInformation(Class<T> domainClass) {
	// return null;
	// }

}

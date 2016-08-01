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

import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import jef.database.Session;
import jef.database.jpa.JefEntityManager;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.query.AbstractJpaQuery;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.util.Assert;

import com.github.geequery.springdata.repository.query.GqQueryExecution.CollectionExecution;
import com.github.geequery.springdata.repository.query.GqQueryExecution.ModifyingExecution;
import com.github.geequery.springdata.repository.query.GqQueryExecution.PagedExecution;
import com.github.geequery.springdata.repository.query.GqQueryExecution.SingleEntityExecution;

/**
 * Abstract base class to implement {@link RepositoryQuery}s.
 * 
 * Jiyi
 */
public abstract class AbstractGqQuery implements RepositoryQuery {

	private final GqQueryMethod method;
	private final EntityManager em;

	/**
	 * Creates a new {@link AbstractJpaQuery} from the given
	 * {@link GqQueryMethod}.
	 * 
	 * @param method
	 * @param resultFactory
	 * @param em
	 */
	public AbstractGqQuery(GqQueryMethod method, EntityManager em) {
		Assert.notNull(method);
		Assert.notNull(em);

		this.method = method;
		this.em = em;
	}

	public GqQueryMethod getQueryMethod() {
		return method;
	}

	protected GqQueryExecution getExecution() {
		if (method.isProcedureQuery()) {
			throw new UnsupportedOperationException();
		} else if (method.isCollectionQuery()) {
			return new CollectionExecution();
		} else if (method.isPageQuery()) {
			return new PagedExecution(method.getParameters());
		} else if (method.isModifyingQuery()) {
			return method.getClearAutomatically() ? new ModifyingExecution(method, em) : new ModifyingExecution(method, null);
		} else {
			return new SingleEntityExecution();
		}
	}

	public Object execute(Object[] values) {
		GqQueryExecution execution = getExecution();
		return execution.execute(this, values);
	}

	protected abstract List<?> getResultList(Object[] values, Pageable page);

	protected abstract Object getSingleResult(Object[] values);

	protected abstract int executeUpdate(Object[] values);

	protected abstract int executeDelete(Object[] values);

	protected abstract long getResultCount(Object[] values);

	protected Session getSession() {
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

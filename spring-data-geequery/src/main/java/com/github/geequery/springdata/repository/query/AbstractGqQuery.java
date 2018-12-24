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

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.util.Assert;

import com.github.geequery.springdata.repository.query.GqQueryExecution.CollectionExecution;
import com.github.geequery.springdata.repository.query.GqQueryExecution.ModifyingExecution;
import com.github.geequery.springdata.repository.query.GqQueryExecution.PagedExecution;
import com.github.geequery.springdata.repository.query.GqQueryExecution.SingleEntityExecution;
import com.github.geequery.springdata.repository.query.GqQueryExecution.StreamExecution;

import jef.database.Session;
import jef.database.SessionFactory;

/**
 * Abstract base class to implement {@link RepositoryQuery}s.
 * 
 * Jiyi
 */
public abstract class AbstractGqQuery implements RepositoryQuery {

	private final GqQueryMethod method;
	private final SessionFactory emf;

	/**
	 * Creates a new {@link AbstractJpaQuery} from the given
	 * {@link GqQueryMethod}.
	 * 
	 * @param method
	 * @param resultFactory
	 * @param em
	 */
	public AbstractGqQuery(GqQueryMethod method, SessionFactory emf) {
		Assert.notNull(method);
		Assert.notNull(emf);

		this.method = method;
		this.emf = emf;
	}

	public GqQueryMethod getQueryMethod() {
		return method;
	}

	protected GqQueryExecution getExecution() {
		if (method.isProcedureQuery()) {
			throw new UnsupportedOperationException();
		} else if (method.isCollectionQuery()) {
			return new CollectionExecution();
		} else if (method.isStreamQuery()) {
		    return new StreamExecution(new CollectionExecution());
		} else if (method.isPageQuery()) {
			return new PagedExecution(method.getParameters());
		} else if (method.isModifyingQuery()) {
			return method.getClearAutomatically() ? new ModifyingExecution(method, emf) : new ModifyingExecution(method, null);
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
		return emf.getSession();
	}
}

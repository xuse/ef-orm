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

import java.lang.reflect.Method;

import jef.database.NativeQuery;
import jef.database.jpa.JefEntityManagerFactory;
import jef.tools.StringUtils;

import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.orm.jpa.EntityManagerProxy;

import com.github.geequery.springdata.provider.PersistenceProvider;

/**
 * Query lookup strategy to execute finders.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public final class GqQueryLookupStrategy implements QueryLookupStrategy {
	private final EntityManagerProxy em;
	private final Key key;
	private final EvaluationContextProvider provider;
	private final JefEntityManagerFactory emf;

	public GqQueryLookupStrategy(EntityManagerProxy em, Key key, EvaluationContextProvider evaluationContextProvider) {
		this.em = em;
		this.key = key;
		this.provider = evaluationContextProvider;
		this.emf = (JefEntityManagerFactory) em.getEntityManagerFactory();
	}

	@Override
	public RepositoryQuery resolveQuery(Method m, RepositoryMetadata metadata, ProjectionFactory factory, NamedQueries namedQueries) {
		GqQueryMethod method = new GqQueryMethod(m, metadata, factory);
		// boolean flag = m.isCollectionQuery();
		// flag = m.isModifyingQuery();
		// flag = m.isNativeQuery();
		// flag = m.isPageQuery();
		// flag = m.isProcedureQuery();
		// flag = m.isSliceQuery();
		// flag = m.isQueryForEntity();
		// flag = m.isStreamQuery();
		//
		String qName = method.getNamedQueryName();
		String qSql = method.getAnnotatedQuery();
		if (method.isStreamQuery()) {
			throw new UnsupportedOperationException();
		} else if (method.isProcedureQuery()) {
			return new GqProcedureQuery(method, em);
		} else if (StringUtils.isNotEmpty(qSql)) {
			throw new UnsupportedOperationException();
		}
		if (emf.getDefault().hasNamedQuery(qName)) {
			NativeQuery<?> q = (NativeQuery<?>) em.getTargetEntityManager().createNamedQuery(qName);
			return new GqNativeQuery(method, em, q);
		} else {
			return new PartTreeGqQuery(method, em, PersistenceProvider.GEEQUERY);
		}
	}

}

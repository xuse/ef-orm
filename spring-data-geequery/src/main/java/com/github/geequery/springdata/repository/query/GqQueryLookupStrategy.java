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
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.orm.jpa.EntityManagerProxy;

/**
 * Query lookup strategy to execute finders.
 */
public final class GqQueryLookupStrategy implements QueryLookupStrategy {
	private final EntityManagerProxy em;
	private final JefEntityManagerFactory emf;

	public GqQueryLookupStrategy(EntityManagerProxy em) {
		this.em = em;
		this.emf = (JefEntityManagerFactory) em.getEntityManagerFactory();
	}

	@Override
	public RepositoryQuery resolveQuery(Method m, RepositoryMetadata metadata, ProjectionFactory factory, NamedQueries namedQueries) {
		GqQueryMethod method = new GqQueryMethod(m, metadata, factory, emf);
		String qName = method.getNamedQueryName();
		String qSql = method.getAnnotatedQuery();
		if (method.isStreamQuery()) {
			throw new UnsupportedOperationException();
		} else if (method.isProcedureQuery()) {
			return new GqProcedureQuery(method, em);
		} else if (StringUtils.isNotEmpty(qSql)) {
			JefEntityManagerFactory emf = (JefEntityManagerFactory) em.getEntityManagerFactory();
			NativeQuery<?> q;
			if(method.isNativeQuery()){
				q= (NativeQuery<?>) emf.getDefault().createNativeQuery(qSql,method.getReturnedObjectType());
			}else{
				q= (NativeQuery<?>) emf.getDefault().createQuery(qSql,method.getReturnedObjectType());
			}
			return new GqNativeQuery(method, em, q);
		}
		if (emf.getDefault().hasNamedQuery(qName)) {
			JefEntityManagerFactory emf = (JefEntityManagerFactory) em.getEntityManagerFactory();
			NativeQuery<?> q = (NativeQuery<?>) emf.getDefault().createNamedQuery(qName,method.getReturnedObjectType());
			return new GqNativeQuery(method, em, q);
		} else {
			if (qName.endsWith(".".concat(method.getName()))) {
				try {
					return new GqPartTreeQuery(method, em);
				} catch (Exception e) {
					throw new IllegalArgumentException(method + ": " + e.getMessage(), e);
				}
			} else {
				throw new IllegalArgumentException("Named query not found: '" + qName + "' in method" + method);
			}
		}
	}

}

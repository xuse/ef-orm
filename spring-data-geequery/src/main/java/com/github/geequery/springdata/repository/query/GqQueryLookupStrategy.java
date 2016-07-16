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

import jef.database.jpa.JefEntityManagerFactory;

import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.RepositoryQuery;

/**
 * Query lookup strategy to execute finders.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public final class GqQueryLookupStrategy implements QueryLookupStrategy{

	public GqQueryLookupStrategy(JefEntityManagerFactory emf, Key key, EvaluationContextProvider evaluationContextProvider) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, ProjectionFactory factory, NamedQueries namedQueries) {
		// TODO Auto-generated method stub
		return null;
	}

	
}

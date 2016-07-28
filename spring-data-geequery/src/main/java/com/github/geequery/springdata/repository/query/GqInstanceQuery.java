/*
 * Copyright 2008-2014 the original author or authors.
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

import javax.persistence.EntityManager;

import jef.database.NativeQuery;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.RepositoryQuery;

/**
 * Implementation of {@link RepositoryQuery}.
 * 
 * @author Jiyi
 * @author Thomas Darimont
 */
final class GqInstanceQuery extends AbstractGqQuery {

	/**
	 * Creates a new {@link GqInstanceQuery}.
	 */
	private GqInstanceQuery(GqQueryMethod method, EntityManager em) {
		super(method, em);
	}
//
//	@Override
//	protected NativeQuery<?> doCreateQuery(Object[] values) {
//		throw new UnsupportedOperationException();
//	}
//
//	@Override
//	protected NativeQuery<?> doCreateCountQuery(Object[] values) {
//		throw new UnsupportedOperationException();
//	}

	@Override
	protected List<?> getResultList(Object[] values, Pageable page) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Object getSingleResult(Object[] values) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected int executeUpdate(Object[] values) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected int executeDelete(Object[] values) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected long getResultCount(Object[] values) {
		// TODO Auto-generated method stub
		return 0;
	}


}

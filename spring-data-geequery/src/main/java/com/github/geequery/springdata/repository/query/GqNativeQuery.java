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
 * TODO 目前的NativeQuery中的绑定了Session的，实际上传入的EM是一个ProxyEM，
 * 因此实际执行的时候需要从当时的线程上下文中获得真实的EM再进行查询执行才可以。
 * 
 */
final class GqNativeQuery extends AbstractGqQuery {
	
	private NativeQuery<?> query;

	/**
	 * Creates a new {@link GqNativeQuery}.
	 */
	GqNativeQuery(GqQueryMethod method, EntityManager em, NativeQuery<?> nq) {
		super(method, em);

//		this.queryName = method.getNamedQueryName();
//		this.countQueryName = method.getNamedCountQueryName();
//		this.extractor = method.getQueryExtractor();
//		this.countProjection = method.getCountQueryProjection();
//
//		Parameters<?, ?> parameters = method.getParameters();
//
//		if (parameters.hasSortParameter()) {
//			throw new IllegalStateException(String.format("Finder method %s is backed " + "by a NamedQuery and must "
//					+ "not contain a sort parameter as we cannot modify the query! Use @Query instead!", method));
//		}
//
//		this.namedCountQueryIsPresent = hasNamedQuery(em, countQueryName);
//
//		boolean weNeedToCreateCountQuery = !namedCountQueryIsPresent && method.getParameters().hasPageableParameter();
//		boolean cantExtractQuery = !this.extractor.canExtractQuery();
//
//		if (weNeedToCreateCountQuery && cantExtractQuery) {
//			throw QueryCreationException.create(method, CANNOT_EXTRACT_QUERY);
//		}
//
//		if (parameters.hasPageableParameter()) {
//			LOG.warn("Finder method {} is backed by a NamedQuery" + " but contains a Pageable parameter! Sorting delivered "
//					+ "via this Pageable will not be applied!", method);
//		}
	}

	@Override
	protected List<?> getResultList(Object[] values, Pageable page) {
		if(page!=null){
			query.setRange(page.getOffset(), page.getPageSize());
		}
		applyParamters(values);
		return query.getResultList();
	}

	private void applyParamters(Object[] values) {
		// TODO Auto-generated method stub
		
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

//	@Override
//	protected NativeQuery<?> doCreateQuery(Object[] values) {
//		throw new UnsupportedOperationException();
////		return null;
//	}
//
//	@Override
//	protected NativeQuery<?> doCreateCountQuery(Object[] values) {
//		throw new UnsupportedOperationException();
////		return null;
//	}


}

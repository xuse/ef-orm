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
import java.util.NoSuchElementException;

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;

import jef.database.NativeQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.github.geequery.springdata.repository.query.GqParameters.GqParameter;

/**
 * 基于Query的Spring-data查询
 */
final class GqNativeQuery extends AbstractGqQuery {

	private NativeQuery<?> query;
	private Logger log = LoggerFactory.getLogger(this.getClass());

	/**
	 * Creates a new {@link GqNativeQuery}.
	 */
	GqNativeQuery(GqQueryMethod method, EntityManagerFactory em, NativeQuery<?> nq) {
		super(method, em);
		this.query = nq;
	}

	@Override
	protected List<?> getResultList(Object[] values, Pageable page) {
		NativeQuery<?> query = getThreadQuery();
		if (page != null) {
			query.setRange(page.getOffset(), page.getPageSize());
			assertNoSort(page.getSort());
		}
		applyParamters(query, values);
		return query.getResultList();
	}

	private void assertNoSort(Sort sort) {
		if (sort != null) {
			log.warn("The input parameter Sort [" + sort + "]can not be set into a SQL Query, and was ignored.");
		}
	}

	@Override
	protected Object getSingleResult(Object[] values) {
		NativeQuery<?> query = getThreadQuery();
		applyParamters(query, values);
		return query.getSingleResult();
	}

	@Override
	protected int executeUpdate(Object[] values) {
		NativeQuery<?> query = getThreadQuery();
		applyParamters(query, values);
		return query.executeUpdate();
	}

	@Override
	protected int executeDelete(Object[] values) {
		NativeQuery<?> query = getThreadQuery();
		applyParamters(query, values);
		return query.executeUpdate();
	}

	@Override
	protected long getResultCount(Object[] values) {
		NativeQuery<?> query = getThreadQuery();
		applyParamters(query, values);
		return query.getResultCount();
	}

	private void applyParamters(NativeQuery<?> query, Object[] values) {
		GqParameters ps = getQueryMethod().getParameters();
		int i = 0;
		for (GqParameter p : ps) {
			Object obj = values[i++];
			if (p.isSpecialParameter()) {
				if(Sort.class.isAssignableFrom(p.getType())){
					assertNoSort((Sort)obj);
				}
				continue;
			}
			
			if (p.getIgnoreIf() != null && QueryUtils.isIgnore(p.getIgnoreIf(), obj)) {
				continue;
			}
			if (p.isNamedParameter()) {
				try {
					query.setParameter(p.getName(), obj);
				} catch (NoSuchElementException e) {
					throw new PersistenceException("The parameter [:" + p.getName() + "] is not defined in the query '" + super.getQueryMethod().getName()
							+ "', please make sure there is \":name\" expression in the Query.", e);
				}
			} else {
				try {
					// 写在Query中的参数一般都是 ?1 ?2从1开始的，而方法的参数序号是从0开始的，因此+1
					query.setParameter(p.getIndex() + 1, obj);
				} catch (NoSuchElementException e) {
					throw new PersistenceException("The parameter [?" + (p.getIndex() + 1) + "] is not defined in the query '" + super.getQueryMethod().getName()
							+ "', please make sure that using @Param(\"name\") to mapping parameter into query.", e);
				}
			}
		}
	}

	private NativeQuery<?> getThreadQuery() {
		return query.rebind(getSession(), null);
	}
}

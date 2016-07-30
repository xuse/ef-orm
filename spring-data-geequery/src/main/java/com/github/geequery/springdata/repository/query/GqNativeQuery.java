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

import jef.database.NativeQuery;

import org.springframework.data.domain.Pageable;
import org.springframework.orm.jpa.EntityManagerProxy;

import com.github.geequery.springdata.repository.query.GqParameters.JpaParameter;

/**
 * TODO 目前的NativeQuery中的绑定了Session的，实际上传入的EM是一个ProxyEM，
 * 因此实际执行的时候需要从当时的线程上下文中获得真实的EM再进行查询执行才可以。
 * 
 */
final class GqNativeQuery extends AbstractGqQuery {

	private NativeQuery<?> query;

	private EntityManagerProxy pxy;
	/**
	 * Creates a new {@link GqNativeQuery}.
	 */
	GqNativeQuery(GqQueryMethod method, EntityManagerProxy em, NativeQuery<?> nq) {
		super(method, em);
		this.query = nq;
		this.pxy=em;
	}

	@Override
	protected List<?> getResultList(Object[] values, Pageable page) {
		NativeQuery<?> query = getThreadQuery();
		if (page != null) {
			query.setRange(page.getOffset(), page.getPageSize());
		}
		applyParamters(query,values);
		return query.getResultList();
	}

	@Override
	protected Object getSingleResult(Object[] values) {
		NativeQuery<?> query = getThreadQuery();
		applyParamters(query,values);
		return query.getSingleResult();
	}

	@Override
	protected int executeUpdate(Object[] values) {
		NativeQuery<?> query = getThreadQuery();
		applyParamters(query,values);
		return query.executeUpdate();
	}

	@Override
	protected int executeDelete(Object[] values) {
		NativeQuery<?> query = getThreadQuery();
		applyParamters(query,values);
		return query.executeUpdate();
	}

	@Override
	protected long getResultCount(Object[] values) {
		NativeQuery<?> query = getThreadQuery();
		applyParamters(query,values);
		return query.getResultCount();
	}

	private void applyParamters(NativeQuery<?> query,Object[] values) {
		GqParameters ps=getQueryMethod().getParameters();
		int i=0;
		for(JpaParameter p:ps){
			query.setParameter(p.getName(), values[i++]);
		}
	}

	private NativeQuery<?> getThreadQuery() {
		return query.clone(getSession(), null);
	}
}

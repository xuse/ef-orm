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
package com.github.geequery.springdata.repository.support;

import java.util.List;

import javax.persistence.EntityManager;

import jef.database.jpa.JefEntityManagerFactory;
import jef.database.query.ConditionQuery;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.github.geequery.springdata.repository.GqRepository;
import com.github.geequery.springdata.repository.JpaSpecificationExecutor;

/**
 * Default implementation of the
 * {@link org.springframework.data.repository.CrudRepository} interface. This
 * will offer you a more sophisticated interface than the plain
 * {@link EntityManager} .
 * 
 * @author Oliver Gierke
 * @author Eberhard Wolff
 * @author Thomas Darimont
 * @author Mark Paluch
 * @param <T>
 *            the type of the entity to handle
 * @param <ID>
 *            the type of the entity's identifier
 */
@Repository
@Transactional(readOnly = true)
public class GqRepositoryImpl<T> implements GqRepository<T>, JpaSpecificationExecutor<T> {
	
	private MetamodelInformation<T> meta;
	//这是Spring的SharedEntityManager的代理，只可从中提取EMF，不可直接转换，因此这个EM上携带了基于线程的事务上下文
	private EntityManager em;

	public GqRepositoryImpl(MetamodelInformation<T> meta, EntityManager emf) {
		this.meta = meta;
		this.em = emf;
	}

	@Override
	public Page<T> findAll(Pageable pageable) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <S extends T> S save(S entity) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public T findOne(IdValues id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean exists(IdValues id) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public long count() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void delete(IdValues id) {
		// TODO Auto-generated method stub

	}

	@Override
	public void delete(T entity) {
		// TODO Auto-generated method stub

	}

	@Override
	public void delete(Iterable<? extends T> entities) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteAll() {
		// TODO Auto-generated method stub

	}

	@Override
	public <S extends T> S findOne(Example<S> example) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <S extends T> Page<S> findAll(Example<S> example, Pageable pageable) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <S extends T> long count(Example<S> example) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public <S extends T> boolean exists(Example<S> example) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public T findOne(ConditionQuery spec) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<T> findAll(ConditionQuery spec) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Page<T> findAll(ConditionQuery spec, Pageable pageable) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<T> findAll(ConditionQuery spec, Sort sort) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long count(ConditionQuery spec) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<T> findAll() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<T> findAll(Sort sort) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<T> findAll(Iterable<IdValues> ids) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <S extends T> List<S> save(Iterable<S> entities) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void flush() {
		// TODO Auto-generated method stub

	}

	@Override
	public <S extends T> S saveAndFlush(S entity) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteInBatch(Iterable<T> entities) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteAllInBatch() {
		// TODO Auto-generated method stub

	}

	@Override
	public T getOne(IdValues id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <S extends T> List<S> findAll(Example<S> example) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <S extends T> List<S> findAll(Example<S> example, Sort sort) {
		// TODO Auto-generated method stub
		return null;
	}

}

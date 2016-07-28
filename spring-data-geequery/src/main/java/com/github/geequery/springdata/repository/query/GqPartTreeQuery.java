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

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import jef.database.DbUtils;
import jef.database.QB;
import jef.database.Session;
import jef.database.jpa.JefEntityManager;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.database.query.Query;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.EntityManagerProxy;

import com.github.geequery.springdata.provider.PersistenceProvider;
import com.github.geequery.springdata.repository.query.GqQueryExecution.DeleteExecution;

/**
 * A {@link AbstractJpaQuery} implementation based on a {@link PartTree}.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public class GqPartTreeQuery extends AbstractGqQuery {

	private final ITableMetadata domainClass;
	private final PartTree tree;
	private final GqParameters parameters;
	private final EntityManagerProxy em;

	/**
	 * Creates a new {@link PartTreeJpaQuery}.
	 * 
	 * @param method
	 *            must not be {@literal null}.
	 * @param factory
	 *            must not be {@literal null}.
	 * @param em
	 *            must not be {@literal null}.
	 */
	public GqPartTreeQuery(GqQueryMethod method, EntityManagerProxy em, PersistenceProvider persistenceProvider) {
		super(method, em);
		this.em = em;
		this.domainClass = MetaHolder.getMeta(method.getEntityInformation().getJavaType());
		this.tree = new PartTree(method.getName(), domainClass.getThisType());
		this.parameters = method.getParameters();
		boolean recreationRequired = parameters.hasDynamicProjection() || parameters.potentiallySortsDynamically();
		boolean isCount = tree.isCountProjection();
	}

	@Override
	protected GqQueryExecution getExecution() {
		return this.tree.isDelete() ? new DeleteExecution(em) : super.getExecution();
	}

	private Query<?> createQuery(Object[] values) {
		Query<?> q=QB.create(domainClass);
		ParametersParameterAccessor accessor = new ParametersParameterAccessor(parameters, values);
		Pageable page=accessor.getPageable();
		Sort sort=accessor.getSort();
		
		
		
		
		return q;
	}

	private GqQueryCreator createCreator(ParametersParameterAccessor accessor, PersistenceProvider persistenceProvider) {
		return new GqQueryCreator(tree,this.domainClass);
	}

	@Override
	protected List<?> getResultList(Object[] values, Pageable page) {
		Query<?> q=createQuery(values);
		if(page!=null){
			setPageOrder(q,page);
		}
		try{
			return getSession().select(q);
		}catch(SQLException e){
			throw DbUtils.toRuntimeException(e);
		}
	}

	private void setPageOrder(Query<?> q, Pageable page) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected Object getSingleResult(Object[] values) {
		Query<?> q=createQuery(values);
		try{
			return getSession().load(q,false);
		}catch(SQLException e){
			throw DbUtils.toRuntimeException(e);
		}
	}

	@Override
	protected int executeUpdate(Object[] values) {
		Query<?> q=createQuery(values);
		try{
			return getSession().update(q.getInstance());
		}catch(SQLException e){
			throw DbUtils.toRuntimeException(e);
		}
	}

	@Override
	protected int executeDelete(Object[] values) {
		Query<?> q=createQuery(values);
		try{
			return getSession().delete(q);
		}catch(SQLException e){
			throw DbUtils.toRuntimeException(e);
		}
	}

	@Override
	protected long getResultCount(Object[] values) {
		Query<?> q=createQuery(values);
		try{
			return getSession().countLong(q);
		}catch(SQLException e){
			throw DbUtils.toRuntimeException(e);
		}
	}
	
	private Session getSession(){
		EntityManagerFactory emf=em.getEntityManagerFactory();
		EntityManager em=EntityManagerFactoryUtils.doGetTransactionalEntityManager(emf,null);
		if(em==null){ //当无事务时。Spring返回null
			em=emf.createEntityManager(null,Collections.EMPTY_MAP);
		}	
		if(em instanceof JefEntityManager){
			return ((JefEntityManager) em).getSession();
		}
		throw new IllegalArgumentException(em.getClass().getName());
	}

}

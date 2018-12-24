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
package com.github.geequery.springdata.repository.support;

import java.io.Serializable;

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.RepositoryDefinition;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.core.support.TransactionalRepositoryFactoryBeanSupport;

import jef.database.SessionFactory;
import jef.tools.StringUtils;
import jef.tools.reflect.ClassEx;
import jef.tools.reflect.FieldEx;

/**
 * Special adapter for Springs
 * {@link org.springframework.beans.factory.FactoryBean} interface to allow easy
 * setup of repository factories via Spring configuration.
 * 
 * @param <T> the type of the repository
 */
public class GqRepositoryFactoryBean<T extends Repository<S, ID>, S, ID extends Serializable> extends TransactionalRepositoryFactoryBeanSupport<T, S, ID> implements ApplicationContextAware {

	private ConfigurableApplicationContext context;
	private Class<?> repositoryInterface;

	private String namedQueryLocation;
	private String entityManagerFactoryRef;
	private String repositoryImplementationPostfix = "Impl";

	private static Logger log = LoggerFactory.getLogger(GqRepositoryFactoryBean.class);

	protected GqRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
		super(repositoryInterface);
		this.repositoryInterface = repositoryInterface;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport
	 * # setMappingContext(org.springframework.data.mapping.context.MappingContext )
	 */
	@Override
	public void setMappingContext(MappingContext<?, ?> mappingContext) {
		super.setMappingContext(mappingContext);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.repository.support.
	 * TransactionalRepositoryFactoryBeanSupport#doCreateRepositoryFactory()
	 */
	@Override
	protected RepositoryFactorySupport doCreateRepositoryFactory() {
		SessionFactory emf;
		if (entityManagerFactoryRef == null) {
			emf = context.getBean(SessionFactory.class);
		} else {
			emf = context.getBean(entityManagerFactoryRef, SessionFactory.class);
		}
		return new GqRepositoryFactory(emf);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() {
		Object custom = generateCustomImplementation();
		if (custom != null)
			this.setCustomImplementation(custom);
		super.afterPropertiesSet();

	}

	private Object generateCustomImplementation() {
		EntityManagerFactory emf = context.getBean(entityManagerFactoryRef, EntityManagerFactory.class);
		for (Class<?> clz : repositoryInterface.getInterfaces()) {
			if (Repository.class.isAssignableFrom(clz)) {
				continue;
			} else if (clz.getAnnotation(RepositoryDefinition.class) != null) {
				continue;
			}
			String implementation = clz.getName() + StringUtils.trimToEmpty(repositoryImplementationPostfix);
			ClassEx implClz = ClassEx.forName(implementation);
			if (implClz == null) {
				log.error("Lack of implementation class: " + clz.getName());
				throw new IllegalArgumentException("Lack of implementation class: " + implementation);
			}
			try {
				Object obj = implClz.newInstance();
				for (FieldEx field : implClz.getDeclaredFields()) {
					if (field.getAnnotation(PersistenceContext.class) != null) {
						field.set(obj, emf);
					}
				}
				if (obj instanceof ApplicationContextAware) {
					((ApplicationContextAware) obj).setApplicationContext(context);
				}
				if (obj instanceof InitializingBean) {
					((InitializingBean) obj).afterPropertiesSet();
				}
				return obj;
			} catch (Exception ex) {
				log.error("", ex);
				return null;
			}
		}
		return null;
	}

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.context = (ConfigurableApplicationContext) context;
	}

	public void setNamedQueryLocation(String namedQueryLocation) {
		this.namedQueryLocation = namedQueryLocation;
	}

	public void setEntityManagerFactoryRef(String entityManagerFactoryRef) {
		this.entityManagerFactoryRef = entityManagerFactoryRef;
	}

	public void setRepositoryImplementationPostfix(String repositoryImplementationPostfix) {
		if (repositoryImplementationPostfix != null)
			this.repositoryImplementationPostfix = repositoryImplementationPostfix;
	}

}

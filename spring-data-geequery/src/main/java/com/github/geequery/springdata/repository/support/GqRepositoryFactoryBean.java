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

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import jef.tools.reflect.ClassEx;
import jef.tools.reflect.FieldEx;

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
import org.springframework.util.Assert;

/**
 * Special adapter for Springs
 * {@link org.springframework.beans.factory.FactoryBean} interface to allow easy
 * setup of repository factories via Spring configuration.
 * 
 * @param <T>
 *            the type of the repository
 */
public class GqRepositoryFactoryBean<T extends Repository<S, ID>, S, ID extends Serializable> extends TransactionalRepositoryFactoryBeanSupport<T, S, ID> implements ApplicationContextAware {

	protected GqRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
		super(repositoryInterface);
		this.repositoryInterface = repositoryInterface;
	}

	private EntityManager em;
	private ConfigurableApplicationContext context;
	private Class<?> repositoryInterface;

	private static Logger log = LoggerFactory.getLogger(GqRepositoryFactoryBean.class);

	/**
	 * The {@link EntityManager} to be used.
	 * 
	 * @param entityManager
	 *            the entityManager to set
	 */
	@PersistenceContext
	public void setEntityManager(EntityManager entityManager) {
		this.em = entityManager;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport
	 * #
	 * setMappingContext(org.springframework.data.mapping.context.MappingContext
	 * )
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
		return createRepositoryFactory(em);
	}

	/**
	 * Returns a {@link RepositoryFactorySupport}.
	 * 
	 * @param entityManager
	 * @return
	 */
	protected RepositoryFactorySupport createRepositoryFactory(EntityManager entityManager) {
		return new GqRepositoryFactory(entityManager);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() {
		Assert.notNull(em, "EntityManager must not be null!");
		Object custom = generateCustomImplementation();
		if (custom != null)
			this.setCustomImplementation(custom);
		super.afterPropertiesSet();

	}

	/*
	 * FIXME
	 * 土法炼钢：看spring-data源码，由于使用太多容器注入特性，没法发现CustomImplementation对象是从哪里创建出来的。
	 * （虽然说做个案例 DEBUG一下应该能跟出来，但最近实在没时间……）
	 * 所以这里简单粗暴的将自定义的扩展Repository构造出来用了再说。以后有时间还是要修改得更优雅一点。
	 */
	private Object generateCustomImplementation() {
		for (Class<?> clz : repositoryInterface.getInterfaces()) {
			if (Repository.class.isAssignableFrom(clz)) {
				continue;
			} else if (clz.getAnnotation(RepositoryDefinition.class) != null) {
				continue;
			}
			ClassEx implClz = ClassEx.forName(clz.getName() + "Impl");
			if (implClz == null) {
				log.error("Lack of implementation of class: " + clz.getName());
			}
			try {
				Object obj = implClz.newInstance();
				for (FieldEx field : implClz.getDeclaredFields()) {
					if (field.getAnnotation(PersistenceContext.class) != null) {
						field.set(obj, em);
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
}

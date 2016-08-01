/*
 * Copyright 2015 the original author or authors.
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

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.springframework.util.Assert;

import com.github.geequery.springdata.repository.JpaContext;

public class DefaultGqContext implements JpaContext {

	private final EntityManager entityManagers;

	public DefaultGqContext(EntityManagerFactory entityManagers) {
		Assert.notNull(entityManagers, "EntityManagers must not be null!");
		this.entityManagers = entityManagers.createEntityManager();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.geequery.springdata.repository.JpaContext#getByManagedType
	 * (java.lang.Class)
	 */
	@Override
	public EntityManager getEntityManagerByManagedType(Class<?> type) {
		return entityManagers;
	}
}

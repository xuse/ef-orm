/*
 * Copyright 2011 the original author or authors.
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

import org.springframework.data.domain.Persistable;
import org.springframework.data.repository.core.support.AbstractEntityInformation;
import org.springframework.util.Assert;

import jef.database.jpa.MetaProvider;
import jef.database.meta.ITableMetadata;

/**
 * Base class for {@link JpaEntityInformation} implementations to share common method implementations.
 * 
 * @author Oliver Gierke
 */
public abstract class JpaEntityInformationSupport<T, ID extends Serializable> extends AbstractEntityInformation<T, ID>
		implements JpaEntityInformation<T, ID> {

	private ITableMetadata metadata;

	/**
	 * Creates a new {@link JpaEntityInformationSupport} with the given domain class.
	 * 
	 * @param domainClass must not be {@literal null}.
	 */
	@SuppressWarnings("unchecked")
	public JpaEntityInformationSupport(ITableMetadata metadata) {
		super((Class<T>) metadata.getThisType());
		this.metadata = metadata;
	}

	/**
	 * Creates a {@link JpaEntityInformation} for the given domain class and {@link EntityManager}.
	 * 
	 * @param domainClass must not be {@literal null}.
	 * @param em must not be {@literal null}.
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T> JpaEntityInformation<T, ?> getEntityInformation(Class<T> domainClass, MetaProvider emf) {

		Assert.notNull(domainClass);
		Assert.notNull(emf);

		MetaProvider metamodel = emf;

		if (Persistable.class.isAssignableFrom(domainClass)) {
			return new JpaPersistableEntityInformation(domainClass, metamodel);
		} else {
			return new JpaMetamodelEntityInformation(domainClass, metamodel);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.github.geequery.springdata.repository.support.JpaEntityMetadata#getEntityName()
	 */
	public String getEntityName() {
		return metadata.getName();
	}
}

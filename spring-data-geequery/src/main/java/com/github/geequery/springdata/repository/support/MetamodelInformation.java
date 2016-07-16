/*
 * Copyright 2011-2015 the original author or authors.
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.SingularAttribute;

import jef.database.DbUtils;
import jef.database.IQueryableEntity;
import jef.database.dialect.type.ColumnMapping;
import jef.database.jpa.MetaProvider;
import jef.database.meta.AbstractMetadata;
import jef.database.meta.ITableMetadata;

import org.springframework.beans.BeanWrapper;
import org.springframework.data.repository.core.support.AbstractEntityInformation;
import org.springframework.data.util.DirectFieldAccessFallbackBeanWrapper;
import org.springframework.util.Assert;

/**
 * Implementation of
 * {@link org.springframework.data.repository.core.EntityInformation} that uses
 * GeeQuery metamodel
 * 
 * 由于标准JPA模型的复合主键处理方式一般为ID class，而本框架推荐使用多值，因此干脆统一所有的主键定义为Object[]
 * 
 * @author Jiyi
 */
public class MetamodelInformation<T> extends AbstractEntityInformation<T, IdValues> implements GQEntityInformation<T,IdValues> {

	private ITableMetadata metadata;

	
	private final IdMetadata<T> idMetadata;
	private final SingularAttribute<? super T, ?> versionAttribute;
	private final MetaProvider metamodel;
	private final String entityName;

	/**
	 * Creates a new {@link MetamodelInformation} for the given domain class and
	 * {@link Metamodel}.
	 * 
	 * @param domainClass
	 *            must not be {@literal null}.
	 * @param metamodel
	 *            must not be {@literal null}.
	 */
	public MetamodelInformation(Class<T> domainClass, MetaProvider metamodel) {
		super(domainClass);
		Assert.notNull(metamodel);
		this.metamodel = metamodel;
		AbstractMetadata type = metamodel.managedType(domainClass);
		if (type == null) {
			throw new IllegalArgumentException("The given domain class can not be found in the given Metamodel!");
		}
		this.metadata=	type;
		this.entityName = type.getName();
		this.idMetadata = new IdMetadata<T>(type);
		this.versionAttribute = null;
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.geequery.springdata.repository.support.JpaEntityInformationSupport
	 * #getEntityName()
	 */
	@Override
	public String getEntityName() {
		return entityName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.data.repository.core.EntityInformation#getId(java
	 * .lang.Object)
	 */
	public IdValues getId(T entity) {
		return new IdValues(DbUtils.getPKValueSafe((IQueryableEntity)entity).toArray());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.data.repository.core.EntityInformation#getIdType()
	 */
	public Class<IdValues> getIdType() {
		return IdValues.class;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.geequery.springdata.repository.support.JpaEntityInformation
	 * #getIdAttribute()
	 */
	public ColumnMapping getIdAttribute() {
		return idMetadata.getSimpleIdAttribute();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.geequery.springdata.repository.support.JpaEntityInformation
	 * #hasCompositeId()
	 */
	public boolean hasCompositeId() {
		return !idMetadata.hasSimpleId();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.geequery.springdata.repository.support.JpaEntityInformation
	 * #getIdAttributeNames()
	 */
	public Iterable<String> getIdAttributeNames() {

		List<String> attributeNames = new ArrayList<String>(idMetadata.attributes.size());

		for (ColumnMapping attribute : idMetadata.attributes) {
			attributeNames.add(attribute.fieldName());
		}

		return attributeNames;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.geequery.springdata.repository.support.JpaEntityInformation
	 * #getCompositeIdAttributeValue(java.io.Serializable, java.lang.String)
	 */
	public Object getCompositeIdAttributeValue(Serializable id, String idAttribute) {
		Assert.isTrue(hasCompositeId());
		return new DirectFieldAccessFallbackBeanWrapper(id).getPropertyValue(idAttribute);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.data.repository.core.support.AbstractEntityInformation
	 * #isNew(java.lang.Object)
	 */
	@Override
	public boolean isNew(T entity) {

		if (versionAttribute == null || versionAttribute.getJavaType().isPrimitive()) {
			return super.isNew(entity);
		}

		BeanWrapper wrapper = new DirectFieldAccessFallbackBeanWrapper(entity);
		Object versionValue = wrapper.getPropertyValue(versionAttribute.getName());

		return versionValue == null;
	}

	/**
	 * Simple value object to encapsulate id specific metadata.
	 * 
	 * @author Oliver Gierke
	 * @author Thomas Darimont
	 */
	private static class IdMetadata<T> implements Iterable<ColumnMapping> {
		private final AbstractMetadata type;
		private final Set<ColumnMapping> attributes;

		public IdMetadata(AbstractMetadata source) {
			this.type = source;
			Set<ColumnMapping> set = new LinkedHashSet<ColumnMapping>();
			for (ColumnMapping mapping : source.getPKFields()) {
				set.add(mapping);
			}
			this.attributes = set;
		}

		public boolean hasSimpleId() {
			return attributes.size() == 1;
		}

		public ColumnMapping getSimpleIdAttribute() {
			return attributes.iterator().next();
		}

		public Iterator<ColumnMapping> iterator() {
			return attributes.iterator();
		}
	}
}

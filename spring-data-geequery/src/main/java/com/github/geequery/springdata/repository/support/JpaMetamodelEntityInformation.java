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

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.data.util.DirectFieldAccessFallbackBeanWrapper;
import org.springframework.util.Assert;

import jef.database.dialect.type.ColumnMapping;
import jef.database.jpa.MetaProvider;
import jef.database.meta.AbstractMetadata;
import jef.database.meta.EntityType;

/**
 * Implementation of {@link org.springframework.data.repository.core.EntityInformation} that uses JPA {@link Metamodel}
 * to find the domain class' id field.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Christoph Strobl
 */
public class JpaMetamodelEntityInformation<T, ID extends Serializable> extends JpaEntityInformationSupport<T, ID> {

	private final IdMetadata<T> idMetadata;
	private final SingularAttribute<? super T, ?> versionAttribute;
	private final MetaProvider metamodel;
	private final String entityName;

	/**
	 * Creates a new {@link JpaMetamodelEntityInformation} for the given domain class and {@link Metamodel}.
	 * 
	 * @param domainClass must not be {@literal null}.
	 * @param metamodel must not be {@literal null}.
	 */
	public JpaMetamodelEntityInformation(Class<T> domainClass, MetaProvider metamodel) {
		super(metamodel.managedType(domainClass));

		Assert.notNull(metamodel);
		this.metamodel = metamodel;

		AbstractMetadata type = metamodel.managedType(domainClass);

		if (type == null) {
			throw new IllegalArgumentException("The given domain class can not be found in the given Metamodel!");
		}

		this.entityName = type.getName();
		this.idMetadata = new IdMetadata<T>(type);
		this.versionAttribute = null;
	}

	/*
	 * (non-Javadoc)
	 * @see com.github.geequery.springdata.repository.support.JpaEntityInformationSupport#getEntityName()
	 */
	@Override
	public String getEntityName() {
		return entityName != null ? entityName : super.getEntityName();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.EntityInformation#getId(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	public ID getId(T entity) {

		BeanWrapper entityWrapper = new DirectFieldAccessFallbackBeanWrapper(entity);

		if (idMetadata.hasSimpleId()) {
			return (ID) entityWrapper.getPropertyValue(idMetadata.getSimpleIdAttribute().fieldName());
		}

		BeanWrapper idWrapper = new IdentifierDerivingDirectFieldAccessFallbackBeanWrapper(idMetadata.getType(), metamodel);
		boolean partialIdValueFound = false;

		for (ColumnMapping attribute : idMetadata) {
			Object propertyValue = entityWrapper.getPropertyValue(attribute.fieldName());

			if (propertyValue != null) {
				partialIdValueFound = true;
			}

			idWrapper.setPropertyValue(attribute.fieldName(), propertyValue);
		}

		return (ID) (partialIdValueFound ? idWrapper.getWrappedInstance() : null);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.EntityInformation#getIdType()
	 */
	@SuppressWarnings("unchecked")
	public Class<ID> getIdType() {
		return (Class<ID>) idMetadata.getType();
	}

	/*
	 * (non-Javadoc)
	 * @see com.github.geequery.springdata.repository.support.JpaEntityInformation#getIdAttribute()
	 */
	public ColumnMapping getIdAttribute() {
		return idMetadata.getSimpleIdAttribute();
	}

	/*
	 * (non-Javadoc)
	 * @see com.github.geequery.springdata.repository.support.JpaEntityInformation#hasCompositeId()
	 */
	public boolean hasCompositeId() {
		return !idMetadata.hasSimpleId();
	}

	/*
	 * (non-Javadoc)
	 * @see com.github.geequery.springdata.repository.support.JpaEntityInformation#getIdAttributeNames()
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
	 * @see com.github.geequery.springdata.repository.support.JpaEntityInformation#getCompositeIdAttributeValue(java.io.Serializable, java.lang.String)
	 */
	public Object getCompositeIdAttributeValue(Serializable id, String idAttribute) {
		Assert.isTrue(hasCompositeId());
		return new DirectFieldAccessFallbackBeanWrapper(id).getPropertyValue(idAttribute);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.AbstractEntityInformation#isNew(java.lang.Object)
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
			Set<ColumnMapping> set=new LinkedHashSet<ColumnMapping>();
			for(ColumnMapping mapping:source.getPKFields()){
				set.add(mapping);
			}
			this.attributes=set;
		}

		public boolean hasSimpleId() {
			return attributes.size() == 1;
		}

		public Class<?> getType() {
			if(attributes.isEmpty()){
				return null;
			}else if(attributes.size()>1){
				throw new UnsupportedOperationException();
			}else{
				return attributes.iterator().next().getFieldType();
			}
		}

		public ColumnMapping getSimpleIdAttribute() {
			return attributes.iterator().next();
		}

		public Iterator<ColumnMapping> iterator() {
			return attributes.iterator();
		}
	}

	/**
	 * Custom extension of {@link DirectFieldAccessFallbackBeanWrapper} that allows to derived the identifier if composite
	 * keys with complex key attribute types (e.g. types that are annotated with {@code @Entity} themselves) are used.
	 * 
	 * @author Thomas Darimont
	 */
	private static class IdentifierDerivingDirectFieldAccessFallbackBeanWrapper
			extends DirectFieldAccessFallbackBeanWrapper {

		private final MetaProvider metamodel;

		public IdentifierDerivingDirectFieldAccessFallbackBeanWrapper(Class<?> type, MetaProvider metamodel) {
			super(type);
			this.metamodel = metamodel;
		}

		/**
		 * In addition to the functionality described in {@link BeanWrapperImpl} it is checked whether we have a nested
		 * entity that is part of the id key. If this is the case, we need to derive the identifier of the nested entity.
		 * 
		 * @see com.github.geequery.springdata.repository.support.JpaMetamodelEntityInformation.DirectFieldAccessFallbackBeanWrapper#setPropertyValue(java.lang.String,
		 *      java.lang.Object)
		 */
		@Override
		public void setPropertyValue(String propertyName, Object value) {

			if (isIdentifierDerivationNecessary(value)) {

				// Derive the identifer from the nested entity that is part of the composite key.
				@SuppressWarnings({ "rawtypes", "unchecked" })
				JpaMetamodelEntityInformation nestedEntityInformation = new JpaMetamodelEntityInformation(value.getClass(),
						this.metamodel);
				Object nestedIdPropertyValue = new DirectFieldAccessFallbackBeanWrapper(value)
						.getPropertyValue(nestedEntityInformation.getIdAttribute().fieldName());
				super.setPropertyValue(propertyName, nestedIdPropertyValue);

				return;
			}

			super.setPropertyValue(propertyName, value);
		}

		/**
		 * @param value
		 * @return {@literal true} if the given value is not {@literal null} and a mapped persistable entity otherwise
		 *         {@literal false}
		 */
		private boolean isIdentifierDerivationNecessary(Object value) {

			if (value == null) {
				return false;
			}

			try {
				AbstractMetadata managedType = this.metamodel.managedType(value.getClass());
				return managedType != null && managedType.getType() == EntityType.NATIVE;
			} catch (IllegalArgumentException iae) {
				// no mapped type
				return false;
			}
		}
	}
}

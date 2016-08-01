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
import java.util.List;

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
public class MetamodelInformation<T,ID extends Serializable> extends AbstractEntityInformation<T, ID> implements GQEntityInformation<T,ID> {

	private ITableMetadata metadata;

	
	private final SingularAttribute<? super T, ?> versionAttribute;
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
		AbstractMetadata type = metamodel.managedType(domainClass);
		if (type == null) {
			throw new IllegalArgumentException("The given domain class can not be found in the given Metamodel!");
		}
		this.metadata=	type;
		this.entityName = type.getName();
		this.versionAttribute = null;
	}

	public ITableMetadata getMetadata() {
		return metadata;
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
	@SuppressWarnings("unchecked")
	public ID getId(T entity) {
		if(metadata.getPKFields().size()==1){
			return (ID)DbUtils.getPKValueSafe((IQueryableEntity)entity).get(0);
		}else{
			return (ID) DbUtils.getPKValueSafe((IQueryableEntity)entity);			
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.data.repository.core.EntityInformation#getIdType()
	 */
	@SuppressWarnings("unchecked")
	public Class<ID> getIdType() {
		if(metadata.getPKFields().size()==1){
			return (Class<ID>) metadata.getPKFields().get(0).getFieldType();
		}
		return (Class<ID>) List.class;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.geequery.springdata.repository.support.JpaEntityInformation
	 * #getIdAttribute()
	 */
	public ColumnMapping getIdAttribute() {
		List<ColumnMapping> columns=metadata.getPKFields();
		if(columns.size()!=1){
			throw new UnsupportedOperationException();
		}
		return columns.get(0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.geequery.springdata.repository.support.JpaEntityInformation
	 * #hasCompositeId()
	 */
	public boolean hasCompositeId() {
		return metadata.getPKFields().size()>1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * #getIdAttributeNames()
	 */
	public Iterable<String> getIdAttributeNames() {
		List<ColumnMapping> columns=metadata.getPKFields();
		List<String> result=new ArrayList<String>(columns.size());
		for(ColumnMapping c:columns){
			result.add(c.fieldName());
		}
		return result;
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

	public boolean isComplexPK() {
		return this.metadata.getPKFields().size()>1;
	}
}

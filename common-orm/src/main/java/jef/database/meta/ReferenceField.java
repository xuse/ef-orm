/*
 * JEF - Copyright 2009-2010 Jiyi (mr.jiyi@gmail.com)
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
package jef.database.meta;

import jef.database.DbUtils;
import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.ColumnMapping;
import jef.database.query.SqlContext;
import jef.tools.Assert;

/**
 * 描述框架内部对象之间关联的关系字段
 * @author jiyi
 *
 */
public final class ReferenceField extends AbstractRefField implements IReferenceColumn{
	/**
	 * 引用到目标对象的某个字段上，如果为null则表示引用对象本身。
	 * 如果不是完全引用对象本身，不会触发关联操作，只有在Load时会自动装载填充这些字段。
	 *  举例：
	 *   Class Person 有Field: name, departmenetId,现新建一个ReferenceField departmentName，引用到
	 *   Department表的Name字段上。 （Many VS One）
	 *   此时每次载入Person对象时，会自动载入DepartmenetName。但是该对字段作任何修改，都不会保存到数据库中去。
	 */
	private ColumnMapping<?> targetField;
	/**
	 * 构造
	 * @param fName  字段名
	 * @param ref  关联关系
	 * @param fieldName 目标字段 （为null表示持有整个对象）
	 */
	public ReferenceField(Class<?> container,String fName,Reference ref,ColumnMapping<?> fieldName,CascadeConfig config) {
		super(container,fName,ref,config==null?null:config.asMap);
		Assert.notNull(fieldName);
		this.targetField=fieldName;
	}
	public ColumnMapping<?> getTargetField() {
		return targetField;
	}

	public String getSelectedAlias(String alias,DatabaseDialect profile) {
		return 	new StringBuilder(36).append(profile.getColumnNameToUse(alias)).append(SqlContext.DIVEDER)
			.append(profile.getColumnNameToUse(targetField.fieldName())).toString();
	}
	
	@Override
	public String getResultAlias(String alias, DatabaseDialect profile) {
		return 	new StringBuilder(36).append(alias.toUpperCase()).append(SqlContext.DIVEDER)
				.append(targetField.fieldName().toUpperCase()).toString();
	}

	public String getSelectItem(DatabaseDialect profile,String tableAlias,SqlContext context) {
		if(targetField==null)return null;
		return DbUtils.toColumnName(targetField, profile, tableAlias);
	}
	
	public ISelectProvider toNestedDesc(String lastName){
		if(lastName==null){
			return this;
		}
		return new NestedReferenceField(lastName.concat(".").concat(getName()));
	}

	public boolean isSingleColumn() {
		return true;
	}
	

	public ColumnMapping<?> getTargetColumnType() {
		return targetField;
	}
	
	private final class NestedReferenceField implements IReferenceColumn{
		String name;
		public NestedReferenceField(String newName) {
			this.name=newName;
		}
		public String getName() {
			return name;
		}
		public int getProjection() {
			return 0;
		}
		public String getSelectItem(DatabaseDialect profile, String tableAlias,SqlContext context) {
			return ReferenceField.this.getSelectItem(profile, tableAlias,context);
		}
		public String getSelectedAlias(String tableAlias, DatabaseDialect profile) {
			return ReferenceField.this.getSelectedAlias(tableAlias,profile);
		}
		public boolean isSingleColumn() {
			return true;
		}
		public ColumnMapping<?> getTargetColumnType() {
			return ReferenceField.this.getTargetColumnType();
		}
		@Override
		public String getResultAlias(String tableAlias, DatabaseDialect profile) {
			return ReferenceField.this.getResultAlias(tableAlias,profile);
		}
	}
}

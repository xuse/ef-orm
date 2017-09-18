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
package jef.database.query;

import java.io.Serializable;

import jef.database.DbUtils;
import jef.database.Field;
import jef.database.dialect.DatabaseDialect;
import jef.database.meta.FBIField;
import jef.database.meta.ITableMetadata;
import jef.database.meta.TupleField;
import jef.tools.Assert;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * 描述一个排序字段
 * 
 * @author Administrator
 * 
 */
public final class OrderField implements Serializable {
	private static final long serialVersionUID = -5112285033060486050L;
	private Field field;
	private boolean asc;

	/**
	 * 是否正序
	 * 
	 * @return
	 */
	public boolean isAsc() {
		return asc;
	}

	/**
	 * 排序字段
	 * 
	 * @return
	 */
	public Field getField() {
		return field;
	}

	/**
	 * 是否和查询绑定
	 * 
	 * @return
	 */
	public boolean isBind() {
		if (!(field instanceof RefField))
			return false;
		RefField ref = (RefField) field;
		return ref.isBind();
	}

	/**
	 * 构造
	 * 
	 * @param field
	 *            字段
	 * @param asc
	 *            正序
	 */
	public OrderField(Field field, boolean asc) {
		this.field = field;
		this.asc = asc;
	}

	public int hashCode() {
		return new HashCodeBuilder().append(field.name()).append(asc).toHashCode();
	}

	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!(obj instanceof OrderField)) {
			return false;
		}
		OrderField rhs = (OrderField) obj;
		return new EqualsBuilder().append(this.asc, rhs.asc).append(this.field.name(), rhs.field.name()).isEquals();
	}

	// 返回两个String,第一个是在OrderBy中的现实
	public String toString(DatabaseDialect profile, SqlContext context) {
		if (field instanceof RefField) {
			Assert.notNull(context);
			RefField ref = (RefField) field;
			String alias = context.getAliasOf(ref.getInstanceQuery(context));
			return DbUtils.toColumnName(ref.getField(), profile, alias);// 无法支持分库排序
		} else if (field instanceof Enum || field instanceof TupleField) {
			String alias = context == null ? null : context.getCurrentAliasAndCheck(field);
			return DbUtils.toColumnName(field, profile, alias);//
		} else if (field instanceof FBIField) {
			FBIField fbi = (FBIField) field;
			return fbi.toSqlAndBindAttribs(context, profile);
		} else if (field instanceof SqlExpression) {
			return field.name();
		} else {
			throw new IllegalArgumentException("The field type " + field.getClass().getName() + " is invalid!");
		}
	}

	/**
	 * 原先绑定在所属的Query上（可能是以ref的形式暂存在其中，但作为join的一部分，现在要准备离开这个存储的位置了）
	 * 
	 * @param query
	 *            所属的query
	 * @param context
	 */
	void prepareFlow(Query<?> query) {
		// if(isBind()){
		// return;
		// }
		if (field instanceof RefField) {
			return;
		}
		ITableMetadata type = DbUtils.getTableMeta(field);
		if (type != query.getMeta()) {
			field = new RefField(field);
		} else {
			field = new RefField(query, field);
		}
	}
}

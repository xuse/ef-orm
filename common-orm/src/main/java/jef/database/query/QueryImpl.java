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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jef.database.Condition;
import jef.database.Condition.Operator;
import jef.database.DataObject;
import jef.database.DbUtils;
import jef.database.DebugUtil;
import jef.database.Field;
import jef.database.IConditionField;
import jef.database.IQueryableEntity;
import jef.database.ORMConfig;
import jef.database.annotation.JoinType;
import jef.database.meta.AbstractRefField;
import jef.database.meta.EntityType;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.database.meta.MetadataAdapter;
import jef.database.meta.Reference;
import jef.database.meta.TupleField;
import jef.database.wrapper.populator.Transformer;
import jef.tools.Assert;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public final class QueryImpl<T extends IQueryableEntity> extends AbstractQuery<T>{
	private static final long serialVersionUID = -8921719771049568842L;
	
	private boolean cascadeViaOuterJoin=ORMConfig.getInstance().isUseOuterJoin();
	
	final List<Condition> conditions = new ArrayList<Condition>(6);
	/**
	 * 当使用延迟过滤条件时，存储这些条件
	 */
	private Map<Reference, List<Condition>> cascadeCondition;
	
	final List<OrderField> orderBy = new ArrayList<OrderField>();
	
	EntityMappingProvider selected;
	
	private Map<String, Object> attribute;

	/**
	 * 当使用ref field时，存储ref field的对象提供者
	 */
	private List<Query<?>> otherQueryProvider = null;


	public QueryImpl(T p) {
		this.instance = p;
		type = MetaHolder.getMeta(p);
	}
	
	public QueryImpl(T p,String key) {
		this.instance = p;
		DebugUtil.bindQuery((DataObject) p, this);
		setAttribute(ConditionQuery.CUSTOM_TABLE_TYPE, key);
		type = MetaHolder.getMeta(p);
	}

	public void setOrderBy(boolean asc, Field... orderbys) {
		orderBy.clear();
		addOrderBy(asc, orderbys);
	}

	public void addOrderBy(boolean flag, Field... orderbys) {
		for (Field f : orderbys) {
			if (!(f instanceof RefField) && !(f instanceof SqlExpression)) {
				ITableMetadata cls = DbUtils.getTableMeta(f);
				if(!cls.isAssignableFrom(this.type)){
					throw new IllegalArgumentException("the field ["+f.name()+"]which belongs to " + cls.getName() + " is not current type:" + getType().getName());
				}
			}
			orderBy.add(new OrderField(f, flag));
		}
	}

	public void clearQuery() {
		conditions.clear();	
		orderBy.clear();
		if(cascadeCondition!=null)
			cascadeCondition.clear();
		
	}

	@SuppressWarnings("unchecked")
	public Collection<OrderField> getOrderBy() {
		if (orderBy == null)
			return Collections.EMPTY_LIST;
		return orderBy;
	}

	public List<Condition> getConditions() {
		return conditions;
	}

	public Query<T> addCondition(IConditionField field) {
		return addCondition(field, Condition.Operator.EQUALS, null);
	}

	/*
	 * 检查Condition，将需要迟绑定的Field标记为迟邦定（即嵌套上RefField对象）
	 */
	private void wrapRef(IConditionField field) {
		for(Condition c:field.getConditions()){
			wrapRef(c);
		}
	}
	/*
	 * 检查Condition，将需要迟绑定的Field标记为迟邦定（即嵌套上RefField对象）
	 */
	private void wrapRef(Condition c) {
		Field field=c.getField();
		if(field instanceof Enum || field instanceof TupleField){
			ITableMetadata meta=DbUtils.getTableMeta(field);
			if(meta!=type){
				c.setField(new RefField(field));
			}
		}else if(field instanceof IConditionField){
			wrapRef((IConditionField)field);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see jef.database.query.Query#addCondition(jef.database.Condition)
	 */
	public Query<T> addCondition(Condition condition) {
		if(!conditions.contains(condition))
			conditions.add(condition);
		allRecords=false;
		wrapRef(condition);
		return this;
	}

	public Query<T> addCondition(Field field, Object value) {
		return addCondition(Condition.get(field, Condition.Operator.EQUALS, value));
	}

	public Query<T> addCondition(Field field, Operator oper, Object value) {
		return addCondition(Condition.get(field, oper, value));
	}
	
	public Query<T> addExtendQuery(Query<?> query) {
		if (query != null){
			if(otherQueryProvider==null){
				otherQueryProvider=new ArrayList<Query<?>>(4);
			}
			otherQueryProvider.add(query);
		}
		return this;
	}
	
	public Query<T> addExtendQuery(Class<?> emptyQuery) {
		MetadataAdapter meta=MetaHolder.getMeta(emptyQuery);
		if(meta.getType()==EntityType.POJO){
			throw new IllegalArgumentException();
		}
		return addExtendQuery(ReadOnlyQuery.getEmptyQuery(meta));
	}
	boolean allRecords;

	public Query<T> setAllRecordsCondition() {
		conditions.clear();
		allRecords=true;
		otherQueryProvider = null;
		return this;
	}

	public boolean isCascadeViaOuterJoin() {
		return cascadeViaOuterJoin;
	}

	public void setCascadeViaOuterJoin(boolean cascadeOuterJoin) {
		this.cascadeViaOuterJoin = cascadeOuterJoin;
	}

	//由于指定了RefName，因此变成了早绑定
	//问题1：级联的级联条件是否可以用xx.xx.xx这样的ref名称来描述
	public Query<T> addCascadeCondition(String refName,Condition... conds) {
		int n=refName.indexOf('.');
		
		ITableMetadata type=this.type;
		Reference reference;
		while(n>-1){
			AbstractRefField rField=type.getRefFieldsByName().get(refName.substring(0,n));
			Assert.notNull(rField,"the input field '"+refName+"' is not Cascade field in "+ type.getName());
			if(rField.isSingleColumn()){
				throw new IllegalArgumentException();
			}
			reference=rField.getReference();
			type=reference.getTargetType();
			refName=refName.substring(n+1);
			n=refName.indexOf('.');	
		}
		AbstractRefField rField=type.getRefFieldsByName().get(refName);
		Assert.notNull(rField,"the input field '"+refName+"' is not Cascade field in "+ type.getName());
		reference=rField.getReference();
		return addFilterCondition(reference,conds);
	}
	
	public Query<T> addCascadeCondition(Condition condition) {
		ITableMetadata meta=DbUtils.getTableMeta(condition.getField());
		Reference ref=type.findDistinctPath(meta);
		return addFilterCondition(ref,condition);
	}

	private void checkRefs(Field c) {
		if (c instanceof RefField) {
			RefField f=(RefField)c;
			Reference ref=type.findPath(DbUtils.getTableMeta(f.getField()));
			ensureRef(ref);
		} else if (c instanceof IConditionField) {
			IConditionField ic = (IConditionField) c;
			for (Condition cc : ic.getConditions()) {
				checkRefs(cc.getField());
			}
		}
	}
	
	private void ensureRef(Reference ref) {
		if(!this.cascadeViaOuterJoin){
			if(otherQueryProvider!=null){
				for(Query<?> q:otherQueryProvider){
					if(q.getMeta()==ref.getTargetType()){
						return;
					}
				}
			}
			this.addExtendQuery(ReadOnlyQuery.getEmptyQuery(ref.getTargetType()));
		}
	}
	
	private Query<T> addFilterCondition(Reference ref, Condition... cs) {
		if (cascadeCondition == null)
			cascadeCondition = new HashMap<Reference, List<Condition>>(4);
		List<Condition> cond = cascadeCondition.get(ref);
		if (cond == null) {
			cond = new ArrayList<Condition>();
			cascadeCondition.put(ref, cond);
		}
		cond.addAll(Arrays.asList(cs));
		return this;
	}


	public EntityMappingProvider getSelectItems() {
		return selected;
	}

	public void setSelectItems(Selects select) {
		this.selected = select;
	}

	public SqlContext prepare() {
		if (this.selected != null) {
			if (selected instanceof SqlContext)
				return (SqlContext) selected;
			selected = new SqlContext((SelectsImpl) selected);
			((SqlContext) selected).attribute = this.attribute;
			return (SqlContext) selected;
		} else {
			SqlContext context = new SqlContext("t", this);
			context.attribute = this.attribute;
			this.selected=context;
			return context;
		}
	}

	/**
	 * 这里我曾经发生过一个很困惑的错误，即可变对象的HashCode计算问题。
	 * 当此对象作为key放入到Map中去时，orderby还没设过值。此时HashMap按照HashA值存放数据。
	 * 当我循环获取对象时，orderby已经设置过值。此时HashMap按照HashB值去定位数据。 非常不幸的…… for(Query<?>
	 * query: map.keySet()){ String leftAlias=sqlContext.getAliasOf(query);
	 * boolean flag=map.containsKey(query);//此时居然返回false. Object
	 * data=map.get(query); //返回的是null. }
	 * 原因是HashMap是根据新的Hash值去定位数据，此时所查找的那格table中自然没有数据，HashMap就认为不含这个Key了。
	 * 这个问题需要引起警惕，由此我们得到教训——
	 * 1、如果你希望一个可变对象一直都返回同一个hashCode，那么最好在第一次获取HashCode之后将其储存起来。
	 * 2、对于Map进行遍历，使用entrySet()遍历比使用keySet()更健壮。
	 */
	@Override
	public int hashCode() {
		HashCodeBuilder hash = new HashCodeBuilder();
		hash.append(type);
		if (conditions != null) {
			int code = 0;
			for (Condition d : conditions) {
				code += d.hashCode();
			}
			hash.append(code);
		}
		return hash.toHashCode();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof QueryImpl)) {
			return false;
		}
		QueryImpl rhs = (QueryImpl) obj;
		// 先判断conditions
		if ((conditions == null) != (rhs.conditions == null)) {
			return false;
		}
		if (conditions != null) {
			if (conditions.size() != rhs.conditions.size()) {
				return false;
			}
			if (!conditions.containsAll(rhs.conditions)) {
				return false;
			}
		}
		EqualsBuilder eb = new EqualsBuilder();
		eb.append(this.type, rhs.type);
		// eb.append(this.conditions, rhs.conditions);
		eb.append(this.orderBy, rhs.orderBy);
		eb.append(this.selected, rhs.selected);
		eb.append(this.otherQueryProvider, rhs.otherQueryProvider);
		eb.append(this.cascadeCondition, rhs.cascadeCondition);
		return eb.isEquals();
	}

	//这个方法是最终查询执行前的标识,在这里分清楚哪些条件是事后，哪些是实时的
	public Query<?>[] getOtherQueryProvider() {
		if(!cascadeViaOuterJoin){
			for(Condition c:this.conditions){
				checkRefs(c.getField());
			}
		}
		return otherQueryProvider == null ? EMPTY_Q : otherQueryProvider.toArray(new Query<?>[otherQueryProvider.size()]);
	}

	public void setAttribute(String key, Object value) {
		if (attribute == null) {
			attribute = new HashMap<String, Object>();
		}
		attribute.put(key, value);
	}

	public Object getAttribute(String key) {
		if (attribute == null) {
			return null;
		}
		return attribute.get(key);
	}

	public Map<Reference, List<Condition>> getFilterCondition() {
		return cascadeCondition;
	}

	@Override
	public String toString() {
		return type.getThisType().getSimpleName() + conditions.toString();
	}
	


	public void setCustomTableName(String name) {
		setAttribute(CUSTOM_TABLE_NAME,name);
	}


	public Map<String, Object> getAttributes() {
		return attribute;
	}

	/**
	 * 针对每个FilterCondition，来判断该Filter是否允许合并查询
	 * 该条件是否允许作用于合并后的外连接查询.
	 * 只有当左外连接，并且是xxxToOne的连接，才允许作用于合并后的外连接查询上。
	 * 
	 * @return 该条件是否允许作用于合并后的外连接查询.
	 */
	public static boolean isAllowMergeAsOuterJoin(Reference ref){
		if(ref==null){
			throw new IllegalStateException();
		}
		ReferenceType type=ref.getType();
		if(type.isToOne()){
			return ref.getJoinType()==JoinType.LEFT; 
		}
		return false;
	}


	public void setCascade(boolean cascade) {
		if(t==null){
			t=new Transformer(type);
		}
		t.setLoadVsMany(cascade);
		t.setLoadVsOne(cascade);
		this.cascadeViaOuterJoin=false;
	}

	@Override
	public boolean isAll() {
		return allRecords && conditions.isEmpty();
	}

	@Override
	public Terms terms() {
		return new Terms(this);
	}
}

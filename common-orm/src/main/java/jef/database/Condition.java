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
package jef.database;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jef.common.log.LogUtil;
import jef.database.VariableCallback.Like;
import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.ColumnMapping;
import jef.database.dialect.type.ColumnMappings;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.meta.FBIField;
import jef.database.meta.Feature;
import jef.database.meta.ITableMetadata;
import jef.database.meta.TupleField;
import jef.database.query.JpqlExpression;
import jef.database.query.LazyQueryBindField;
import jef.database.query.RefField;
import jef.database.query.SqlContext;
import jef.database.query.SqlExpression;
import jef.database.wrapper.clause.SqlBuilder;
import jef.database.wrapper.variable.BindVariableDescription;
import jef.tools.ArrayUtils;
import jef.tools.Assert;
import jef.tools.StringUtils;
import jef.tools.collection.CollectionUtils;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * 描述一个条件对象，一个条件一般是形如 {@code name='John Smith'}这样的表达式。
 * 一个典型的条件由 左表达式、运算符、右表达式三部分组成
 * @author Administrator
 *
 */
public class Condition implements Serializable{
	private static final long serialVersionUID = -864876729368518762L;
	
//	public static final Condition AllRecordsCondition=new Condition();
	private static final Class<?>[] VALID_FIELD_TYPE_FOR_CONDITION={FBIField.class,RefField.class};
	private static final Operator[] NULL_ABLE_VALUE=new Operator[]{Operator.IS_NULL,Operator.IS_NOT_NULL};
	
	protected Condition(){}
	
	public Condition(Field field2, Operator equals, Object value2) {
		this.field=field2;
		this.operator=equals;
		this.value=value2;
	}

	/**
	 * 字段（也可用于描述一个条件组）
	 */
	protected Field field;
	/**
	 * 运算符
	 */
	protected Operator operator;
	/**
	 * 表达式
	 */
	protected Object value;
	
	
	/**
	 * 根据运算构造一个条件
	 * @param field
	 * @param oper
	 * @param value
	 * @return
	 */
	public static Condition get(Field field,Operator oper,Object value){
		Assert.notNull(field);
		boolean valid=(field instanceof IConditionField) || (field instanceof Enum)|| (field instanceof TupleField);
		if(!valid){
			valid=ArrayUtils.fastContains(VALID_FIELD_TYPE_FOR_CONDITION, field.getClass());
		}
		Assert.isTrue(valid,"The field type "+ field.getClass()+" is not a valid field for condition!");
		Condition c=new Condition();
		c.field=field;
		c.operator=oper;
		c.value=value;
		return c;
	}
	
	/**
	 * 条件分为两类 （基本条件/容器条件)
	 * 根据Field的类型来判断
	 * 元模型字段，FBIField.class,RefField.class 三种为基本条件
	 * IConditionField为容器条件
	 * @return
	 */
	public boolean isContainer(){
		return field instanceof IConditionField;
	}
	
	/**
	 * 条件运算符
	 * <li>=  等于</li>
	 * <li>>  大于</li>
	 * <li><  小于</li>
	 * <li>>= 大于等于</li>
	 * <li><= 小于等于</li>
	 * <li>!= 不等于</li>
	 * <li>^= 匹配字符串头</li>
	 * <li>$= 匹配字符串尾</li>
	 * <li>*= 匹配字符串任意位置</li>
	 * <li>in in</li>
	 * <li>[] BETWEEN</li>
	 */
	public enum Operator{
		EQUALS("="),
		GREAT(">"),
		LESS("<"),
		GREAT_EQUALS(">="),
		LESS_EQUALS("<="),
		MATCH_ANY("*="," like "),
		MATCH_START("^="," like "),
		MATCH_END("$="," like "),
		IN("in"," in "),
		NOT_IN("not in"),
		NOT_EQUALS("!="),
		BETWEEN_L_L("[]"),
		IS_NULL("=NULL"),
		IS_NOT_NULL("!=NULL");
		
		Operator(String key,String oper){
			this.key=key;
			this.oper=oper;
		}
		Operator(String key){
			this.key=key;
			this.oper=key;
		}
		
		private String key;
		private String oper;
		public String getKey() {
			return key;
		}
		//
		public static Operator valueOfKey(String key){
			for(Operator o: Operator.values()){
				if(o.key.equals(key)){
					return o;
				}
			}
			if("!=".equals(key)){
				return Operator.NOT_EQUALS;
			}
			return null;
		}
	}
	
	public Field getField() {
		return field;
	}
	public void setField(Field field) {
		this.field = field;
	}
	public Operator getOperator() {
		return operator;
	}
	public void setOperator(Operator operator) {
		this.operator = operator;
	}
	public Object getValue() {
		return value;
	}
	public void setValue(Object value) {
		this.value = value;
	}
	
	//产生非绑定变量下的where条件
	public String toSqlClause(ITableMetadata meta,SqlContext context, SqlProcessor processor,IQueryableEntity instance,DatabaseDialect profile){
		if(field==null) {
			throw new NullPointerException("Condition not complete!");
		}
		Field field = this.field;
		//特殊Field条件
		if(field instanceof IConditionField){
			return ((IConditionField)field).toSql(meta,processor,context,instance,profile);
		}else if(field instanceof RefField){
			RefField ref=(RefField) field;
			ITableMetadata refMeta=((RefField) field).getInstanceQuery(context).getMeta();
			SqlContext    refContext=context.getContextOf(ref.getInstanceQuery(context));
			//替换上下文，然后继续处理……
			field=ref.getField();
			meta=refMeta;
			context=refContext;
			instance=ref.getInstanceQuery(context).getInstance();
		}
		//Like条件下
		if(ArrayUtils.contains(Like.LIKE_OPERATORS, operator)){
			Like like=new Like(field,operator,value);
			return like.toSql(meta, profile, context, instance);
		}
		String columnName;
		if(field instanceof JpqlExpression){
			columnName=((JpqlExpression) field).toSqlAndBindAttribs(context, profile);
		}else{
			columnName=DbUtils.toColumnName(field,profile,context==null?null:context.getCurrentAliasAndCheck(field));
		}
		if(operator==null){
			return columnName;
		}
		if(profile.has(Feature.EMPTY_CHAR_IS_NULL) && value instanceof CharSequence){
			if(((CharSequence)value).length()==0){
				value=null;
			}
		}
		//常规Field条件
		if(ArrayUtils.notContains(NULL_ABLE_VALUE,operator) && value==null){
			if(operator==Operator.EQUALS){
				operator=Operator.IS_NULL;
			}else if(operator==Operator.NOT_EQUALS){
				operator=Operator.IS_NOT_NULL;
			}else{
				throw new NullPointerException("Condition value can not be null: " + field.name()+" "+operator.getKey());	
			}
		}
		
		ColumnMapping type = null;
		if(field instanceof Enum || field instanceof TupleField){//计算列的数据类型
			type=meta.getColumnDef(field);
			if(type==null) {
				throw new IllegalArgumentException("There is no proper field in "+meta.getThisType().getName()+" to populate a SQL condition expression:"+field.name());
			}
		}
		return toSql(columnName,operator,value,profile,context,type);
	}
	
	/*
	 *产生用于批处理的Update语句的Where字句中的sql语句，形如 xx=?   等等 xx is null
	 *预处理若干复杂的Field类型 
	 */
	public void toPrepareSqlClause(
			SqlBuilder builder,
			ITableMetadata meta,
			SqlContext context,
			SqlProcessor processor,
			IQueryableEntity instance,DatabaseDialect profile) {
		//特殊Field条件 
		if(field instanceof IConditionField){
			 ((IConditionField)field).toPrepareSql(builder,meta,processor,context,instance,profile);
			 return;
		}else if(field instanceof LazyQueryBindField){
			LazyQueryBindField ref=(LazyQueryBindField) field;
			ITableMetadata refMeta=ref.getInstanceQuery(context).getMeta();
			SqlContext    refContext=context.getContextOf(ref.getInstanceQuery(context));
			if(ref instanceof RefField){
				toPrepareSqlClause(builder,refMeta,refContext,processor,ref.getInstanceQuery(context).getInstance(),((RefField)ref).getField(),profile);
				return;
			}
		}
		toPrepareSqlClause(builder,meta,context,processor,instance,field,profile); 
	}
	
	/**
	 * 实际生成条件 
	 * @return
	 */
	private void toPrepareSqlClause(SqlBuilder builder,
			ITableMetadata meta, SqlContext context,
			SqlProcessor processor, IQueryableEntity instance, Field rawField,DatabaseDialect profile) {
		//当value为Field的时候……
		if(value instanceof jef.database.Field && value.getClass().isEnum()){//当value为基本field时
			value=new RefField((Field)value);
		}
		//表达式对象无需绑定变量
		if((value instanceof Expression)||value instanceof RefField){
			builder.append(toSqlClause(meta,context,processor,instance,profile));
			return;
		}
		//Like条件下
		if(ArrayUtils.contains(Like.LIKE_OPERATORS, operator)){
			Like like=new Like(rawField,operator,value);
			like.toPrepareSql(builder, meta, profile,context, instance);
			return;
		}
		//其他简单条件情况下
		if(operator==null ||rawField==null){
			throw new NullPointerException("Condition not complete!");
		}
		if(profile.has(Feature.EMPTY_CHAR_IS_NULL) && value instanceof CharSequence){
			if(((CharSequence)value).length()==0){
				value=null;
			}
		}
		//修复is null和is not null两种特殊情况
		if(value==null && ArrayUtils.notContains(NULL_ABLE_VALUE,operator)){
			if(operator == Operator.EQUALS){
				operator=Operator.IS_NULL;
			}else if(operator == Operator.NOT_EQUALS){
				operator=Operator.IS_NOT_NULL;
			}else{
				throw new IllegalArgumentException("The value of condition ["+field+" "+operator.getKey()+" (...)] must not be null!");				
			}
		}
		StringBuilder sb=new StringBuilder();
		
		String columnName;
		if(rawField instanceof JpqlExpression){
			columnName=((JpqlExpression) rawField).toSqlAndBindAttribs(context, profile);
		}else{
			columnName=DbUtils.toColumnName(rawField,profile,context==null?null:context.getCurrentAliasAndCheck(rawField));
		}
		
		if(operator==Operator.IS_NULL){
			sb.append(columnName).append(" is null");
			return;
		}else if (operator==Operator.IS_NOT_NULL){
			sb.append(columnName).append(" is not null");
			return;
		}
		if (getInBetweenOper(operator)!=null){
			processBetween(builder,columnName,rawField,profile,meta,context,processor,instance);
			return;
		}else{
			builder.append(columnName,operator.oper,"?");
			builder.addBind(new BindVariableDescription(rawField,operator,value));
		}
	}

	private void processBetween(SqlBuilder builder,String columnName,Field rawField,DatabaseDialect profile
			,ITableMetadata meta, SqlContext context,
			SqlProcessor processor, IQueryableEntity instance) {
		String[] spOpers=getInBetweenOper(operator);
		String oper=spOpers[0];
		String div=spOpers[1];
		String tailer=spOpers[2];
		//修改，支持String
		if(value instanceof CharSequence){
			String[] value1=StringUtils.split(value.toString(), ',');
			for(int i=0;i<value1.length;i++){
				value1[i]=value1[i].trim();
			}
			value=value1;
		}
		if(CollectionUtils.isArrayOrCollection(value.getClass())){
			int len=CollectionUtils.length(value);
			if(operator==Operator.BETWEEN_L_L && len!=2){//无效的BETWEEN操作
				throw new RuntimeException("The between operator must have 2 params");
			}
			if(len==0){
				if(Operator.IN==operator){//对于空集合的IN条件为永假
					builder.append("1=2");	
				}else if(Operator.NOT_IN==operator){//对于空集合的NOT IN条件为永真
					builder.append("1=1");
				}
			}else{
				builder.append(columnName,oper);
				int n=0;
				for(Iterator<Object> iter=CollectionUtils.iterator(value,Object.class);iter.hasNext();){
					Object o=iter.next();
					if(n>0)builder.append(div);
					if(o instanceof Field){
						Condition c=get((Field)o,null,null);
						builder.append(c.toSqlClause(meta, context, processor, instance,profile));
					}else{
						builder.append("?");
						builder.addBind(new BindVariableDescription(rawField,operator,o));
					}
					n++;
				}
				builder.append(tailer);
			}
		}else{
			throw new RuntimeException("Error param, the value of in operator must be a array or string :" + value.getClass().getName());				
		}
	
	}

	private static final String[] IN_OPER=new String[]{" in (",", ",")"};
	private static final String[] NOT_IN_OPER=new String[]{" not in (",", ",")"};
	private static final String[] BETWEEN_OPER=new String[]{" between "," and ",""};
	
	private static String[] getInBetweenOper(Operator oper){
		if(oper==Operator.IN){
			return IN_OPER;
		}else if(oper==Operator.BETWEEN_L_L){
			return BETWEEN_OPER;
		}else if(oper==Operator.NOT_IN){
			return NOT_IN_OPER;
		}
		return null;
	}

	public static String toSql(String columnName,Operator operator,Object value,DatabaseDialect profile,SqlContext context,ColumnMapping type){
		if(value instanceof jef.database.Field && value.getClass().isEnum()){//基本field
			value=new RefField((Field)value);
		}
		//生成条件SQL
		String[] spOpers;
		StringBuilder sb=new StringBuilder();
		if(operator==Operator.IS_NULL){
			sb.append(columnName).append(" is null");
		}else if(value==Operator.IS_NOT_NULL){
			sb.append(columnName).append(" is not null");
		}else if(value instanceof RefField){
			RefField rf=(RefField) value;
			String alias=context==null?"":context.getAliasOf(rf.getInstanceQuery(context));
			if(alias!=null){
				sb.append(columnName).append(operator.oper).append(alias);
				if(alias.length()>0)sb.append('.');
				sb.append(DbUtils.toColumnName(rf.getFieldDef(),profile,null));
			}else{
				LogUtil.show("Not found Table Alias for ref-field:" + rf.toString());
				sb.append(columnName).append(operator.oper).append(DbUtils.toColumnName(rf.getFieldDef(),profile,null));	
			}
		}else if ((spOpers=getInBetweenOper(operator))!=null){
			String oper=spOpers[0];
			String div=spOpers[1];
			String tailer=spOpers[2];
			if(value instanceof CharSequence){
				sb.append(columnName).append(oper).append('\'').append(value.toString()).append('\'').append(tailer);
			}else{
				List<String> sqlValues=new ArrayList<String>();
				Iterator<Object> iter=CollectionUtils.iterator(value,Object.class);
				Assert.notNull(iter,"Error param, the value of in operator must be a array or string :" + value.getClass().getName());
				for(;iter.hasNext();){
					Object v=iter.next();
					sqlValues.add(type.getSqlStr(v, profile));
				}
				if(operator==Operator.BETWEEN_L_L && sqlValues.size()!=2){//无效的BETWEEN操作
					throw new RuntimeException("The between operator must have 2 params");
				}
				if(sqlValues.size()==0){//无效果的in操作
				   sb.append("1=2");
				}else{
					sb.append(columnName).append(oper).append(StringUtils.join(sqlValues, div)).append(tailer);	
				}	
			}
		}else if(value instanceof SqlExpression){
			sb.append(columnName).append(operator.oper).append(((SqlExpression) value).getText());
		}else if(value instanceof JpqlExpression){
			JpqlExpression jpql=(JpqlExpression) value;
			sb.append(columnName).append(operator.oper).append(jpql.toSqlAndBindAttribs(context,profile));
		}else{
			sb.append(columnName).append(operator.oper).append(type==null?ColumnMappings.getSqlStr(value,profile):type.getSqlStr(value,profile));
		}
		return sb.toString();
	}
	
	
	public String toString(){
		StringBuilder sb=new StringBuilder();
		if(field!=null){
			sb.append(field.name()).append(' ');
			if(field instanceof IConditionField){
				return sb.toString();
			}
		}
		sb.append(operator==null?"=":operator.getKey()).append(' ');
		if(value==null){
			sb.append("null");
		}else if(value.getClass().isArray()){
			sb.append(ArrayUtils.toString(value));
		}else{
			sb.append(value);	
		}
		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if(o==this)return true;
		if(!(o instanceof Condition)) return false;
		Condition oo=(Condition)o;
		EqualsBuilder eb=new EqualsBuilder();
		eb.append(this.field, oo.field);
		eb.append(this.operator, oo.operator);
		eb.append(this.value, oo.value);
		return eb.isEquals();
	}

	@Override
	public int hashCode() {
		HashCodeBuilder b=new HashCodeBuilder();
		//modify by mjj,增加value值得变化因子，以达到交换属性值时算出的hashcode不一样
		//return b.append(field).append(operator).append(value).toHashCode();
		return b.append(field).append(operator).append(value).toHashCode()*(value==null?3:new HashCodeBuilder().append(value).toHashCode());
	}
}

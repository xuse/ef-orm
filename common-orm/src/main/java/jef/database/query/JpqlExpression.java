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

import java.util.Collections;
import java.util.Map;

import jef.common.log.LogUtil;
import jef.database.DbUtils;
import jef.database.Field;
import jef.database.JPQLSelectConvert;
import jef.database.QueryAlias;
import jef.database.SqlProcessor;
import jef.database.dialect.DatabaseDialect;
import jef.database.jsqlparser.SqlFunctionlocalization;
import jef.database.jsqlparser.UndoableVisitor;
import jef.database.jsqlparser.VisitorAdapter;
import jef.database.jsqlparser.expression.Expression;
import jef.database.jsqlparser.expression.ExpressionVisitor;
import jef.database.jsqlparser.expression.JpqlParameter;
import jef.database.jsqlparser.parser.ParseException;
import jef.database.jsqlparser.schema.Column;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;

import org.apache.commons.lang.ObjectUtils;

public class JpqlExpression implements Expression,LazyQueryBindField {
	protected Query<?> instance;

	protected Expression st;

	// 当基于某个对象进行带函数的查询时，使用此方法构造
	// 但是目前JEF上不支持SQL词法分析
	public JpqlExpression(String str, Query<?> clz) {
		try {
			this.st = DbUtils.parseExpression(str);
		} catch (ParseException e) {
			LogUtil.exception(e);
			throw new RuntimeException(e.getMessage());
		}
		this.instance = clz;
	}

	public JpqlExpression(Expression exp, Query<?> q) {
		this.st = exp;
		this.instance = q;
	}

	public JpqlExpression(String str) {
		this(str, (Query<?>) null);
	}

	@SuppressWarnings("unchecked")
	public String toSqlAndBindAttribs(final SqlContext context, final DatabaseDialect profile) {
		st.accept(new SqlFunctionlocalization(profile, null));
		if (context != null) {
			final Map<String, Object> attribs = context.attribute == null ? Collections.EMPTY_MAP : context.attribute;
			st.accept(new VisitorAdapter() {
				@Override
				public void visit(JpqlParameter parameter) {
					if (parameter.getName() == null)
						return;
					Object obj = attribs.get(parameter.getName());
					if (obj == null) {
						if (!attribs.containsKey(parameter.getName())) {
							throw new IllegalArgumentException("You have not set the value of param '" + parameter.getName() + "'");
						} else {
							parameter.setResolved("null");
						}
					} else {
						if (obj instanceof Number) {
							parameter.setResolved(String.valueOf(obj));
						} else {
							parameter.setResolved("'" + String.valueOf(obj) + "'");
						}
					}
				}
			});

		}

		if (instance == null)
			return st.toString();
		String alias = context == null ? null : context.getCurrentAlias();
		ColumnAliasApplier convert = new ColumnAliasApplier(alias, profile);
		st.accept(convert);
		String result = st.toString();
		convert.undo();
		return result;
	}

	public String toSqlAndBindAttribs(final SqlContext context,final SqlProcessor processor){
		//本地化
		st.accept(new SqlFunctionlocalization(processor.getProfile(), null));
		//属性提供
		if(context!=null){
			@SuppressWarnings("unchecked")
			final Map<String,Object> attribs=context.attribute==null?Collections.EMPTY_MAP:context.attribute;
			st.accept(new VisitorAdapter(){
				@Override
				public void visit(JpqlParameter parameter) {
					if(parameter.getName()==null)return;
					Object obj=attribs.get(parameter.getName());
					if(obj==null){
						if(!attribs.containsKey(parameter.getName())){
							throw new IllegalArgumentException("You have not set the value of param '"+parameter.getName()+"'");
						}else{
							parameter.setResolved("null");	
						}
					}else{
						if(obj instanceof Number){
							parameter.setResolved(String.valueOf(obj));
						}else{
							parameter.setResolved("'"+String.valueOf(obj)+"'");
						}	
					}
				}
			});	
		}
		//字段转换
		String result;
		if(instance==null){
			st.accept(new JPQLSelectConvert(processor));
			result=st.toString();
		}else{
			String alias=context==null?null:context.getAliasOf(instance);
			ColumnAliasApplier convert=new ColumnAliasApplier(alias,processor.getProfile());
			st.accept(convert);
			result=st.toString();
			convert.undo();
		}
		return result;
	}
	/**
	 * 用于对简单对象中的列前引用的表别名进行替换的程序
	 * @author Administrator
	 *
	 */
	private class ColumnAliasApplier extends UndoableVisitor<Column,String[]>{
		private String alias;
		private DatabaseDialect profile;
		
		public ColumnAliasApplier(String alias,DatabaseDialect profile){
			this.alias=alias;
			this.profile=profile;
		}
		
		@Override
		public void visit(Column tableColumn) {
			if(instance==null){
				return;
			}
			ITableMetadata meta=MetaHolder.getMeta(instance.getInstance());
			Field f=meta.getField(tableColumn.getColumnName());
			String oldAlias=tableColumn.getTableAlias();
			if(f!=null){
				savePoint(tableColumn, new String[]{oldAlias,tableColumn.getColumnName()});
				tableColumn.setTableAlias(alias);
				tableColumn.setColumnName(meta.getColumnName(f,profile,true));
			}else{
				//FIXME 现在设计没找到列是不替换名称的
			}
		}

		@Override
		protected void undo(Column key, String[] value) {
			key.setTableAlias(value[0]);
			key.setColumnName(value[1]);
		}
	}
	public boolean isBind() {
		return instance!=null;
	}

	
	public ITableMetadata getMeta() {
		if (instance == null)
			return null;
		return MetaHolder.getMeta(instance.getInstance());
	}

	public String toString() {
		return st.toString();
	}
	
	public void appendTo(StringBuilder sb) {
		st.appendTo(sb);
	}
	

	public Query<?> getInstanceQuery(AbstractEntityMappingProvider context) {
		if(instance!=null){
			return instance;	
		}
		if(context!=null && context.getReference().size()==1){
			this.instance=((QueryAlias)context.queries.get(0)).getQuery();
		}
		return instance;
	}

	public void accept(ExpressionVisitor expressionVisitor) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int hashCode() {
		return st.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !JpqlExpression.class.isAssignableFrom(obj.getClass()))
			return false;
		JpqlExpression o = (JpqlExpression) obj;
		if (!ObjectUtils.equals(this.instance, o.instance))
			return false;
		if (!ObjectUtils.equals(this.st, o.st))
			return false;
		return true;
	}

	public void setBind(Query<?> query) {
		this.instance=query;
	}
}

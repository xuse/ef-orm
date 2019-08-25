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

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

import jef.common.IntList;
import jef.database.Condition;
import jef.database.IConditionField;
import jef.database.IQueryableEntity;
import jef.database.SqlProcessor;
import jef.database.dialect.DatabaseDialect;
import jef.database.jsqlparser.parser.StSqlParser;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.ExpressionType;
import jef.database.jsqlparser.visitor.ExpressionVisitor;
import jef.database.jsqlparser.visitor.VisitorSimpleAdapter;
import jef.database.meta.ITableMetadata;
import jef.database.wrapper.clause.SqlBuilder;
import jef.database.wrapper.variable.ConstantVariable;
import jef.tools.Exceptions;

/**
 * A SQL Expression.
 * @author jiyi
 */
public final class SqlExpression implements Expression,IConditionField{
	private static final long serialVersionUID = 1L;
	
	private String sql;
	
	private final IntList params = new IntList();
	
	public SqlExpression(String text){
		this(text,false);
	}
	
	public SqlExpression(String text,boolean parse){
		if(parse) {
			StSqlParser parser = new StSqlParser(new StringReader(text));
			try {
				Expression exp=parser.Condition();
				exp.accept(new VisitorSimpleAdapter() {
					public void visit(jef.database.jsqlparser.expression.JdbcParameter jdbcParameter) {
						params.add(jdbcParameter.getId());
					}
				});
				this.sql=exp.toString();
			} catch (Throwable e) {
				throw Exceptions.illegalState("解析错误:{}",text,e);
			}
		}else {
			this.sql=text;
		}
	}
	
	public String getText() {
		return sql;
	}
	
	public void accept(ExpressionVisitor expressionVisitor) {
	}
	
	@Override
	public String toString() {
		return sql;
	}
	public String name() {
		return sql;
	}
	public List<Condition> getConditions() {
		return Arrays.asList();
	}
	public String toSql(ITableMetadata meta, SqlProcessor processor, SqlContext context, IQueryableEntity instance,DatabaseDialect profile, boolean batch) {
		return sql;
	}
	public void toPrepareSql(SqlBuilder fields, ITableMetadata meta, SqlProcessor processor, SqlContext context, IQueryableEntity instance,DatabaseDialect profile,boolean batch) {
		fields.append(sql);
		for(int i: params.toArrayUnsafe()) {
			Object o=context.attribute.get(String.valueOf(i));
			fields.addBind(new ConstantVariable(o));
		}
	}
	
	public void appendTo(StringBuilder sb) {
		if(!params.isEmpty()) {
			throw new IllegalArgumentException("不支持的参数用法");
		}
		sb.append(sql);
	}
	public ExpressionType getType() {
		return ExpressionType.complex;
	}
}

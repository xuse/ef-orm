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

import java.util.Arrays;
import java.util.List;

import jef.database.Condition;
import jef.database.IConditionField;
import jef.database.IQueryableEntity;
import jef.database.SqlProcessor;
import jef.database.dialect.DatabaseDialect;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.ExpressionType;
import jef.database.jsqlparser.visitor.ExpressionVisitor;
import jef.database.meta.ITableMetadata;
import jef.database.wrapper.clause.SqlBuilder;

/**
 * A SQL Expression.
 * @author jiyi
 */
public final class SqlExpression implements Expression,IConditionField{
	private static final long serialVersionUID = 1L;
	
	private String sql;
	
	public SqlExpression(String text){
		this.sql=text;
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
	public String toSql(ITableMetadata meta, SqlProcessor processor, SqlContext context, IQueryableEntity instance,DatabaseDialect profile) {
		return sql;
	}
	public void toPrepareSql(SqlBuilder fields, ITableMetadata meta, SqlProcessor processor, SqlContext context, IQueryableEntity instance,DatabaseDialect profile) {
		fields.append(sql);
	}
	public void appendTo(StringBuilder sb) {
		sb.append(sql);
	}
	public ExpressionType getType() {
		return ExpressionType.complex;
	}
}

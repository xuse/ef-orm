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

import jef.database.BindVariableDescription;
import jef.database.Condition;
import jef.database.IConditionField;
import jef.database.IQueryableEntity;
import jef.database.SqlProcessor;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.ExpressionType;
import jef.database.jsqlparser.visitor.ExpressionVisitor;
import jef.database.meta.ITableMetadata;

public class SqlExpression implements Expression,IConditionField{
	private static final long serialVersionUID = 1L;
	private String text;
	
	public SqlExpression(String text){
		this.text=text;
	}
	public String getText() {
		return text;
	}
	public void accept(ExpressionVisitor expressionVisitor) {
		//
	}
	@Override
	public String toString() {
		return text;
	}
	public String name() {
		return text;
	}
	public Iterable<Condition> getConditions() {
		return Arrays.asList();
	}
	public String toSql(ITableMetadata meta, SqlProcessor processor, SqlContext context, IQueryableEntity instance) {
		return text;
	}
	public String toPrepareSql(List<BindVariableDescription> fields, ITableMetadata meta, SqlProcessor processor, SqlContext context, IQueryableEntity instance) {
		return text;
	}
	public void appendTo(StringBuilder sb) {
		sb.append(text);
	}
	public ExpressionType getType() {
		return ExpressionType.complex;
	}
}

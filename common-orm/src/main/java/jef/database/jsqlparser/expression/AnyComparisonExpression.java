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
package jef.database.jsqlparser.expression;

import jef.database.jsqlparser.statement.select.SubSelect;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.ExpressionType;
import jef.database.jsqlparser.visitor.ExpressionVisitor;

//SQL Server才有的关键字
public class AnyComparisonExpression implements Expression {

    private SubSelect subSelect;

    public AnyComparisonExpression(SubSelect subSelect) {
        this.subSelect = subSelect;
    }

    public SubSelect GetSubSelect() {
        return subSelect;
    }

    public void accept(ExpressionVisitor expressionVisitor) {
        expressionVisitor.visit(this);
    }

	public void appendTo(StringBuilder sb) {
		sb.append("ANY(");
		subSelect.appendTo(sb);
		sb.append(')');
	}

	@Override
	public String toString() {
		StringBuilder sb=new StringBuilder(256);
		appendTo(sb);
		return sb.toString();
	}
	
	public ExpressionType getType() {
		return ExpressionType.complex;
	}
}

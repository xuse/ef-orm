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
package jef.database.jsqlparser.expression.operators.relational;

import jef.database.jsqlparser.expression.Expression;
import jef.database.jsqlparser.expression.ExpressionVisitor;

public class IsNullExpression implements Expression {

    private Expression leftExpression;

    private boolean not = false;

    public Expression getLeftExpression() {
        return leftExpression;
    }

    public boolean isNot() {
        return not;
    }

    public void setLeftExpression(Expression expression) {
        leftExpression = expression;
    }

    public void setNot(boolean b) {
        not = b;
    }

    public void accept(ExpressionVisitor expressionVisitor) {
        expressionVisitor.visit(this);
    }

    public String toString() {
    	StringBuilder sb=new StringBuilder(64);
    	appendTo(sb);
        return sb.toString();
    }

	public void appendTo(StringBuilder sb) {
		leftExpression.appendTo(sb);
		sb.append(" IS ");
		if(not)
			sb.append("NOT ");
		sb.append("NULL");
		
	}
}

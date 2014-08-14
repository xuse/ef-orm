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

import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.ExpressionType;
import jef.database.jsqlparser.visitor.ExpressionVisitor;
import jef.database.jsqlparser.visitor.Notable;

/**
 * It represents an expression like "(" expression ")"
 */
public class Parenthesis implements Expression,Notable {

    private Expression expression;

    private boolean not = false;

    public Parenthesis() {
    }

    public Parenthesis(Expression expression) {
        setExpression(expression);
    }

    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    public void accept(ExpressionVisitor expressionVisitor) {
        expressionVisitor.visit(this);
    }

    public void setNot() {
        not = true;
    }

    public boolean isNot() {
        return not;
    }

    public String toString() {
    	StringBuilder sb=new StringBuilder(64);
    	appendTo(sb);
        return sb.toString();
    }

	public void appendTo(StringBuilder sb) {
		if(not)
			sb.append("NOT ");
		sb.append('(');
		expression.appendTo(sb);
		sb.append(')');
	}
	
	public ExpressionType getType() {
		return ExpressionType.parenthesis;
	}

	public void setNot(boolean not) {
		this.not=not;
	}
}

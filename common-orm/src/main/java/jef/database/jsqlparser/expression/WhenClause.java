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

/**
 * A clause of following syntax: 
 * WHEN condition THEN expression.
 * Which is part of a CaseExpression.
 * 
 * @author Havard Rast Blok
 */
public class WhenClause implements Expression {

    private Expression whenExpression;

    private Expression thenExpression;

    public void accept(ExpressionVisitor expressionVisitor) {
        expressionVisitor.visit(this);
    }

    /**
	 * @return Returns the thenExpression.
	 */
    public Expression getThenExpression() {
        return thenExpression;
    }

    /**
	 * @param thenExpression The thenExpression to set.
	 */
    public void setThenExpression(Expression thenExpression) {
        this.thenExpression = thenExpression;
    }

    /**
	 * @return Returns the whenExpression.
	 */
    public Expression getWhenExpression() {
        return whenExpression;
    }

    /**
	 * @param whenExpression The whenExpression to set.
	 */
    public void setWhenExpression(Expression whenExpression) {
        this.whenExpression = whenExpression;
    }

    public String toString() {
    	StringBuilder sb=new StringBuilder();
    	appendTo(sb);
        return sb.toString();
    }

	public void appendTo(StringBuilder sb) {
		sb.append("WHEN ");
		whenExpression.appendTo(sb);
		sb.append(" THEN ");
		thenExpression.appendTo(sb);
	}
	
	public ExpressionType getType() {
		return ExpressionType.complex;
	}
}

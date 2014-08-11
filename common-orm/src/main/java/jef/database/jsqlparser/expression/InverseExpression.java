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
import jef.database.jsqlparser.visitor.ExpressionVisitor;

/**
 * It represents a "-" before an expression 
 */
public class InverseExpression implements Expression {

    private Expression expression;

    public InverseExpression() {
    }

    public InverseExpression(Expression expression) {
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

	@Override
	public String toString() {
		StringBuilder sb=new StringBuilder();
		appendTo(sb);
		return sb.toString();
	}
	
	/**
	 * 取相反的值
	 * @param ex
	 * @return
	 */
	public static Expression getInverse(Expression ex){
		if(ex instanceof InverseExpression){
			return ((InverseExpression) ex).getExpression();
		}else{
			if(ex instanceof BinaryExpression){
				ex=new Parenthesis(ex);
			}
			return new InverseExpression(ex);
		}
	}

	public void appendTo(StringBuilder sb) {
		expression.appendTo(sb.append('-'));
	}
}

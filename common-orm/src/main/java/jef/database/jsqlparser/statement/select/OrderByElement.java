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
package jef.database.jsqlparser.statement.select;

import jef.database.jsqlparser.expression.Expression;
import jef.database.jsqlparser.statement.SqlAppendable;

/**
 * An element (column reference) in an "ORDER BY" clause.
 */
public class OrderByElement implements SqlAppendable{

    private Expression expression;

    private boolean asc = true;

    public boolean isAsc() {
        return asc;
    }

    public void setAsc(boolean b) {
        asc = b;
    }

    public void accept(OrderByVisitor orderByVisitor) {
        orderByVisitor.visit(this);
    }

    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    public String toString() {
    	StringBuilder sb=new StringBuilder();
    	appendTo(sb);
        return sb.toString();
    }

	public void appendTo(StringBuilder sb) {
		expression.appendTo(sb);
		if(!asc){
			sb.append(" DESC");
		}
	}
}

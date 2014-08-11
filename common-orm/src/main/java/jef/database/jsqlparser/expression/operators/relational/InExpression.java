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

import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.ExpressionVisitor;
import jef.database.jsqlparser.visitor.Ignorable;
import jef.database.jsqlparser.visitor.ItemsList;

public class InExpression implements Expression,Ignorable {

    private Expression leftExpression;
  //变量绑定值是否为空
    private ThreadLocal<Boolean> isEmpty = new ThreadLocal<Boolean>();
    
    public boolean isEmpty() {
    	Boolean e=isEmpty.get();
		return e!=null && e;
	}
    
    private ItemsList itemsList;

    private boolean not = false;

    public InExpression() {
    }

    public InExpression(Expression leftExpression, ItemsList itemsList) {
        setLeftExpression(leftExpression);
        setItemsList(itemsList);
    }

    public ItemsList getItemsList() {
        return itemsList;
    }

    public Expression getLeftExpression() {
        return leftExpression;
    }

    public void setEmpty(Boolean isEmpty) {
		this.isEmpty.set(isEmpty);
	}
    
    public void setItemsList(ItemsList list) {
        itemsList = list;
    }

    public void setLeftExpression(Expression expression) {
        leftExpression = expression;
    }

    public boolean isNot() {
        return not;
    }

    public void setNot(boolean b) {
        not = b;
    }

    public void accept(ExpressionVisitor expressionVisitor) {
        expressionVisitor.visit(this);
    }

    public String toString() {
    	StringBuilder sb=new StringBuilder();
    	appendTo(sb);
    	return sb.toString();
    }

	public void appendTo(StringBuilder sb) {
		leftExpression.appendTo(sb);
		sb.append(' ');
		if(not)sb.append("NOT ");
		sb.append("IN ");
		itemsList.appendTo(sb);
	}
}

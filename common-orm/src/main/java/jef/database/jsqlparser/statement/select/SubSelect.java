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

import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.ExpressionType;
import jef.database.jsqlparser.visitor.ExpressionVisitor;
import jef.database.jsqlparser.visitor.FromItem;
import jef.database.jsqlparser.visitor.ItemsList;
import jef.database.jsqlparser.visitor.SelectBody;
import jef.database.jsqlparser.visitor.SelectItemVisitor;

/**
 * A subselect followed by an optional alias.
 */
public class SubSelect implements FromItem, Expression,ItemsList{

    private SelectBody selectBody;

    private String alias;

    public void accept(SelectItemVisitor fromItemVisitor) {
        fromItemVisitor.visit(this);
    }

    public SelectBody getSelectBody() {
        return selectBody;
    }

    public void setSelectBody(SelectBody body) {
        selectBody = body;
    }

    public void accept(ExpressionVisitor expressionVisitor) {
        expressionVisitor.visit(this);
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String string) {
        alias = string;
    }

    public String toString() {
    	StringBuilder sb=new StringBuilder();
    	appendTo(sb);
        return sb.toString();
    }

	public void appendTo(StringBuilder sb) {
		sb.append('(');
		selectBody.appendTo(sb);
		sb.append(')');
		if(alias!=null)
			sb.append(' ').append(alias);
	}

	public String toWholeName() {
		StringBuilder sb=new StringBuilder();
		sb.append('(');
		selectBody.appendTo(sb);
		sb.append(')');
		return sb.toString();
	}

	public ExpressionType getType() {
		return ExpressionType.complex;
	}
}

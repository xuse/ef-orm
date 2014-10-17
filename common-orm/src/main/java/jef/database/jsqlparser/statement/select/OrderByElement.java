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

import java.util.List;

import jef.database.jsqlparser.expression.Column;
import jef.database.jsqlparser.statement.SqlAppendable;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.SelectItem;
import jef.database.jsqlparser.visitor.SelectItemVisitor;
import jef.tools.StringUtils;

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

    public void accept(SelectItemVisitor orderByVisitor) {
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

	public void reverseAppendTo(StringBuilder sb,String tmpTableAlias,List<SelectItem> items) {
		if(expression instanceof Column){
			Column c=(Column)expression;
			if(items!=null){
				fixWithSelects(c,items);
			}
			if(c.getTableAlias()!=null){
				c.setTableAlias(tmpTableAlias);
			}
		}
		expression.appendTo(sb);
		if(asc){
			sb.append(" DESC");
		}
	}

	private void fixWithSelects(Column c, List<SelectItem> items) {
		for(SelectItem item:items){
			if(item instanceof AllTableColumns){
				if(StringUtils.equalsIgnoreCase(c.getTableAlias(),((AllTableColumns) item).getTable().getAlias())){
					break;
				}else{
					continue;
				}
			}else if(item instanceof AllColumns){
				break;
			}

			SelectExpressionItem sex=item.getAsSelectExpression();
			Expression ex=sex.getExpression();
			if(ex instanceof Column){
				Column exc=(Column)ex;
				if(isMatch(exc,c)){
					if(sex.getAlias()!=null){
						c.setColumnName(sex.getAlias());
					}
				}
			}
		}
		
	}

	private boolean isMatch(Column exc, Column c) {
		if(StringUtils.equalsIgnoreCase(exc.getColumnName(), c.getColumnName())){
			if(c.getTableAlias()==null || exc.getTableAlias()==null){
				return true;
			}
			return StringUtils.equalsIgnoreCase(c.getTableAlias(), exc.getTableAlias());
		}
		return false;
	}
}

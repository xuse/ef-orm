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
package jef.database.jsqlparser.statement.update;

import java.util.List;

import jef.database.jsqlparser.expression.Column;
import jef.database.jsqlparser.parser.Token;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.FromItem;
import jef.database.jsqlparser.visitor.Statement;
import jef.database.jsqlparser.visitor.StatementVisitor;

/**
 * The update statement.
 */
public class Update implements Statement {
    /**
     * 其實只有兩種可能JPQL和table兩種可能
     * @return
     */
    private FromItem table;

    private Expression where;

    private List<Column> columns;

    private List<Expression> expressions;

    private String hint;
    public void setHint(Token t){
    	if(t!=null && t.specialToken!=null){
    		this.hint=t.specialToken.image;	
    	}
    }
    
    public void accept(StatementVisitor statementVisitor) {
        statementVisitor.visit(this);
    }

    public FromItem getTable() {
        return table;
    }

    public Expression getWhere() {
        return where;
    }

    public void setTable(FromItem name) {
        table = name;
    }

    public void setWhere(Expression expression) {
        where = expression;
    }

    /**
	 * The {@link jef.database.jsqlparser.expression.Column}s in this update (as col1 and col2 in UPDATE col1='a', col2='b')
	 * @return a list of {@link jef.database.jsqlparser.expression.Column}s
	 */
    public List<Column> getColumns() {
        return columns;
    }

    /**
	 * The {@link Expression}s in this update (as 'a' and 'b' in UPDATE col1='a', col2='b')
	 * @return a list of {@link Expression}s
	 */
    public List<Expression> getExpressions() {
        return expressions;
    }

    public void setColumns(List<Column> list) {
        columns = list;
    }

    public void setExpressions(List<Expression> list) {
        expressions = list;
    }
    
    public String toString() {
        StringBuilder sql = new StringBuilder("update ");
        if(hint!=null){
        	sql.append(hint).append(' ');
        }
        sql.append(table).append(" set ");
        int i=0;
        for(Column col: columns){
        	if(i>0){
        		sql.append(',');
        	}
        	sql.append(col).append(" = ").append(expressions.get(i++));
        }
        if(where!=null)
        	sql.append(" where ").append(where);
        return sql.toString();
    }
}

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
package jef.database.jsqlparser.statement.delete;

import jef.database.jsqlparser.parser.Token;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.FromItem;
import jef.database.jsqlparser.visitor.Statement;
import jef.database.jsqlparser.visitor.StatementVisitor;

public class Delete implements Statement {

    private FromItem table;

    private Expression where;
    private String alias;
    private String hint;
    
    public void setHint(Token t){
    	if(t!=null && t.specialToken!=null){
    		this.hint=t.specialToken.image;	
    	}
    }

    public String getAlias() {
		return alias;
	}


	public void setAlias(String alias) {
		this.alias = alias;
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

    public String toString() {
    	StringBuilder sb=new StringBuilder(128);
    	sb.append("delete ");
    	if(hint!=null){
    		sb.append(hint).append(' ');
    	}
    	if(alias!=null){
    		sb.append(alias).append(' ');
    	}
    	sb.append("from ");
    	table.appendTo(sb);
    	if(where !=null){
    		sb.append(" where ");
    		where.appendTo(sb);
    	}
        return sb.toString();
    }
}

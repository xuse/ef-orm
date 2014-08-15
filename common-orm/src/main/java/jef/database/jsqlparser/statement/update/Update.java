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

import java.util.Iterator;
import java.util.List;

import jef.common.Pair;
import jef.database.jsqlparser.expression.Column;
import jef.database.jsqlparser.parser.Token;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.FromItem;
import jef.database.jsqlparser.visitor.Ignorable;
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

    private List<Pair<Column,Expression>> sets;

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

    public List<Pair<Column, Expression>> getSets() {
		return sets;
	}
    
    public Pair<Column, Expression> getSet(int i) {
    	return sets.get(i);
    }

	public void setSets(List<Pair<Column, Expression>> sets) {
		this.sets = sets;
	}

	public String getHint() {
		return hint;
	}

	public void setHint(String hint) {
		this.hint = hint;
	}
	public void addSet(Column c, Expression exp){
		this.sets.add(new Pair<Column,Expression>(c,exp));
	}

	public String toString() {
        StringBuilder sb = new StringBuilder("update ");
        if(hint!=null){
        	sb.append(hint).append(' ');
        }
        sb.append(table).append(" set ");
        
        
        Iterator<Pair<Column,Expression>> iter=sets.iterator();
        if(iter.hasNext()){
        	Pair<Column,Expression> pair=iter.next();
        	pair.first.appendTo(sb);
        	sb.append(" = ");
        	pair.second.appendTo(sb);
        }
        while(iter.hasNext()){
        	Pair<Column,Expression> pair=iter.next();
        	sb.append(',');
        	pair.first.appendTo(sb);
        	sb.append(" = ");
        	pair.second.appendTo(sb);
        }
        if(where!=null){
        	appendWhere(sb,where);
        }
        return sb.toString();
    }
	
	private void appendWhere(StringBuilder sb, Expression where) {
		if (where instanceof Ignorable) {
			if (((Ignorable) where).isEmpty()) {
				return;
			}
		}
		sb.append(" where ");
		int len = sb.length();
		where.appendTo(sb);
		// 防止动态条件均为生效后多余的where关键字引起SQL错误
		if (sb.length() - len < 2) {
			sb.setLength(len - 7);
		}
	}
}

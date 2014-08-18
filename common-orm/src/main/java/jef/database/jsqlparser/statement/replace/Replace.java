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
package jef.database.jsqlparser.statement.replace;

import java.util.List;

import jef.database.jsqlparser.Util;
import jef.database.jsqlparser.expression.Column;
import jef.database.jsqlparser.expression.Table;
import jef.database.jsqlparser.statement.select.PlainSelect;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.ItemsList;
import jef.database.jsqlparser.visitor.Statement;
import jef.database.jsqlparser.visitor.StatementVisitor;

/**
 * The replace statement.
 */
public class Replace implements Statement {

    private Table table;

    private List<Column> columns;

    private ItemsList itemsList;

    private List<Expression> expressions;

    private boolean useValues = true;

    public void accept(StatementVisitor statementVisitor) {
        statementVisitor.visit(this);
    }

    public Table getTable() {
        return table;
    }

    public void setTable(Table name) {
        table = name;
    }

    /**
	 * A list of {@link jef.database.jsqlparser.expression.Column}s either from a "REPLACE mytab (col1, col2) [...]" or a "REPLACE mytab SET col1=exp1, col2=exp2". 
	 * @return a list of {@link jef.database.jsqlparser.expression.Column}s
	 */
    public List<Column> getColumns() {
        return columns;
    }

    /**
	 * An {@link ItemsList} (either from a "REPLACE mytab VALUES (exp1,exp2)" or a "REPLACE mytab SELECT * FROM mytab2")  
	 * it is null in case of a "REPLACE mytab SET col1=exp1, col2=exp2"  
	 */
    public ItemsList getItemsList() {
        return itemsList;
    }

    public void setColumns(List<Column> list) {
        columns = list;
    }

    public void setItemsList(ItemsList list) {
        itemsList = list;
    }

    /**
	 * A list of {@link jef.database.jsqlparser.visitor.Expression}s (from a "REPLACE mytab SET col1=exp1, col2=exp2"). <br>
	 * it is null in case of a "REPLACE mytab (col1, col2) [...]"  
	 */
    public List<Expression> getExpressions() {
        return expressions;
    }

    public void setExpressions(List<Expression> list) {
        expressions = list;
    }

    public boolean isUseValues() {
        return useValues;
    }

    public void setUseValues(boolean useValues) {
        this.useValues = useValues;
    }

    public String toString() {
        StringBuilder sb=new StringBuilder(256).append("REPLACE ").append(table.toString());
        if (expressions != null && columns != null) {
        	sb.append(" SET ");
            for (int i = 0, s = columns.size(); i < s; i++) {
            	sb.append(columns.get(i).toString()).append('=');
            	sb.append(expressions.get(i).toString());
            	if(i < s - 1){
            		sb.append(", ");
            	}
            }
        } else if (columns != null) {
        	sb.append(' ');
        	Util.getStringList(sb,columns, ",", true);
        }
        if (itemsList != null) {
            if (useValues) {
            	sb.append(" VALUES");
            }
            sb.append(' ').append(itemsList.toString());
        }
        return sb.toString();
    }
}

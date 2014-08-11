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
package jef.database.jsqlparser.statement.insert;

import java.util.List;

import jef.database.jsqlparser.expression.Column;
import jef.database.jsqlparser.parser.Token;
import jef.database.jsqlparser.statement.select.PlainSelect;
import jef.database.jsqlparser.visitor.FromItem;
import jef.database.jsqlparser.visitor.ItemsList;
import jef.database.jsqlparser.visitor.Statement;
import jef.database.jsqlparser.visitor.StatementVisitor;

/**
 * The insert statement.
 * Every column name in <code>columnNames</code> matches an item in <code>itemsList</code>
 */
public class Insert implements Statement {

    private FromItem table;

    private List<Column> columns;

    private ItemsList itemsList;

    private boolean useValues = true;

    private String hint;
    
    public void accept(StatementVisitor statementVisitor) {
        statementVisitor.visit(this);
    }
    
    public void setHint(Token t){
    	if(t!=null && t.specialToken!=null){
    		this.hint=t.specialToken.image;	
    	}
    }

    public FromItem getTable() {
        return table;
    }

    public void setTable(FromItem name) {
        table = name;
    }

    /**
	 * Get the columns (found in "INSERT INTO (col1,col2..) [...]" )
	 * @return a list of {@link jef.database.jsqlparser.expression.Column}
	 */
    public List<Column> getColumns() {
        return columns;
    }

    public void setColumns(List<Column> list) {
        columns = list;
    }

    /**
	 * Get the values (as VALUES (...) or SELECT) 
	 * @return the values of the insert
	 */
    public ItemsList getItemsList() {
        return itemsList;
    }

    public void setItemsList(ItemsList list) {
        itemsList = list;
    }

    public boolean isUseValues() {
        return useValues;
    }

    public void setUseValues(boolean useValues) {
        this.useValues = useValues;
    }

    public String toString() {
    	StringBuilder sb=new StringBuilder(128);
        sb.append("insert ");
        if(hint!=null){
        	sb.append(hint).append(' ');
        }
        sb.append("into ");
        sb.append(table).append(' ');
        if(columns != null){
        	PlainSelect.getStringList(sb,columns, ",", true);
        	sb.append(' ');
        }
        if (useValues) {
        	sb.append("values ").append(itemsList);
        } else {
        	sb.append(itemsList);
        }
        return sb.toString();
    }
}

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
package jef.database.jsqlparser.statement.create;

import java.util.List;

import jef.database.jsqlparser.expression.Table;
import jef.database.jsqlparser.statement.select.PlainSelect;
import jef.database.jsqlparser.visitor.Statement;
import jef.database.jsqlparser.visitor.StatementVisitor;
import jef.tools.StringUtils;

/**
 * A "CREATE TABLE" statement
 */
public class CreateTable implements Statement {

    private Table table;

    private List<String> tableOptionsStrings;

    private List<ColumnDefinition> columnDefinitions;

    private List<Index> indexes;

    public void accept(StatementVisitor statementVisitor) {
        statementVisitor.visit(this);
    }

    /**
     * The name of the table to be created
     */
    public Table getTable() {
        return table;
    }

    public void setTable(Table table) {
        this.table = table;
    }

    /**
     * A list of {@link ColumnDefinition}s of this table.
     */
    public List<ColumnDefinition> getColumnDefinitions() {
        return columnDefinitions;
    }

    public void setColumnDefinitions(List<ColumnDefinition> list) {
        columnDefinitions = list;
    }

    /**
     * A list of options (as simple strings) of this table definition, as ("TYPE", "=", "MYISAM") 
     */
    public List<String> getTableOptionsStrings() {
        return tableOptionsStrings;
    }

    public void setTableOptionsStrings(List<String> list) {
        tableOptionsStrings = list;
    }

    /**
     * A list of {@link Index}es (for example "PRIMARY KEY") of this table.<br>
     * Indexes created with column definitions (as in mycol INT PRIMARY KEY) are not inserted into this list.  
     */
    public List<Index> getIndexes() {
        return indexes;
    }

    public void setIndexes(List<Index> list) {
        indexes = list;
    }

    public String toString() {
        StringBuilder sb=new StringBuilder().append("CREATE TABLE ");
        sb.append(table).append(" (");
        PlainSelect.getStringList(sb,columnDefinitions, ",", false);
        if (indexes != null && indexes.size() > 0) {
        	sb.append(", ");
            PlainSelect.getStringList(sb,indexes,",",false);
        }
        sb.append(") ");
        StringUtils.joinTo(tableOptionsStrings, " ", sb);
        return sb.toString();
    }
}

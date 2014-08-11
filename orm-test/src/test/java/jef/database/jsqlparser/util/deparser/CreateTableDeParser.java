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
package jef.database.jsqlparser.util.deparser;

import java.util.Iterator;

import jef.database.jsqlparser.statement.create.ColumnDefinition;
import jef.database.jsqlparser.statement.create.CreateTable;
import jef.database.jsqlparser.statement.create.Index;

/**
 * A class to de-parse (that is, tranform from JSqlParser hierarchy into a string)
 * a {@link jef.database.jsqlparser.statement.create.table.CreateTable}
 */
public class CreateTableDeParser {

    protected StringBuilder buffer;

    /**
	 * @param buffer the buffer that will be filled with the select
	 */
    public CreateTableDeParser(StringBuilder buffer) {
        this.buffer = buffer;
    }

    public void deParse(CreateTable createTable) {
        buffer.append("CREATE TABLE " + createTable.getTable().toWholeName());
        if (createTable.getColumnDefinitions() != null) {
            buffer.append(" { ");
            for (Iterator<ColumnDefinition> iter = createTable.getColumnDefinitions().iterator(); iter.hasNext(); ) {
                ColumnDefinition columnDefinition = (ColumnDefinition) iter.next();
                buffer.append(columnDefinition.getColumnName());
                buffer.append(" ");
                buffer.append(columnDefinition.getColDataType().getDataType());
                if (columnDefinition.getColDataType().getArgumentsStringList() != null) {
                    for (Iterator<String> iterator = columnDefinition.getColDataType().getArgumentsStringList().iterator(); iterator.hasNext(); ) {
                        buffer.append(" ");
                        buffer.append((String) iterator.next());
                    }
                }
                if (columnDefinition.getColumnSpecStrings() != null) {
                    for (Iterator<String> iterator = columnDefinition.getColumnSpecStrings().iterator(); iterator.hasNext(); ) {
                        buffer.append(" ");
                        buffer.append((String) iterator.next());
                    }
                }
                if (iter.hasNext()) buffer.append(",\n");
            }
            for (Iterator<Index> iter = createTable.getIndexes().iterator(); iter.hasNext(); ) {
                buffer.append(",\n");
                Index index = (Index) iter.next();
                buffer.append(index.getType() + " " + index.getName());
                buffer.append("(");
                for (Iterator<String> iterator = index.getColumnsNames().iterator(); iterator.hasNext(); ) {
                    buffer.append((String) iterator.next());
                    if (iterator.hasNext()) {
                        buffer.append(", ");
                    }
                }
                buffer.append(")");
                if (iter.hasNext()) buffer.append(",\n");
            }
            buffer.append(" \n} ");
        }
    }

    public StringBuilder getBuffer() {
        return buffer;
    }

    public void setBuffer(StringBuilder buffer) {
        this.buffer = buffer;
    }
}

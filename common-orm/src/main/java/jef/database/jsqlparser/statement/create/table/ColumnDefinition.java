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
package jef.database.jsqlparser.statement.create.table;

import java.util.List;

import jef.database.jsqlparser.statement.SqlAppendable;
import jef.tools.StringUtils;

/**
 * A column definition in a CREATE TABLE statement.<br>
 * Example: mycol VARCHAR(30) NOT NULL
 */
public class ColumnDefinition implements SqlAppendable{

    private String columnName;

    private ColDataType colDataType;

    private List<String> columnSpecStrings;

    /**
	 * A list of strings of every word after the datatype of the column.<br>
	 * Example ("NOT", "NULL")
	 */
    public List<String> getColumnSpecStrings() {
        return columnSpecStrings;
    }

    public void setColumnSpecStrings(List<String> list) {
        columnSpecStrings = list;
    }

    /**
	 * The {@link ColDataType} of this column definition 
	 */
    public ColDataType getColDataType() {
        return colDataType;
    }

    public void setColDataType(ColDataType type) {
        colDataType = type;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String string) {
        columnName = string;
    }

    public String toString() {
    	StringBuilder sb=new StringBuilder(64);
    	appendTo(sb);
    	return sb.toString();
    }

	public void appendTo(StringBuilder sb) {
		sb.append(columnName);
		sb.append(' ').append(colDataType).append(' ');
		StringUtils.joinTo(columnSpecStrings, " ", sb);
	}
}

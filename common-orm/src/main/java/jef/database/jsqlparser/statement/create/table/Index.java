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
 * An index (unique, primary etc.) in a CREATE TABLE statement 
 */
public class Index implements SqlAppendable {

    private String type;

    private List<String> columnsNames;

    private String name;

    /**
     * A list of strings of all the columns regarding this index  
     */
    public List<String> getColumnsNames() {
        return columnsNames;
    }

    public String getName() {
        return name;
    }

    /**
     * The type of this index: "PRIMARY KEY", "UNIQUE", "INDEX"
     */
    public String getType() {
        return type;
    }
    
    public void setParimaryKey() {
        type="PRIMARY KEY";
    }

    public void setColumnsNames(List<String> list) {
        columnsNames = list;
    }

    public void setName(String string) {
        name = string;
    }

    public void setType(String string) {
        type = string;
    }

    public String toString() {
    	StringBuilder sb=new StringBuilder(64);
    	appendTo(sb);
        return sb.toString();
    }

	public void appendTo(StringBuilder sb) {
		sb.append(type).append(' ');
		if(columnsNames!=null){
    		sb.append('(');
    		StringUtils.joinTo(columnsNames, ",", sb);
    		sb.append(')');
    	}
    	if(name!=null){
    		sb.append(' ').append(name);
    	}
	}
}

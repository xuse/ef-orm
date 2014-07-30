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
package jef.database.jsqlparser.schema;

import jef.database.jsqlparser.statement.select.FromItem;
import jef.database.jsqlparser.statement.select.FromItemVisitor;
import jef.tools.StringUtils;

/**
 * A table. It can have an alias and the schema name it belongs to. 
 */
public class Table implements FromItem {

    private String schemaName;

    private String name;

    private String alias;

    public Table() {
    }

    public Table(String schemaName, String name) {
        this.schemaName = schemaName;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getSchemaName() {
        return schemaName;
    }
    
    public void addDbLink(String dbLink){
    	if(dbLink!=null)
    		name=StringUtils.concat(name,"@",dbLink);
    }
    
    public void setName(String string) {
        name = string;
    }

    public void setSchemaName(String string) {
        schemaName = string;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String string) {
        alias = string;
    }

    public String getWholeTableName() {
        if (name == null) {
            return null;
        }
        if (schemaName != null) {
           return  schemaName + "." + name;
        } else {
            return name;
        }
    }

    public void accept(FromItemVisitor fromItemVisitor) {
        fromItemVisitor.visit(this);
    }

    /*
     * Jiyi modified at 2011-6-2
     * The oracle does't not support 'AS' in table alias, since it is a keyword. remove the key.
     * @see java.lang.Object#toString()
     */
    public String toString() {
    	StringBuilder sb=new StringBuilder();
    	appendTo(sb);
        return sb.toString();
    }

	public void appendTo(StringBuilder sb) {
		if(name==null)return;
		if(schemaName!=null){
			sb.append(schemaName).append('.');
		}
		sb.append(name);
		if(alias!=null){
			sb.append(' ').append(alias);
		}
	}
}

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
package jef.database.jsqlparser.statement.select;

import java.util.List;

import jef.database.jsqlparser.statement.SqlAppendable;
import jef.database.jsqlparser.visitor.SelectItem;

/**
 * A DISTINCT [ON (expression, ...)] clause
 */
public class Distinct implements SqlAppendable{

    private List<SelectItem> onSelectItems;

    /**
	 * A list of {@link SelectItem}s expressions, as in "select DISTINCT ON (a,b,c) a,b FROM..." 
	 * @return a list of {@link SelectItem}s expressions
	 */
    public List<SelectItem> getOnSelectItems() {
        return onSelectItems;
    }

    public void setOnSelectItems(List<SelectItem> list) {
        onSelectItems = list;
    }

    public void appendTo(StringBuilder sb){
    	sb.append("DISTINCT");
    	if (onSelectItems != null && onSelectItems.size() > 0) {
        	sb.append(" ON ");
            PlainSelect.getStringList(sb,onSelectItems,",",true);
        }
    }
    
    public String toString() {
    	StringBuilder sb=new StringBuilder(64);
    	appendTo(sb);
    	return 	sb.toString();
    }
}

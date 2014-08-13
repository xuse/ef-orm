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
import jef.database.jsqlparser.visitor.SelectBody;
import jef.database.jsqlparser.visitor.SelectItem;
import jef.database.jsqlparser.visitor.SelectVisitor;

/**
 * One of the parts of a "WITH" clause of a "SELECT" statement  
 */
public class WithItem implements SqlAppendable{

    private String name;

    private List<SelectItem> withItemList;

    private SelectBody selectBody;

    /**
	 * The name of this WITH item (for example, "myWITH" in "WITH myWITH AS (SELECT A,B,C))"
	 * @return the name of this WITH
	 */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
	 * The {@link SelectBody} of this WITH item is the part after the "AS" keyword
	 * @return {@link SelectBody} of this WITH item
	 */
    public SelectBody getSelectBody() {
        return selectBody;
    }

    public void setSelectBody(SelectBody selectBody) {
        this.selectBody = selectBody;
    }

    /**
	 * The {@link SelectItem}s in this WITH (for example the A,B,C in "WITH mywith (A,B,C) AS ...")
	 * @return a list of {@link SelectItem}s
	 */
    public List<SelectItem> getWithItemList() {
        return withItemList;
    }

    public void setWithItemList(List<SelectItem> withItemList) {
        this.withItemList = withItemList;
    }

    public String toString() {
    	StringBuilder sb=new StringBuilder(80);
    	appendTo(sb);
        return sb.toString();
    }

	public void appendTo(StringBuilder sb) {
		sb.append(name);
    	if(withItemList != null){
    		sb.append(' ');
    		PlainSelect.getStringList(sb,withItemList, ",", true); 
    	}
    	sb.append(" AS (");
    	selectBody.appendTo(sb);
    	sb.append(')');
	}
	
	public void accept(SelectVisitor visitor){
		visitor.visit(this);
	}
}

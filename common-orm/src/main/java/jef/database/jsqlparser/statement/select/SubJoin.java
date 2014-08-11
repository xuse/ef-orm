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

import jef.database.jsqlparser.visitor.FromItem;
import jef.database.jsqlparser.visitor.SelectItemVisitor;

/**
 * A table created by "(tab1 join tab2)".
 */
public class SubJoin implements FromItem {

    private FromItem left;

    private Join join;

    private String alias;

    public void accept(SelectItemVisitor fromItemVisitor) {
        fromItemVisitor.visit(this);
    }

    public FromItem getLeft() {
        return left;
    }

    public void setLeft(FromItem l) {
        left = l;
    }

    public Join getJoin() {
        return join;
    }

    public void setJoin(Join j) {
        join = j;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String string) {
        alias = string;
    }

    public String toString() {
    	StringBuilder sb=new StringBuilder();
    	appendTo(sb);
        return sb.toString();
    }

	public String toWholeName() {
		StringBuilder sb=new StringBuilder();
		sb.append('(');
		left.appendTo(sb);
		sb.append(' ');
		join.appendTo(sb);
		return sb.append(')').toString();
	}

	public void appendTo(StringBuilder sb) {
		sb.append('(');
		left.appendTo(sb);
		sb.append(' ');
		join.appendTo(sb);
		sb.append(')');
		if(alias!=null){
			sb.append(' ').append(alias);
		}
	}
}

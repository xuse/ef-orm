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

import java.util.Iterator;
import java.util.List;

import jef.database.jsqlparser.statement.SqlAppendable;
import jef.database.jsqlparser.visitor.SelectBody;
import jef.database.jsqlparser.visitor.Statement;
import jef.database.jsqlparser.visitor.StatementVisitor;

public class Select implements Statement,SqlAppendable,Cloneable {

    private SelectBody selectBody;

    private List<WithItem> withItemsList;

    public void accept(StatementVisitor statementVisitor) {
        statementVisitor.visit(this);
    }

    public SelectBody getSelectBody() {
        return selectBody;
    }

    public void setSelectBody(SelectBody body) {
        selectBody = body;
    }

    public String toString() {
        StringBuilder retval = new StringBuilder();
        appendTo(retval);
        return retval.toString();
    }

    public List<WithItem> getWithItemsList() {
        return withItemsList;
    }

    public void setWithItemsList(List<WithItem> withItemsList) {
        this.withItemsList = withItemsList;
    }

	public void appendTo(StringBuilder sb) {
        if (withItemsList != null && !withItemsList.isEmpty()) {
            sb.append("WITH ");
            for (Iterator<WithItem> iter = withItemsList.iterator(); iter.hasNext(); ) {
                WithItem withItem = iter.next();
                withItem.appendTo(sb);
                if (iter.hasNext()) sb.append(",");
                sb.append(" ");
            }
        }
        selectBody.appendTo(sb);
	}
	
}

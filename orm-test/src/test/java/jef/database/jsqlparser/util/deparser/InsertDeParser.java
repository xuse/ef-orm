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

import jef.database.jsqlparser.expression.Column;
import jef.database.jsqlparser.expression.operators.relational.ExpressionList;
import jef.database.jsqlparser.statement.insert.Insert;
import jef.database.jsqlparser.statement.select.SubSelect;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.ExpressionVisitor;
import jef.database.jsqlparser.visitor.ItemsListVisitor;
import jef.database.jsqlparser.visitor.SelectVisitor;

/**
 * A class to de-parse (that is, tranform from JSqlParser hierarchy into a string)
 * an {@link jef.database.jsqlparser.statement.insert.Insert}
 */
public class InsertDeParser implements ItemsListVisitor {

    protected StringBuilder buffer;

    protected ExpressionVisitor expressionVisitor;

    protected SelectVisitor selectVisitor;

    public InsertDeParser() {
    }

    /**
	 * @param expressionVisitor a {@link ExpressionVisitor} to de-parse {@link jef.database.jsqlparser.visitor.Expression}s. It has to share the same<br>
	 * StringBuffer (buffer parameter) as this object in order to work
	 * @param selectVisitor a {@link SelectVisitor} to de-parse {@link jef.database.jsqlparser.statement.select.Select}s.
	 * It has to share the same<br>
	 * StringBuffer (buffer parameter) as this object in order to work
	 * @param buffer the buffer that will be filled with the insert
	 */
    public InsertDeParser(ExpressionVisitor expressionVisitor, SelectVisitor selectVisitor, StringBuilder buffer) {
        this.buffer = buffer;
        this.expressionVisitor = expressionVisitor;
        this.selectVisitor = selectVisitor;
    }

    public StringBuilder getBuffer() {
        return buffer;
    }

    public void setBuffer(StringBuilder buffer) {
        this.buffer = buffer;
    }

    public void deParse(Insert insert) {
        buffer.append("INSERT INTO ");
        buffer.append(insert.getTable().toWholeName());
        if (insert.getColumns() != null) {
            buffer.append("(");
            for (Iterator<Column> iter = insert.getColumns().iterator(); iter.hasNext(); ) {
                Column column = (Column) iter.next();
                buffer.append(column.getColumnName());
                if (iter.hasNext()) {
                    buffer.append(", ");
                }
            }
            buffer.append(")");
        }
        insert.getItemsList().accept(this);
    }

    public void visit(ExpressionList expressionList) {
        buffer.append(" VALUES (");
        for (Iterator<Expression> iter = expressionList.getExpressions().iterator(); iter.hasNext(); ) {
            Expression expression = (Expression) iter.next();
            expression.accept(expressionVisitor);
            if (iter.hasNext()) buffer.append(", ");
        }
        buffer.append(")");
    }

    public void visit(SubSelect subSelect) {
        subSelect.getSelectBody().accept(selectVisitor);
    }

    public ExpressionVisitor getExpressionVisitor() {
        return expressionVisitor;
    }

    public SelectVisitor getSelectVisitor() {
        return selectVisitor;
    }

    public void setExpressionVisitor(ExpressionVisitor visitor) {
        expressionVisitor = visitor;
    }

    public void setSelectVisitor(SelectVisitor visitor) {
        selectVisitor = visitor;
    }
}

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

import jef.database.jsqlparser.expression.Column;
import jef.database.jsqlparser.statement.update.Update;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.ExpressionVisitor;

/**
 * A class to de-parse (that is, tranform from JSqlParser hierarchy into a string)
 * an {@link jef.database.jsqlparser.statement.update.Update}
 */
public class UpdateDeParser {

    protected StringBuilder buffer;

    protected ExpressionVisitor expressionVisitor;

    public UpdateDeParser() {
    }

    /**
	 * @param expressionVisitor a {@link ExpressionVisitor} to de-parse expressions. It has to share the same<br>
	 * StringBuffer (buffer parameter) as this object in order to work
	 * @param buffer the buffer that will be filled with the select
	 */
    public UpdateDeParser(ExpressionVisitor expressionVisitor, StringBuilder buffer) {
        this.buffer = buffer;
        this.expressionVisitor = expressionVisitor;
    }

    public StringBuilder getBuffer() {
        return buffer;
    }

    public void setBuffer(StringBuilder buffer) {
        this.buffer = buffer;
    }

    public void deParse(Update update) {
        buffer.append("UPDATE " + update.getTable().getWholeTableName() + " SET ");
        for (int i = 0; i < update.getColumns().size(); i++) {
            Column column = (Column) update.getColumns().get(i);
            buffer.append(column.getWholeColumnName() + "=");
            Expression expression = (Expression) update.getExpressions().get(i);
            expression.accept(expressionVisitor);
            if (i < update.getColumns().size() - 1) {
                buffer.append(", ");
            }
        }
        if (update.getWhere() != null) {
            buffer.append(" WHERE ");
            update.getWhere().accept(expressionVisitor);
        }
    }

    public ExpressionVisitor getExpressionVisitor() {
        return expressionVisitor;
    }

    public void setExpressionVisitor(ExpressionVisitor visitor) {
        expressionVisitor = visitor;
    }
}

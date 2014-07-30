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

import jef.database.jsqlparser.expression.ExpressionVisitor;
import jef.database.jsqlparser.statement.delete.Delete;

/**
 * A class to de-parse (that is, tranform from JSqlParser hierarchy into a string)
 * a {@link jef.database.jsqlparser.statement.delete.Delete}
 */
public class DeleteDeParser {

    protected StringBuffer buffer;

    protected ExpressionVisitor expressionVisitor;

    public DeleteDeParser() {
    }

    /**
	 * @param expressionVisitor a {@link ExpressionVisitor} to de-parse expressions. It has to share the same<br>
	 * StringBuffer (buffer parameter) as this object in order to work
	 * @param buffer the buffer that will be filled with the select
	 */
    public DeleteDeParser(ExpressionVisitor expressionVisitor, StringBuffer buffer) {
        this.buffer = buffer;
        this.expressionVisitor = expressionVisitor;
    }

    public StringBuffer getBuffer() {
        return buffer;
    }

    public void setBuffer(StringBuffer buffer) {
        this.buffer = buffer;
    }

    public void deParse(Delete delete) {
        buffer.append("DELETE FROM " + delete.getTable().getWholeTableName());
        if (delete.getWhere() != null) {
            buffer.append(" WHERE ");
            delete.getWhere().accept(expressionVisitor);
        }
    }

    public ExpressionVisitor getExpressionVisitor() {
        return expressionVisitor;
    }

    public void setExpressionVisitor(ExpressionVisitor visitor) {
        expressionVisitor = visitor;
    }
}

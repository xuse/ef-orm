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
package jef.database.jsqlparser.expression.operators.relational;

import jef.database.jsqlparser.expression.BinaryExpression;
import jef.database.jsqlparser.visitor.ExpressionVisitor;

public class LikeExpression extends BinaryExpression {

    private boolean not = false;

    private String escape = null;

    public boolean isNot() {
        return not;
    }

    public void setNot(boolean b) {
        not = b;
    }

    public void accept(ExpressionVisitor expressionVisitor) {
        expressionVisitor.visit(this);
    }

    public String getStringExpression() {
        return ((not) ? "NOT " : "") + "LIKE";
    }

    @Override
	public void appendTo(StringBuilder sb) {
		super.appendTo(sb);
		if (escape != null) {
            sb.append(" ESCAPE ").append('\'').append(escape).append('\'');
//            retval +=  + "'" + escape + "'";
        }
	}

	public String getEscape() {
        return escape;
    }

    public void setEscape(String escape) {
        this.escape = escape;
    }
}

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
package jef.database.jsqlparser.expression;

import java.util.List;

import jef.database.jsqlparser.statement.select.PlainSelect;

/**
 * CASE/WHEN expression.
 * 
 * Syntax:
 * <code><pre>
 * CASE 
 * WHEN condition THEN expression
 * [WHEN condition THEN expression]...
 * [ELSE expression]
 * END
 * </pre></code>
 * 
 * <br/>
 * or <br/>
 * <br/>
 * 
 * <code><pre>
 * CASE expression 
 * WHEN condition THEN expression
 * [WHEN condition THEN expression]...
 * [ELSE expression]
 * END
 * </pre></code>
 *  
 *  See also:
 *  https://aurora.vcu.edu/db2help/db2s0/frame3.htm#casexp
 *  http://sybooks.sybase.com/onlinebooks/group-as/asg1251e/commands/@ebt-link;pt=5954?target=%25N%15_52628_START_RESTART_N%25
 *  
 *  
 * @author Havard Rast Blok
 */
public class CaseExpression implements Expression {

    private Expression switchExpression;

    private List<WhenClause> whenClauses;

    private Expression elseExpression;

    public void accept(ExpressionVisitor expressionVisitor) {
        expressionVisitor.visit(this);
    }

    /**
	 * @return Returns the switchExpression.
	 */
    public Expression getSwitchExpression() {
        return switchExpression;
    }

    /**
	 * @param switchExpression The switchExpression to set.
	 */
    public void setSwitchExpression(Expression switchExpression) {
        this.switchExpression = switchExpression;
    }

    /**
	 * @return Returns the elseExpression.
	 */
    public Expression getElseExpression() {
        return elseExpression;
    }

    /**
	 * @param elseExpression The elseExpression to set.
	 */
    public void setElseExpression(Expression elseExpression) {
        this.elseExpression = elseExpression;
    }

    /**
	 * @return Returns the whenClauses.
	 */
    public List<WhenClause> getWhenClauses() {
        return whenClauses;
    }

    /**
	 * @param whenClauses The whenClauses to set.
	 */
    public void setWhenClauses(List<WhenClause> whenClauses) {
        this.whenClauses = whenClauses;
    }

    public String toString() {
    	StringBuilder sb=new StringBuilder(128);
    	appendTo(sb);
        return sb.toString();
    }

	public void appendTo(StringBuilder sb) {
		sb.append("CASE ");
    	if(switchExpression != null){
    		switchExpression.appendTo(sb);
    		sb.append(' ');
    	}
    	PlainSelect.getStringList(sb,whenClauses, " ", false);
    	sb.append(' ');
    	if(elseExpression != null) {
    		sb.append("ELSE ");
    		elseExpression.appendTo(sb);
    		sb.append(' ');
    	}
    	sb.append("END");
	}
}

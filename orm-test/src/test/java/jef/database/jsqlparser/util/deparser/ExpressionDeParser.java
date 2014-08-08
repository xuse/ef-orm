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
import java.util.List;

import jef.database.jsqlparser.expression.AllComparisonExpression;
import jef.database.jsqlparser.expression.AnyComparisonExpression;
import jef.database.jsqlparser.expression.BinaryExpression;
import jef.database.jsqlparser.expression.CaseExpression;
import jef.database.jsqlparser.expression.DateValue;
import jef.database.jsqlparser.expression.DoubleValue;
import jef.database.jsqlparser.expression.Expression;
import jef.database.jsqlparser.expression.ExpressionVisitor;
import jef.database.jsqlparser.expression.Function;
import jef.database.jsqlparser.expression.Interval;
import jef.database.jsqlparser.expression.InverseExpression;
import jef.database.jsqlparser.expression.JdbcParameter;
import jef.database.jsqlparser.expression.JpqlParameter;
import jef.database.jsqlparser.expression.LongValue;
import jef.database.jsqlparser.expression.NullValue;
import jef.database.jsqlparser.expression.Over;
import jef.database.jsqlparser.expression.Parenthesis;
import jef.database.jsqlparser.expression.StringValue;
import jef.database.jsqlparser.expression.TimeValue;
import jef.database.jsqlparser.expression.TimestampValue;
import jef.database.jsqlparser.expression.WhenClause;
import jef.database.jsqlparser.expression.operators.arithmetic.Addition;
import jef.database.jsqlparser.expression.operators.arithmetic.BitwiseAnd;
import jef.database.jsqlparser.expression.operators.arithmetic.BitwiseOr;
import jef.database.jsqlparser.expression.operators.arithmetic.BitwiseXor;
import jef.database.jsqlparser.expression.operators.arithmetic.Concat;
import jef.database.jsqlparser.expression.operators.arithmetic.Division;
import jef.database.jsqlparser.expression.operators.arithmetic.Mod;
import jef.database.jsqlparser.expression.operators.arithmetic.Multiplication;
import jef.database.jsqlparser.expression.operators.arithmetic.Subtraction;
import jef.database.jsqlparser.expression.operators.conditional.AndExpression;
import jef.database.jsqlparser.expression.operators.conditional.OrExpression;
import jef.database.jsqlparser.expression.operators.relational.Between;
import jef.database.jsqlparser.expression.operators.relational.EqualsTo;
import jef.database.jsqlparser.expression.operators.relational.ExistsExpression;
import jef.database.jsqlparser.expression.operators.relational.ExpressionList;
import jef.database.jsqlparser.expression.operators.relational.GreaterThan;
import jef.database.jsqlparser.expression.operators.relational.GreaterThanEquals;
import jef.database.jsqlparser.expression.operators.relational.InExpression;
import jef.database.jsqlparser.expression.operators.relational.IsNullExpression;
import jef.database.jsqlparser.expression.operators.relational.ItemsListVisitor;
import jef.database.jsqlparser.expression.operators.relational.LikeExpression;
import jef.database.jsqlparser.expression.operators.relational.Matches;
import jef.database.jsqlparser.expression.operators.relational.MinorThan;
import jef.database.jsqlparser.expression.operators.relational.MinorThanEquals;
import jef.database.jsqlparser.expression.operators.relational.NotEqualsTo;
import jef.database.jsqlparser.schema.Column;
import jef.database.jsqlparser.statement.select.SelectVisitor;
import jef.database.jsqlparser.statement.select.StartWithExpression;
import jef.database.jsqlparser.statement.select.SubSelect;

/**
 * A class to de-parse (that is, tranform from JSqlParser hierarchy into a string)
 * an {@link jef.database.jsqlparser.expression.Expression}
 */
public class ExpressionDeParser implements ExpressionVisitor, ItemsListVisitor {

    protected StringBuffer buffer;

    protected SelectVisitor selectVisitor;

    protected boolean useBracketsInExprList = true;

    public ExpressionDeParser() {
    }

    /**
     * @param selectVisitor a SelectVisitor to de-parse SubSelects. It has to share the same<br>
     * StringBuffer as this object in order to work, as:
     * <pre>
     * <code>
     * StringBuffer myBuf = new StringBuffer();
     * MySelectDeparser selectDeparser = new  MySelectDeparser();
     * selectDeparser.setBuffer(myBuf);
     * ExpressionDeParser expressionDeParser = new ExpressionDeParser(selectDeparser, myBuf);
     * </code>
     * </pre>
     * @param buffer the buffer that will be filled with the expression
     */
    public ExpressionDeParser(SelectVisitor selectVisitor, StringBuffer buffer) {
        this.selectVisitor = selectVisitor;
        this.buffer = buffer;
    }

    public StringBuffer getBuffer() {
        return buffer;
    }

    public void setBuffer(StringBuffer buffer) {
        this.buffer = buffer;
    }

    public void visit(Addition addition) {
        visitBinaryExpression(addition, " + ");
    }

    public void visit(AndExpression andExpression) {
        visitBinaryExpression(andExpression, " AND ");
    }

    public void visit(Between between) {
        between.getLeftExpression().accept(this);
        if (between.isNot()) buffer.append(" NOT");
        buffer.append(" BETWEEN ");
        between.getBetweenExpressionStart().accept(this);
        buffer.append(" AND ");
        between.getBetweenExpressionEnd().accept(this);
    }

	public void visit(Mod mod) {
	    visitBinaryExpression(mod, " % ");
	}
	
    public void visit(Division division) {
        visitBinaryExpression(division, " / ");
    }

    public void visit(DoubleValue doubleValue) {
        buffer.append(doubleValue.getValue());
    }

    public void visit(EqualsTo equalsTo) {
        visitBinaryExpression(equalsTo, " = ");
    }

    public void visit(GreaterThan greaterThan) {
        visitBinaryExpression(greaterThan, " > ");
    }

    public void visit(GreaterThanEquals greaterThanEquals) {
        visitBinaryExpression(greaterThanEquals, " >= ");
    }

    public void visit(InExpression inExpression) {
        inExpression.getLeftExpression().accept(this);
        if (inExpression.isNot()) buffer.append(" NOT");
        buffer.append(" IN ");
        inExpression.getItemsList().accept(this);
    }

    public void visit(InverseExpression inverseExpression) {
        buffer.append("-");
        inverseExpression.getExpression().accept(this);
    }

    public void visit(IsNullExpression isNullExpression) {
        isNullExpression.getLeftExpression().accept(this);
        if (isNullExpression.isNot()) {
            buffer.append(" IS NOT NULL");
        } else {
            buffer.append(" IS NULL");
        }
    }
    
    public void visit(JdbcParameter jdbcParameter) {
        buffer.append("?");
    }
    
	public void visit(JpqlParameter jdbcParameter) {
		 buffer.append(jdbcParameter.toString());
	}

    public void visit(LikeExpression likeExpression) {
        visitBinaryExpression(likeExpression, " LIKE ");
    }

    public void visit(ExistsExpression existsExpression) {
        if (existsExpression.isNot()) {
            buffer.append(" NOT EXISTS ");
        } else {
            buffer.append(" EXISTS ");
        }
        existsExpression.getRightExpression().accept(this);
    }

    public void visit(LongValue longValue) {
        buffer.append(longValue.getStringValue());
    }

    public void visit(MinorThan minorThan) {
        visitBinaryExpression(minorThan, " < ");
    }

    public void visit(MinorThanEquals minorThanEquals) {
        visitBinaryExpression(minorThanEquals, " <= ");
    }

    public void visit(Multiplication multiplication) {
        visitBinaryExpression(multiplication, " * ");
    }

    public void visit(NotEqualsTo notEqualsTo) {
        visitBinaryExpression(notEqualsTo, " <> ");
    }

    public void visit(NullValue nullValue) {
        buffer.append("NULL");
    }

    public void visit(OrExpression orExpression) {
        visitBinaryExpression(orExpression, " OR ");
    }

    public void visit(Parenthesis parenthesis) {
        if (parenthesis.isNot()) buffer.append(" NOT ");
        buffer.append("(");
        parenthesis.getExpression().accept(this);
        buffer.append(")");
    }

    public void visit(StringValue stringValue) {
        buffer.append("'" + stringValue.getValue() + "'");
    }

    public void visit(Subtraction subtraction) {
        visitBinaryExpression(subtraction, "-");
    }

    private void visitBinaryExpression(BinaryExpression binaryExpression, String operator) {
        if (binaryExpression.isNot()) buffer.append(" NOT ");
        binaryExpression.getLeftExpression().accept(this);
        buffer.append(operator);
        binaryExpression.getRightExpression().accept(this);
    }

    public void visit(SubSelect subSelect) {
        buffer.append("(");
        subSelect.getSelectBody().accept(selectVisitor);
        buffer.append(")");
    }

    public void visit(Column tableColumn) {
        String tableName = tableColumn.getTableAlias();
        if (tableName != null) {
            buffer.append(tableName + ".");
        }
        buffer.append(tableColumn.getColumnName());
    }

    public void visit(Function function) {
        if (function.isEscaped()) {
            buffer.append("{fn ");
        }
        buffer.append(function.getName());
        if (function.isAllColumns()) {
            buffer.append("(*)");
        } else if (function.getParameters() == null) {
            buffer.append("()");
        } else {
            boolean oldUseBracketsInExprList = useBracketsInExprList;
            if (function.isDistinct()) {
                useBracketsInExprList = false;
                buffer.append("(DISTINCT ");
            }
            visit(function.getParameters());
            useBracketsInExprList = oldUseBracketsInExprList;
            if (function.isDistinct()) {
                buffer.append(")");
            }
        }
        if (function.isEscaped()) {
            buffer.append("}");
        }
    }

    public void visit(ExpressionList expressionList) {
        if (useBracketsInExprList) buffer.append("(");
        for (Iterator<Expression> iter = expressionList.getExpressions().iterator(); iter.hasNext(); ) {
            Expression expression = (Expression) iter.next();
            expression.accept(this);
            if (iter.hasNext()) buffer.append(",");
        }
        if (useBracketsInExprList) buffer.append(")");
    }

    public SelectVisitor getSelectVisitor() {
        return selectVisitor;
    }

    public void setSelectVisitor(SelectVisitor visitor) {
        selectVisitor = visitor;
    }

    public void visit(DateValue dateValue) {
        buffer.append("{d '" + dateValue.getValue().toString() + "'}");
    }

    public void visit(TimestampValue timestampValue) {
        buffer.append("{ts '" + timestampValue.getValue().toString() + "'}");
    }

    public void visit(TimeValue timeValue) {
        buffer.append("{t '" + timeValue.getValue().toString() + "'}");
    }

    public void visit(CaseExpression caseExpression) {
        buffer.append("CASE ");
        Expression switchExp = caseExpression.getSwitchExpression();
        if (switchExp != null) {
            switchExp.accept(this);
        }
        List<WhenClause> clauses = caseExpression.getWhenClauses();
        for (Iterator<WhenClause> iter = clauses.iterator(); iter.hasNext(); ) {
            Expression exp = (Expression) iter.next();
            exp.accept(this);
        }
        Expression elseExp = caseExpression.getElseExpression();
        if (elseExp != null) {
            elseExp.accept(this);
        }
        buffer.append(" END");
    }

    public void visit(WhenClause whenClause) {
        buffer.append(" WHEN ");
        whenClause.getWhenExpression().accept(this);
        buffer.append(" THEN ");
        whenClause.getThenExpression().accept(this);
    }

    public void visit(AllComparisonExpression allComparisonExpression) {
        buffer.append(" ALL ");
        allComparisonExpression.GetSubSelect().accept((ExpressionVisitor) this);
    }

    public void visit(AnyComparisonExpression anyComparisonExpression) {
        buffer.append(" ANY ");
        anyComparisonExpression.GetSubSelect().accept((ExpressionVisitor) this);
    }

    public void visit(Concat concat) {
        visitBinaryExpression(concat, " || ");
    }

    public void visit(Matches matches) {
        visitBinaryExpression(matches, " @@ ");
    }

    public void visit(BitwiseAnd bitwiseAnd) {
        visitBinaryExpression(bitwiseAnd, " & ");
    }

    public void visit(BitwiseOr bitwiseOr) {
        visitBinaryExpression(bitwiseOr, " | ");
    }

    public void visit(BitwiseXor bitwiseXor) {
        visitBinaryExpression(bitwiseXor, " ^ ");
    }

	public void visit(Interval interval) {
		interval.getValue().accept(this);
	}

	public void visit(StartWithExpression startWithExpression) {
		Expression start=startWithExpression.getStartExpression();
		Expression connectBy=startWithExpression.getConnectExpression();
		if(start!=null)start.accept(this);
		if(connectBy!=null)connectBy.accept(this);
	}

	public void visit(Over over) {
		if(over.getPartition()!=null){
			for(Expression exp: over.getPartition()){
				exp.accept(this);
			}
		}
		if(over.getOrderBy()!=null){
//			over.getOrderBy().accept(this);
		}
	}


}

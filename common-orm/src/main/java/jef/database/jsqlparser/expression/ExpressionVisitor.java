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
import jef.database.jsqlparser.expression.operators.relational.GreaterThan;
import jef.database.jsqlparser.expression.operators.relational.GreaterThanEquals;
import jef.database.jsqlparser.expression.operators.relational.InExpression;
import jef.database.jsqlparser.expression.operators.relational.IsNullExpression;
import jef.database.jsqlparser.expression.operators.relational.LikeExpression;
import jef.database.jsqlparser.expression.operators.relational.Matches;
import jef.database.jsqlparser.expression.operators.relational.MinorThan;
import jef.database.jsqlparser.expression.operators.relational.MinorThanEquals;
import jef.database.jsqlparser.expression.operators.relational.NotEqualsTo;
import jef.database.jsqlparser.schema.Column;
import jef.database.jsqlparser.statement.select.StartWithExpression;
import jef.database.jsqlparser.statement.select.SubSelect;

public interface ExpressionVisitor {

    public void visit(NullValue nullValue);

    public void visit(Function function);

    public void visit(InverseExpression inverseExpression);

    public void visit(JpqlParameter parameter);
    
    public void visit(JdbcParameter parameter);

    public void visit(DoubleValue doubleValue);

    public void visit(LongValue longValue);

    public void visit(DateValue dateValue);

    public void visit(TimeValue timeValue);

    public void visit(TimestampValue timestampValue);

    public void visit(Parenthesis parenthesis);

    public void visit(StringValue stringValue);

    public void visit(Addition addition);

    public void visit(Division division);

    public void visit(Multiplication multiplication);

    public void visit(Subtraction subtraction);

    public void visit(AndExpression andExpression);

    public void visit(OrExpression orExpression);

    public void visit(Between between);

    public void visit(EqualsTo equalsTo);

    public void visit(GreaterThan greaterThan);

    public void visit(GreaterThanEquals greaterThanEquals);

    public void visit(InExpression inExpression);

    public void visit(IsNullExpression isNullExpression);

    public void visit(LikeExpression likeExpression);

    public void visit(MinorThan minorThan);

    public void visit(MinorThanEquals minorThanEquals);

    public void visit(NotEqualsTo notEqualsTo);

    public void visit(Column tableColumn);

    public void visit(SubSelect subSelect);

    public void visit(CaseExpression caseExpression);

    public void visit(WhenClause whenClause);

    public void visit(ExistsExpression existsExpression);

    public void visit(AllComparisonExpression allComparisonExpression);

    public void visit(AnyComparisonExpression anyComparisonExpression);

    public void visit(Concat concat);

    public void visit(Matches matches);

    public void visit(BitwiseAnd bitwiseAnd);

    public void visit(BitwiseOr bitwiseOr);

    public void visit(BitwiseXor bitwiseXor);

	public void visit(Interval interval);

	public void visit(StartWithExpression startWithExpression);

	public void visit(Mod mod);
}

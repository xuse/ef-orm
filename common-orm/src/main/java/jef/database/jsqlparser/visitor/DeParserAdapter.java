package jef.database.jsqlparser.visitor;

import java.util.Iterator;
import java.util.List;

import jef.common.Pair;
import jef.database.jsqlparser.Util;
import jef.database.jsqlparser.expression.AllComparisonExpression;
import jef.database.jsqlparser.expression.AnyComparisonExpression;
import jef.database.jsqlparser.expression.BinaryExpression;
import jef.database.jsqlparser.expression.CaseExpression;
import jef.database.jsqlparser.expression.Column;
import jef.database.jsqlparser.expression.DateValue;
import jef.database.jsqlparser.expression.DoubleValue;
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
import jef.database.jsqlparser.expression.Table;
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
import jef.database.jsqlparser.expression.operators.relational.LikeExpression;
import jef.database.jsqlparser.expression.operators.relational.MinorThan;
import jef.database.jsqlparser.expression.operators.relational.MinorThanEquals;
import jef.database.jsqlparser.expression.operators.relational.NotEqualsTo;
import jef.database.jsqlparser.statement.create.ColumnDefinition;
import jef.database.jsqlparser.statement.create.CreateTable;
import jef.database.jsqlparser.statement.create.Index;
import jef.database.jsqlparser.statement.delete.Delete;
import jef.database.jsqlparser.statement.drop.Drop;
import jef.database.jsqlparser.statement.insert.Insert;
import jef.database.jsqlparser.statement.replace.Replace;
import jef.database.jsqlparser.statement.select.AllColumns;
import jef.database.jsqlparser.statement.select.AllTableColumns;
import jef.database.jsqlparser.statement.select.Join;
import jef.database.jsqlparser.statement.select.Limit;
import jef.database.jsqlparser.statement.select.OrderBy;
import jef.database.jsqlparser.statement.select.OrderByElement;
import jef.database.jsqlparser.statement.select.PlainSelect;
import jef.database.jsqlparser.statement.select.Select;
import jef.database.jsqlparser.statement.select.SelectExpressionItem;
import jef.database.jsqlparser.statement.select.StartWithExpression;
import jef.database.jsqlparser.statement.select.SubJoin;
import jef.database.jsqlparser.statement.select.SubSelect;
import jef.database.jsqlparser.statement.select.Top;
import jef.database.jsqlparser.statement.select.Union;
import jef.database.jsqlparser.statement.select.WithItem;
import jef.database.jsqlparser.statement.truncate.Truncate;
import jef.database.jsqlparser.statement.update.Update;
import jef.tools.StringUtils;

/**
 * 将AST重新组装成SQL的访问者，可通过修改访问者的行为来变化SQL的组装行为。
 * @author jiyi
 *
 */
public class DeParserAdapter implements SelectVisitor, ExpressionVisitor, StatementVisitor, SelectItemVisitor {
	protected StringBuilder sb;

	public DeParserAdapter(StringBuilder buffer) {
		this.sb = buffer;
	}

	public DeParserAdapter() {
		this.sb = new StringBuilder();
	}

	public StringBuilder getBuffer() {
		return sb;
	}

	public void visit(Addition addition) {
		visitBinaryExpression(addition, " + ");
	}

	public void visit(AndExpression andExpression) {
		visitBinaryExpression(andExpression, " AND ");
	}

	public void visit(Between between) {
		between.getLeftExpression().accept(this);
		if (between.isNot())
			sb.append(" NOT");
		sb.append(" BETWEEN ");
		between.getBetweenExpressionStart().accept(this);
		sb.append(" AND ");
		between.getBetweenExpressionEnd().accept(this);
	}

	public void visit(Mod mod) {
		visitBinaryExpression(mod, " % ");
	}

	public void visit(Division division) {
		visitBinaryExpression(division, " / ");
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
		if (inExpression.isNot())
			sb.append(" NOT");
		sb.append(" IN ");
		inExpression.getItemsList().accept(this);
	}

	public void visit(InverseExpression inverseExpression) {
		sb.append("-");
		inverseExpression.getExpression().accept(this);
	}

	public void visit(IsNullExpression isNullExpression) {
		isNullExpression.getLeftExpression().accept(this);
		if (isNullExpression.isNot()) {
			sb.append(" IS NOT NULL");
		} else {
			sb.append(" IS NULL");
		}
	}

	public void visit(JdbcParameter jdbcParameter) {
		sb.append("?");
	}

	public void visit(JpqlParameter jdbcParameter) {
		Object obj = jdbcParameter.getResolved();
		if (obj instanceof String) {
			sb.append(obj);
			return;
		}
		Integer value = (Integer) obj;
		if (value == null || value < 0) {// 未解析
			if (jdbcParameter.isNamedParam()) {
				sb.append(':').append(jdbcParameter.getName());
			} else {
				sb.append('?').append(jdbcParameter.getIndex());
			}
		} else if (value == 0) {
			sb.append('?');
		} else {// value>0
			sb.append('?');
			StringUtils.repeat(sb, ",?", value - 1);
		}
		if (jdbcParameter.getAlias() != null) {
			sb.append(' ').append(jdbcParameter.getAlias());
		}
	}

	public void visit(DoubleValue doubleValue) {
		sb.append(doubleValue.getValue());
	}

	public void visit(LikeExpression likeExpression) {
		visitBinaryExpression(likeExpression, " LIKE ");
	}

	public void visit(ExistsExpression existsExpression) {
		if (existsExpression.isNot()) {
			sb.append(" NOT EXISTS ");
		} else {
			sb.append(" EXISTS ");
		}
		existsExpression.getRightExpression().accept(this);
	}

	public void visit(LongValue longValue) {
		sb.append(longValue.getStringValue());
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
		sb.append("NULL");
	}

	public void visit(OrExpression orExpression) {
		visitBinaryExpression(orExpression, " OR ");
	}

	public void visit(Parenthesis parenthesis) {
		if (parenthesis.isNot())
			sb.append(" NOT ");
		sb.append("(");
		parenthesis.getExpression().accept(this);
		sb.append(")");
	}

	public void visit(StringValue stringValue) {
		sb.append("'" + stringValue.getValue() + "'");
	}

	public void visit(Subtraction subtraction) {
		visitBinaryExpression(subtraction, "-");
	}

	private void visitBinaryExpression(BinaryExpression binaryExpression, String operator) {
		if (binaryExpression.isNot())
			sb.append(" NOT ");
		binaryExpression.getLeftExpression().accept(this);
		sb.append(operator);
		binaryExpression.getRightExpression().accept(this);
	}

	public void visit(SubSelect subSelect) {
		sb.append("(");
		subSelect.getSelectBody().accept(this);
		sb.append(")");
	}

	public void visit(Function function) {
		if (function.isEscaped()) {
			sb.append("{fn ");
		}
		sb.append(function.getName());
		if (function.isAllColumns()) {
			sb.append("(*)");
		} else if (function.getParameters() == null) {
			sb.append("()");
		} else {
			if (function.isDistinct()) {
				sb.append("(DISTINCT ");
			} else {
				sb.append('(');
			}
			String between = function.getParameters().getBetween();
			Iterator<Expression> iter = function.getParameters().getExpressions().iterator();
			if (iter.hasNext()) {
				iter.next().accept(this);
				while (iter.hasNext()) {
					sb.append(between);
					iter.next().accept(this);
				}
			}
			sb.append(')');
		}
		if (function.isEscaped()) {
			sb.append("}");
		}
	}

	public void visit(ExpressionList expressionList) {
		sb.append("(");
		for (Iterator<Expression> iter = expressionList.getExpressions().iterator(); iter.hasNext();) {
			Expression expression = (Expression) iter.next();
			expression.accept(this);
			if (iter.hasNext())
				sb.append(expressionList.getBetween());
		}
		sb.append(")");
	}

	public void visit(DateValue dateValue) {
		sb.append("{d '" + dateValue.getValue().toString() + "'}");
	}

	public void visit(TimestampValue timestampValue) {
		sb.append("{ts '" + timestampValue.getValue().toString() + "'}");
	}

	public void visit(TimeValue timeValue) {
		sb.append("{t '" + timeValue.getValue().toString() + "'}");
	}

	public void visit(CaseExpression caseExpression) {
		sb.append("CASE ");
		Expression switchExp = caseExpression.getSwitchExpression();
		if (switchExp != null) {
			switchExp.accept(this);
		}
		List<WhenClause> clauses = caseExpression.getWhenClauses();
		for (Iterator<WhenClause> iter = clauses.iterator(); iter.hasNext();) {
			Expression exp = (Expression) iter.next();
			exp.accept(this);
		}
		Expression elseExp = caseExpression.getElseExpression();
		if (elseExp != null) {
			elseExp.accept(this);
		}
		sb.append(" END");
	}

	public void visit(WhenClause whenClause) {
		sb.append(" WHEN ");
		whenClause.getWhenExpression().accept(this);
		sb.append(" THEN ");
		whenClause.getThenExpression().accept(this);
	}

	public void visit(AllComparisonExpression allComparisonExpression) {
		sb.append(" ALL ");
		allComparisonExpression.GetSubSelect().accept((ExpressionVisitor) this);
	}

	public void visit(AnyComparisonExpression anyComparisonExpression) {
		sb.append(" ANY ");
		anyComparisonExpression.GetSubSelect().accept((ExpressionVisitor) this);
	}

	public void visit(Concat concat) {
		visitBinaryExpression(concat, " || ");
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
		Expression start = startWithExpression.getStartExpression();
		Expression connectBy = startWithExpression.getConnectExpression();
		if (start != null)
			start.accept(this);
		if (connectBy != null)
			connectBy.accept(this);
	}

	public void visit(Over over) {
		sb.append(" over(");
		if (over.getPartition() != null && !over.getPartition().isEmpty()) {
			sb.append("partition by");
			for (Expression exp : over.getPartition()) {
				exp.accept(this);
			}
		}
		if (over.getOrderBy() != null) {
			over.getOrderBy().accept(this);
		}
		sb.append(')');
	}

	public void visit(PlainSelect plainSelect) {
		sb.append("select ");
		writeSelectItems(plainSelect);
		writeFromAndWhere(plainSelect);
		writeGroupByAndHaving(plainSelect);
		writeOrderAndLimit(plainSelect);
	}

	protected void writeFromAndWhere(PlainSelect plainSelect) {
		sb.append(" ");
		if (plainSelect.getFromItem() != null) {
			sb.append("from ");
			plainSelect.getFromItem().accept(this);
		}
		if (plainSelect.getJoins() != null) {
			for (Iterator<Join> iter = plainSelect.getJoins().iterator(); iter.hasNext();) {
				Join join = iter.next();
				join.accept(this);
			}
		}
		if (plainSelect.getWhere() != null) {
			sb.append(" where ");
			plainSelect.getWhere().accept(this);
		}
	}

	protected void writeOrderAndLimit(PlainSelect plainSelect) {
		if (plainSelect.getOrderBy() != null) {
			plainSelect.getOrderBy().accept(this);
		}
		if (plainSelect.getLimit() != null) {
			plainSelect.getLimit().accept(this);
		}
	}
	
	

	protected void writeGroupByAndHaving(PlainSelect plainSelect) {
		if (plainSelect.getGroupByColumnReferences() != null) {
			sb.append(" group by ");
			for (Iterator<Expression> iter = plainSelect.getGroupByColumnReferences().iterator(); iter.hasNext();) {
				Expression columnReference = iter.next();
				columnReference.accept(this);
				if (iter.hasNext()) {
					sb.append(",");
				}
			}
		}
		if (plainSelect.getHaving() != null) {
			sb.append(" having ");
			plainSelect.getHaving().accept(this);
		}
	}

	protected void writeSelectItems(PlainSelect plainSelect) {
		Top top = plainSelect.getTop();
		if (top != null)
			top.toString();
		if (plainSelect.getDistinct() != null) {
			sb.append("DISTINCT ");
			if (plainSelect.getDistinct().getOnSelectItems() != null) {
				sb.append("ON (");
				for (Iterator<SelectItem> iter = plainSelect.getDistinct().getOnSelectItems().iterator(); iter.hasNext();) {
					SelectItem selectItem = iter.next();
					selectItem.accept(this);
					if (iter.hasNext()) {
						sb.append(", ");
					}
				}
				sb.append(") ");
			}
		}
		for (Iterator<SelectItem> iter = plainSelect.getSelectItems().iterator(); iter.hasNext();) {
			SelectItem selectItem = iter.next();
			selectItem.accept(this);
			if (iter.hasNext()) {
				sb.append(",");
			}
		}
	}

	public void visit(Union union) {
		for (Iterator<PlainSelect> iter = union.getPlainSelects().iterator(); iter.hasNext();) {
			sb.append("(");
			PlainSelect plainSelect = iter.next();
			plainSelect.accept(this);
			sb.append(")");
			if (iter.hasNext()) {
				sb.append(" UNION ");
			}
		}
		if (union.getOrderBy() != null) {
			union.getOrderBy().accept(this);
		}
		if (union.getLimit() != null) {
			union.getLimit().accept(this);
		}
	}

	public void visit(OrderByElement orderBy) {
		orderBy.getExpression().accept(this);
		if (orderBy.isAsc())
			sb.append(" ASC");
		else
			sb.append(" DESC");
	}

	public void visit(Column column) {
		sb.append(column.getWholeColumnName());
	}

	public void visit(AllColumns allColumns) {
		sb.append("*");
	}

	public void visit(AllTableColumns allTableColumns) {
		sb.append(allTableColumns.getTable().toWholeName() + ".*");
	}

	public void visit(SelectExpressionItem selectExpressionItem) {
		selectExpressionItem.getExpression().accept(this);
		if (selectExpressionItem.getAlias() != null) {
			sb.append(" AS " + selectExpressionItem.getAlias());
		}
	}

	public void visit(Table tableName) {
		sb.append(tableName.toWholeName());
		String alias = tableName.getAlias();
		if (alias != null && alias.length() > 0) {
			sb.append(" " + alias);
		}
	}

	public void visit(SubJoin subjoin) {
		sb.append('(');
		subjoin.getLeft().accept(this);
		sb.append(' ');
		subjoin.getJoin().accept(this);
		sb.append(')');
	}

	public void visit(OrderBy orderBy) {
		sb.append(" order by ");
		for (int i = 0; i < orderBy.getOrderByElements().size(); i++) {
			if (i > 0)
				sb.append(',');
			OrderByElement ele = orderBy.getOrderByElements().get(i);
			ele.accept(this);

		}
	}

	public void visit(Join join) {
		if (join.isSimple())
			sb.append(", ");
		else {
			if (join.isRight())
				sb.append("RIGHT ");
			else if (join.isNatural())
				sb.append("NATURAL ");
			else if (join.isFull())
				sb.append("FULL ");
			else if (join.isLeft())
				sb.append("LEFT ");
			if (join.isOuter())
				sb.append("OUTER ");
			else if (join.isInner())
				sb.append("INNER ");
			sb.append("JOIN ");
		}
		FromItem fromItem = join.getRightItem();
		fromItem.accept(this);
		if (join.getOnExpression() != null) {
			sb.append(" ON ");
			join.getOnExpression().accept(this);
		}
		if (join.getUsingColumns() != null) {
			sb.append(" USING ( ");
			for (Iterator<Column> iterator = join.getUsingColumns().iterator(); iterator.hasNext();) {
				Column column = (Column) iterator.next();
				sb.append(column.getWholeColumnName());
				if (iterator.hasNext()) {
					sb.append(" ,");
				}
			}
			sb.append(")");
		}
	}

	public void visit(CreateTable createTable) {
		sb.append("CREATE TABLE " + createTable.getTable().toWholeName());
		if (createTable.getColumnDefinitions() != null) {
			sb.append(" { ");
			for (Iterator<ColumnDefinition> iter = createTable.getColumnDefinitions().iterator(); iter.hasNext();) {
				ColumnDefinition columnDefinition = (ColumnDefinition) iter.next();
				sb.append(columnDefinition.getColumnName());
				sb.append(" ");
				sb.append(columnDefinition.getColDataType().getDataType());
				if (columnDefinition.getColDataType().getArgumentsStringList() != null) {
					for (Iterator<String> iterator = columnDefinition.getColDataType().getArgumentsStringList().iterator(); iterator.hasNext();) {
						sb.append(" ");
						sb.append((String) iterator.next());
					}
				}
				if (columnDefinition.getColumnSpecStrings() != null) {
					for (Iterator<String> iterator = columnDefinition.getColumnSpecStrings().iterator(); iterator.hasNext();) {
						sb.append(" ");
						sb.append((String) iterator.next());
					}
				}
				if (iter.hasNext())
					sb.append(",\n");
			}
			for (Iterator<Index> iter = createTable.getIndexes().iterator(); iter.hasNext();) {
				sb.append(",\n");
				Index index = (Index) iter.next();
				sb.append(index.getType() + " " + index.getName());
				sb.append("(");
				for (Iterator<String> iterator = index.getColumnsNames().iterator(); iterator.hasNext();) {
					sb.append((String) iterator.next());
					if (iterator.hasNext()) {
						sb.append(", ");
					}
				}
				sb.append(")");
				if (iter.hasNext())
					sb.append(",\n");
			}
			sb.append(" \n} ");
		}
	}

	public void visit(Delete delete) {
		sb.append("DELETE FROM " + delete.getTable().toWholeName());
		if (delete.getWhere() != null) {
			sb.append(" WHERE ");
			delete.getWhere().accept(this);
		}
	}

	public void visit(Drop drop) {
	}

	public void visit(Insert insert) {
		sb.append("INSERT INTO ");
		sb.append(insert.getTable().toWholeName());
		if (insert.getColumns() != null) {
			sb.append("(");
			for (Iterator<Column> iter = insert.getColumns().iterator(); iter.hasNext();) {
				Column column = (Column) iter.next();
				sb.append(column.getColumnName());
				if (iter.hasNext()) {
					sb.append(", ");
				}
			}
			sb.append(")");
		}
		if (insert.getItemsList() instanceof ExpressionList) {
			ExpressionList exps = (ExpressionList) insert.getItemsList();
			sb.append(" VALUES (");
			for (Iterator<Expression> iter = exps.getExpressions().iterator(); iter.hasNext();) {
				Expression expression = (Expression) iter.next();
				expression.accept(this);
				if (iter.hasNext())
					sb.append(", ");
			}
			sb.append(")");
		} else {
			insert.getItemsList().accept(this);
		}
	}

	public void visit(Replace replace) {
		sb.append("REPLACE " + replace.getTable().toWholeName());
		if (replace.getItemsList() != null) {
			if (replace.getColumns() != null) {
				sb.append(" (");
				for (int i = 0; i < replace.getColumns().size(); i++) {
					Column column = (Column) replace.getColumns().get(i);
					sb.append(column.getWholeColumnName());
					if (i < replace.getColumns().size() - 1) {
						sb.append(", ");
					}
				}
				sb.append(") ");
			} else {
				sb.append(" ");
			}
		} else {
			sb.append(" SET ");
			for (int i = 0; i < replace.getColumns().size(); i++) {
				Column column = (Column) replace.getColumns().get(i);
				sb.append(column.getWholeColumnName() + "=");
				Expression expression = (Expression) replace.getExpressions().get(i);
				expression.accept(this);
				if (i < replace.getColumns().size() - 1) {
					sb.append(", ");
				}
			}
		}
		if (replace.getItemsList() instanceof ExpressionList) {
			ExpressionList exps = (ExpressionList) replace.getItemsList();
			sb.append(" VALUES (");
			for (Iterator<Expression> iter = exps.getExpressions().iterator(); iter.hasNext();) {
				Expression expression = (Expression) iter.next();
				expression.accept(this);
				if (iter.hasNext())
					sb.append(", ");
			}
			sb.append(")");
		} else {
			replace.getItemsList().accept(this);
		}
	}

	public void visit(Select select) {
		if (select.getWithItemsList() != null && !select.getWithItemsList().isEmpty()) {
			sb.append("WITH ");
			for (Iterator<WithItem> iter = select.getWithItemsList().iterator(); iter.hasNext();) {
				WithItem withItem = iter.next();
				withItem.accept(this);
				if (iter.hasNext())
					sb.append(",");
				sb.append(" ");
			}
		}
		select.getSelectBody().accept(this);
	}

	public void visit(Truncate truncate) {
	}

	public void visit(Update update) {
		sb.append("UPDATE " + update.getTable().toWholeName() + " SET ");
		int size = update.getSets().size();
		for (int i = 0; i < size; i++) {
			Pair<Column, Expression> pair = update.getSets().get(i);
			Column column = pair.first;
			sb.append(column.getWholeColumnName() + "=");
			Expression expression = pair.second;
			expression.accept(this);
			if (i < size - 1) {
				sb.append(", ");
			}
		}
		if (update.getWhere() != null) {
			sb.append(" WHERE ");
			update.getWhere().accept(this);
		}
	}

	public void visit(WithItem with) {
		sb.append(with.getName());
    	if(with.getWithItemList() != null){
    		sb.append(' ');
    		Util.getStringList(sb,with.getWithItemList(), ",", true); 
    	}
    	sb.append(" AS (");
    	with.getSelectBody().accept(this);
    	sb.append(')');
	}

	@Override
	public String toString() {
		return sb.toString();
	}

	@Override
	public void visit(Limit limit) {
		sb.append(" LIMIT ");
		if (limit.isRowCountJdbcParameter()) {
			sb.append("?");
		} else if (limit.getRowCount() != 0) {
			sb.append(limit.getRowCount());
		} else {
			sb.append("18446744073709551615");
		}
		if (limit.getOffsetJdbcParameter() != null) {
			sb.append(" OFFSET ?");
		} else if (limit.getOffset() != 0) {
			sb.append(" OFFSET " + limit.getOffset());
		}
	}
}

package jef.database.routing.sql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jef.common.wrapper.IntRange;
import jef.database.Condition.Operator;
import jef.database.jsqlparser.RemovedDelayProcess;
import jef.database.jsqlparser.expression.BinaryExpression;
import jef.database.jsqlparser.expression.BinaryExpression.Prior;
import jef.database.jsqlparser.expression.Column;
import jef.database.jsqlparser.expression.JpqlParameter;
import jef.database.jsqlparser.expression.Parenthesis;
import jef.database.jsqlparser.expression.Table;
import jef.database.jsqlparser.expression.operators.relational.Between;
import jef.database.jsqlparser.expression.operators.relational.EqualsTo;
import jef.database.jsqlparser.expression.operators.relational.ExpressionList;
import jef.database.jsqlparser.expression.operators.relational.InExpression;
import jef.database.jsqlparser.statement.select.Limit;
import jef.database.jsqlparser.statement.select.PlainSelect;
import jef.database.jsqlparser.statement.select.Select;
import jef.database.jsqlparser.statement.select.StartWithExpression;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.ExpressionType;
import jef.database.jsqlparser.visitor.SqlValue;
import jef.database.jsqlparser.visitor.Statement;
import jef.database.jsqlparser.visitor.VisitorAdapter;
import jef.database.query.ParameterProvider;
import jef.database.wrapper.clause.InMemoryPaging;
import jef.database.wrapper.clause.InMemoryStartWithConnectBy;
import jef.database.wrapper.populator.ColumnDescription;
import jef.database.wrapper.populator.ColumnMeta;
import jef.database.wrapper.result.MultipleResultSet;
import jef.tools.StringUtils;

import com.alibaba.druid.proxy.jdbc.JdbcParameter;

public class SqlAndParameter implements InMemoryOperateProvider {
	public Statement statement;
	public List<Object> params;
	private ParameterProvider rawParams;
	private Map<Expression, Object> paramsMap;
	// 后处理
	private StartWithExpression startWith;
	private Limit limit;

	/**
	 * @param st
	 *            SQL Statement
	 * @param params
	 *            参数
	 * @param rawParams
	 *            参数
	 */
	public SqlAndParameter(Statement st, List<Object> params, ParameterProvider rawParams) {
		this.statement = st;
		this.params = params;
		this.rawParams = rawParams;
		paramsMap = SqlAnalyzer.reverse(st, params); // 参数对应关系还原
	}

	public Map<Expression, Object> getParamsMap() {
		return paramsMap;
	}

	private InMemoryStartWithConnectBy parseStartWith(ColumnMeta columns) {
		if (statement instanceof Select) {
			return parse((Select) statement, columns);
		}
		throw new UnsupportedOperationException();
	}

	private InMemoryStartWithConnectBy parse(Select st, ColumnMeta columns) {
		if (st.getSelectBody() instanceof PlainSelect) {
			return parse((PlainSelect) st.getSelectBody(), columns);

		}
		throw new UnsupportedOperationException();
	}

	private InMemoryStartWithConnectBy parse(PlainSelect selectBody, ColumnMeta columns) {
		if (startWith == null) {
			return null;
		}
		// 1 收集别名和表名的关系
		Map<String, String> maps = new HashMap<String, String>();
		selectBody.accept(new TableCollector(maps)); // 收集为大写别名 和 大写表名
		// 2解析
		InMemoryStartWithConnectBy result = new InMemoryStartWithConnectBy();

		parseStartWith(startWith.getStartExpression(), result, maps, columns);
		parseConnectBy(getAsEqualsTo(startWith.getConnectExpression()), result, maps, columns);
		return result;
	}

	private EqualsTo getAsEqualsTo(Expression ex) {
		ExpressionType type = ex.getType();
		if (type == ExpressionType.parenthesis) {
			return getAsEqualsTo(((Parenthesis) ex).getExpression());
		} else if (type == ExpressionType.eq) {
			return (EqualsTo) ex;
		}
		throw new UnsupportedOperationException(ex.toString());
	}

	private void parseStartWith(Expression startExpression, InMemoryStartWithConnectBy result, Map<String, String> maps, ColumnMeta columns) {
		int leftColumn;
		Operator op;
		Object value;
		switch (startExpression.getType()) {
		case eq:
			op = Operator.EQUALS;
			leftColumn = getColumnId(((BinaryExpression) startExpression).getLeftExpression(), columns, maps);
			value = getAsValue(((BinaryExpression) startExpression).getRightExpression());
			break;
		case ge:
			op = Operator.GREAT_EQUALS;
			leftColumn = getColumnId(((BinaryExpression) startExpression).getLeftExpression(), columns, maps);
			value = getAsValue(((BinaryExpression) startExpression).getRightExpression());
			break;
		case gt:
			op = Operator.GREAT;
			leftColumn = getColumnId(((BinaryExpression) startExpression).getLeftExpression(), columns, maps);
			value = getAsValue(((BinaryExpression) startExpression).getRightExpression());
			break;
		case in:
			op = Operator.IN;
			InExpression in = (InExpression) startExpression;
			leftColumn = getColumnId(in.getLeftExpression(), columns, maps);
			if (in.getItemsList() instanceof ExpressionList) {
				List<Object> values = new ArrayList<Object>();
				for (Expression ex : ((ExpressionList) in.getItemsList()).getExpressions()) {
					Object v = getAsValue(ex);
					if (v instanceof Object[]) {
						values.addAll(Arrays.asList((Object[]) v));
					} else {
						values.add(v);
					}
				}
				value = values;
			} else {
				throw new UnsupportedOperationException(in.getItemsList().toString());
			}
			break;
		case lt:
			op = Operator.LESS;
			leftColumn = getColumnId(((BinaryExpression) startExpression).getLeftExpression(), columns, maps);
			value = getAsValue(((BinaryExpression) startExpression).getRightExpression());
			break;
		case le:
			op = Operator.LESS_EQUALS;
			leftColumn = getColumnId(((BinaryExpression) startExpression).getLeftExpression(), columns, maps);
			value = getAsValue(((BinaryExpression) startExpression).getRightExpression());
			break;
		case like:
			op = Operator.MATCH_ANY;
			leftColumn = getColumnId(((BinaryExpression) startExpression).getLeftExpression(), columns, maps);
			value = getAsValue(((BinaryExpression) startExpression).getRightExpression());
			break;
		case ne:
			op = Operator.NOT_EQUALS;
			leftColumn = getColumnId(((BinaryExpression) startExpression).getLeftExpression(), columns, maps);
			value = getAsValue(((BinaryExpression) startExpression).getRightExpression());
			break;
		case between:
			op = Operator.BETWEEN_L_L;
			Between be = (Between) startExpression;
			leftColumn = getColumnId(be.getLeftExpression(), columns, maps);
			value = Arrays.asList(getAsValue(be.getBetweenExpressionStart()), getAsValue(be.getBetweenExpressionEnd()));
			break;
		default:
			throw new UnsupportedOperationException();
		}
		result.startWithColumn = leftColumn;
		result.startWithOperator = op;
		result.startWithValue = value;
	}

	private int getColumnId(Expression leftExpression, ColumnMeta columns, Map<String, String> maps) {
		if (leftExpression instanceof Column) {
			return getColumn(columns, (Column) leftExpression, maps);
		}
		throw new UnsupportedOperationException(leftExpression.toString());
	}

	private Object getAsValue(Expression exp) {
		if (exp.getType() == ExpressionType.value) {
			SqlValue value = (SqlValue) exp;
			return value.getValue();
		} else if (exp instanceof JpqlParameter) {
			JpqlParameter jp = (JpqlParameter) exp;
			Object value;
			if (jp.getName() == null) {
				value = rawParams.getIndexedParam(jp.getIndex());
			} else {
				value = rawParams.getNamedParam(jp.getName());
			}
			return value;
		} else if (exp instanceof JdbcParameter) {
			throw new UnsupportedOperationException();
		}
		throw new UnsupportedOperationException(exp.toString());
	}

	private void parseConnectBy(EqualsTo equals, InMemoryStartWithConnectBy result, Map<String, String> maps, ColumnMeta columns) {
		Expression parent;
		Expression current;
		if (equals.getPrior() == Prior.LEFT) {
			parent = equals.getRightExpression();
			current = equals.getLeftExpression();
		} else if (equals.getPrior() == Prior.RIGHT) {
			parent = (Column) equals.getLeftExpression();
			current = (Column) equals.getRightExpression();
		} else {
			throw new UnsupportedOperationException("NO PRIOR Found");
		}
		if (parent instanceof Column && current instanceof Column) {
			Column c1 = (Column) current;
			Column c2 = (Column) parent;
			int n1 = getColumn(columns, c1, maps);
			int n2 = getColumn(columns, c2, maps);
			if (n1 > 0 && n2 > 0) {
				result.connectPrior = n1;
				result.connectParent = n2;
				return;
			}
			throw new UnsupportedOperationException("The connect condition parser Error:" + c1 + "=" + c2);
		}
		throw new UnsupportedOperationException("Prior conditions must be Column");

	}

	private int getColumn(ColumnMeta columns, Column c1, Map<String, String> maps) {
		String table1 = null;
		if (StringUtils.isNotEmpty(c1.getTableAlias())) {
			table1 = maps.get(StringUtils.upperCase(c1.getTableAlias()));
		}
		int n1 = 0;
		for (ColumnDescription cd : columns.getColumns()) {
			if (match(table1, c1.getColumnName(), cd)) {
				n1 = cd.getN();
			}
		}
		return n1;
	}

	private boolean match(String table1, String columnName, ColumnDescription cd) {
		if (table1 != null) {
			if (!StringUtils.equalsIgnoreCase(table1, cd.getTable())) {
				return false;
			}
		}
		return StringUtils.equalsIgnoreCase(columnName, cd.getName());
	}

	static class TableCollector extends VisitorAdapter {
		Map<String, String> tableAlias;

		TableCollector(Map<String, String> map) {
			tableAlias = map;
		}

		@Override
		public void visit(Table table) {
			if (StringUtils.isNotEmpty(table.getAlias())) {
				tableAlias.put(StringUtils.upperCase(table.getAlias()), StringUtils.upperCase(table.getName()));
			}
		}
	}

	private int[] getLimitLength(Limit limit) {
		int offset = 0;
		int rowcount = 0;
		if (limit.getOffsetJdbcParameter() != null) {
			Object obj=getParamsMap().get(limit.getOffsetJdbcParameter());
			if(obj instanceof Number){
				offset = ((Number) obj).intValue();
			}
		} else {
			offset = (int)limit.getOffset();
		}
		if (limit.getRowCountJdbcParameter() != null) {
			Object obj=getParamsMap().get(limit.getRowCountJdbcParameter());
			if(obj instanceof Number){
				rowcount = ((Number) obj).intValue();						
			}

		} else {
			rowcount = (int)limit.getRowCount();
		}
		return new int[]{offset,rowcount};
	}
	
	
	public InMemoryPaging parseLimit(Limit limit,ColumnMeta columns) {
		int[] value=getLimitLength(limit);
		int offset=value[0];
		int rowcount=value[1];
		if(offset>0 || rowcount>0){
			return new InMemoryPaging(offset, offset+rowcount);
		}else{
			return null;
		}
	}


	public long getLimitSpan() {
		if(limit!=null){
			int[] values=getLimitLength(limit);
			return values[1];
		}
		return 0;
	}
	
	@Override
	public boolean hasInMemoryOperate() {
		return startWith!=null || limit!=null;
	}

	@Override
	public void parepareInMemoryProcess(IntRange range, MultipleResultSet mrs) {
		if(startWith!=null){
			mrs.setInMemoryConnectBy(parseStartWith(mrs.getColumns()));
		}
		if(limit!=null){
			mrs.setInMemoryPage(parseLimit(limit,mrs.getColumns()));
		}
	}

	/**
	 * 设置可能需要内存计算的任务
	 * 
	 * @param delays
	 */
	public void setInMemoryClause(RemovedDelayProcess delays) {
		if(delays!=null){
			this.startWith = delays.startWith;
			this.limit=delays.limit;
		}
	}

	public Limit getLimit() {
		return limit;
	}

	public void setNewLimit(IntRange range) {
		if(range==null){
			limit=null;
		}else{
			int[] values=range.toStartLimitSpan();
			Limit limit=new Limit();
			limit.setOffset(values[0]);
			limit.setRowCount(values[1]);
			this.limit=limit;
		}
	}

	public void setLimit(Limit countLimit) {
		this.limit=countLimit;
	}
	
	
	private boolean reverseResultSet;

	@Override
	public boolean isReverseResult() {
		return reverseResultSet;
	}
	
	public void setReverseResultSet(boolean reverseResultSet) {
		this.reverseResultSet = reverseResultSet;
	}
}

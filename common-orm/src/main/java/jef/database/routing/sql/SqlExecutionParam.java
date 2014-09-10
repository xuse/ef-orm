package jef.database.routing.sql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jef.database.Condition.Operator;
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
import jef.database.jsqlparser.statement.select.PlainSelect;
import jef.database.jsqlparser.statement.select.Select;
import jef.database.jsqlparser.statement.select.StartWithExpression;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.ExpressionType;
import jef.database.jsqlparser.visitor.SqlValue;
import jef.database.jsqlparser.visitor.Statement;
import jef.database.jsqlparser.visitor.VisitorAdapter;
import jef.database.query.ParameterProvider;
import jef.database.wrapper.clause.InMemoryStartWithConnectBy;
import jef.database.wrapper.populator.ColumnDescription;
import jef.database.wrapper.populator.ColumnMeta;
import jef.tools.StringUtils;

public class SqlExecutionParam {
	public Statement statement;
	public List<Object> params;
	public StartWithExpression removedStartWith;
	private ParameterProvider rawParams;

	
	public SqlExecutionParam(Statement st2, List<Object> params2,ParameterProvider rawParams) {
		this.statement = st2;
		this.params = params2;
		this.rawParams=rawParams;
	}

	
	public InMemoryStartWithConnectBy parseStartWith(ColumnMeta columns) {
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
		// 1 收集别名和表名的关系
		Map<String, String> maps = new HashMap<String, String>();
		selectBody.accept(new TableCollector(maps)); // 收集为大写别名 和 大写表名
		// 2解析
		InMemoryStartWithConnectBy result = new InMemoryStartWithConnectBy();

		parseStartWith(removedStartWith.getStartExpression(), result, maps,columns);
		parseConnectBy(getAsEqualsTo(removedStartWith.getConnectExpression()), result, maps, columns);
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

	private void parseStartWith(Expression startExpression, InMemoryStartWithConnectBy result, Map<String, String> maps,ColumnMeta columns) {
		int leftColumn;
		Operator op;
		Object value;
		switch (startExpression.getType()) {
		case eq:
			op = Operator.EQUALS;
			leftColumn=getColumnId(((BinaryExpression) startExpression).getLeftExpression(),columns,maps);
			value = getAsValue(((BinaryExpression) startExpression).getRightExpression());
			break;
		case ge:
			op = Operator.GREAT_EQUALS;
			leftColumn=getColumnId(((BinaryExpression) startExpression).getLeftExpression(),columns,maps);
			value = getAsValue(((BinaryExpression) startExpression).getRightExpression());
			break;
		case gt:
			op = Operator.GREAT;
			leftColumn=getColumnId(((BinaryExpression) startExpression).getLeftExpression(),columns,maps);
			value = getAsValue(((BinaryExpression) startExpression).getRightExpression());
			break;
		case in:
			op = Operator.IN;
			InExpression in = (InExpression) startExpression;
			leftColumn=getColumnId(in.getLeftExpression(),columns,maps);
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
				value=values;
			} else {
				throw new UnsupportedOperationException(in.getItemsList().toString());
			}
			break;
		case lt:
			op = Operator.LESS;
			leftColumn=getColumnId(((BinaryExpression) startExpression).getLeftExpression(),columns,maps);
			value = getAsValue(((BinaryExpression) startExpression).getRightExpression());
			break;
		case le:
			op = Operator.LESS_EQUALS;
			leftColumn=getColumnId(((BinaryExpression) startExpression).getLeftExpression(),columns,maps);
			value = getAsValue(((BinaryExpression) startExpression).getRightExpression());
			break;
		case like:
			op = Operator.MATCH_ANY;
			leftColumn=getColumnId(((BinaryExpression) startExpression).getLeftExpression(),columns,maps);
			value = getAsValue(((BinaryExpression) startExpression).getRightExpression());
			break;
		case ne:
			op = Operator.NOT_EQUALS;
			leftColumn=getColumnId(((BinaryExpression) startExpression).getLeftExpression(),columns,maps);
			value = getAsValue(((BinaryExpression) startExpression).getRightExpression());
			break;
		case between:
			op = Operator.BETWEEN_L_L;
			Between be = (Between) startExpression;
			leftColumn=getColumnId(be.getLeftExpression(),columns,maps);
			value =Arrays.asList(getAsValue(be.getBetweenExpressionStart()), getAsValue(be.getBetweenExpressionEnd()));
			break;
		default:
			throw new UnsupportedOperationException();
		}
		result.startWithColumn=leftColumn;
		result.startWithOperator=op;
		result.startWithValue=value;
	}

	private int getColumnId(Expression leftExpression,ColumnMeta columns,Map<String,String> maps) {
		if(leftExpression instanceof Column){
			return getColumn(columns, (Column)leftExpression, maps);
		}
		throw new UnsupportedOperationException(leftExpression.toString());
	}

	private Object getAsValue(Expression exp) {
		if (exp.getType() == ExpressionType.value) {
			SqlValue value = (SqlValue) exp;
			return value.getValue();
		} else if (exp.getType() == ExpressionType.param) {
			JpqlParameter jp=(JpqlParameter)exp;
			Object value;
			if(jp.getName()==null){
				value= rawParams.getIndexedParam(jp.getIndex());	
			}else{
				value=rawParams.getNamedParam(jp.getName());
			}
			return value;
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
			int n1=getColumn(columns,c1,maps);
			int n2=getColumn(columns,c2,maps);
			if (n1 > 0 && n2 > 0) {
				result.connectPrior = n1;
				result.connectParent = n2;
				return;
			}
			throw new UnsupportedOperationException("The connect condition parser Error:" + c1 + "=" + c2);
		}
		throw new UnsupportedOperationException("Prior conditions must be Column");

	}

	private int getColumn(ColumnMeta columns, Column c1,Map<String,String> maps) {
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
		if(table1!=null){
			if(!StringUtils.equalsIgnoreCase(table1, cd.getTable())){
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
}

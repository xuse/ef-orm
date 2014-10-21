package jef.database.dialect;

import java.util.Arrays;

import javax.persistence.PersistenceException;

import jef.database.DbUtils;
import jef.database.jsqlparser.expression.Function;
import jef.database.jsqlparser.parser.ParseException;
import jef.database.jsqlparser.statement.select.OrderBy;
import jef.database.jsqlparser.statement.select.OrderByElement;
import jef.database.jsqlparser.statement.select.PlainSelect;
import jef.database.jsqlparser.statement.select.Select;
import jef.database.jsqlparser.statement.select.SelectExpressionItem;
import jef.database.jsqlparser.statement.select.Union;
import jef.database.query.SqlExpression;
import jef.database.wrapper.clause.BindSql;

public class SQL2005LimitHandlerSlowImpl extends SQL2000LimitHandlerSlowImpl {

	private OrderBy defaultOrder;

	public SQL2005LimitHandlerSlowImpl() {
		super();
		defaultOrder = new OrderBy();
		OrderByElement oe = new OrderByElement();
		oe.setExpression(new Function("CURRENT_TIMESTAMP"));
		defaultOrder.setOrderByElements(Arrays.asList(oe));
	}
	
	protected BindSql processToPageSQL(String sql, int[] offsetLimit) {
		try {
			Select select = DbUtils.parseNativeSelect(sql);
			if(select.getSelectBody() instanceof PlainSelect){
				return toPage(offsetLimit,(PlainSelect)select.getSelectBody(),sql);
			}else{
				return toPage(offsetLimit,(Union)select.getSelectBody(),sql);
			}
		} catch (ParseException e) {
			throw new PersistenceException(e);
		}
	}

	private BindSql toPage(int[] offsetLimit, Union union, String raw) {
		OrderBy order = union.getOrderBy();
		if (order == null) {
			order = union.getLastPlainSelect().getOrderBy();
			if (order != null) {
				// 解析器问题，会把属于整个union的排序条件当做是属于最后一个查询的
				union.getLastPlainSelect().setOrderBy(null);
			} else {
				order = defaultOrder;
			}
		}else{
			union.setOrderBy(null);
		}
		// order可以直接移出
		StringBuilder sb = new StringBuilder();
		sb.append("select _tmp.* from ( \nselect row_number() over (");
		order.appendTo(sb);
		sb.append(") as __rn, _tmp1.* from (");
		union.appendTo(sb);
		sb.append(") _tmp1) _tmp2 where __rn between ");
		sb.append(offsetLimit[0] + 1).append(" and ").append(offsetLimit[0] + offsetLimit[1]);
		return new BindSql(sb.toString());
	}

	private BindSql toPage(int[] offsetLimit, PlainSelect selectBody, String raw) {
		OrderBy order = selectBody.getOrderBy();
		if (order == null) {
			order = defaultOrder;
		} else {
			selectBody.setOrderBy(null);
		}
		StringBuilder sb1 = new StringBuilder("row_number() over (");
		order.appendTo(sb1);
		sb1.append(")");
		selectBody.getSelectItems().add(0, new SelectExpressionItem(new SqlExpression(sb1.toString()), "__rn"));

		StringBuilder sb = new StringBuilder("select _tmp1.* from (");
		selectBody.appendTo(sb);
		sb.append(") _tmp1 where __rn between ");
		sb.append(offsetLimit[0] + 1).append(" and ").append(offsetLimit[0] + offsetLimit[1]);
		return new BindSql(sb.toString());
	}
}

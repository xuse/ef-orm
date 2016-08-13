package jef.database.dialect.handler;

import jef.database.wrapper.clause.BindSql;

import com.alibaba.druid.sql.ast.SQLOrderBy;
import com.alibaba.druid.sql.ast.SQLOver;
import com.alibaba.druid.sql.ast.expr.SQLAggregateExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.statement.SQLSelect;
import com.alibaba.druid.sql.ast.statement.SQLSelectItem;
import com.alibaba.druid.sql.ast.statement.SQLSelectOrderByItem;
import com.alibaba.druid.sql.ast.statement.SQLUnionQuery;
import com.alibaba.druid.sql.dialect.sqlserver.ast.SQLServerSelectQueryBlock;
import com.alibaba.druid.sql.dialect.sqlserver.visitor.SQLServerOutputVisitor;

public class SQL2005LimitHandler extends SQL2000LimitHandler {
	
	private SQLOrderBy defaultOrder;
	
	public SQL2005LimitHandler() {
		super();
		defaultOrder = new SQLOrderBy();
		SQLSelectOrderByItem oe = new SQLSelectOrderByItem();
		oe.setExpr(new SQLIdentifierExpr("CURRENT_TIMESTAMP"));
		defaultOrder.getItems().add(oe);
	}



	@Override
	protected BindSql toPage(int[] offsetLimit, SQLServerSelectQueryBlock selectBody, SQLSelect select, String raw) {
		SQLOrderBy order = select.getOrderBy();
		if (order == null) {
			order = defaultOrder;
		} else {
			select.setOrderBy(null);
		}
		
		SQLAggregateExpr arg=new SQLAggregateExpr("row_number");
		SQLOver over=new SQLOver();
		over.setOrderBy(order);
		arg.setOver(over);
		selectBody.getSelectList().add(0, new SQLSelectItem(arg, "__rn"));

		StringBuilder sb = new StringBuilder("SELECT _tmp1.* FROM (");
		SQLServerOutputVisitor visitor=new SQLServerOutputVisitor(sb);
		visitor.setPrettyFormat(false);
		select.accept(visitor);
		sb.append(") _tmp1 WHERE __rn between ");
		sb.append(offsetLimit[0] + 1).append(" and ").append(offsetLimit[0] + offsetLimit[1]);
		return new BindSql(sb.toString());
	}



	@Override
	protected BindSql toPage(int[] offsetLimit, SQLUnionQuery union, SQLSelect select, String raw) {
		SQLOrderBy order = super.removeOrder(union);
		if(order==null){
			order = defaultOrder;
		}
		// order可以直接移出
		StringBuilder sb = new StringBuilder();
		SQLServerOutputVisitor visitor=new SQLServerOutputVisitor(sb);
		visitor.setPrettyFormat(false);
		sb.append("SELECT _tmp2.* FROM ( \nSELECT row_number() OVER (");
		order.accept(visitor);
		sb.append(") AS __rn, _tmp1.* FROM (");
		union.accept(visitor);
		sb.append(") _tmp1) _tmp2 WHERE __rn BETWEEN ");
		sb.append(offsetLimit[0] + 1).append(" and ").append(offsetLimit[0] + offsetLimit[1]);
		return new BindSql(sb.toString());
	}

}

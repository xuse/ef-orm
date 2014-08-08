package jef.database.jsqlparser.expression;

import java.util.List;

import jef.database.jsqlparser.statement.SqlAppendable;
import jef.database.jsqlparser.statement.select.OrderBy;
import jef.database.jsqlparser.statement.select.PlainSelect;

/**
 * 分析函数的 over后面的部分
 * @author jiyi
 *
 */
public class Over implements SqlAppendable{
	private OrderBy orderBy;
	private List<Expression> partition;
	
	public void appendTo(StringBuilder sb) {
		sb.append(" over(");
		if(partition!=null && !partition.isEmpty()){
			PlainSelect.getFormatedList(sb, partition, "partition by", false);
		}
		if(orderBy!=null){
			orderBy.appendTo(sb);
		}
		sb.append(')');
	}


	public OrderBy getOrderBy() {
		return orderBy;
	}
	public void setOrderBy(OrderBy orderBy) {
		this.orderBy = orderBy;
	}

	public List<Expression> getPartition() {
		return partition;
	}

	public void setPartition(List<Expression> partition) {
		this.partition = partition;
	}


	public void accept(ExpressionVisitor visitorAdapter) {
		visitorAdapter.visit(this);
	}

}

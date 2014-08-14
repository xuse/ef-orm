package jef.database.jsqlparser.expression;

import java.util.List;

import jef.database.jsqlparser.statement.SqlAppendable;
import jef.database.jsqlparser.statement.select.OrderBy;
import jef.database.jsqlparser.statement.select.PlainSelect;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.ExpressionType;
import jef.database.jsqlparser.visitor.ExpressionVisitor;

/**
 * 分析函数的 over后面的部分(开窗函数)
 * @author jiyi
 *
 */
public class Over implements SqlAppendable{
	/**
	 * 分区表达式
	 */
	private List<Expression> partition;
	/**
	 * 排序表达式
	 */
	private OrderBy orderBy;
	
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

	@Override
	public String toString() {
		StringBuilder sb=new StringBuilder();
		appendTo(sb);
		return sb.toString();
	}
	
	public ExpressionType getType() {
		return ExpressionType.complex;
	}
	
	
}

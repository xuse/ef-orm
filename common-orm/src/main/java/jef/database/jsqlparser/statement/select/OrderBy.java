package jef.database.jsqlparser.statement.select;

import java.util.ArrayList;
import java.util.List;

import jef.database.jsqlparser.statement.SqlAppendable;
import jef.database.jsqlparser.visitor.SelectVisitor;

public class OrderBy implements SqlAppendable {
	
	protected List<OrderByElement> orderByElements = new ArrayList<OrderByElement>();
	private boolean nullsLast;

	public List<OrderByElement> getOrderByElements() {
		return orderByElements;
	}

	public void add(OrderByElement ele) {
		orderByElements.add(ele);
	}

	public void appendTo(StringBuilder sb) {
		PlainSelect.getFormatedList(sb, orderByElements, " order by", false);
		if(nullsLast){
			sb.append(" NULLS LAST");
		}
	}

	public void setOrderByElements(List<OrderByElement> orderByElements) {
		this.orderByElements = orderByElements;
	}

	public boolean isNullsLast() {
		return nullsLast;
	}

	public void setNullsLast(boolean nullsLast) {
		this.nullsLast = nullsLast;
	}

	public void accept(SelectVisitor orderByVisitor) {
		orderByVisitor.visit(this);
	 }

	@Override
	public String toString() {
		StringBuilder sb=new StringBuilder();
		appendTo(sb);
		return sb.toString();
	}
}

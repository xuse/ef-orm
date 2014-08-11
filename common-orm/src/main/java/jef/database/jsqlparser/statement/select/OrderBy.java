package jef.database.jsqlparser.statement.select;

import java.util.ArrayList;
import java.util.List;

import jef.database.jsqlparser.statement.SqlAppendable;

public class OrderBy implements SqlAppendable {
	protected final List<OrderByElement> orderByElements = new ArrayList<OrderByElement>();
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

	public boolean isNullsLast() {
		return nullsLast;
	}

	public void setNullsLast(boolean nullsLast) {
		this.nullsLast = nullsLast;
	}

	public void accept(SelectVisitor orderByVisitor) {
		orderByVisitor.visit(this);
	 }
}

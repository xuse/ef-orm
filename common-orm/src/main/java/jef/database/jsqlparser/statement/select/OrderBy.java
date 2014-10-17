package jef.database.jsqlparser.statement.select;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jef.database.jsqlparser.Util;
import jef.database.jsqlparser.statement.SqlAppendable;
import jef.database.jsqlparser.visitor.SelectItem;
import jef.database.jsqlparser.visitor.SelectItemVisitor;

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
		Util.getFormatedList(sb, orderByElements, " order by", false);
		if(nullsLast){
			sb.append(" NULLS LAST");
		}
	}
	
	public void reverseAppendTo(StringBuilder sb,String tmpTableAlias,List<SelectItem> items) {
		sb.append( " order by ");
		Iterator<OrderByElement> iter=orderByElements.iterator();
		if(iter.hasNext()){
			iter.next().reverseAppendTo(sb,tmpTableAlias,items);
		}
		for(;iter.hasNext();){
			sb.append(',');
			iter.next().reverseAppendTo(sb,tmpTableAlias,items);
		}
//		if(nullsLast){ //SQLServer不支持,故不用考虑
//			sb.append(" NULLS LAST");
//		}
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

	public void accept(SelectItemVisitor orderByVisitor) {
		orderByVisitor.visit(this);
	 }

	@Override
	public String toString() {
		StringBuilder sb=new StringBuilder();
		appendTo(sb);
		return sb.toString();
	}
}

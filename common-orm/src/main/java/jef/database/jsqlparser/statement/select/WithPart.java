package jef.database.jsqlparser.statement.select;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import jef.database.jsqlparser.statement.SqlAppendable;
import jef.database.jsqlparser.visitor.SelectVisitor;

public final class WithPart implements SqlAppendable {
	private boolean recursive;
	private List<WithItem> withItemsList;

	public WithPart(List<WithItem> withItemsList, boolean recursive) {
		this.withItemsList = withItemsList;
		this.recursive = recursive;
	}

	public WithPart() {
		this.withItemsList = Collections.emptyList();
	}

	public boolean isRecursive() {
		return recursive;
	}

	public void setRecursive(boolean recursive) {
		this.recursive = recursive;
	}

	public List<WithItem> getWithItemsList() {
		return withItemsList;
	}

	public void setWithItemsList(List<WithItem> withItemsList) {
		this.withItemsList = withItemsList;
	}

	public void accept(SelectVisitor selectVisitor) {
		selectVisitor.visit(this);
	}

	@Override
	public void appendTo(StringBuilder sb) {
		sb.append("WITH ");
		if (recursive) {
			sb.append("RECURSIVE ");
		}
		for (Iterator<WithItem> iter = withItemsList.iterator(); iter.hasNext();) {
			WithItem withItem = iter.next();
			withItem.appendTo(sb);
			if (iter.hasNext())
				sb.append(",");
			sb.append(" ");
		}
	}
}

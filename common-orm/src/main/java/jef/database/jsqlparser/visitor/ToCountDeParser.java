package jef.database.jsqlparser.visitor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import jef.database.dialect.DatabaseDialect;
import jef.database.jsqlparser.statement.select.Distinct;
import jef.database.jsqlparser.statement.select.PlainSelect;
import jef.database.jsqlparser.statement.select.SelectExpressionItem;
import jef.database.jsqlparser.statement.select.Union;
import jef.database.meta.Feature;

/**
 * 将语句转换为COUNT语句
 * @author jiyi
 *
 */
public class ToCountDeParser extends DeParserAdapter{
	protected final Deque<Object> visitPath = new ArrayDeque<Object>();
	private DatabaseDialect profile;
	
	public ToCountDeParser(DatabaseDialect profile){
		this.profile=profile;
	}
	
	@Override
	public void visit(Union union) {
		if (!visitPath.isEmpty()) {// 仅对顶层的PlainSelect操作
			super.visit(union);
			return;
		}
		visitPath.push(union);
		
		sb.append("select count(*) as count from (");
		for (Iterator<PlainSelect> iter = union.getPlainSelects().iterator(); iter.hasNext();) {
			sb.append("(");
			PlainSelect plainSelect = iter.next();
			plainSelect.accept(this);
			sb.append(")");
			if (iter.hasNext()) {
				sb.append(" UNION ");
			}
		}
		//去除原来语句中的order和limit
//		if (union.getOrderBy() != null) {
//			union.getOrderBy().accept(this);
//		}
//		if (union.getLimit() != null) {
//			deparseLimit(union.getLimit());
//		}
		sb.append(')');
		visitPath.pop();
		
	}
	@Override
	public void visit(PlainSelect plainSelect) {
		if (!visitPath.isEmpty()) {// 仅对顶层的PlainSelect操作
			super.visit(plainSelect);
			return;
		}
		visitPath.push(plainSelect);
		sb.append("select ");
		rewriteSelectItem(plainSelect);
		writeFromAndWhere(plainSelect);
		writeGroupByAndHaving(plainSelect);
		// writeOrderAndLimit(plainSelect);
		visitPath.pop();
	}

	private void rewriteSelectItem(PlainSelect plainSelect) {
		sb.append("count(");
		Distinct dis = plainSelect.getDistinct();
		if (dis == null) {
			sb.append('*');
		} else {
			rewriteDistinctCount(dis, plainSelect.getSelectItems());
		}
		sb.append(")");
	}

	private void rewriteDistinctCount(Distinct dis, List<SelectItem> items) {
		sb.append(dis.toString()).append(' ');
		String concatStart = "concat(";
		String concat = ",";
		String concatEnd = ")";
		if (profile.has(Feature.SUPPORT_CONCAT)) {
			concatStart = "";
			concat = "||";
			concatEnd = "";
		}
		int n = 0;
		if (items.size() > 1) {
			sb.append(concatStart);
		}
		for (SelectItem v : items) {
			if (n > 0)
				sb.append(concat);
			if (v.isAllColumns()) {
				sb.append(v.toString());
			} else {
				SelectExpressionItem item = v.getAsSelectExpression();
				item.getExpression().accept(this);
			}
			n++;
		}
		if (items.size() > 1) {
			sb.append(concatEnd);
		}
	}
}

package jef.database.jsqlparser;

import java.util.List;

import jef.database.dialect.DatabaseDialect;
import jef.database.jsqlparser.statement.select.Distinct;
import jef.database.jsqlparser.statement.select.SelectExpressionItem;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.SelectItem;
import jef.database.jsqlparser.visitor.SelectItemVisitor;
import jef.database.meta.Feature;

/**
 * 负责将转换为count中的查询项
 * @author Administrator
 * @deprecated 容易出问题
 *
 */
public class CountSelectItemConverter implements SelectItem {
	private Distinct dis;
	private List<SelectItem> items;
	private DatabaseDialect profile;
	
	public CountSelectItemConverter(List<SelectItem> items, Distinct dis,DatabaseDialect profile){
		this.items=items;
		this.dis=dis;
		this.profile=profile;
	}

	public void accept(SelectItemVisitor selectItemVisitor) {
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(128);
		appendTo(sb);
		return sb.toString();
	}

	public void appendTo(StringBuilder sb) {
		sb.append("count(");
		if (dis != null) {
			sb.append(dis.toString()).append(' ');
			String concatStart="concat(";
			String concat=",";
			String concatEnd=")";
			if(profile.has(Feature.SUPPORT_CONCAT)){
				concatStart="";
				concat="||";
				concatEnd="";
			}
			int n = 0;
			if(items.size()>1){
				sb.append(concatStart);
			}
			for (SelectItem v : items) {
				if (n > 0)
					sb.append(concat);
				if(v instanceof SelectExpressionItem){
					String alias=((SelectExpressionItem) v).getAlias();
					((SelectExpressionItem) v).setAlias(null);
					v.appendTo(sb); //sb.append(v.toString());
					((SelectExpressionItem) v).setAlias(alias);
				}else{
					sb.append(v.toString());
				}
				n++;
			}
			if(items.size()>1){
				sb.append(concatEnd);
			}
		} else {
			sb.append("*");
		}
		sb.append(")");
	}

	public Expression getExpression() {
		throw new UnsupportedOperationException();
	}

	public String getAlias() {
		throw new UnsupportedOperationException();
	}

	public void appendTo(StringBuilder sb, boolean noGroupFunc) {
		throw new UnsupportedOperationException();
		
	}
}

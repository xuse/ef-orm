package jef.database.jsqlparser;

import java.util.Iterator;
import java.util.List;

import jef.database.jsqlparser.statement.SqlAppendable;

public class Util {
	private Util(){}
	public static void getFormatedList(StringBuilder sb, List<? extends SqlAppendable> list, String expression, boolean useBrackets) {
		if (list == null || list.isEmpty())
			return;
		if (expression != null) {
			sb.append(expression).append(' ');
		}
		getStringList(sb, list, ",", useBrackets);
	}

	public static void getStringList(StringBuilder sb, List<? extends SqlAppendable> list, String comma, boolean useBrackets) {
		if (list != null) {
			if (useBrackets) {
				sb.append('(');
				if (!list.isEmpty()) {
					Iterator<? extends SqlAppendable> iterator = list.iterator();
					iterator.next().appendTo(sb);
					while (iterator.hasNext()) {
						iterator.next().appendTo(sb.append(comma));
					}
				}
				sb.append(')');
			} else {
				if (!list.isEmpty()) {
					Iterator<? extends SqlAppendable> iterator = list.iterator();
					iterator.next().appendTo(sb);
					while (iterator.hasNext()) {
						iterator.next().appendTo(sb.append(comma));
					}
				}
			}
		}
	}
}

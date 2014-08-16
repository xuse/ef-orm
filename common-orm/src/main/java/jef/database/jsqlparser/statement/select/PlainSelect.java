/*
 * JEF - Copyright 2009-2010 Jiyi (mr.jiyi@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jef.database.jsqlparser.statement.select;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import jef.database.jsqlparser.expression.Table;
import jef.database.jsqlparser.parser.Token;
import jef.database.jsqlparser.statement.SqlAppendable;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.FromItem;
import jef.database.jsqlparser.visitor.Ignorable;
import jef.database.jsqlparser.visitor.SelectBody;
import jef.database.jsqlparser.visitor.SelectItem;
import jef.database.jsqlparser.visitor.SelectVisitor;
import jef.tools.StringUtils;

/**
 * The core of a "SELECT" statement (no UNION, no ORDER BY)
 */
public class PlainSelect implements SelectBody {

	protected Distinct distinct = null;

	protected List<SelectItem> selectItems;

	protected Table into;

	protected FromItem fromItem;

	protected List<Join> joins;

	protected Expression where;

	protected List<Expression> groupByColumnReferences;

	protected OrderBy orderBy;

	protected Expression having;

	protected Limit limit;

	protected Top top;

	protected StartWithExpression startWith;
	private String hint;

	public StartWithExpression getStartWith() {
		return startWith;
	}

	public void setHint(Token t) {
		if (t != null && t.specialToken != null) {
			this.hint = t.specialToken.image;
		}
	}

	public String getHint() {
		return hint;
	}

	public void setHint(String hint) {
		this.hint = hint;
	}

	public void setStartWith(StartWithExpression startWith) {
		this.startWith = startWith;
	}

	/**
	 * The {@link FromItem} in this query
	 * 
	 * @return the {@link FromItem}
	 */
	public FromItem getFromItem() {
		return fromItem;
	}

	public Table getInto() {
		return into;
	}

	/**
	 * The {@link SelectItem}s in this query (for example the A,B,C in
	 * "SELECT A,B,C")
	 * 
	 * @return a list of {@link SelectItem}s
	 */
	public List<SelectItem> getSelectItems() {
		return selectItems;
	}

	public Expression getWhere() {
		return where;
	}

	public void setFromItem(FromItem item) {
		fromItem = item;
	}

	public void setInto(Table table) {
		into = table;
	}

	public void setSelectItems(List<SelectItem> list) {
		selectItems = list;
	}

	public void setWhere(Expression where) {
		this.where = where;
	}

	/**
	 * The list of {@link Join}s
	 * 
	 * @return the list of {@link Join}s
	 */
	public List<Join> getJoins() {
		return joins;
	}

	public void setJoins(List<Join> list) {
		joins = list;
	}

	public void accept(SelectVisitor selectVisitor) {
		selectVisitor.visit(this);
	}

	public OrderBy getOrderBy() {
		return orderBy;
	}

	public void setOrderBy(OrderBy orderBy) {
		this.orderBy = orderBy;
	}

	public Limit getLimit() {
		return limit;
	}

	public void setLimit(Limit limit) {
		this.limit = limit;
	}

	public Top getTop() {
		return top;
	}

	public void setTop(Top top) {
		this.top = top;
	}

	public Distinct getDistinct() {
		return distinct;
	}

	public void setDistinct(Distinct distinct) {
		this.distinct = distinct;
	}

	public Expression getHaving() {
		return having;
	}

	public void setHaving(Expression expression) {
		having = expression;
	}

	/**
	 * A list of {@link Expression}s of the GROUP BY clause. It is null in case
	 * there is no GROUP BY clause
	 * 
	 * @return a list of {@link Expression}s
	 */
	public List<Expression> getGroupByColumnReferences() {
		return groupByColumnReferences;
	}

	public void setGroupByColumnReferences(List<Expression> list) {
		groupByColumnReferences = list;
	}

	/**
	 * 将Select部分内容添加到目标中
	 * 
	 * @param sb
	 * @param noDistinct
	 */
	public void appendSelect(StringBuilder sb, boolean noDistinct) {
		sb.append("select ");
		if (!noDistinct && hint != null) {
			sb.append(hint).append(' ');
		}
		if (!noDistinct && distinct != null) {
			distinct.appendTo(sb);
			sb.append(' ');
		}
		if (!noDistinct && top != null) {
			top.appendTo(sb);
			sb.append(' ');
		}

		if (!selectItems.isEmpty()) {
			Iterator<SelectItem> iterator = selectItems.iterator();
			iterator.next().appendTo(sb);
			while (iterator.hasNext()) {
				iterator.next().appendTo(sb.append(','));
			}
		}
	}

	/**
	 * 将gourp having order limit等部分添加到字符串
	 * @param sb
	 * @param noHaving
	 * @param noOrder
	 * @param noLimit
	 */
	public void appendGroupHavingOrderLimit(StringBuilder sb, boolean noHaving,boolean noOrder,boolean noLimit) {
		getFormatedList(sb, groupByColumnReferences, " group by", false);
		if (!noHaving && having != null) {
			having.appendTo(sb.append(" having "));
		}
		if (!noOrder && orderBy != null) {
			orderBy.appendTo(sb);
		}
		if (!noLimit && limit != null) {
			limit.appendTo(sb);
		}
	}
	
	private void appendSelectItemsWitoutGroupAndAlias(Iterator<SelectItem> items,StringBuilder sb){
		Set<String> alreadyField=new HashSet<String>();
		List<String> columns=new ArrayList<String>();
		for(;items.hasNext();){
			SelectItem i=items.next();
			String s=i.getStringWithoutGroupFuncAndAlias();
			int point=s.indexOf('.');
			String key=point==-1?s:s.substring(point+1);
			if("*".equals(key)){
				columns.clear();
				columns.add(s);
				break;
			}
			if(!alreadyField.contains(key)){
				alreadyField.add(key);
				columns.add(s);
			}
		}
		StringUtils.joinTo(columns, ",", sb);
	}

	/**
	 * 根据参数对传出的语句做一定的调整
	 * 
	 * @param sb
	 * @param noGroupHaving
	 *            取消group by和having
	 * @param noOrder
	 *            取消排序
	 * @param noLimits
	 *            取消limit
	 * @param noDistinct
	 *            取消distinct
	 */
	public void appendTo(StringBuilder sb, boolean noGroup, boolean noHaving, boolean noOrder, boolean noLimit, boolean noDistinct) {
		sb.append("select ");
		if (!noDistinct && hint != null) {
			sb.append(hint).append(' ');
		}
		if (!noDistinct && distinct != null) {
			distinct.appendTo(sb);
			sb.append(' ');
		}
		if (!noDistinct && top != null) {
			top.appendTo(sb);
			sb.append(' ');
		}

		if (!selectItems.isEmpty()) {
			Iterator<SelectItem> iterator = selectItems.iterator();
			if(noGroup){
				appendSelectItemsWitoutGroupAndAlias(iterator,sb);
			}else{
				iterator.next().appendTo(sb);
				while (iterator.hasNext()) {
					iterator.next().appendTo(sb.append(','));
				}	
			}
		}

		sb.append(" from ");
		fromItem.appendTo(sb);// append(fromItem.toString());
		if (joins != null) {
			Iterator<Join> it = joins.iterator();
			while (it.hasNext()) {
				Join join = (Join) it.next();
				if (join.isSimple()) {
					join.appendTo(sb.append(", "));
				} else {
					join.appendTo(sb.append(' '));
				}
			}
		}
		if ((where != null)) {
			appendWhere(sb, where);
		}
		if (startWith != null) {
			startWith.appendTo(sb);
		}
		if (!noGroup) {
			getFormatedList(sb, groupByColumnReferences, " group by", false);
			if (!noHaving && having != null) {
				having.appendTo(sb.append(" having "));
			}
		}
		if (!noOrder && orderBy != null) {
			orderBy.appendTo(sb);
		}
		if (!noLimit && limit != null) {
			limit.appendTo(sb);
		}
	}

	public void appendTo(StringBuilder sb) {
		sb.append("select ");
		if (hint != null) {
			sb.append(hint).append(' ');
		}
		if (distinct != null) {
			distinct.appendTo(sb);
			sb.append(' ');
		}
		if (top != null) {
			top.appendTo(sb);
			sb.append(' ');
		}
		getStringList(sb, selectItems, ",", false);
		sb.append(" from ");
		fromItem.appendTo(sb);// append(fromItem.toString());
		if (joins != null) {
			Iterator<Join> it = joins.iterator();
			while (it.hasNext()) {
				Join join = (Join) it.next();
				if (join.isSimple()) {
					join.appendTo(sb.append(", "));
				} else {
					join.appendTo(sb.append(' '));
				}
			}
		}
		if ((where != null)) {
			appendWhere(sb, where);
		}
		if (startWith != null) {
			startWith.appendTo(sb);
		}

		getFormatedList(sb, groupByColumnReferences, " group by", false);
		if (having != null) {
			having.appendTo(sb.append(" having "));
		}
		if (orderBy != null) {
			orderBy.appendTo(sb);
		}
		if (limit != null) {
			limit.appendTo(sb);
		}
	}


	private void appendWhere(StringBuilder sb, Expression where) {
		if (where instanceof Ignorable) {
			if (((Ignorable) where).isEmpty()) {
				return;
			}
		}
		sb.append(" where ");
		int len = sb.length();
		where.appendTo(sb);
		// 防止动态条件均为生效后多余的where关键字引起SQL错误
		if (sb.length() - len < 2) {
			sb.setLength(len - 7);
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder(256);
		appendTo(sb);
		return sb.toString();
	}

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

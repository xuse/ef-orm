package jef.database.wrapper.clause;

import java.util.Collections;
import java.util.List;

import jef.common.Entry;
import jef.database.wrapper.populator.ColumnDescription;
import jef.database.wrapper.populator.ColumnMeta;
import jef.http.client.support.CommentEntry;
import jef.tools.StringUtils;

public class OrderClause {
	@SuppressWarnings("unchecked")
	public static final OrderClause DEFAULT=new OrderClause("",Collections.EMPTY_LIST);
	
	//其实现代码和排序
	private List<Entry<String,Boolean>> asSelect;
	private String sql;
	public OrderClause(String sql, List<Entry<String,Boolean>> rs) {
		this.sql=sql;
		this.asSelect =rs;
	}
	public List<Entry<String, Boolean>> getAsSelect() {
		return asSelect;
	}

	public void setAsSelect(List<Entry<String, Boolean>> asSelect) {
		this.asSelect = asSelect;
	}

	public String getSql() {
		return sql;
	}
	public void setSql(String sql) {
		this.sql = sql;
	}
	@Override
	public String toString() {
		return sql;
	}
	public boolean isNotEmpty(){
		return !asSelect.isEmpty();
	}
	
	/**
	 * 当多库查询时，通过解析需要排序的列在结果集中的位置，计算重排序策略。
	 * @param select
	 * @param columns
	 * @return
	 */
	public InMemoryOrderBy parseAsSelectOrder(SelectPart select,ColumnMeta columns) {
		List<CommentEntry> selectItems=select.getEntries();
		int[] orders = new int[asSelect.size()];
		boolean[] orderAsc = new boolean[asSelect.size()];

		for (int i = 0; i < asSelect.size(); i++) {
			Entry<String, Boolean> order = asSelect.get(i);
			String alias = findAlias(order.getKey(), selectItems);
			if (alias == null) {
				throw new IllegalArgumentException("The order field " + order.getKey() + " does not selected in SQL!");
			}
			// 可能为null
			ColumnDescription selectedColumn = columns.getByFullName(alias);
			if (selectedColumn == null) {
				throw new IllegalArgumentException("The order field " + alias + " does not found in this Query!");
			}
			orders[i] = selectedColumn.getN();//
			orderAsc[i] = order.getValue();
		}
		return new InMemoryOrderBy(orders,orderAsc);
	}
	
	private String findAlias(String key, List<CommentEntry> selectItems) {
		String alias = null;
		for (CommentEntry c : selectItems) {
			if (key.equals(c.getKey())) {
				alias = c.getValue();
				break;
			}
		}
		if (alias == null) {
			alias = StringUtils.substringAfterIfExist(key, ".");
		}
		return alias;
	}

}

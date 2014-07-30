package jef.database.wrapper;

import java.util.Collections;
import java.util.List;

import jef.common.Entry;

public class OrderResult {
	@SuppressWarnings("unchecked")
	public static final OrderResult DEFAULT=new OrderResult("",Collections.EMPTY_LIST);
	
	//其实现代码和排序
	private List<Entry<String,Boolean>> asSelect;
	private String sql;
	public OrderResult(String string, List<Entry<String,Boolean>> rs) {
		this.sql=string;
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
	
}

package jef.database.wrapper.clause;

import jef.database.Condition.Operator;

public class HavingEle {
	private int index;
	public String column;
	public String sql;
	public Operator havingCondOperator;// 当为Having字句时的操作符
	public Object havingCondValue; // 当为Having字句时的比较值

	@Override
	public String toString() {
		return sql;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}
}

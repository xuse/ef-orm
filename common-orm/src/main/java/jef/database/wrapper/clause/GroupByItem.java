package jef.database.wrapper.clause;

public class GroupByItem {
	private int index;
	private GroupFunctionType type;
	private String alias;
	public GroupByItem(int i, GroupFunctionType type, String alias) {
		this.index=i;
		this.type=type;
		this.alias=alias;
	}
	public int getIndex() {
		return index;
	}
	public void setIndex(int index) {
		this.index = index;
	}
	public GroupFunctionType getType() {
		return type;
	}
	public void setType(GroupFunctionType type) {
		this.type = type;
	}
	public String getAlias() {
		return alias;
	}
	public void setAlias(String alias) {
		this.alias = alias;
	}
}

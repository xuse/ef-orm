package jef.database.wrapper.clause;

import java.util.List;


public class DdlResult implements SqlResult{
	private List<String> sqls;

	public List<String> getSqls() {
		return sqls;
	}

	public void setSqls(List<String> sqls) {
		this.sqls = sqls;
	}
}

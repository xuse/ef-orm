package jef.database.dialect;

import jef.database.wrapper.clause.BindSql;
import jef.tools.StringUtils;

public class DerbyLimitHandler implements LimitHandler {
	private final static String DERBY_PAGE = " offset %start% row fetch next %next% rows only";

	public BindSql toPageSQL(String sql, int[] range) {
		String start = String.valueOf(range[0]);
		String next = String.valueOf(range[1]);
		String limit = StringUtils.replaceEach(DERBY_PAGE, new String[] { "%start%", "%next%" }, new String[] { start, next });
		sql = sql.concat(limit);
		return new BindSql(sql);
	}

	public BindSql toPageSQL(String sql, int[] range, boolean isUnion) {
		return toPageSQL(sql, range);
	}
}

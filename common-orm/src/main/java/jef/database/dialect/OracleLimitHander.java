package jef.database.dialect;

import jef.database.dialect.statement.LimitHandler;
import jef.database.wrapper.clause.BindSql;

public class OracleLimitHander implements LimitHandler {

	private final static String ORACLE_PAGE1 = "select * from (select tb__.*, rownum rid__ from (";
	private final static String ORACLE_PAGE2 = " ) tb__ where rownum <= %end%) where rid__ > %start%";

	public BindSql toPageSQL(String sql, int[] range) {
		sql = ORACLE_PAGE1 + sql;
		String limit = ORACLE_PAGE2.replace("%start%", String.valueOf(range[0]));
		limit = limit.replace("%end%", String.valueOf(range[0]+range[1]));
		sql = sql.concat(limit);
		return new BindSql(sql);
	}


	@Override
	public BindSql toPageSQL(String sql, int[] offsetLimit, boolean isUnion) {
		return toPageSQL(sql, offsetLimit);
	}

}

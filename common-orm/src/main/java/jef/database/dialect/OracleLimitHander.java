package jef.database.dialect;

import java.sql.ResultSet;

import jef.common.wrapper.IntRange;
import jef.database.dialect.statement.LimitHandler;

public class OracleLimitHander implements LimitHandler {

	private final static String ORACLE_PAGE1 = "select * from (select tb__.*, rownum rid__ from (";
	private final static String ORACLE_PAGE2 = " ) tb__ where rownum <= %end%) where rid__ >= %start%";

	public String toPageSQL(String sql, IntRange range) {
		sql = ORACLE_PAGE1 + sql;
		String limit = ORACLE_PAGE2.replace("%start%", String.valueOf(range.getLeastValue()));
		limit = limit.replace("%end%", String.valueOf(range.getGreatestValue()));
		sql = sql.concat(limit);
		return sql;
	}


	@Override
	public String toPageSQL(String sql, IntRange offsetLimit, boolean isUnion) {
		return toPageSQL(sql, offsetLimit);
	}

	@Override
	public ResultSet afterProcess(ResultSet rs, IntRange offsetLimit) {
		return rs;
	}

}

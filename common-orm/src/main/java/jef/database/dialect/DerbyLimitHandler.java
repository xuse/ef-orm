package jef.database.dialect;

import java.sql.ResultSet;

import jef.common.wrapper.IntRange;
import jef.database.dialect.statement.LimitHandler;
import jef.tools.StringUtils;

public class DerbyLimitHandler implements LimitHandler {
	private final static String DERBY_PAGE = " offset %start% row fetch next %next% rows only";
	public String toPageSQL(String sql, IntRange range) {
//		boolean isUnion=false;
//		try {
//			Select select=DbUtils.parseNativeSelect(sql);
//			if(select.getSelectBody() instanceof Union){
//				isUnion=true;
//			}
//			select.getSelectBody();
//		} catch (ParseException e) {
//			LogUtil.exception("SqlParse Error:",e);
//		}
		String start = String.valueOf(range.getLeastValue() - 1);
		String next = String.valueOf(range.getGreatestValue() - range.getLeastValue() + 1);
		String limit = StringUtils.replaceEach(DERBY_PAGE, new String[] { "%start%", "%next%" }, new String[] { start, next });
//		if (isUnion) {
//			sql = StringUtils.concat("select * from (", sql, ") tb", limit);
//		} else {
			sql = sql.concat(limit);
//		}
		return sql;
	}
	
	public String toPageSQL(String sql, IntRange range,boolean isUnion) {
		String start = String.valueOf(range.getLeastValue() - 1);
		String next = String.valueOf(range.getGreatestValue() - range.getLeastValue() + 1);
		String limit = StringUtils.replaceEach(DERBY_PAGE, new String[] { "%start%", "%next%" }, new String[] { start, next });
//		if (isUnion) {
//			return StringUtils.concat("select * from (", sql, ") tb", limit);
//		} else {
			return sql.concat(limit);
//		}
	}

	@Override
	public ResultSet afterProcess(ResultSet rs, IntRange offsetLimit) {
		return rs;
	}

}

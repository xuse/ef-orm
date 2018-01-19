package jef.database.dialect.handler;

import jef.database.wrapper.clause.BindSql;
import jef.tools.PageLimit;

public class OracleLimitHander implements LimitHandler {

	private final static String ORACLE_PAGE1 = "select * from (select tb__.*, rownum rid__ from (";
	private final static String ORACLE_PAGE2 = " ) tb__ where rownum <= %end%) where rid__ > %start%";

	public BindSql toPageSQL(String sql, PageLimit range) {
		if(range.getOffset()==0){
			return new BindSql("select tb__.* from (\n"+sql+") tb__ where rownum <= "+range.getLimit());
		}else{
			sql = ORACLE_PAGE1 + sql;
			String limit = ORACLE_PAGE2.replace("%start%", String.valueOf(range.getOffset()));
			limit = limit.replace("%end%", String.valueOf(range.getEnd()));
			sql = sql.concat(limit);
			return new BindSql(sql);	
		}
		
	}


	@Override
	public BindSql toPageSQL(String sql, PageLimit offsetLimit, boolean isUnion) {
		return toPageSQL(sql, offsetLimit);
	}

}

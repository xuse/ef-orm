package jef.database.dialect.handler;

import jef.database.wrapper.clause.BindSql;
import jef.tools.PageLimit;

public class DerbyLimitHandler implements LimitHandler {

	public BindSql toPageSQL(String sql, PageLimit range) {
		String limit; 
		if(range.getOffset()==0){
			limit=" fetch next "+range.getLimit()+" rows only";
		}else{
			limit=" offset "+range.getOffset()+" row fetch next "+range.getLimit()+" rows only";
		}
		sql = sql.concat(limit);
		return new BindSql(sql);
	}

	public BindSql toPageSQL(String sql, PageLimit range, boolean isUnion) {
		return toPageSQL(sql, range);
	}
}

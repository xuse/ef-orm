package jef.database.dialect;

import jef.database.wrapper.clause.BindSql;

public class DerbyLimitHandler implements LimitHandler {

	public BindSql toPageSQL(String sql, int[] range) {
		String limit; 
		if(range[0]==0){
			limit=" fetch next "+range[1]+" rows only";
		}else{
			limit=" offset "+range[0]+" row fetch next "+range[1]+" rows only";
		}
		sql = sql.concat(limit);
		return new BindSql(sql);
	}

	public BindSql toPageSQL(String sql, int[] range, boolean isUnion) {
		return toPageSQL(sql, range);
	}
}

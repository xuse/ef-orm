package jef.database.dialect;

import jef.common.wrapper.IntRange;
import jef.database.meta.Feature;
import jef.database.query.Func;
import jef.database.query.function.StandardSQLFunction;
import jef.tools.StringUtils;

/**
 * SQL Server 2012
 * @author jiyi
 *
 */
public class SQLServer2012Dialect extends SQLServer2008Dialect{
	
	public SQLServer2012Dialect(){
		//2012开始支持SEQUENCE
		features.add(Feature.SUPPORT_SEQUENCE);
		
		addFunctions();
	}
	
	/*
	 * 2012新增的14个函数
	 * Reference
	 * http://msdn.microsoft.com/en-us/library/09f0096e-ab95-4be0-8c01-f98753255747(v=sql.110)
	 */
	private void addFunctions() {
		registerNative(new StandardSQLFunction("parse"));
		registerNative(new StandardSQLFunction("try_convert"));
		registerNative(new StandardSQLFunction("try_parse"));
		registerNative(new StandardSQLFunction("datefromparts"));
		registerNative(new StandardSQLFunction("datetime2fromparts"));
		registerNative(new StandardSQLFunction("datetimefromparts"));
		registerNative(new StandardSQLFunction("datetimeoffsetfromparts"));
		registerNative(new StandardSQLFunction("eomonth"));
		registerNative(new StandardSQLFunction("smalldatetimefromparts"));
		registerNative(new StandardSQLFunction("timefromparts"));
		
		registerNative(new StandardSQLFunction("choose"));
		registerNative(new StandardSQLFunction("iif"));
		registerNative(new StandardSQLFunction("format"));
		super.features.remove(Feature.CONCAT_IS_ADD);
		registerNative(Func.concat);//2012开始支持原生的concat函数。
		
//	2012新增的分析函数	
//		CUME_DIST (Transact-SQL) 
//		 LAST_VALUE (Transact-SQL) 
//		 PERCENTILE_DISC (Transact-SQL) 
//		 
//		FIRST_VALUE (Transact-SQL) 
//		 LEAD (Transact-SQL) 
//		 PERCENT_RANK (Transact-SQL) 
//		 
//		LAG (Transact-SQL) 
//		 PERCENTILE_CONT (Transact-SQL) 
//		 

	}

	private final static String PAGE_SQL_2012 = " offset %start% row fetch next %next% rows only";


	@Override
	public String toPageSQL(String sql, IntRange range) {
		String start = String.valueOf(range.getLeastValue() - 1);
		String next = String.valueOf(range.getGreatestValue() - range.getLeastValue() + 1);
		String limit = StringUtils.replaceEach(PAGE_SQL_2012, new String[] { "%start%", "%next%" }, new String[] { start, next });
		return sql.concat(limit);
	}
	
}

package jef.database.dialect.statement;

import jef.database.dialect.SQL2000LimitHandlerSlowImpl;
import jef.database.wrapper.clause.BindSql;

public class RegexpSQL2005LimitHandler extends SQL2000LimitHandlerSlowImpl{
	private static final String ORDERBY = "order by";
	private static final String SELECT = "select";
	private static final String SELECT_DISTINCT = "select distinct";
	
	
	
	@Override
	protected BindSql processToPageSQL(String sql, int[] offsetLimit) {
		int offset=offsetLimit[0];
		int limit=offsetLimit[1];
		int endIndex = offset + limit;
		StringBuilder sb = new StringBuilder(sql.replaceAll("\\s{2,}", " ").toLowerCase().trim());
		if(sb.indexOf(SELECT_DISTINCT)==sb.indexOf(SELECT)){
			throw new UnsupportedOperationException(getClass().getName()+".getLimitString(String queryString, int limit, int offset) unsupport key DISTINCT in query");
		}
		int orderByIndex = sb.indexOf(ORDERBY);
		//判断queryString中是否有order by 若没有则创建。
		CharSequence orderby = orderByIndex > 0 ? sb.subSequence(orderByIndex, sb.length()) : " order by CURRENT_TIMESTAMP";
		if(orderByIndex > 0){//原句中有orderby时将其提取至Over()内
			sb.delete(orderByIndex, orderByIndex + orderby.length());
		}

		int selectEndIndex = sb.indexOf(SELECT) + SELECT.length();
		//构建分页sql
		sb.insert(selectEndIndex, " ROW_NUMBER() OVER (" + orderby + ") as __hik_row_nr__,");
		sb.insert(0, "WITH hik_page_query AS (").append(") SELECT * FROM hik_page_query ");
		sb.append("WHERE __hik_row_nr__ between ").append(offset+1).append(" and ").append(endIndex);
		return new BindSql(sb.toString());
	}
}

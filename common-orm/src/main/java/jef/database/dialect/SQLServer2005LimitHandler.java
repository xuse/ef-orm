package jef.database.dialect;

import java.sql.ResultSet;

import jef.common.wrapper.IntRange;

import org.apache.commons.lang.StringUtils;

public class SQLServer2005LimitHandler extends SQLServer2000LimitHandler {

	@Override
	public String toPageSQL(String sql, IntRange range) {
		int[] offsetLimit=range.toStartLimitSpan();
		int offset=offsetLimit[0];
		if(offset==0){//没有offset可以简化处理
			int indexDistinct=StringUtils.indexOfIgnoreCase(sql, "select distinct");
			int index=StringUtils.indexOfIgnoreCase(sql, "select");
			return new StringBuilder( sql.length() + 8 )
			.append(sql).insert(index + (indexDistinct == index ? 15 : 6), " top " + offsetLimit[1] ).toString();
		}
		
		return super.toPageSQL(sql, range);
	}

	@Override
	public String toPageSQL(String sql, IntRange offsetLimit, boolean isUnion) {
		return super.toPageSQL(sql, offsetLimit,isUnion);
	}

	@Override
	public ResultSet afterProcess(ResultSet rs, IntRange offsetLimit) {
		return rs;
	}

	
//	private static final String pageTemplate="select top %limit% from (select ROW_NUMBER() over (%order%) as __row_num, * from " 
//	SELECT TOP 10 * FROM 
//	 (SELECT ROW_NUMBER() OVER (ORDER BY id) AS __ROW_NUM,* FROM table1) A
//	WHERE __ROW_NUM > 40;
//	@Override
//	protected String processToPageSQL(String sql, int[] offsetLimit) {
//		
//		final StringBuilder sb = new StringBuilder( sql );
//		if ( sb.charAt( sb.length() - 1 ) == ';' ) {
//			sb.setLength( sb.length() - 1 );
//		}
//
//		if ( LimitHelper.hasFirstRow( selection ) ) {
//			final String selectClause = fillAliasInSelectClause( sb );
//
//			final int orderByIndex = shallowIndexOfWord( sb, ORDER_BY, 0 );
//			if ( orderByIndex > 0 ) {
//				// ORDER BY requires using TOP.
//				addTopExpression( sb );
//			}
//
//			encloseWithOuterQuery( sb );
//
//			// Wrap the query within a with statement:
//			sb.insert( 0, "WITH query AS (" ).append( ") SELECT " ).append( selectClause ).append( " FROM query " );
//			sb.append( "WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?" );
//		}
//	}

}

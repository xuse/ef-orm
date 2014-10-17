package jef.database.dialect;

import java.sql.ResultSet;

import javax.persistence.PersistenceException;

import jef.common.wrapper.IntRange;
import jef.database.DbUtils;
import jef.database.dialect.statement.LimitHandler;
import jef.database.jsqlparser.parser.ParseException;
import jef.database.jsqlparser.statement.select.OrderBy;
import jef.database.jsqlparser.statement.select.PlainSelect;
import jef.database.jsqlparser.statement.select.Select;
import jef.database.jsqlparser.statement.select.SubSelect;
import jef.database.jsqlparser.statement.select.Top;
import jef.database.jsqlparser.statement.select.Union;

import org.apache.commons.lang.StringUtils;

public class SQLServer2000LimitHandler implements LimitHandler {
	public String toPageSQL(String sql, IntRange range) {
		int[] offsetLimit=range.toStartLimitSpan();
		int offset=offsetLimit[0];
		if(offset==0){//没有offset可以简化处理
			int indexDistinct=StringUtils.indexOfIgnoreCase(sql, "select distinct");
			int index=StringUtils.indexOfIgnoreCase(sql, "select");
			return new StringBuilder( sql.length() + 8 )
			.append(sql).insert(index + (indexDistinct == index ? 15 : 6), " top " + offsetLimit[1] ).toString();
		}
		return processToPageSQL(sql,offsetLimit);
	}

	protected String processToPageSQL(String sql, int[] offsetLimit) {
		try {
			Select select = DbUtils.parseNativeSelect(sql);
			if(select.getSelectBody() instanceof PlainSelect){
				return toPage(offsetLimit,(PlainSelect)select.getSelectBody(),sql);
			}else{
				return toPage(offsetLimit,(Union)select.getSelectBody(),sql);
			}
		} catch (ParseException e) {
			throw new PersistenceException(e);
		}
	}

	private String toPage(int[] offsetLimit, Union union,String raw) {
		OrderBy order=union.getOrderBy();
		if(order==null){
			order=union.getLastPlainSelect().getOrderBy();
			if(order!=null){
				//解析器问题，会把属于整个union的排序条件当做是属于最后一个查询的
				union.getLastPlainSelect().setOrderBy(null);
				union.setOrderBy(order);
			}else{
				throw new UnsupportedOperationException("Select must have order to page");
			}
		}
		StringBuilder sb=new StringBuilder(raw.length()+40);
		sb.append("select top ").append(offsetLimit[1]).append(" * from (\nselect top ");
		sb.append(offsetLimit[0]+offsetLimit[1]).append(" * from");
		SubSelect s=new SubSelect();
		union.setOrderBy(null);
		s.setSelectBody(union);
		s.setAlias("__ef_tmp1");
		s.appendTo(sb);
		sb.append('\n');
		order.appendTo(sb);
		sb.append(") __ef_tmp2\n");
		order.reverseAppendTo(sb,"__ef_tmp2",null);
		return sb.toString();
	}

	private String toPage(int[] offsetLimit, PlainSelect selectBody,String raw) {
		OrderBy order=selectBody.getOrderBy();
		if(order==null){
			throw new UnsupportedOperationException("Select must have order to page");
		}
		selectBody.setTop(new Top(offsetLimit[0]+offsetLimit[1]));
		
		StringBuilder sb=new StringBuilder(raw.length()+30);
		sb.append("select top ").append(offsetLimit[1]).append(" * from (");
		selectBody.appendTo(sb);
		sb.append(") __ef_t");
		order.reverseAppendTo(sb,"__ef_t",selectBody.getSelectItems());
		return sb.toString();
	}

	@Override
	public ResultSet afterProcess(ResultSet rs, IntRange offsetLimit) {
		return new ReverseResultSet(rs);
	}

	@Override
	public String toPageSQL(String sql, IntRange offsetLimit, boolean isUnion) {
		return toPageSQL(sql, offsetLimit);
	}

}

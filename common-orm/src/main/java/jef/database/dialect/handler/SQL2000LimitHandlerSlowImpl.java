package jef.database.dialect.handler;

import javax.persistence.PersistenceException;

import jef.database.DbUtils;
import jef.database.jdbc.statement.ResultSetLaterProcess;
import jef.database.jsqlparser.parser.ParseException;
import jef.database.jsqlparser.statement.select.OrderBy;
import jef.database.jsqlparser.statement.select.PlainSelect;
import jef.database.jsqlparser.statement.select.Select;
import jef.database.jsqlparser.statement.select.SubSelect;
import jef.database.jsqlparser.statement.select.Top;
import jef.database.jsqlparser.statement.select.Union;
import jef.database.wrapper.clause.BindSql;
import jef.tools.PageLimit;

import org.apache.commons.lang.StringUtils;

public class SQL2000LimitHandlerSlowImpl implements LimitHandler {
	public BindSql toPageSQL(String sql, PageLimit offsetLimit) {
		long offset=offsetLimit.getOffset();
		if(offset==0){//没有offset可以简化处理
			int indexDistinct=StringUtils.indexOfIgnoreCase(sql, "select distinct");
			int index=StringUtils.indexOfIgnoreCase(sql, "select");
			return new BindSql(new StringBuilder( sql.length() + 8 )
			.append(sql).insert(index + (indexDistinct == index ? 15 : 6), " top " + offsetLimit.getLimit() ).toString());
		}
		return processToPageSQL(sql,offsetLimit);
	}

	protected BindSql processToPageSQL(String sql, PageLimit offsetLimit) {
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

	private BindSql toPage(PageLimit offsetLimit, Union union,String raw) {
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
		sb.append("select top ");
		sb.append(offsetLimit.getEnd()).append(" * from");
		SubSelect s=new SubSelect();
		union.setOrderBy(null);
		s.setSelectBody(union);
		s.setAlias("__ef_tmp1");
		s.appendTo(sb);
		sb.append('\n');
		order.appendTo(sb);
//		sb.append(") __ef_tmp2\n");
//		order.reverseAppendTo(sb,"__ef_tmp2",null);
		return new BindSql(sb.toString()).setReverseResult(new ResultSetLaterProcess(offsetLimit.getOffset()));
	}

	private BindSql toPage(PageLimit offsetLimit, PlainSelect selectBody,String raw) {
		OrderBy order=selectBody.getOrderBy();
		if(order==null){
			throw new UnsupportedOperationException("Select must have order to page");
		}
		selectBody.setTop(new Top(offsetLimit.getEnd()));
		
		StringBuilder sb=new StringBuilder(raw.length()+30);
//		sb.append("select top ").append(offsetLimit[1]).append(" * from (");
		selectBody.appendTo(sb);
//		sb.append(") __ef_t");
//		order.reverseAppendTo(sb,"__ef_t",selectBody.getSelectItems());
		return new BindSql(sb.toString()).setReverseResult(new ResultSetLaterProcess(offsetLimit.getOffset()));
	}


	@Override
	public BindSql toPageSQL(String sql, PageLimit offsetLimit, boolean isUnion) {
		return toPageSQL(sql, offsetLimit);
	}

}

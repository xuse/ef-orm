package jef.database.dialect;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import jef.database.dialect.statement.LimitHandler;
import jef.database.wrapper.clause.BindSql;

import org.apache.commons.lang.StringUtils;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLOrderBy;
import com.alibaba.druid.sql.ast.SQLOrderingSpecification;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLIntegerExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.statement.SQLSelect;
import com.alibaba.druid.sql.ast.statement.SQLSelectItem;
import com.alibaba.druid.sql.ast.statement.SQLSelectOrderByItem;
import com.alibaba.druid.sql.ast.statement.SQLSubqueryTableSource;
import com.alibaba.druid.sql.ast.statement.SQLUnionQuery;
import com.alibaba.druid.sql.dialect.sqlserver.ast.SQLServerSelectQueryBlock;
import com.alibaba.druid.sql.dialect.sqlserver.ast.SQLServerTop;
import com.alibaba.druid.sql.dialect.sqlserver.parser.SQLServerSelectParser;
import com.alibaba.druid.sql.dialect.sqlserver.visitor.SQLServerOutputVisitor;

public class SQL2000LimitHandler implements LimitHandler {
	public BindSql toPageSQL(String sql, int[] offsetLimit) {
		int offset = offsetLimit[0];
		if (offset == 0) {// 没有offset可以简化处理
			int indexDistinct = StringUtils.indexOfIgnoreCase(sql, "select distinct");
			int index = StringUtils.indexOfIgnoreCase(sql, "select");
			return new BindSql(new StringBuilder(sql.length() + 8).append(sql).insert(index + (indexDistinct == index ? 15 : 6), " top " + offsetLimit[1]).toString());
		}
		return processToPageSQL(sql, offsetLimit);
	}

	private BindSql processToPageSQL(String sql, int[] offsetLimit) {
		SQLServerSelectParser parser = new SQLServerSelectParser(sql);
		SQLSelect select = parser.select();
		if(select.getQuery() instanceof SQLUnionQuery){
			return toPage(offsetLimit,(SQLUnionQuery)select.getQuery(),select,sql);
		}else{
			return toPage(offsetLimit,(SQLServerSelectQueryBlock)select.getQuery(),select ,sql);
		}


	}
	
	protected BindSql toPage(int[] offsetLimit, SQLServerSelectQueryBlock selectBody, SQLSelect select, String raw) {
		SQLOrderBy order = select.getOrderBy();
		if (order == null) {
			throw new UnsupportedOperationException("Select must have order to page");
		}
		SQLServerTop top=new SQLServerTop();
		top.setExpr(new SQLIntegerExpr(offsetLimit[0] + offsetLimit[1]));
		selectBody.setTop(top);
		StringBuilder sb = new StringBuilder(raw.length() + 30);
		sb.append("SELECT TOP ").append(offsetLimit[1]).append(" * FROM (");
		SQLServerOutputVisitor visitor=new SQLServerOutputVisitor(sb);
		visitor.setPrettyFormat(false);
		select.accept(visitor);
		sb.append(") __ef_t");
		
		appendOrderReverse(order,visitor, "__ef_t", selectBody.getSelectList());
		return new BindSql(sb.toString()).setReverseResult(true);
	}

	

	protected BindSql toPage(int[] offsetLimit, SQLUnionQuery union,SQLSelect select, String raw) {
		SQLOrderBy order=removeOrder(union);
		if(order==null){
			throw new UnsupportedOperationException("Select must have order to page");
		}

		StringBuilder sb = new StringBuilder(raw.length() + 40);
		sb.append("SELECT TOP ").append(offsetLimit[1]).append(" * FROM (\nSELECT TOP ");
		sb.append(offsetLimit[0] + offsetLimit[1]).append(" * FROM");
		SQLSubqueryTableSource s = new SQLSubqueryTableSource(select,"__ef_tmp1");
		
		SQLServerOutputVisitor visitor=new SQLServerOutputVisitor(sb);
		visitor.setPrettyFormat(false);
		s.accept(visitor);
		sb.append('\n');
		order.accept(visitor);
		sb.append(") __ef_tmp2\n");
		appendOrderReverse(order,visitor,"__ef_tmp2",null);
//		order.reverseAppendTo(sb, "__ef_tmp2", null);
		return new BindSql(sb.toString()).setReverseResult(true);
	}

	private void appendOrderReverse(SQLOrderBy order, SQLServerOutputVisitor visitor,String tmpTableAlias,List<SQLSelectItem> items) {
		StringBuilder sb=(StringBuilder)visitor.getAppender();
		sb.append( " ORDER BY ");
		Iterator<SQLSelectOrderByItem> iter=order.getItems().iterator();
		if(iter.hasNext()){
			reverseAppendTo(iter.next(),visitor,tmpTableAlias,items);
		}
		for(;iter.hasNext();){
			sb.append(',');
			reverseAppendTo(iter.next(),visitor,tmpTableAlias,items);
		}
		
	}

	private void reverseAppendTo(SQLSelectOrderByItem order, SQLServerOutputVisitor visitor, String tmpTableAlias, List<SQLSelectItem> items) {
		SQLExpr expression=order.getExpr();
		
		if(expression instanceof SQLPropertyExpr){
			SQLPropertyExpr c=(SQLPropertyExpr)expression;
			if(items!=null){
				fixWithSelects(c,items);
			}
			if(c.getOwner()!=null){
				c.setOwner(new SQLIdentifierExpr(tmpTableAlias));
			}
		}
		expression.accept(visitor);
		if(order.getType()==SQLOrderingSpecification.ASC || order.getType()==null){
			try {
				visitor.getAppender().append(" DESC");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	private void fixWithSelects(SQLPropertyExpr c, List<SQLSelectItem> items) {
		for(SQLSelectItem item:items){
			SQLExpr ex=item.getExpr();
			if(ex instanceof SQLPropertyExpr){
				SQLPropertyExpr exc=(SQLPropertyExpr)ex;
				if(isMatch(exc,c)){
					if(item.getAlias()!=null){
						c.setName(item.getAlias());
					}
				}
			}
		}
	}

	
	private boolean isMatch(SQLPropertyExpr exc, SQLPropertyExpr c) {
		if(StringUtils.equalsIgnoreCase(exc.getName(), c.getName())){
			if(c.getOwner()==exc.getOwner()){
				return true;
			}
			return StringUtils.equalsIgnoreCase(String.valueOf(c.getOwner()), String.valueOf(exc.getOwner()));
		}
		return false;
	}
	

	protected SQLOrderBy removeOrder(SQLUnionQuery union) {
		if(union.getRight() instanceof SQLUnionQuery){
			return removeOrder((SQLUnionQuery)union.getRight()); 
		}else{
			SQLOrderBy order=union.getOrderBy();
			union.setOrderBy(null);
			return order;
		}
	}


	@Override
	public BindSql toPageSQL(String sql, int[] offsetLimit, boolean isUnion) {
		return toPageSQL(sql, offsetLimit);
	}

}

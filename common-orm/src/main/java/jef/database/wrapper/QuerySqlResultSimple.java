package jef.database.wrapper;

import java.util.List;

import jef.common.wrapper.IntRange;
import jef.database.BindVariableDescription;
import jef.database.annotation.PartitionResult;
import jef.database.cache.CacheKey;
import jef.database.dialect.DatabaseDialect;

public class QuerySqlResultSimple implements IQuerySqlResult {
	private String body;
	private OrderResult orderbyPart;
	private List<BindVariableDescription> bind;
	private IntRange pageRange;
	private boolean isUnion;
	
	public IntRange getPageRange() {
		return pageRange;
	}
	public void setPageRange(IntRange pageRange) {
		this.pageRange = pageRange;
	}
	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}
	public OrderResult getOrderbyPart() {
		return orderbyPart;
	}
	public void setOrderbyPart(OrderResult orderbyPart) {
		this.orderbyPart = orderbyPart;
	}
	public List<BindVariableDescription> getBind() {
		return bind;
	}
	public void setBind(List<BindVariableDescription> bind) {
		this.bind = bind;
	}
	@Override
	public String toString() {
		return getSql(null).getSql();
	}
	public BindSql getSql(PartitionResult site) {
		return new BindSql(withPage(body.concat(orderbyPart.getSql())),bind);
	}
	private DatabaseDialect profile;
	public QuerySqlResultSimple(DatabaseDialect profile,boolean isUnion){
		this.profile=profile;
		this.isUnion=isUnion;
	}

	private String withPage(String sql) {
		if (pageRange != null) {
			return profile.toPageSQL(sql, pageRange,isUnion);
		}
		return sql;
	}
	static final PartitionResult[] P=new PartitionResult[]{new PartitionResult("")};
	public PartitionResult[] getTables() {
		return null;
	}
	public SelectResult getSelectPart() {
		return null;
	}
	public CacheKey getCacheKey() {
		return null;
	}
}

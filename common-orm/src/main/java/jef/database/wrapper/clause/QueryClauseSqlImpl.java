package jef.database.wrapper.clause;

import java.util.List;

import jef.common.wrapper.IntRange;
import jef.database.BindVariableDescription;
import jef.database.annotation.PartitionResult;
import jef.database.cache.CacheKey;
import jef.database.dialect.DatabaseDialect;

public class QueryClauseSqlImpl implements IQueryClause {
	private String body;
	private OrderClause orderbyPart;
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
	public OrderClause getOrderbyPart() {
		return orderbyPart;
	}
	public void setOrderbyPart(OrderClause orderbyPart) {
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
	public QueryClauseSqlImpl(DatabaseDialect profile,boolean isUnion){
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
	public SelectPart getSelectPart() {
		return null;
	}
	public CacheKey getCacheKey() {
		return null;
	}
	public boolean isGroupBy() {
		return false;
	}
	public boolean isEmpty() {
		return false;
	}
	public boolean isMultiDatabase() {
		return false;
	}
	public GroupClause getGrouphavingPart() {
		return GroupClause.EMPTY;
	}
}

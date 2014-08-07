package jef.database.wrapper;

import jef.common.wrapper.IntRange;
import jef.database.annotation.PartitionResult;
import jef.database.cache.CacheKeyProvider;


public interface IQuerySqlResult extends SqlResult,CacheKeyProvider{
	BindSql getSql(PartitionResult site);
	PartitionResult[] getTables();
	OrderResult getOrderbyPart();
	SelectResult getSelectPart() ;
	boolean isGroupBy();
	boolean isEmpty();
	void setOrderbyPart(OrderResult orderClause);
	void setPageRange(IntRange range);
}

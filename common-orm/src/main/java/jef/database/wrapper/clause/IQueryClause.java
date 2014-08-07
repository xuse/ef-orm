package jef.database.wrapper.clause;

import jef.common.wrapper.IntRange;
import jef.database.annotation.PartitionResult;
import jef.database.cache.CacheKeyProvider;


public interface IQueryClause extends SqlResult,CacheKeyProvider{
	BindSql getSql(PartitionResult site);
	PartitionResult[] getTables();
	OrderClause getOrderbyPart();
	SelectPart getSelectPart() ;
	boolean isGroupBy();
	boolean isEmpty();
	void setOrderbyPart(OrderClause orderClause);
	void setPageRange(IntRange range);
	boolean isMultiDatabase();
	GroupClause getGrouphavingPart(); 
}

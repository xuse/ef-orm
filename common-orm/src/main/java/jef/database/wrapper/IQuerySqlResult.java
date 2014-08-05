package jef.database.wrapper;

import jef.database.annotation.PartitionResult;
import jef.database.cache.CacheKeyProvider;


public interface IQuerySqlResult extends SqlResult,CacheKeyProvider{
	BindSql getSql(PartitionResult site);
	PartitionResult[] getTables();
	OrderResult getOrderbyPart();
	SelectResult getSelectPart() ;
}

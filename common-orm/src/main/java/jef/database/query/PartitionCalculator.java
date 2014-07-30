package jef.database.query;

import jef.database.IQueryableEntity;
import jef.database.annotation.PartitionResult;
import jef.database.innerpool.PartitionSupport;
import jef.database.meta.ITableMetadata;

/**
 * 分表规则计算器
 * @author Administrator
 *
 */
public interface PartitionCalculator {
	/**
	 * 根据类获得表名列表，支持分表
	 * @param cls     entity类
	 * @param customName 指定强制返回名称
	 * @param instance
	 * @param q
	 * @param processor
	 * @return
	 * @see PartitionResult
	 */
	PartitionResult[] toTableNames(ITableMetadata meta, IQueryableEntity instance, Query<?> q, PartitionSupport processor);
	
	/**
	 * 计算Query对应的表名
	 * @param cls   entity类
	 * @param customName  指定强制返回名称
	 * @param instance
	 * @param q
	 * @param processor
	 * @return
	 * @see PartitionResult
	 */
	PartitionResult toTableName(ITableMetadata meta, IQueryableEntity instance, Query<?> q,PartitionSupport processor);
	/**
	 * 在无实例的情况下计算表名
	 * 将会计算出全部可能的实现
	 * @param meta
	 * @param processor
	 * @param includeBaseTable
	 * @return
	 */
	PartitionResult[] toTableNames(ITableMetadata meta, PartitionSupport processor,boolean includeBaseTable);
}

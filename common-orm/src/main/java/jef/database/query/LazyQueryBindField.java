package jef.database.query;

import jef.database.MetadataContainer;

/**
 * 可绑定的Field
 * 早绑定（显式绑定，即开发人员编程时就指定该Field或表达式与某个特定查询绑定）
 * 迟绑定 (隐式绑定，编程时仅指定要绑定的表类型，在执行查询时在所有的参与表中选择相同的表绑定。 这种隐式绑定不能处理同一个表在一个查询中出现多次的情况)
 * 
 * @author jiyi
 *
 */
public interface LazyQueryBindField extends MetadataContainer{
	/**
	 * 该Field是否绑定
	 * @return
	 */
	public boolean isBind();
//	/**
//	 * 绑定的表的元数据，所谓的迟绑定，其实就是根据MetaData去比对，参与Join的几张表中，哪张表metadata一致。
//	 * @return
//	 */
//	public ITableMetadata getMeta();
	
	/**
	 * 得到绑定的实例。如果是已经绑定的Field，直接返回绑定对象即可。如果是没有绑定的Field，从传入的context中选择合适的表绑定
	 * @param context Join上下文
	 * @return
	 */
	public Query<?> getInstanceQuery(AbstractEntityMappingProvider context);

	/**
	 * 显式的绑定到某个特性的查询表上
	 * @param query
	 */
	public void setBind(Query<?> query);
}

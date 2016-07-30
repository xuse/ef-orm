package jef.database.query;

import java.util.Collection;

import jef.database.Field;
import jef.database.SelectProcessor;
import jef.database.wrapper.clause.QueryClause;
import jef.database.wrapper.populator.Transformer;

/**
 * 所有查询的接口
 * @author jiyi
 *
 */
public interface ConditionQuery{
	static final String CUSTOM_TABLE_NAME="_table_name";
	static final String CUSTOM_TABLE_TYPE="_table_type";
	
	/**
	 * 清除全部的请求数据
	 * 
	 */
	void clearQuery();

	/**
	 * 获取现有排序字段
	 * 
	 * @return
	 */
	Collection<OrderField> getOrderBy();

	/**
	 * 设置排序
	 * 
	 * @param asc
	 *            true is ASC, false is DESC
	 * @param orderby
	 */
	void setOrderBy(boolean asc, Field... orderby);

	/**
	 * 添加排序
	 * 
	 * @param asc
	 *            true is ASC, false is DESC
	 * @param orderby
	 */
	void addOrderBy(boolean asc, Field... orderby);
	/**
	 * 设置最大结果集限制
	 * @param size
	 */
	void setMaxResult(int size);
	/**
	 * 设置FetchSize
	 * @param size
	 */
	void setFetchSize(int size);
	/**
	 * 设置超时时间，单位秒
	 * @param timout
	 */
	void setQueryTimeout(int timout);
	/**
	 * 最大结果数限制
	 * @return
	 */
	int getMaxResult();
	/**
	 * 缓存读取大小
	 * @return
	 */
	int getFetchSize();
	/**
	 * 请求超时时间（秒）
	 * @return
	 */
	int getQueryTimeout();
	
	/**
	 * 获得结果转换器
	 * @return
	 */
	Transformer getResultTransformer();
	
	/**
	 * 内部使用：
	 * 准备进行查询。
	 * @return
	 */
	SqlContext prepare();
	/**
	 * 内部使用：转换为查询语句无绑定
	 * @param processor
	 * @return
	 */
	QueryClause toQuerySql(SelectProcessor processor,SqlContext context,boolean order);
	
//	/**
//	 * 内部使用：转换为查询语句(绑定)
//	 * @param processor
//	 * @param context
//	 * @return
//	 */
//	QueryClause toPrepareQuerySql(SelectProcessor processor, SqlContext context,boolean order);
	
	/**
	 * 该查询的Select部分是否经过了自定义。
	 * 一般来说，使用了SelectItems或者PopulateStrategy都会使该查询成为一个复杂定义查询，而一级缓存和二级缓存对复杂定义查询不生效。
	 * @return
	 */
	boolean isSelectCustomized();
	

	/**
	 * 默认cacheable=true，此时全局缓存开关开启则会尽量使用缓存，如果确定某个查询操作不希望用缓存，则可以设置为false
	 * @param cacheAble
	 */
	void setCacheable(boolean cacheAble);
	
	/**
	 * 查询是否允许使用查询缓存。
	 * @return
	 */
	boolean isCacheable();
}

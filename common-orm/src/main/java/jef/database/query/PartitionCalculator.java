package jef.database.query;

import java.util.Map;

import jef.database.innerpool.PartitionSupport;
import jef.database.meta.AbstractMetadata;
import jef.database.routing.PartitionResult;

/**
 * 分表规则计算器
 * @author Administrator
 *
 */
public interface PartitionCalculator {
	/**
	 * 根据类获得表名列表，支持分表
	 * 
	 * 该方法一般都在查询和更新时使用。
	 * 
	 * @param meta 表结构元数据
	 * @param instance 对象实例
	 * @param q    请求
	 * @param context  上下文
	 * @return 分表计算结果
	 * @see PartitionResult
	 */
	PartitionResult[] toTableNames(Query<?> q, PartitionSupport context,boolean filter);
	
	/**
	 * 根据从SQL中分析得到的维度矢量进行路由计算
	 * @param meta
	 * @param val
	 * @param processor
	 * @return
	 */
	PartitionResult[] toTableNames(AbstractMetadata meta, Map<String,Dimension> val, PartitionSupport processor,boolean filter); 
	
	/**
	 * 根据从SQL中分析得到的维度矢量进行路由计算
	 * @param meta
	 * @param val
	 * @param processor
	 * @return
	 */
	PartitionResult toTableName(AbstractMetadata meta, Map<String,Dimension> val,PartitionSupport processor);
	
	
	
	
	
	
	
	
	/**
	 * 在无实例的情况下计算表名,将会计算出全部可能的实现
	 * 
	 * 该方法基本上都在维护表的DDL中使用。其他地方不用。
	 * 
	 * @param meta  表结构数据
	 * @param context 上下文 
	 * @param operateType 。0基表 1 分表，不含基表  2 分表+基表 3 数据库中的存在表（不含基表） 4所有存在的表
	 * @return 分表计算结果
	 */
	PartitionResult[] toTableNames(AbstractMetadata meta, PartitionSupport context,int operateType);
	
	/**
	 * 计算Query对应的表名，要求落在一个固定存在的表上。
	 * 该方法一般在插入等操作中使用。此外在Join等明确不支持多表的场合下用于返回单表
	 * 
	 * @param meta 元数据
	 * @param instance 数据实例
	 * @param q  请求
	 * @param context 相关上下文
	 * @return 计算结果
	 * @see PartitionResult
	 */
	PartitionResult toTableName(AbstractMetadata meta, Object instance, PartitionSupport context);
	
}

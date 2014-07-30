package org.easyframe.enterprise.spring;

import jef.database.IQueryableEntity;
import jef.database.meta.ITableMetadata;

/**
 * 用于生成唯一标识 的工具类
 * @author jiyi
 *
 */
public interface UniqueIdManager {
	
	/**
	 * 获取一个自增键值
	 * @param meta 区分sequence序列的关键字，如“表名”。
	 * @return 相同key的情况下，值不会重复
	 */
	long nextLong(ITableMetadata meta);
	
	/**
	 * 获取一个自增键值
	 * @param key 区分sequence序列的关键字，如“表名”。
	 * @return 相同key的情况下，值不会重复
	 */
	long nextLong(Class<? extends IQueryableEntity> key);
	
	
	/**
	 * 获取一个自增键值
	 * 不处理schema重定向
	 * @param seqName
	 * @return
	 */
	long nextLong(String seqName);

	/**
	 * 获取一个自增键值
	 * @param dbKey 指定数据源
	 * @param seqName Sequence名称
	 * @return
	 */
	long nextLong(String dbKey,String seqName);
	
	/**
	 * 获取一个自增键值,无需key,默认全局累加生成键值
	 * @return 不带参数的nextLong()方法返回的值不会重复。
	 */
	long nextLong();
	
	/**
	 * 获取一个长度为36位的不重复的文本键值
	 * @return
	 */
	String nextGUID();

}

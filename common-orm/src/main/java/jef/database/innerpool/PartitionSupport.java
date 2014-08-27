package jef.database.innerpool;

import java.sql.SQLException;
import java.util.Collection;

import jef.database.annotation.PartitionResult;
import jef.database.dialect.DatabaseDialect;
import jef.database.meta.ITableMetadata;

/**
 * 分库分表的环境上下文
 * @author jiyi
 *
 */
public interface PartitionSupport {
	/**
	 * 获得目前所有库
	 * @return 所有数据源名称
	 */
	Collection<String> getDdcNames();
	/**
	 * 得到在所有库上，某个表的全部分表。表名结果全部大写
	 * @param meta 表元模型
	 * @return  所有库上所有表。
	 */
	PartitionResult[] getSubTableNames(ITableMetadata meta);	
	/**
	 * 获得在指定数据源上的基于某张分表的所有实例表
	 * @param dbkey 数据源名称
	 * @param meta 表元模型
	 * @return 在某个数据源上所有分表
	 * @throws SQLException
	 */
	Collection<String> getSubTableNames(String dbkey,ITableMetadata meta) throws SQLException;

	
	/**
	 * 得到某个数据源的数据库方言
	 * @param dbkey 数据源名称
	 * @return 方言
	 */
	DatabaseDialect getProfile(String dbkey);
	
	/**
	 * 确保表存在，没有就创建
	 * @param dbkey  数据源名称
	 * @param table 表名
	 * @param meta  表元模型
	 * @throws SQLException
	 */
	void ensureTableExists(String dbkey,String table,ITableMetadata meta)throws SQLException;
	
	/**
	 * 询问表是否存在
	 * @param dbkey 数据源名称
	 * @param table 表名
	 * @param meta 表元模型
	 * @return 如果分表存在返回true，反之。
	 */
	boolean isExist(String dbkey, String table,ITableMetadata meta);
}

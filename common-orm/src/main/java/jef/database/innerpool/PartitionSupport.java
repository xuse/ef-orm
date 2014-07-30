package jef.database.innerpool;

import java.sql.SQLException;
import java.util.Collection;

import jef.database.dialect.DatabaseDialect;
import jef.database.meta.ITableMetadata;

public interface PartitionSupport {

	
	
	/**
	 * 获得基于某张分表的所有实例表
	 * @param pTable
	 * @return
	 * @throws SQLException
	 */
	Collection<String> getSubTableNames(String dbName,ITableMetadata pTable) throws SQLException;

	/**
	 * 获得目前所有库
	 * @return
	 */
	Collection<String> getDdcNames();
	
	/**
	 * 供分表计算器使用，是否以单站点模式（即不分库，忽略所有的database返回值）
	 * @return
	 */
	boolean isSingleSite();
	
	
	DatabaseDialect getProfile(String dbkey);
}

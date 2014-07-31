package jef.database.innerpool;

import java.sql.SQLException;
import java.util.Collection;

import jef.database.annotation.PartitionResult;
import jef.database.dialect.DatabaseDialect;
import jef.database.meta.ITableMetadata;

public interface PartitionSupport {

	

	/**
	 * 获得目前所有库
	 * @return
	 */
	Collection<String> getDdcNames();
	/**
	 * 全部大写
	 * @param meta
	 * @return
	 */
	PartitionResult[] getSubTableNames(ITableMetadata meta);	
	/**
	 * 获得基于某张分表的所有实例表
	 * @param pTable
	 * @return
	 * @throws SQLException
	 */
	Collection<String> getSubTableNames(String dbName,ITableMetadata pTable) throws SQLException;

	
	
	DatabaseDialect getProfile(String dbkey);


	void ensureTableExists(String db,String table,ITableMetadata meta)throws SQLException;
	
	
	boolean isExist(String dbName, String table,ITableMetadata meta);
}

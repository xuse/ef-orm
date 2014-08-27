package jef.database.innerpool;

import java.sql.SQLException;
import java.util.Date;

import jef.database.ConnectInfo;
import jef.database.DbMetaData;
import jef.database.dialect.DatabaseDialect;
import jef.database.meta.ITableMetadata;
import jef.database.support.MetadataEventListener;

public interface MetadataService {
	public static final Class<?>[] SIMPLE_CLASSES = new Class<?>[] { String.class, Integer.class, Long.class, Float.class, Double.class, Boolean.class, Date.class ,Object.class};

	/**
	 * 得到指定主键的metadata
	 * @param dbkey
	 * @return
	 */
	DbMetaData getMetadata(String dbkey);
	
	/**
	 * 得到数据简要表
	 * @return
	 */
	DatabaseDialect getProfile(String dbkey);
	
	/**
	 * 得到基本信息
	 * @return
	 */
	ConnectInfo getInfo(String dbkey);
	
	/**
	 * 
	 * @param dbKey
	 * @return
	 */
	PartitionSupport getPartitionSupport();
	

	/**
	 * 刷新表
	 * @param meta
	 * @param listener
	 * @throws SQLException
	 */
	void tableRefresh(ITableMetadata meta,MetadataEventListener listener) throws SQLException;
}

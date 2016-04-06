package jef.database.innerpool;

import java.util.Date;

import jef.database.ConnectInfo;
import jef.database.DbMetaData;
import jef.database.dialect.DatabaseDialect;

public interface MetadataService {
	public static final Class<?>[] SIMPLE_CLASSES = new Class<?>[] { String.class, Integer.class, Long.class, Float.class, Double.class, Boolean.class, Date.class, Object.class,
			Integer.TYPE, Long.TYPE, Short.TYPE, Boolean.TYPE, Float.TYPE, Double.TYPE, Character.TYPE };

	/**
	 * 得到指定主键的metadata
	 * 
	 * @param dbkey
	 * @return
	 */
	DbMetaData getMetadata(String dbkey);

	/**
	 * 得到数据简要表
	 * 
	 * @return
	 */
	DatabaseDialect getProfile(String dbkey);

	/**
	 * 得到基本信息
	 * 
	 * @return
	 */
	ConnectInfo getInfo(String dbkey);
}

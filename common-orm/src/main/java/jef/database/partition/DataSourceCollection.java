package jef.database.partition;

import java.util.Collection;

/**
 * 描述多个数据源的配置
 * @author Administrator
 *
 */
public interface DataSourceCollection {
	
	/**
	 * 是否为单独的datasource
	 * @return
	 */
	boolean isSingleDatasource();
	
	/**
	 * 获取所有的DataSource
	 * @return
	 */
	Collection<javax.sql.DataSource> getDataSources();
	
	
	/**
	 * 获得缺省的DataSource名称
	 * @return
	 */
	String getDefaultDsName();
}

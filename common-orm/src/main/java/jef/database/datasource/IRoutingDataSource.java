package jef.database.datasource;

import java.sql.SQLException;
import java.util.Map.Entry;
import java.util.Set;

import javax.sql.DataSource;

import jef.common.Callback;

/**
 * 
 * @author jiyi
 *
 */
public interface IRoutingDataSource{
	/**
	 * 询问目前是否只有一个datasource
	 * @return
	 */
	boolean isSingleDatasource();
	/**
	 * 返回所有路由的数据源名称
	 * @return
	 */
	Set<String> getDataSourceNames();
	/**
	 * 得真正的datasource
	 * @param lookupKey
	 * @return
	 */
	DataSource getDataSource(String lookupKey);

	/**
	 * 得到缺省的datrasoruce
	 * @return
	 */
	Entry<String,DataSource> getDefaultDatasource();
	
	/**
	 * 设置初始化回调
	 */
	void setCallback(Callback<String,SQLException> callback);
}

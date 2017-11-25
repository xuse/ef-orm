package jef.database.datasource;

import java.sql.SQLException;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
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
	 * @return Is there only one datasource or not.
	 */
	boolean isSingleDatasource();
	/**
	 * 返回所有路由的数据源名称
	 * @return All datAasource names
	 */
	Set<String> getDataSourceNames();
	
	/**
	 * 得真正的datasource
	 * @param lookupKey
	 * @return 指定key的DataSource
	 * @throws NoSuchElementException 指定的DataSource 没有找到
	 */
	DataSource getDataSource(String lookupKey) throws NoSuchElementException;

	/**
	 * @return 得到缺省的datrasoruce
	 */
	Entry<String,DataSource> getDefaultDatasource();
	
	/**
	 * 设置初始化回调
	 * @param callback
	 */
	void setCallback(Callback<String,SQLException> callback);
}

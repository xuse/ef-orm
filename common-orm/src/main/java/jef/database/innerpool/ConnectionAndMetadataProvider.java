package jef.database.innerpool;

import java.sql.SQLException;
import java.util.Collection;

import jef.common.Callback;

public interface ConnectionAndMetadataProvider extends MetadataService{
	/**
	 * 返回全部的datasource名称。
	 * 對於只支持但数据源2的连接池实现，返回空集合即可
	 * @return
	 */
	Collection<String> getAllDatasourceNames();
	
	/**
	 * 是否为路由池
	 * @return 如果是路由的连接池返回true，反之
	 */
	boolean isRouting();
	
	/**
	 * 可以注册一个回调函数，当数据库在首次初始化的时候执行
	 * @param callback 回调函数
	 */
	void registeDbInitCallback(Callback<String, SQLException> callback);
	
	
	/**
	 * 获得连接
	 * @return
	 */
	IConnection get();
}

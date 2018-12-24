package com.github.geequery.support.spring;

import jef.database.datasource.RoutingDataSource;

public interface TransactionProvider {
	
	/**
	 * 生成用于在日志中显示当前连接所在数据库（事务）等信息的代码
	 * @param parentName
	 * @return
	 */
	String getTransactionId(String parentName);
	
	/**
	 * 是否为路由数据源
	 * @return
	 */
	boolean isRouting();
	
	/**
	 * 如果是多源数据提供，返回RoutingDataSource
	 * @return
	 */
	RoutingDataSource getRoudingDataSource();
}

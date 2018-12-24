package com.github.geequery.support.spring;

import jef.database.datasource.RoutingDataSource;

public interface MultiDataSourceProvider {
	/**
	 * get the multiple-datasource.
	 * @return Multiple-datasource
	 */
	RoutingDataSource getRoutingDataSource();
}

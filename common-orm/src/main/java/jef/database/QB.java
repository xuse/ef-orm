package jef.database;

import jef.database.query.QueryBuilder;



/**
 * 继承QueryBuilder,相当于提供一个QueryBuilder的别名
 * 无实际内容，仅仅为了在拼装查询时可以少打几个字
 * @author Administrator
 *
 */
public final class QB extends QueryBuilder{
	private QB(){}
}

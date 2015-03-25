package jef.database.routing.sql;

import jef.database.routing.PartitionResult;

/**
 * 执行计划
 * @author jiyi
 *
 */
public interface ExecutionPlan {

	/**
	 * 是否为多库查询。
	 * @return
	 */
	boolean isMultiDatabase();
	
	/**
	 * 是否为单库单表查询
	 * @return
	 */
	boolean isSimple();
	
	/**
	 * 得到路由结果
	 * @return
	 */
	PartitionResult[] getSites();
	
	/**
	 * 得到在指定表上的SQL语句
	 * @param table
	 * @return
	 */
	public String getSql(String table);
	
}

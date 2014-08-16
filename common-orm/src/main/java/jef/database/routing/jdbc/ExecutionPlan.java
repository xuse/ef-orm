package jef.database.routing.jdbc;

import jef.database.OperateTarget;
import jef.database.annotation.PartitionResult;

public interface ExecutionPlan {

	/**
	 * 是否为多库查询。
	 * @return
	 */
	boolean isMultiDatabase();

	/**
	 * 得到路由结果
	 * @return
	 */
	PartitionResult[] getSites();

	/**
	 * 执行操作
	 * @param site
	 * @param session
	 * @return
	 */
	int processUpdate(PartitionResult site, OperateTarget session);

}

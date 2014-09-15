package jef.database.routing.sql;

import java.sql.SQLException;
import java.util.List;

import jef.database.OperateTarget;
import jef.database.Transaction;
import jef.database.annotation.PartitionResult;
import jef.database.routing.jdbc.ParameterContext;

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
	 * 执行操作
	 * @param site
	 * @param session
	 * @return
	 */
	int processUpdate(PartitionResult site, OperateTarget session) throws SQLException;
	
	/**
	 * 非分库分表，仅切换数据源即可
	 * @return
	 */
	String isChangeDatasource();
	
	public String getSql(String table);
	
}

package jef.database.routing.sql;

import jef.common.wrapper.IntRange;
import jef.database.dialect.statement.ResultSetLaterProcess;
import jef.database.wrapper.result.MultipleResultSet;

/**
 * 在内存中进行计算处理的运算提供者
 * 
 */
public interface InMemoryOperateProvider {
	/**
	 * 是否有需要在内存中计算的任务
	 * @return
	 */
	boolean hasInMemoryOperate();

	/**
	 * 根据查询结果，对照查询语句分析，是否需要内存操作
	 * @return  return true if need In Memory Process.
	 */
	void parepareInMemoryProcess(IntRange range, MultipleResultSet rs);
	
	
	/**
	 * 是否结果集要倒序？
	 * @return
	 */
	ResultSetLaterProcess isReverseResult();
}

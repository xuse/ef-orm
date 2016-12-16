package jef.database;

import java.sql.SQLException;

public interface ILazyLoadContext {

	/**
	 * 返回任务编号，-1表示无需执行
	 */
	int needLoad(String fieldname);
	/**
	 * 执行指定编号的延迟加载任务
	 *  
	 * @return 如果全部延迟加载任务都执行完毕了，返回true
	 */
	boolean process(DataObject dataObject, int fieldname)throws SQLException;
	
	/**
	 * 获取处理器
	 * @return
	 */
	LazyLoadProcessor getProcessor();
	
	/**
	 * 直接标记为无需加载
	 * @param id
	 */
	void markProcessed(String field);
}

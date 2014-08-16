package jef.database.wrapper.clause;

import java.sql.SQLException;
import java.util.List;

import jef.database.rowset.CachedRowSetImpl;
import jef.database.rowset.Row;
import jef.http.client.support.CommentEntry;

/**
 * 在内存中实现结果集的递归查询
 * 
 * 对于Oracle或Postgres的递归查询，在不支持的数据库上或者在分库分表后，只能通过人工在内存中连接来支持。
 * 
 *  start with connectBy如果去掉，可以理解是查出了整个树的所有节点。本方法的目的就是过滤掉那些挂不到树上去的节点。
 *  
 * @author jiyi
 * TODO implement it
 */
public class InMemoryStartWithConnectBy implements InMemoryProcessor{
	private Object startWith;
	
	//可以设置多个连接字段.格式如
	// 子节点上的父节点属性  = 父节点上的ID
	//  parentid=id
	//  type=type
	private List<CommentEntry> connectBy;
	
	
	public void process(CachedRowSetImpl rows) throws SQLException {
		
		
	}
	
	static class RowNode{
		private Object key;
		private Row row;
	}

	public String getName() {
		return "CONNECT_BY";
	}
	
	

}

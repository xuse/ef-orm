package jef.database.wrapper.result;

import java.sql.ResultSet;
import java.sql.Statement;

import jef.database.DbUtils;
import jef.database.OperateTarget;

public final class ResultSetHolder {
	private Statement st;
	ResultSet rs;
	OperateTarget db;

	public OperateTarget getDb() {
		return db;
	}
	public ResultSetHolder(OperateTarget tx,Statement st,ResultSet rs) {
		this.db=tx;
		this.st=st;
		this.rs=rs;
	}
	
	public void close(boolean closeResultSet) {
		if(closeResultSet && rs!=null){
			DbUtils.close(rs);
			rs=null;
		}
		if(st!=null){
			DbUtils.close(st);
			st=null;	
		}
		if(db!=null){
			db.releaseConnection();
			//而目前设计约束凡是用户持有游标的场景，必须嵌套到一个内部的事务中去。因此实际上不会出现非当前线程的方法来释放连接的可能。
			//如果是为了持有结果集专门设计的连接，那么直接就关闭掉			
			if(db.isResultSetHolderTransaction()){
				db.commitAndClose();
			}	
			db=null;
		}
	}
}

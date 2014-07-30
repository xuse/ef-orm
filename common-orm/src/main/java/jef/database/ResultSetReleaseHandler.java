package jef.database;

import java.sql.SQLException;
import java.sql.Statement;


/**
 * 某些情况下，查询结果集（游标）不会被尽快关闭，需要将其在整个用户操作过程中保持住。
 * 相应的Statement和Connection也都不能释放。必须等到结果集关闭的时候才能释放。为了保证这一设计约束，我们将ResultSetReleaseHandler存储在自行封装的ResultSet里。
 * 
 * 然后JEF的内部优化机制中，如果是分配给线程的非事务连接，即便这个连接没有被归还，下次同一线程也会再次或者这个链接（这种设计优化有无必要？不管怎么说现在是这样做的）
 * 这种设计下没有归还的连接有可能在下一次的操作中被取走然后归还。此时相关的结果集还不想关闭。
 * 为了避免这种情况的发生，凡是游标需要保持打开的场景中.
 * 
 * 
 * @author jiyi
 *
 */
public final class ResultSetReleaseHandler {
	OperateTarget db;
	private Statement st;
	
	public ResultSetReleaseHandler(OperateTarget db,Statement st){
		this.db=db;
		this.st=st;
	}
	
	public void release(){
		DbUtils.close(st);
		if(db!=null){
			db.releaseConnection();//释放，关于是否有可能在其他线程中释放的问题：这种情况只会发生在用户持有游标的场景下，ORM框架自身关闭结果集一定是在同一个线程中的。（目前不考虑多线程拼装结果）
			//而目前设计约束凡是用户持有游标的场景，必须嵌套到一个内部的事务中去。因此实际上不会出现非当前线程的方法来释放连接的可能。
			if(db.isResultSetHolderTransaction()){//如果是为了持有结果集专门设计的连接，那么直接就关闭掉
				 try {
					db.commitAndClose();
				} catch (SQLException e) {
					throw DbUtils.toRuntimeException(e);
				}
			}
			db=null;
		}
	}

	public static void release(OperateTarget db, Statement st) {
		if(st!=null){
			try{
				st.close();
			}catch(SQLException e){//Do nothing.
			}
		}
		if(db!=null){
			db.releaseConnection();
		}
	}

	public OperateTarget getDb() {
		return db;
	}
}

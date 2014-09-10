package jef.database.innerpool;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import jef.database.DbUtils;

public final class Savepoints implements Savepoint{
	private final List<Entry<Connection,Savepoint>> list=new ArrayList<Entry<Connection,Savepoint>>(5);
	private String name;
	private int id=-1;

	/**
	 * 构造，带ID
	 * @param id
	 */
	public Savepoints(int id) {
		this.id=id;
	}
	/**
	 * 构造，带名称
	 * @param name
	 */
	public Savepoints(String name) {
		this.name=name;
	}
	
	/**
	 * 构造单连接带名称
	 * @param name 恢复点名，可为null
	 * @param conn
	 * @throws SQLException
	 */
	public Savepoints(String name,Connection conn) throws SQLException {
		this.name=name;
		Savepoint setSavepoint;
		if(name==null){
			setSavepoint=conn.setSavepoint();
			this.id=setSavepoint.getSavepointId();
		}else{
			setSavepoint=conn.setSavepoint(name);
		}
		list.add(new jef.common.Entry<Connection,Savepoint>(conn,setSavepoint));
	}
	
	/**
	 * 构造，单连接不带名称
	 * @param conn
	 * @throws SQLException
	 */
	public Savepoints(Connection conn) throws SQLException {
		Savepoint setSavepoint=conn.setSavepoint(name);
		this.id=setSavepoint.getSavepointId();
		list.add(new jef.common.Entry<Connection,Savepoint>(conn,setSavepoint));
	}

	public void add(Connection conn,Savepoint point){
		list.add(new jef.common.Entry<Connection,Savepoint>(conn,point));
	}
	
	public void releaseSavepoints()throws SQLException{
		List<SQLException> error=new ArrayList<SQLException>(list.size());
		for(Entry<Connection,Savepoint> e:list){
			try{
				e.getKey().releaseSavepoint(e.getValue());
			}catch(SQLException ex){
				error.add(ex);
			}
		}
		if(!error.isEmpty()){
			throw DbUtils.wrapExceptions(error);
		}
	}
	
	public void rollbackSavepoints()throws SQLException{
		List<SQLException> error=new ArrayList<SQLException>(list.size());
		for(Entry<Connection,Savepoint> e:list){
			try{
				e.getKey().rollback(e.getValue());
			}catch(SQLException ex){
				error.add(ex);
			}
		}
		if(!error.isEmpty()){
			throw DbUtils.wrapExceptions(error);
		}
	}
	@Override
	public int getSavepointId() throws SQLException {
		if(name!=null){
			throw new SQLException("the savepoint has a String name.");
		}
		return id;
	}
	@Override
	public String getSavepointName() throws SQLException {
		if(name==null){
			throw new SQLException("the savepoint is unnamed.");
		}
		return name;
	}
}

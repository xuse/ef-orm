package jef.database.innerpool;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import jef.database.DbUtils;
import jef.tools.Assert;

final class Savepoints implements Savepoint{
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
		Assert.notNull(name);
		this.name=name;
	}

	public void add(Connection conn,Savepoint point){
		list.add(new jef.common.Entry<Connection,Savepoint>(conn,point));
	}
	
	public void doRelease()throws SQLException{
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
	
	public void doRollback()throws SQLException{
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

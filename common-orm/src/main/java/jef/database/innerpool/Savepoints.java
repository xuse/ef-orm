package jef.database.innerpool;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import jef.database.DbUtils;

public final class Savepoints {
	private final List<Entry<Connection,Savepoint>> list=new ArrayList<Entry<Connection,Savepoint>>(5);

	public Savepoints() {
	}
	public Savepoints(Connection conn,Savepoint setSavepoint) {
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
}

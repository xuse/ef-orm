package jef.database.innerpool;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.sql.DataSource;

import jef.database.DbUtils;
import jef.tools.StringUtils;

/**
 * 最简单的路由Connection实现
 * @author jiyi
 *
 */
final class RoutingConnection implements ReentrantConnection{
	private String defaultKey=null;
	private boolean autoCommit;
	private boolean readOnly;
	private int isolation=-1;
	
	private IRoutingConnectionPool parent;
	private Map<String,Connection> connections=new HashMap<String,Connection>();
	private String key;
	
	public RoutingConnection(IRoutingConnectionPool parent) {
		this.parent=parent;
	}

	public void closePhysical(){
		if(parent!=null){
			IRoutingConnectionPool parent=this.parent;
			for(Entry<String,Connection> entry :connections.entrySet()){
				String key=entry.getKey();
				parent.putback(key, entry.getValue());
			}
			connections.clear();//全部归还
		}
	}

	public final void ensureOpen() throws SQLException {
		if(parent==null){
			throw new SQLException("Current Routing Connection is closed already!");
		}
	}

	
	//一般标记为事务的开始
	public void setAutoCommit(boolean flag) throws SQLException {
		if(autoCommit==flag){
			return;
		}
		this.autoCommit=flag;
		for(Connection conn:connections.values()){
			if(conn.getAutoCommit()!=flag){
				conn.setAutoCommit(flag);
			}
		}
	}
	
	//
	public void setReadOnly(boolean flag) throws SQLException {
		if(readOnly==flag){
			return;
		}
		readOnly=flag;
		for(Connection conn:connections.values()){
			if(conn.isReadOnly()!=flag){
				conn.setReadOnly(flag);
			}
		}
	}
	

	public void setKey(String key) {
		if(key!=null && key.length()==0){
			key=null;
		}
		this.key=key;
	}
	

	public void commit() throws SQLException {
		ensureOpen();
		List<String> successed=new ArrayList<String>();
		SQLException error=null;
		String errDsName=null;
		for(Map.Entry<String,Connection> entry:connections.entrySet()){
			try{
				entry.getValue().commit();
				successed.add(entry.getKey());
			}catch(SQLException e){
				errDsName=entry.getKey();
				error=e;
				break;
			}
		}
		if(error==null){
			return;
		}
		if(successed.isEmpty()){//第一个连接提交就出错，事务依然保持一致
			throw error;
		}else{					//第success.size()+1个连接提交出错
			String message=StringUtils.concat("Error while commit data to datasource [",errDsName,"], and this is the ",
					String.valueOf(successed.size()+1),"th commit of ",String.valueOf(connections.size()),
					", there must be some data consistency problem, please check it.");
			SQLException e=new SQLException(message,error);
			e.setNextException(error);
			throw e;
		}
	}
	public void rollback() throws SQLException {
		ensureOpen();
		List<String> successed=new ArrayList<String>();
		SQLException error=null;
		String errDsName=null;
		for(Map.Entry<String,Connection> entry:connections.entrySet()){
			try{
				entry.getValue().rollback();
				successed.add(entry.getKey());
			}catch(SQLException e){
				errDsName=entry.getKey();
				error=e;
				break;
			}
		}
		if(error==null){
			return;
		}
		if(successed.isEmpty()){//第一个连接提交就出错，事务依然保持一致
			throw error;
		}else{					//第success.size()+1个连接提交出错
			String message=StringUtils.concat("Error while rollback data to datasource [",errDsName,"], and this is the ",
					String.valueOf(successed.size()+1),"th rollback of ",String.valueOf(connections.size()),
			", there must be some data consistency problem, please check it.");
			SQLException e=new SQLException(message,error);
			e.setNextException(error);
			throw e;
		}		
	}
	

	

	public Savepoints setSavepoints(String savepointName) throws SQLException {
		ensureOpen();
		List<SQLException> errors=new ArrayList<SQLException>();
		Savepoints sp=new Savepoints();
		for(Connection conn:connections.values()){
			try{
				Savepoint s=conn.setSavepoint(savepointName);
				sp.add(conn, s);
			}catch(SQLException e){
				errors.add(e);
			}
		}
		if(!errors.isEmpty()){
			throw DbUtils.wrapExceptions(errors);
		}
		return sp;
	}


	Connection getConnection() throws SQLException {
		if(key==null){
			if(defaultKey==null){
				Entry<String,DataSource> ds=parent.getRoutingDataSource().getDefaultDatasource();
				defaultKey=ds.getKey();
			}
			return getConnectionOfKey(defaultKey);
		}else{
			return getConnectionOfKey(key);
		}
	}
	
	/**
	 * 实现
	 * @param key
	 * @return
	 * @throws SQLException
	 */
	private Connection getConnectionOfKey(String key) throws SQLException{
		Connection conn=connections.get(key);
		if(conn==null){
			do{
				conn=parent.getCachedConnection(key);
				if(conn.isClosed()){
					DbUtils.closeConnection(conn);
					conn=null;
				}	
			}while(conn==null);	
			
			
			if(conn.isReadOnly()!=readOnly){
				conn.setReadOnly(readOnly);
			}
			if(isolation>=0){
				conn.setTransactionIsolation(isolation);
			}
			if(conn.getAutoCommit()!=autoCommit){
				conn.setAutoCommit(autoCommit);
			}
			connections.put(key, conn);
		}
		return conn;
	}

	public Statement createStatement(int resultSetType, int resultSetConcurrency)
			throws SQLException {
		return getConnection().createStatement(resultSetType, resultSetConcurrency);
	}

	public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
			throws SQLException {
		return getConnection().prepareStatement(sql, autoGeneratedKeys);
	}

	public PreparedStatement prepareStatement(String sql, int resultSetType,
			int resultSetConcurrency) throws SQLException {
		return getConnection().prepareStatement(sql, resultSetType, resultSetConcurrency);
	}

	public PreparedStatement prepareStatement(String sql, String[] columnNames)
			throws SQLException {
		return getConnection().prepareStatement(sql, columnNames);
	}

	public Statement createStatement() throws SQLException {
		return getConnection().createStatement();
	}

	public PreparedStatement prepareStatement(String sql) throws SQLException {
		return getConnection().prepareStatement(sql);
	}

	public CallableStatement prepareCall(String sql) throws SQLException {
		return getConnection().prepareCall(sql);
	}

	public DatabaseMetaData getMetaData() throws SQLException {
		return getConnection().getMetaData();
	}

	private volatile Object used;
	private volatile int count;
	
	public void setUsedByObject(Object user) {
		this.used=user;
		count++;
	}
	
	public Object popUsedByObject() {
		if(--count>0){ 
//			System.out.println("不是真正的归还"+used+"还有"+count+"次使用.");
			return null;
		}else{
			Object o=used;
			used=null;
			return o;
		}
	}

	public void addUsedByObject() {
		count++;
	}

	public boolean isUsed() {
		return count>0;
	}

	public void notifyDisconnect() {
	}

	public int getTransactionIsolation() throws SQLException {
		return isolation;
	}

	public void setTransactionIsolation(int level) throws SQLException {
		if(level<0 || level==isolation){
			return;
		}
		this.isolation=level;
		for(Connection conn:connections.values()){
			if(conn.getTransactionIsolation()!=level){
				conn.setTransactionIsolation(level);
			}
		}
	}
}

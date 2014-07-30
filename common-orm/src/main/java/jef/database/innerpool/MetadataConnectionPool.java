package jef.database.innerpool;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import jef.common.pool.PoolStatus;
import jef.database.DbUtils;
import jef.database.datasource.SimpleDataSource;

/**
 * 这个池是专门为了持有metadata所有的连接而设计的
 * @author jiyi
 *
 */
final class MetadataConnectionPool implements IPool<Connection>,CheckablePool{
	private DataSource ds;
	private Connection conn;
	private final AtomicInteger using=new AtomicInteger();
	private MetadataService parent;
	private String dbKey;

	MetadataConnectionPool(String dbkey,DataSource s,MetadataService service){
		this.dbKey=dbkey;
		this.ds=s;
		this.parent=service;
		PoolCheckThread.getInstance().addPool(this);
	}
	
	public Connection poll() throws SQLException {
		using.incrementAndGet();
		if(conn==null || conn.isClosed()){
			conn=createConnection();
		}
		return conn;
	}

	public void offer(Connection conn) {
		using.decrementAndGet();
	}
	
	private synchronized Connection createConnection() throws SQLException {
		if((ds instanceof SimpleDataSource)&&parent.hasRemarkFeature(dbKey)){
			Properties props = new Properties();
			props.put("remarksReporting", "true");
			return ((SimpleDataSource) ds).getConnectionFromDriver(props);
		}else{
			return ds.getConnection();
		}
	}

	public PoolStatus getStatus() {
		return new PoolStatus(1, 0, conn==null?0:1, using.get(), 0);
	}

	public DataSource getDatasource() {
		return ds;
	}

	/**
	 * 如果连接一段时间不用，就关闭掉
	 */
	public synchronized void closeConnectionTillMin() {
		if(conn!=null && using.get()==0){
			Connection c=conn;
			conn=null;
			DbUtils.closeConnection(c);
		}
	}

	public void close() throws SQLException {
		if(conn!=null){
			DbUtils.closeConnection(conn);	
		}
		ds=null;
	}

	public Iterable<? extends CheckableConnection> getConnectionsToCheck() {
		return Collections.singletonList(new SingleConnection(conn,this));
	}

	private String testSQL;
	
	public String getTestSQL() {
		return testSQL;
	}

	public void setTestSQL(String string) {
		this.testSQL=string;
	}
}

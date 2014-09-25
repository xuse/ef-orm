package jef.database.innerpool;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import jef.common.pool.PoolStatus;
import jef.database.DbUtils;
import jef.database.datasource.SimpleDataSource;

/**
 * 
 * @author jiyi
 *
 */
public abstract class MetadataConnectionPool implements IPool<Connection>,CheckablePool{
	public final Set<String> checkedFunctions=new HashSet<String>();
	protected DataSource ds;
	private Connection conn;
	private final AtomicInteger using=new AtomicInteger();

	protected MetadataConnectionPool(DataSource s){
		this.ds=s;
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
		if((ds instanceof SimpleDataSource)&& hasRemarkFeature()){
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
	
	protected abstract String getTestSQL();

	protected abstract boolean hasRemarkFeature();

	protected abstract boolean processCheck(Connection conn2);
	
	public void doCheck() {
		if(conn!=null){
			Connection con=this.conn;
			if(!processCheck(con)){
				this.conn=null;
				DbUtils.closeConnection(con);
			}
		}
	}
}

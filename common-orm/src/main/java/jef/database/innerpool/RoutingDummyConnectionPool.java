package jef.database.innerpool;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import javax.sql.DataSource;

import jef.common.Callback;
import jef.common.pool.PoolStatus;
import jef.database.ConnectInfo;
import jef.database.DbMetaData;
import jef.database.DbUtils;
import jef.database.ORMConfig;
import jef.database.datasource.IRoutingDataSource;
import jef.database.dialect.DatabaseDialect;

import org.easyframe.enterprise.spring.TransactionMode;

import com.google.common.collect.MapMaker;

/**
 * 启用了外部连接池后的路由伪连接池。
 * 实际上所有真正的连接都是从外部连接池中获取的。每次释放的时候这些连接都被关闭（即释放回外部连接池）
 * 
 * 
 * @author jiyi
 *
 */
public class RoutingDummyConnectionPool implements IRoutingConnectionPool{
	protected IRoutingDataSource datasource;
	final Map<Object, RoutingConnection> usedConnection=new MapMaker().concurrencyLevel(12).weakKeys().makeMap();
	private final AtomicLong pollCount=new AtomicLong();
	private final AtomicLong offerCount=new AtomicLong();
	private final Map<String,DbMetaData> metadatas=new HashMap<String,DbMetaData>(8,0.5f);
	
	public RoutingDummyConnectionPool(IRoutingDataSource ds){
		this.datasource=ds;
	}
	
	public RoutingConnection getConnection(Object transaction) throws SQLException {
		pollCount.incrementAndGet();
		RoutingConnection conn=usedConnection.get(transaction);
		if(conn==null){
			conn=new RoutingConnection(this,ORMConfig.getInstance().isJpaContinueCommitIfError());
			conn.ensureOpen();//对于Routing连接来说，监测是否有效是没有意义的
			usedConnection.put(transaction, conn);
			conn.setUsedByObject(transaction);
		}else{
			conn.addUsedByObject();
		}
		return conn;
	}

	public RoutingConnection poll() throws SQLException {
		return getConnection(Thread.currentThread());
	}
	
	public void offer(ReentrantConnection conn){
		offerCount.incrementAndGet();
		if(conn!=null){
			//处理内部的记录数据
			Object o=conn.popUsedByObject();
			if(o==null)return;//不是真正的归还
			IConnection conn1=usedConnection.remove(o);
			conn.closePhysical();
			if(conn1!=conn){
				throw  new IllegalStateException("The connection returned not match.");
			}
		}
	}
	
	public void close() throws SQLException {
		for(IConnection conn: usedConnection.values()){
			conn.closePhysical();	
		}
		usedConnection.clear();
		PoolService.logPoolStatic(getClass().getSimpleName(),pollCount.get(), offerCount.get());
	}

	public DataSource getDatasource() {
		throw new UnsupportedOperationException();
	}

	public PoolStatus getStatus() {
		int size=usedConnection.size();
		return new PoolStatus(0,0,size,size,0);
	}

	public Collection<String> getAllDatasourceNames() {
		return datasource.getDataSourceNames();
	}

	public IRoutingDataSource getRoutingDataSource() {
		return datasource;
	}

	/**
	 * 默认实现，从不缓存连接
	 * @throws SQLException 
	 */
	public Connection getCachedConnection(String ds) throws SQLException {
		return datasource.getDataSource(ds).getConnection();
	}

	/**
	 * 默认实现，从不缓存连接，连接直接关闭（使用外部连接池的场景）
	 */
	public void putback(String ds, Connection conn) {
		DbUtils.closeConnection(conn);
	}


	public void closeConnectionTillMin() {
		//无需任何功能
	}

	public DbMetaData getMetadata(String dbkey) {
		dbkey=wrapNullKey(dbkey);
		DbMetaData meta=metadatas.get(dbkey);
		if(meta!=null)return meta;
		meta=createMetadata(dbkey);
		return meta;
	}

	private String wrapNullKey(String dbkey) {
		if(dbkey!=null){
			return dbkey;
		}
		Entry<String,DataSource> e=datasource.getDefaultDatasource();
		if(e!=null){
			return e.getKey();
		}else{
			throw new IllegalArgumentException("No default datasource found in "+datasource+"!");
		}
	}
	
	private synchronized DbMetaData createMetadata(String key) {
		DataSource ds=datasource.getDataSource(key);//必须放在双重检查锁定逻辑的外面，否则会因为回调对象的存在而造成元数据对象初始化两遍。。。
		
		DbMetaData meta=metadatas.get(key);
		if(meta==null){
			meta=new DbMetaData(ds,this,key);
			metadatas.put(key, meta);
		}
		return meta;
	}

	public DatabaseDialect getProfile(String dbkey) {
		return getMetadata(dbkey).getProfile();
	}

	public ConnectInfo getInfo(String dbkey) {
		return getMetadata(dbkey).getInfo();
	}


	public boolean isRouting() {
		return true;
	}

	public void registeDbInitCallback(Callback<String, SQLException> callback) {
		this.datasource.setCallback(callback);
	}

	public boolean isDummy() {
		return true;
	}

	private TransactionMode txMode;
	@Override
	public IUserManagedPool setTransactionMode(TransactionMode txMode) {
		this.txMode=txMode;
		return this;
	}

	@Override
	public TransactionMode getTransactionMode() {
		return txMode;
	}
}

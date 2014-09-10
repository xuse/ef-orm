package jef.database.innerpool;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.sql.DataSource;

import jef.database.datasource.DataSources;
import jef.database.datasource.IRoutingDataSource;
import jef.tools.Assert;

/**
 * 路由数据源连接池，其中每个数据源都被装在一个小池(SimplePool,DummyPool)里。
 * 每个小池按照预先的配置来管理大小。
 * 从这个连接池中获得的IConnection对象都是支持路由的。
 * 选择合适的连接进行路由等操作，都封装在IConnection内部实现了。因此对于外部使用者来说，不需要关心IConnection是指向那个真正的数据源的
 * 
 * 
 * @author jiyi
 *
 */
public final class RoutingManagedConnectionPool extends RoutingDummyConnectionPool {
	private int min;
	private int max;
	private boolean autoPool;
	
	//所有已知数据源的缓存连接，按数据源的key分别存放
	//特点，元素数量不多，用较大的空间避免元素冲突
	private final HashMap<String, IPool<Connection>> cache=new HashMap<String, IPool<Connection>>(8,0.5f);
	
	/**
	 * 构造
	 * @param ds 路由数据源
	 * @param max 连接池最大
	 * @param min 连接池最小
	 * @param autoPool
	 */
	RoutingManagedConnectionPool(IRoutingDataSource ds,int max,int min,boolean autoPool) {
		super(ds);
		this.min=min;
		this.max=max;
		this.autoPool=autoPool;
	}

	/**
	 * 从缓存中获取连接
	 */
	@Override
	public Connection getCachedConnection(String ds) throws SQLException {
		IPool<Connection> queue=cache.get(ds);
		if(queue==null){
			queue=putDsItem(ds);
		}
		return queue.poll();
	}

	/**
	 * 将连接放回缓存
	 */
	@Override
	public void putback(String ds, Connection conn){
		IPool<Connection> queue=cache.get(ds);
		if(queue==null){
			queue=putDsItem(ds);
		}
		queue.offer(conn);
	}

	private synchronized IPool<Connection> putDsItem(String ds) {
		IPool<Connection> queue=cache.get(ds);
		if(queue!=null)return queue;
		DataSource realds=datasource.getDataSource(ds);
		if(autoPool && !DataSources.isPool(realds)){
			queue=new SimplePooledDatasource(min,max,realds);	
		}else{
			queue=new DummyPool(realds);
		}
		
		IPool<Connection> old=cache.put(ds, queue);
		Assert.isNull(old);//高并发下是否可能出现其他问题，对此进行检查
		return queue;
	}

	public PrintWriter getLogWriter() throws SQLException {
		throw new UnsupportedOperationException("getLogWriter");
	}

	public void setLogWriter(PrintWriter out) throws SQLException {
		throw new UnsupportedOperationException("setLogWriter");
	}

	public void setLoginTimeout(int seconds) throws SQLException {
		throw new UnsupportedOperationException("setLoginTimeout");
	}

	public int getLoginTimeout() throws SQLException {
		return 0;
	}

	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> iface) throws SQLException {
		Assert.notNull(iface, "Interface argument must not be null");
		if (!DataSource.class.equals(iface)) {
			throw new SQLException(
					"DataSource of type [" + getClass().getName() +
							"] can only be unwrapped as [javax.sql.DataSource], not as ["
							+ iface.getName());
		}
		return (T) this;
	}

	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return false;
	}

	

	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return null;
	}

	@Override
	public boolean isDummy() {
		return false;
	}
	
	public Map<String, IPool<Connection>> getCachedPools(){
		return cache;
	}
}

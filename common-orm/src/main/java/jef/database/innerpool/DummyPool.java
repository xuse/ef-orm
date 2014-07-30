package jef.database.innerpool;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.ReflectionException;
import javax.sql.DataSource;

import jef.common.log.LogUtil;
import jef.common.pool.PoolStatus;
import jef.database.DbUtils;
import jef.tools.StringUtils;
import jef.tools.reflect.BeanUtils;

/**
 * 假的连接池实现，实际上没有池，所有连接真实打开和关闭
 * @author jiyi
 *
 */
final class DummyPool implements IPool<Connection> {
	private DataSource ds;
	private String shutdownMethod;
	private final AtomicInteger used=new AtomicInteger();
	private final AtomicInteger pollCount=new AtomicInteger();
	private final AtomicInteger offerCount=new AtomicInteger();
	
	public DummyPool(DataSource ds){
		this.ds=ds;
	}
	
	public Connection poll() throws SQLException {
		pollCount.incrementAndGet();
		Connection conn=ds.getConnection();
		used.incrementAndGet();
		return conn;
	}

	public void offer(Connection conn){
		offerCount.incrementAndGet();
		DbUtils.closeConnection(conn);
		used.decrementAndGet();
	}

	public PoolStatus getStatus() {
		int size=used.get();
		return new PoolStatus(0,0,size,size,0);
	}

	public DataSource getDatasource() {
		return ds;
	}

	public void closeConnectionTillMin() {
	}

	public void close() throws SQLException {
		if(StringUtils.isNotEmpty(shutdownMethod)){
			try {
				BeanUtils.invokeMethod(ds, shutdownMethod);
			} catch (ReflectionException e) {
				LogUtil.exception(e);
			}
		}
		PoolService.logPoolStatic(getClass().getSimpleName(),pollCount.get(), offerCount.get());
	}
}

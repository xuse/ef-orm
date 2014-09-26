package jef.database.innerpool;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.persistence.PersistenceException;
import javax.sql.DataSource;

import jef.common.Callback;
import jef.common.pool.PoolStatus;
import jef.database.ConnectInfo;
import jef.database.DbMetaData;
import jef.database.dialect.DatabaseDialect;

import com.google.common.collect.MapMaker;

/**
 * 虚连接池，实际上所有连接都是用时打开，用完关闭的。<br>
 * 当使用第三方连接池的时候使用。
 * 
 * @author jiyi
 * 
 */
final class SingleDummyConnectionPool implements IUserManagedPool{
	private DataSource ds;
	final Map<Object, SingleConnection> map = new MapMaker().concurrencyLevel(12).weakKeys().makeMap();
	private final AtomicLong pollCount = new AtomicLong();
	private final AtomicLong offerCount = new AtomicLong();
	private final DbMetaData metadata;
	
	
	public SingleDummyConnectionPool(DataSource ds) {
		this.ds = ds;
		this.metadata = new DbMetaData(ds, this,null);
		PoolReleaseThread.getInstance().addPool(this);
	}

	public SingleConnection poll() throws SQLException {
		return getConnection(Thread.currentThread());
	}

	public SingleConnection getConnection(Object transaction) throws SQLException {
		pollCount.incrementAndGet();
		SingleConnection conn = map.get(transaction);
		if (conn == null) {
			conn = new SingleConnection(ds.getConnection(), this);
			map.put(transaction, conn);
			conn.setUsedByObject(transaction);
		} else {
			conn.addUsedByObject();
		}
		// conn.ensureOpen();连接可用性由第三方连接池保证，此处不用处理
		return conn;
	}

	public void close() throws SQLException {
		for (IConnection conn : map.values()) {
			conn.closePhysical();
		}
		map.clear();
		if(metadata!=null)
			this.metadata.close();
		PoolReleaseThread.getInstance().removePool(this);
		System.out.println("pollCount:" + pollCount + " offer count:" + offerCount);
	}

	public DataSource getDatasource() {
		return ds;
	}

	public PoolStatus getStatus() {
		int size = map.size();
		return new PoolStatus(0, 0, size, size, 0);
	}

	public void offer(ReentrantConnection conn) {
		offerCount.incrementAndGet();
		if (conn != null) {
			// 处理内部的记录数据
			Object o = conn.popUsedByObject();
			if (o == null)
				return;// 不是真正归还
			IConnection conn1 = map.remove(o);
			conn.closePhysical();
			if (conn1 != conn) {
				throw new IllegalStateException("The connection returned not match.");
			}
		}
	}

	public Collection<String> getAllDatasourceNames() {
		return Collections.emptyList();
	}

	public void closeConnectionTillMin() {
		metadata.closeConnectionTillMin();
	}

	public DbMetaData getMetadata(String dbkey) {
		return metadata;
	}

	public DatabaseDialect getProfile(String dbkey) {
		return metadata.getProfile();
	}

	public ConnectInfo getInfo(String dbkey) {
		return getMetadata(dbkey).getInfo();
	}

	public void registeDbInitCallback(Callback<String, SQLException> callback) {
		if (callback != null) {
			try {
				callback.call(null);
			} catch (SQLException e) {
				throw new PersistenceException(e);
			}
		}
	}

	public boolean isRouting() {
		return false;
	}

	public boolean isDummy() {
		return true;
	}
}

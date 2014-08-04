package jef.database.innerpool;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.PersistenceException;
import javax.sql.DataSource;

import jef.common.Callback;
import jef.common.pool.PoolStatus;
import jef.database.ConnectInfo;
import jef.database.DbCfg;
import jef.database.DbMetaData;
import jef.database.DbUtils;
import jef.database.dialect.DatabaseDialect;
import jef.database.meta.Feature;
import jef.database.wrapper.AbstractPopulator;
import jef.tools.JefConfiguration;

import com.google.common.collect.MapMaker;

/**
 * 虚连接池，实际上所有连接都是用时打开，用完关闭的。<br>
 * 当使用第三方连接池的时候使用。
 * 
 * @author jiyi
 * 
 */
final class SingleDummyConnectionPool extends AbstractPopulator implements IUserManagedPool {
	private DataSource ds;
	final Map<Object, ReentrantConnection> map = new MapMaker().concurrencyLevel(12).weakKeys().makeMap();
	private final AtomicInteger pollCount = new AtomicInteger();
	private final AtomicInteger offerCount = new AtomicInteger();

	public SingleDummyConnectionPool(DataSource ds) {
		this.ds = ds;
		this.metaConn = new MetadataConnectionPool(null, ds, this);
		PoolReleaseThread.getInstance().addPool(this);
	}

	public ReentrantConnection poll() throws SQLException {
		return poll(Thread.currentThread());
	}

	public ReentrantConnection poll(Object transaction) throws SQLException {
		pollCount.incrementAndGet();
		ReentrantConnection conn = map.get(transaction);
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
		// 本来就没有池，自然无需大小控制

		// 元数据也要尽量少用连接
		metaConn.closeConnectionTillMin();
	}

	private DbMetaData metadata;
	private MetadataConnectionPool metaConn;

	public DbMetaData getMetadata(String dbkey) {
		if (metadata == null) {
			metadata = new DbMetaData(metaConn, this, null);
		}
		return metadata;
	}

	public DatabaseDialect getProfile(String dbkey) {
		return getMetadata(dbkey).getProfile();
	}

	public ConnectInfo getInfo(String dbkey) {
		return getMetadata(dbkey).getInfo();
	}

	public boolean hasRemarkFeature(String dbkey) {
		if (JefConfiguration.getBoolean(DbCfg.DB_NO_REMARK_CONNECTION, false)) {
			return false;
		}
		DatabaseDialect profile;
		if (metadata != null) {
			profile = metadata.getProfile();
		} else {
			ConnectInfo info = DbUtils.tryAnalyzeInfo(ds, false);
			if (info == null) {
				Connection conn = null;
				try {
					conn = ds.getConnection();
					info = DbUtils.tryAnalyzeInfo(conn);
				} catch (SQLException e) {
				} finally {
					DbUtils.closeConnection(conn);
				}
			}
			profile = info.getProfile();
		}
		return profile.has(Feature.REMARK_META_FETCH);
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

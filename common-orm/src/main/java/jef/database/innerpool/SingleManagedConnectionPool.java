/*
 * JEF - Copyright 2009-2010 Jiyi (mr.jiyi@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jef.database.innerpool;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;
import javax.sql.DataSource;

import jef.common.Callback;
import jef.common.log.LogUtil;
import jef.common.pool.PoolStatus;
import jef.database.ConnectInfo;
import jef.database.DbCfg;
import jef.database.DbMetaData;
import jef.database.dialect.DatabaseDialect;
import jef.database.meta.Feature;
import jef.tools.Assert;
import jef.tools.JefConfiguration;

import org.easyframe.enterprise.spring.TransactionMode;

import com.google.common.collect.MapMaker;

/**
 * 第三版本的连接池 设计简化
 * 
 * 该版本连接池实现的特性：
 * <ul>
 * <li>1、根据IUserManagedPool接口要求，对同一个线程或者事务会返回相同的连接。</li>
 * <li>2、连接池大小控制，会在指定的范围内，自动管理连接数量。</li>
 * <li>3、定时检查连接有效性</li>
 * </ul>
 */
final class SingleManagedConnectionPool implements IManagedConnectionPool, DataSource, CheckablePool {
	private DataSource ds;
	private int max;
	private int min;
	private final DbMetaData metadata;

	// TODO there's nowhrere to set this value;
	private String testSQL;

	/**
	 * 本来是使用usedConnections.size()来计算目前使用中的连接数的, 但是发现Google
	 * map在数量统计时不太靠谱,只好自行记录使用
	 */
	private final AtomicInteger used = new AtomicInteger();
	final Map<Object, ReentrantConnection> usedConnections = new MapMaker().concurrencyLevel(12).weakKeys().makeMap();

	/**
	 * 空闲连接数
	 */
	private final BlockingQueue<ReentrantConnection> freeConns;

	// 统计信息，统计拿取和设置的全不知
	private final AtomicLong pollCount = new AtomicLong();
	private final AtomicLong offerCount = new AtomicLong();

	SingleManagedConnectionPool(DataSource ds, int min, int max) {
		if (min > max)
			min = max;
		this.ds = ds;
		this.min = min;
		this.max = max;
		freeConns = new LinkedBlockingQueue<ReentrantConnection>(max);
		this.metadata = new DbMetaData(ds, this, null);
		metadata.getProfile().accept(metadata);
		PoolReleaseThread.getInstance().addPool(this);
		PoolCheckThread.getInstance().addPool(this);
	}

	public String toString() {
		return ds.toString() + getStatus().toString();
	}

	public DataSource getDatasource() {
		return ds;
	}

	public PoolStatus getStatus() {
		int used = usedConnections.size();
		int free = freeConns.size();
		PoolStatus ps = new PoolStatus(max, min, used + free, used, free);
		ps.setOfferCount(offerCount.get());
		ps.setPollCount(pollCount.get());
		return ps;
	}

	@SuppressWarnings("unchecked")
	public Collection<String> getAllDatasourceNames() {
		return Collections.EMPTY_SET;
	}

	public ReentrantConnection getConnection(Object transaction) throws SQLException {
		pollCount.incrementAndGet();
		try {
			ReentrantConnection conn = usedConnections.get(transaction);
			if (conn == null) {
				if (used.get() < max && freeConns.isEmpty()) {// 尝试用新连接
					used.getAndIncrement(); // 提前计数
					conn = new SingleConnection(ds.getConnection(), this);
					conn.setUsedByObject(transaction);
				} else {
					used.getAndIncrement(); // 提前计数，并发下为了严格阻止连接池超出上限，必须这样做
					conn = freeConns.poll(5000000000L, TimeUnit.NANOSECONDS);// 5秒
					if (conn == null) {
						used.decrementAndGet();
						throw new SQLException("No connection avaliable now." + getStatus());
					}
					conn.ensureOpen();
					conn.setUsedByObject(transaction);
				}
				usedConnections.put(transaction, conn);
			} else {
				conn.addUsedByObject();
			}
			// log(transaction,conn,"get");
			return conn;
		} catch (InterruptedException e) {
			throw new SQLException(e);
		}
	}

//	@SuppressWarnings("unused")
//	private void log(Object transaction, ReentrantConnection conn, String action) {
//		StackTraceElement[] eles = new Throwable().getStackTrace();
//		System.out.println(action + " " + conn);
//		System.out.println(eles[4]);
//		System.out.println(eles[5]);
//	}

	public ReentrantConnection poll() throws SQLException {
		return getConnection(Thread.currentThread());
	}

	public void offer(ReentrantConnection conn) {
		offerCount.incrementAndGet();
		if (conn != null) {
			Object o = conn.popUsedByObject();
			// log(o,conn,"return");

			if (o == null) {
				return;// 不是真正的归还
			}
			ReentrantConnection conn1 = usedConnections.remove(o);
			boolean success = freeConns.offer(conn);
			if (!success) {
				conn.closePhysical();
			}
			// 归还成功，才减低连接池大小
			used.decrementAndGet();
			if (conn1 != conn) {
				throw new IllegalStateException("The connection returned not match." + conn + "\t" + conn1);
			}

		}
	}

	public void close() throws SQLException {
		max = 0;
		min = 0;
		closeConnectionTillMin();
		PoolReleaseThread.getInstance().removePool(this);
		PoolService.logPoolStatic(getClass().getSimpleName(), pollCount.get(), offerCount.get());
	}

	public void closeConnectionTillMin() {
		if (freeConns.size() > min) {
			ReentrantConnection conn;
			// 注意下面两个条件顺序必须确保poll操作在后，因为poll操作会变更集合的Size
			while (freeConns.size() > min && (conn = freeConns.poll()) != null) {
				conn.closePhysical();
			}
		}
	}

	public DbMetaData getMetadata(String dbkey) {
		return metadata;
	}

	public DatabaseDialect getProfile(String dbkey) {
		return getProfile();
	}

	public ConnectInfo getInfo(String dbkey) {
		return getMetadata(dbkey).getInfo();
	}

	public DatabaseDialect getProfile() {
		return metadata.getProfile();

	}

	public boolean hasRemarkFeature(String dbkey) {
		if (JefConfiguration.getBoolean(DbCfg.DB_NO_REMARK_CONNECTION, false) || this.min > 5) {
			return false;
		}
		DatabaseDialect profile = getProfile();
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
			throw new SQLException("DataSource of type [" + getClass().getName() + "] can only be unwrapped as [javax.sql.DataSource], not as [" + iface.getName());
		}
		return (T) this;
	}

	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return false;
	}

	public Connection getConnection() throws SQLException {
		return getConnection(Thread.currentThread());
	}

	public Connection getConnection(String username, String password) throws SQLException {
		return getConnection(Thread.currentThread());
	}

	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return null;
	}

	public void notifyDbDisconnect() {
		if (LogUtil.isDebugEnabled()) {
			LogUtil.debug("Disconnected connection found, notify Checker thread.");
		}
		this.doCheck();
	}

	public boolean isRouting() {
		return false;
	}

	public boolean isDummy() {
		return false;
	}

	public synchronized void doCheck() {
		int total = freeConns.size();
		int invalid = PoolService.doCheck(this.testSQL, freeConns.iterator());
		LogUtil.debug("Checked [{}]. total:{},  invalid:{}", this, total, invalid);
	}

	public boolean isMultipleRdbms() {
		return false;
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

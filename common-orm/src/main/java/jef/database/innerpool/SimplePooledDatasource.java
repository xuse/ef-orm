package jef.database.innerpool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.ConnectionEventListener;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.PooledConnection;
import javax.sql.StatementEventListener;

import jef.common.log.LogUtil;
import jef.common.pool.PoolStatus;
import jef.database.DbCfg;
import jef.database.DbUtils;
import jef.database.datasource.AbstractDataSource;
import jef.database.datasource.DataSourceInfo;
import jef.database.datasource.SimpleDataSource;
import jef.database.innerpool.PoolService.CheckableConnection;
import jef.tools.JefConfiguration;

/**
 * 最简单的连接池实现。
 * 
 * 直接封装为J2EE标准的DataSource的实现。 目前SimplePool已经继承了{@link javax.sql.DataSource}
 * 该连接池的功能 1、在指定的大小范围内自动管理连接数 2、在每次拿取连接时检查连接的可用性，如果不可用会自动重连。
 * 
 * @author jiyi
 * 
 */
public final class SimplePooledDatasource extends AbstractDataSource implements IPool<Connection>, CheckablePool,ConnectionPoolDataSource {
	private DataSource datasource;
	private int max;
	private int min;
	private int validationTimeout;
	private String testSQL;
	private BlockingQueue<Connection> freeConns;
	private AtomicInteger used = new AtomicInteger();// 被取走的连接数

	/**
	 * 空构造器
	 */
	public SimplePooledDatasource() {
		this(JefConfiguration.getInt(DbCfg.DB_CONNECTION_POOL, 3), JefConfiguration.getInt(DbCfg.DB_CONNECTION_POOL_MAX, 50), new SimpleDataSource());
	}

	/**
	 * 带参数构造
	 * 
	 * @param min
	 *            池最小
	 * @param max
	 *            池最大
	 * @param ds
	 *            datasource
	 */
	public SimplePooledDatasource(int min, int max, DataSource ds) {
		if (min > max)
			min = max;
		this.min = min;
		this.max = max;
		this.datasource = ds;
		freeConns = new LinkedBlockingQueue<Connection>(max);
		PoolReleaseThread.getInstance().addPool(this);
		PoolCheckThread.getInstance().addPool(this);
	}

	public void setDatasource(DataSource datasource) {
		checkNotUsed();
		this.datasource = datasource;
	}

	public void setMax(int max) {
		checkNotUsed();
		this.max = max;
		this.freeConns = new LinkedBlockingQueue<Connection>(max);
	}

	public void setMin(int min) {
		checkNotUsed();
		this.min = min;
	}

	public Connection poll() throws SQLException {
		try {
			Connection conn=freeConns.poll();
			if(conn!=null){
				used.incrementAndGet();
				return conn;
			}
			if (used.get() < max) {// 尝试用新连接
				used.incrementAndGet();// 必须立刻累加计数器，否则并发的线程会立刻抢先创建对象，从而超出连接池限制
				conn = datasource.getConnection();
			} else {
				used.incrementAndGet(); // 提前计数，并发下为了严格阻止连接池超出上限，必须这样做
				conn = freeConns.poll(5000000000L, TimeUnit.NANOSECONDS);// 5秒
				if (conn == null) {
					used.decrementAndGet();// 回滚计数器
					throw new SQLException("No connection avaliable now." + getStatus());
				}
				conn = ensureOpen(conn);
			}
			return conn;
		} catch (InterruptedException e) {
			throw new SQLException(e);
		}
	}

	// 检查并确保连接可用
	private Connection ensureOpen(Connection conn) throws SQLException {
		boolean closed = true;
		try {
			closed = conn.isClosed(); //不准确的判断
		} catch (SQLException e) {
			DbUtils.closeConnection(conn);
		}
		if (closed) {
			return datasource.getConnection();
		} else {
			return conn;
		}
	}

	public void offer(Connection conn) {
		boolean success = freeConns.offer(conn);
		if (!success) {
			DbUtils.closeConnection(conn);// 塞不下了。肯定是关闭掉
		}
		used.decrementAndGet();
	}

	public PoolStatus getStatus() {
		int used = this.used.get();
		int free = freeConns.size();
		return new PoolStatus(max, min, used + free, used, free);
	}

	public DataSource getDatasource() {
		return datasource;
	}

	public void closeConnectionTillMin() {
		if (freeConns.size() > min) {
			Connection conn;
			// 注意下面两个条件顺序必须确保poll操作在后，因为poll操作会变更集合的Size
			while (freeConns.size() > min && (conn = freeConns.poll()) != null) {
				DbUtils.closeConnection(conn);
			}
		}
	}

	public void close() throws SQLException {
		max = 0;
		min = 0;
		closeConnectionTillMin();
		PoolReleaseThread.getInstance().removePool(this);
	}

	@Override
	protected Class<? extends DataSource> getWrappedClass() {
		return null;
	}

	/*
	 * 如果当前连接池不是出于初始状态，那么抛出异常
	 */
	private void checkNotUsed() {
		if (used.get() > 0 || (freeConns != null && !freeConns.isEmpty())) {
			throw new IllegalStateException("Current connection pool is in using. please set arguments before use it.");
		}
	}

	// /////////////////////////JDBC的连接池实现(不可重入)/////////////////////////////////
	public Connection getConnection() throws SQLException {
		return new PooledImpl(poll());
	}

	public Connection getConnection(String username, String password) throws SQLException {
		return (Connection) getPooledConnection(username, password);
	}

	private final class PooledImpl extends AbstractJDBCConnection implements PooledConnection,CheckableConnection {
		PooledImpl(Connection conn) {
			this.conn = conn;
		}

		public void close() throws SQLException {
			SimplePooledDatasource.this.offer(this.conn);
		}

		public Connection getConnection() throws SQLException {
			return conn;
		}

		public void addConnectionEventListener(ConnectionEventListener listener) {
		}

		public void removeConnectionEventListener(ConnectionEventListener listener) {
		}

		public void addStatementEventListener(StatementEventListener listener) {
		}

		public void removeStatementEventListener(StatementEventListener listener) {
		}
		public boolean isUsed() {
			return false;
		}
		public void setInvalid() {
			DbUtils.closeConnection(conn);
			conn=null;
		}
		public boolean checkValid(String testSql) throws SQLException {
			if(conn==null)return true;
			PreparedStatement st=conn.prepareStatement(testSql);
			try{
				st.execute();
				return true;
			}finally{
				DbUtils.close(st);
			}
		}
		public boolean checkValid(int timeout) throws SQLException {
			if(conn==null)return true;
			return conn.isValid(timeout);
		}
	}

	public PooledConnection getPooledConnection() throws SQLException {
		return new PooledImpl(poll());
	}

	public PooledConnection getPooledConnection(String username, String password) throws SQLException {
		try {
			Connection conn;
			if (freeConns.isEmpty() && used.get() < max) {// 尝试用新连接
				conn = datasource.getConnection(username, password);
			} else {
				conn = freeConns.poll(5000000000L, TimeUnit.NANOSECONDS);// 5秒
				if (conn == null) {
					throw new SQLException("No connection avaliable now." + getStatus());
				}
				conn = ensureOpen(conn);
			}
			used.incrementAndGet();
			return new PooledImpl(conn);
		} catch (InterruptedException e) {
			throw new SQLException(e);
		}
	}

	public String getUrl() {
		return getDsi().getUrl();
	}

	public void setUrl(String url) {
		getDsi().setUrl(url);
	}

	public String getUsername() {
		return getDsi().getUser();
	}

	public void setUsername(String user) {
		getDsi().setUser(user);
	}

	public String getPassword() {
		return getDsi().getPassword();
	}

	public void setPassword(String password) {
		getDsi().setPassword(password);
	}

	public String getDriverClass() {
		return getDsi().getDriverClass();
	}

	public void setDriverClass(String driverClass) {
		getDsi().setDriverClass(driverClass);
	}

	/**
	 * 获得验证超时时间
	 * 
	 * @return
	 */
	public int getValidationTimeout() {
		return validationTimeout;
	}

	/**
	 * 设置验证超时时间
	 * 
	 * @param validationTimeout
	 */
	public void setValidationTimeout(int validationTimeout) {
		this.validationTimeout = validationTimeout;
	}

	private DataSourceInfo getDsi() {
		if (datasource instanceof DataSourceInfo) {
			return (DataSourceInfo) datasource;
		} else if (datasource == null) {
			SimpleDataSource sds = new SimpleDataSource();
			datasource = sds;
			return sds;
		} else {
			throw new UnsupportedOperationException("The datasource " + datasource.getClass() + " Doesn't support.");
		}
	}

	public String getTestSQL() {
		return testSQL;
	}

	public void setTestSQL(String string) {
		this.testSQL=string;
	}

	private class I implements Iterator<PooledImpl>{
		private Iterator<Connection> raw;
		I(Iterator<Connection> iterator) {
			this.raw=iterator;
		}

		public boolean hasNext() {
			return raw.hasNext();
		}

		public PooledImpl next() {
			return new PooledImpl(raw.next());
		}

		public void remove() {
		}
	}

	public void doCheck() {
		if(freeConns==null)return;
		int total=freeConns.size();
		Iterator<PooledImpl> iter=new I(freeConns.iterator());
		int invalid=PoolService.doCheck(this.testSQL, iter);
		LogUtil.info("Checked [{}]. total:{},  invalid:{}", this, total, invalid);
	}
}

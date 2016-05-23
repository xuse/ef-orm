package jef.database;

import java.sql.SQLException;

import javax.persistence.PersistenceException;

import jef.common.log.LogUtil;
import jef.database.cache.CacheChain;
import jef.database.cache.CacheImpl;
import jef.database.innerpool.IConnection;
import jef.database.support.TransactionTimedOutException;

import org.easyframe.enterprise.spring.TransactionMode;
import org.omg.CORBA.SystemException;

public class TransactionImpl extends Transaction {
	private boolean dirty;
	/**
	 * 当前事务为自动提交状态
	 */
	private boolean autoCommit;

	private boolean rollbackOnly = false;

	/**
	 * 当前事务隔离级别
	 */
	private int isolationLevel = ISOLATION_DEFAULT;

	/**
	 * 事务超时截止时间
	 */
	private Long deadline;

	/**
	 * 当前事务只读
	 */
	private boolean readOnly;

	/**
	 * jef在执行以下两类操作时，会强制要求在一个事务中执行： <li>Batch 批操作</li> <li>Cascade 级联操作</li>
	 * jef会检查要执行的操作是否已经在一个事务上执行，如果此时用户没有开启事务。
	 * 那么jef会为了上述操作单独开启一个事务，并在上述操作完成的时候自动提交这个事务。
	 * 为了将这种内部自动生成的事务和用户事务加以区别，所以通过这个字符串表名事务的类别。 并且可以体现在日志中。
	 */
	TransactionFlag innerFlag;

	private boolean isJpa;

	public void setReadonly(boolean flag) {
		if (this.readOnly == flag)
			return;
		readOnly = flag;
		if (conn != null)
			try {
				conn.setReadOnly(flag);
			} catch (SQLException e) {
				LogUtil.exception(e);
			}
	}

	public boolean isReadonly() {
		return this.readOnly;
	}

	/**
	 * 是否限制回滚
	 * 
	 * @return
	 */
	public boolean isRollbackOnly() {
		return rollbackOnly;
	}

	/**
	 * 设置限制为回滚事务
	 * 
	 * @param rollbackOnly
	 */
	public void setRollbackOnly(boolean rollbackOnly) {
		log.debug("Transaction {} was marked as rollback only", this);
		this.rollbackOnly = rollbackOnly;
	}

	/**
	 * 构造事务
	 * 
	 * @param parent
	 *            DbClient
	 * @param flag
	 *            隐含事务标记，传入null表示为普通事务，其他为隐含事务，具体参见{@link TransactionFlag}
	 * @param readOnly
	 *            只读事务
	 * @throws SQLException
	 * @see TransactionFlag
	 */
	TransactionImpl(DbClient parent, TransactionFlag flag, boolean readOnly) {
		this(parent, 0, -1, readOnly, false);
		this.innerFlag = flag;
	}

	/**
	 * 构造事务
	 * 
	 * @param parent
	 *            DbClient
	 * @param timeout
	 *            事务超时时间
	 * @param isolationLevel
	 *            事务隔离级别
	 * @param readOnly
	 *            只读事务
	 * @param noCache
	 *            取消一级缓存
	 */
	TransactionImpl(DbClient parent, int timeout, int isolationLevel, boolean readOnly, boolean noCache) {
		super();
		this.parent = parent;
		this.isolationLevel = isolationLevel;
		this.readOnly = readOnly;
		this.rProcessor = parent.rProcessor;
		this.selectp = parent.selectp;
		this.insertp = parent.insertp;
		this.updatep = parent.updatep;
		this.deletep = parent.deletep;
		if (noCache || !ORMConfig.getInstance().isCacheLevel1()) {
			// 没有自己的缓存
			cache = parent.getCache();
		} else if (parent.getCache().isDummy()) {
			// 自己有缓存，上级无缓存
			cache = new CacheImpl(rProcessor, selectp, 0, "L1");
		} else {
			// 两级缓存都启用的情况下
			cache = new CacheChain(new CacheImpl(rProcessor, selectp, 0, "L1"), parent.getCache());
		}
		getListener().newTransaction(this);
		if (timeout > 0) {
			this.deadline = System.currentTimeMillis() + timeout * 1000;
		}
		this.isJpa = parent.getTxType() == TransactionMode.JPA;
		log.debug("[JPA DEBUG]:Transaction {} started ,mode is {}.", this, parent.getTxType());
	}

	/**
	 * 提交当前事务
	 * 
	 * @param close
	 *            If true, close the transaction after commit;
	 */
	public void commit(boolean close) {
		// 如果已经关闭什么也不作
		if (parent == null)
			return;
		try {
			if (dirty && !autoCommit) {
				if (rollbackOnly) {
					log.warn("[JPA WARN]:Transaction {} has been marked as rollback-only,so will rollback the transaction.", this);
					doRollback();
				} else {
					doCommit();
				}
			}
		} catch (SQLException e) {
			throw new PersistenceException(e.getSQLState(), e);
		} finally {
			if (close)
				doClose();
		}
	}

	/**
	 * 回滚当前事务中的所有操作
	 * 
	 * @param close
	 *            If true, close the transaction after rollback;
	 */
	public void rollback(boolean close) {
		// 如果已经关闭什么也不作
		if (parent == null)
			return;
		try {
			if (dirty && !autoCommit) {
				doRollback();
			}
		} catch (SQLException e) {
			throw new PersistenceException(e.getSQLState(), e);
		} finally {
			if (close)
				doClose();
		}
	}

	public boolean isOpen() {
		return parent != null;
	}

	public boolean isAutoCommit() {
		return autoCommit;
	}

	public Transaction setAutoCommit(boolean autoCommit) {
		if (autoCommit == this.autoCommit || parent.getTxType() == TransactionMode.JTA) {
			return this;
		}
		this.autoCommit = autoCommit;
		if (conn != null) {
			try {
				this.conn.setAutoCommit(autoCommit);
			} catch (SQLException e) {
				throw new PersistenceException(e);
			}
		}
		return this;
	}

	private void doRollback() throws SQLException {
		if (conn != null && parent.getTxType() != TransactionMode.JTA) {
			getListener().beforeRollback(this);
			long start = System.currentTimeMillis();
			conn.rollback();
			dirty = false;
			log.debug("[JPA DEBUG]:Transaction {} rollback. cost {}ms.", this, (System.currentTimeMillis() - start));
			getListener().postRollback(this);
		}
	}

	private void doCommit() throws SQLException {
		if (conn != null && parent.getTxType() != TransactionMode.JTA) {
			getListener().beforeCommit(this);
			long start = System.currentTimeMillis();
			conn.commit();
			dirty = false;
			log.debug("[JPA DEBUG]:Transaction {} commited. cost {}ms.", this, System.currentTimeMillis() - start);
			getListener().postCommit(this);
		}
	}

	private void doClose() {
		if (parent == null)
			return;

		if (conn != null) {
			try {
				if (!autoCommit && parent.getTxType() != TransactionMode.JTA) {
					conn.setAutoCommit(true);
				}
				// 注意，readOnly必须在AutoCommit设置完成后才能设置，因为在Postgres上，autocommit=false时，认为是在事务中，而事务中不允许修改readOnly属性
				if (readOnly) {
					conn.setReadOnly(false);
				}
			} catch (SQLException e) {
				LogUtil.exception(e);
			}
			conn.close();
			conn = null;
		}
		parentName = parent.getDbName(null);
		try {
			getListener().tracsactionClose(this);
		} catch (Throwable t) {
			log.error("", t);
		}
		parent = null;
	}

	@Override
	public void close() {
		cache.evictAll();
		if (parent != null) {
			if (!readOnly && dirty && !autoCommit) {
				rollback();
			}
			doClose();
		}
	}

	protected IConnection getConnection() throws SQLException {
		if (parent == null) {
			throw new IllegalStateException("Current transaction is closed!|" + getTransactionId(null));
		}
		if (deadline != null) {
			checkTimeToLiveInMillis();
		}
		if (conn == null) {
			IConnection conn = parent.getPool().getConnection(this);
			if (parent.getTxType() != TransactionMode.JTA) {
				conn.setAutoCommit(autoCommit);
				if (readOnly) {
					conn.setReadOnly(true);
				}
				if (isolationLevel >= 0 && ORMConfig.getInstance().isSetTxIsolation()) {
					conn.setTransactionIsolation(isolationLevel);
				}
			}
			this.conn = conn;
		}
		dirty = true;
		return conn;
	}

	private void checkTimeToLiveInMillis() throws TransactionTimedOutException {
		long timeToLive = this.deadline - System.currentTimeMillis();
		boolean deadlineReached = timeToLive <= 0;
		if (deadlineReached) {
			setRollbackOnly(true);
			throw new TransactionTimedOutException("Transaction timed out: deadline was " + this.deadline);
		}
	}

	/**
	 * 事务的超时设置
	 * 
	 * @param timeout单位为秒
	 * @throws SystemException
	 */
	public void setTimeout(int timeout) {
		if (timeout > TIMEOUT_DEFAULT) {
			this.deadline = System.currentTimeMillis() + timeout * 1000;
		}
	}

	/**
	 * Return whether this object has an associated timeout.
	 */
	public boolean hasTimeout() {
		return (this.deadline != null);
	}

	public int getIsolationLevel() {
		return isolationLevel;
	}

	public void setIsolationLevel(int isolationLevel) {
		this.isolationLevel = isolationLevel;
	}

	@Override
	public TransactionFlag getTransactionFlag() {
		return innerFlag;
	}

	@Override
	protected TransactionMode getTxType() {
		return parent.getTxType();
	}

	@Override
	protected boolean isJpaTx() {
		return isJpa;
	}
}

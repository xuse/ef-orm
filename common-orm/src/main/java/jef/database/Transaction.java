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
package jef.database;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import jef.common.log.LogUtil;
import jef.database.cache.CacheDummy;
import jef.database.cache.CacheImpl;
import jef.database.cache.TransactionCache;
import jef.database.dialect.DatabaseDialect;
import jef.database.innerpool.IUserManagedPool;
import jef.database.innerpool.ReentrantConnection;
import jef.database.innerpool.Savepoints;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.database.support.DbOperatorListener;
import jef.database.support.SavepointNotSupportedException;
import jef.database.support.TransactionTimedOutException;
import jef.tools.StringUtils;

import org.omg.CORBA.SystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 事务状态下的数据库连接封装。
 * 
 * @author jiyi
 *
 */
public class Transaction extends Session implements TransactionStatus {
	
	private static Logger log=LoggerFactory.getLogger(Transaction.class);
	
	
//	private static final String COMMIT_LOG = "[JPA DEBUG]:Transaction %s commited.[%sms]";
//	private static final String ROLLBACK_LOG = "[JPA DEBUG]:Transaction %s rollback.[%sms]";
//	private static final String ROLLBACK_ONLY_LOG = "[JPA WARN]:Transaction %s has been marked as rollback-only,so will rollback the transaction.";
//	private static final String CONN_MERGE_LOG = "[CONN MERGE]:Merge '%s' connection with the existing '%s' connection.%s";
//	private static final String GET_CONN_LOG = "[JPA DEBUG]:Transaction %s getConnection count: %s";
	
	/**
	 * Use the default timeout of the underlying transaction system, or none if
	 * timeouts are not supported.
	 */
	final static int TIMEOUT_DEFAULT = -1;

	final static int ISOLATION_DEFAULT = -1;
	
	/**
	 * ORM中部分操作会在非事务场景下进行，而JEF本身的一些内部操作则必须保证在事务环境下执行。（如级联操作、批操作）
	 * 为此JEF会在内部创建一个事务对象。这类内部的食物对象当操作完成后即提交并关闭。对用户透明。
	 * @author jiyi
	 *
	 */
	enum TransactionFlag{
		/**
		 * 因为操作悲观锁模式,为了阻止连接被释放（例如线程重入），因此使用一个事务来独占连接。
		 */
		ResultHolder,
		/**
		 * 级联写操作，为了保证一致性的隐含事务
		 */
		Cascade,
	}
	

	//所有的savePoints
	private Map<String, Savepoints> savepoints;
	private String parentName;//当关闭后parent即为null，此时需要使用该变量显示日志
	private DbClient parent;
	private volatile ReentrantConnection conn;
	
	
	private TransactionCache cache;
	private boolean rollbackOnly = false;
	
	/**
	 * jef在执行以下两类操作时，会强制要求在一个事务中执行： <li>Batch 批操作</li> <li>Cascade 级联操作</li>
	 * jef会检查要执行的操作是否已经在一个事务上执行，如果此时用户没有开启事务。
	 * 那么jef会为了上述操作单独开启一个事务，并在上述操作完成的时候自动提交这个事务。
	 * 为了将这种内部自动生成的事务和用户事务加以区别，所以通过这个字符串表名事务的类别。 并且可以体现在日志中。
	 */
	TransactionFlag innerFlag;

	/**
	 * 事务超时截止时间,目前未生效
	 */
	private Long deadline;
	private int isolationLevel = ISOLATION_DEFAULT;
	private boolean readOnly;

	/**
	 * 构造事务
	 * @param parent DbClient
	 * @param flag 隐含事务标记，传入null表示为普通事务，其他为隐含事务，具体参见{@link TransactionFlag}
	 * @param readOnly 只读事务
	 * @throws SQLException
	 * @see TransactionFlag
	 */
	Transaction(DbClient parent, TransactionFlag flag,boolean readOnly){
		this(parent,0,-1,readOnly,false);
		this.innerFlag = flag;
	}

	/**
	 * 构造事务
	 * @param parent DbClient 
	 * @param timeout 事务超时时间
	 * @param isolationLevel 事务隔离级别
	 * @param readOnly    只读事务
	 * @param noCache     取消一级缓存
	 */
	Transaction(DbClient parent, int timeout, int isolationLevel, boolean readOnly,boolean noCache) {
		super();
		this.parent = parent;
		this.isolationLevel=isolationLevel;
		this.readOnly=readOnly;
		
		rProcessor = parent.rProcessor;
		selectp=parent.selectp;
		p = parent.p;
		insertp=parent.insertp;
		batchinsertp=parent.batchinsertp;
		if(noCache){
			cache=CacheDummy.getInstance();
		}else{
			cache=ORMConfig.getInstance().isCacheLevel1()?new CacheImpl(rProcessor,selectp):CacheDummy.getInstance();
		}
		
		getListener().newTransaction(this);
		if(timeout>0){
			this.deadline=System.currentTimeMillis()+timeout*1000;
		}
	}

	protected ReentrantConnection getConnection() throws SQLException {
		ensureOpen();
		if(deadline!=null){
			checkTimeToLiveInMillis();
		}
		if(conn==null){
			conn = parent.getPool().poll(this);
			conn.setAutoCommit(false);
			if(readOnly){
				conn.setReadOnly(true);
			}
			if(isolationLevel>=0 && ORMConfig.getInstance().isSetTxIsolation()){
				conn.setTransactionIsolation(isolationLevel);	
			}
		}
		return conn;
	}
	
	/**
	 * 回滚当前事务中的所有操作
	 * 
	 * @throws SQLException
	 */
	public void rollback() throws SQLException {
		//如果已经关闭什么也不作
		if(parent==null)return;
		try{
			doRollBack();
		}finally{
			doClose();
		}
	}


	private void doRollBack() throws SQLException {
		if (conn != null) {
			getListener().beforeRollback(this);
			long start = System.currentTimeMillis();
			conn.rollback();
			if(ORMConfig.getInstance().isDebugMode())
				log.info("[JPA DEBUG]:Transaction {} rollback. cost {}ms.",this, (System.currentTimeMillis() - start)); 
			getListener().postRollback(this);
		}
	}


	private void doCommit() throws SQLException {
		if(conn!=null){
			getListener().beforeCommit(this);
			long start = System.currentTimeMillis();
			conn.commit();
			if(ORMConfig.getInstance().isDebugMode())
				log.info("[JPA DEBUG]:Transaction {} commited. cost {}ms.",this,System.currentTimeMillis() - start);
			getListener().postCommit(this);
		}
	}
	
	/**
	 * 提交当前事务中的操作
	 * 
	 * @throws SQLException
	 */
	public void commit() throws SQLException {
		//如果已经关闭什么也不作
		if(parent==null)return;
		try{
			if (rollbackOnly) {
				log.warn("[JPA WARN]:Transaction {} has been marked as rollback-only,so will rollback the transaction.",this);
				doRollBack();
			}else{
				doCommit();
			}			
		}finally{
			doClose();
		}
	}			

	private void ensureOpen() {
		if (parent == null) {
			throw new IllegalStateException("Current transaction is closed!|" + getTransactionId(null));
		}
	}

	/**
	 * 关闭事务，连接释放回连接池
	 */
	private void doClose() {
		if (parent == null)
			return;

		if (conn != null) {
			if(readOnly){
				try {
					conn.setReadOnly(false);
				} catch (SQLException e) {
					LogUtil.exception(e);
				}
			}
			parent.getPool().offer(conn);
			conn = null;
		}
		parentName = parent.getDbName(null);
		try {
			getListener().tracsactionClose(this);
		} catch (Throwable t) {
			log.error("",t);
		}
		parent = null;
	}

	@Override
	public String toString() {
		return getTransactionId(null);
	}

	void releaseConnection(ReentrantConnection conn) {
		// DO nothing.
	}

	/**
	 * 是否限制回滚
	 * 
	 * @return
	 */
	public boolean isRollbackOnly() {
		return rollbackOnly;
	}
	
	public boolean isOpen(){
		return parent!=null;
	}

	/**
	 * 设置限制为回滚事务
	 * 
	 * @param rollbackOnly
	 */
	public void setRollbackOnly(boolean rollbackOnly) {
		log.debug("Transaction {} was marked as rollback only",this);
		this.rollbackOnly = rollbackOnly;
	}

	@Override
	public <T> NativeQuery<T> createNamedQuery(String name, Class<T> resultWrapper){
		if (parent.namedQueries == null)
			parent.initNQ();
		NQEntry nc = parent.namedQueries.get(name);
		if (nc == null)
			return null;
		return asOperateTarget(MetaHolder.getMappingSite(nc.getTag())).createNativeQuery(nc, resultWrapper);
	}
	

	@Override
	public <T> NativeQuery<T> createNamedQuery(String name, ITableMetadata resultMeta){
		if (parent.namedQueries == null)
			parent.initNQ();
		NQEntry nc = parent.namedQueries.get(name);
		if (nc == null)
			return null;
		return asOperateTarget(MetaHolder.getMappingSite(nc.getTag())).createNativeQuery(nc, resultMeta);
	}

	@Override
	protected TransactionCache getCache() {
		return cache;
	}

	@Override
	protected DbOperatorListener getListener() {
		return parent.getListener();
	}


	@Override
	protected String getTransactionId(String dbkey) {
		StringBuilder sb = new StringBuilder();
		if (innerFlag == null) {
			sb.append("[Tx");
		} else {
			sb.append('[').append(innerFlag.name());
		}
		sb.append(StringUtils.toFixLengthString(this.hashCode(), 8)).append('@');
		sb.append(parent != null ? parent.getDbName(null): parentName);
		sb.append('@').append(Thread.currentThread().getId()).append(']');
		return sb.toString();
	}

	/**
	 * 得到当前事务所关联的DbClient
	 * 
	 * @return
	 */
	public DbClient getParent() {
		return parent;
	}

	/**
	 * 设置恢复点
	 * @param savepointName
	 * @throws SQLException
	 * @throws SavepointNotSupportedException
	 */
	public void setSavepoint(String savepointName) throws SQLException, SavepointNotSupportedException {
		//Oracle SavePoint必须用字母开头，不能用数字开头
		if (!parent.asOperateTarget(null).getMetaData().supportsSavepoints())
			throw new SavepointNotSupportedException("Savepoints are not supported by your JDBC driver.");
		//XA模式下不支持savepoint;
		Savepoints sp=getConnection().setSavepoints(savepointName);//如果不支持SP，返回null
		if(sp==null){
			throw new SavepointNotSupportedException("Savepoints are not supported.");
		}else{
			if (savepoints == null) {
				savepoints = new HashMap<String, Savepoints>();
			}
			savepoints.put(savepointName, sp);
		}
	}

	public void rollbackToSavepoint(String savepointName) throws SQLException, SavepointNotSupportedException {
		//Oracle SavePoint必须用字母开头，不能用数字开头
		Savepoints savepoint = savepoints.get(savepointName);
		if (savepoint != null)
			savepoint.rollbackSavepoints();
	}

	public void releaseSavepoint(String savepointName) {
		//Oracle SavePoint必须用字母开头，不能用数字开头
		Savepoints savepoint = savepoints.get(savepointName);
		if (savepoint != null) {
			try {
				savepoint.releaseSavepoints();
			} catch (SQLException e) {
				log.error("",e);
			}
		}
	}

	public int getIsolationLevel() {
		return isolationLevel;
	}

	public void setIsolationLevel(int isolationLevel) {
		this.isolationLevel = isolationLevel;
	}

	/**
	 * 事务的超时设置
	 * 
	 * @param timeout单位为秒
	 * @throws SystemException
	 */
	public void setTimeout(int timeout) {
		if (timeout > TIMEOUT_DEFAULT) {
			setTimeoutInSeconds(timeout);
		}
	}

	/**
	 * Return whether this object has an associated timeout.
	 */
	public boolean hasTimeout() {
		return (this.deadline != null);
	}

	private void checkTimeToLiveInMillis() throws TransactionTimedOutException {
		long timeToLive = this.deadline - System.currentTimeMillis();
		boolean deadlineReached=timeToLive <= 0;
		if (deadlineReached) {
			setRollbackOnly(true);
			throw new TransactionTimedOutException("Transaction timed out: deadline was " + this.deadline);
		}
	}

	/**
	 * Set the timeout for this object in seconds.
	 * 
	 * @param seconds
	 *            number of seconds until expiration
	 */
	private void setTimeoutInSeconds(int seconds) {
		this.deadline = System.currentTimeMillis() + seconds * 1000;
	}


	@Override
	protected OperateTarget asOperateTarget(String dbKey) {
		if (StringUtils.isEmpty(dbKey))
			return new OperateTarget(this,null);
		return new OperateTarget(this,dbKey);
	}

	@Override
	protected Collection<String> getAllDatasourceNames() {
		ensureOpen();
		return parent.getAllDatasourceNames();
	}

	@Override
	protected String getDbName(String dbKey) {
		return parent==null?this.parentName:parent.getDbName(dbKey);
	}

	@Override
	IUserManagedPool getPool() {
		ensureOpen();
		return parent.getPool();
	}

	@Override
	public DbClient getNoTransactionSession() {
		ensureOpen();
		return parent;
	}

	@Override
	public DatabaseDialect getProfile(String key) {
		ensureOpen();
		return parent.getProfile(key);
	}

	@Override
	public void close() {
		cache.evictAll();
		if (parent != null) {
			try {
				rollback();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
}

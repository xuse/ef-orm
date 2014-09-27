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
import java.sql.Savepoint;
import java.util.Collection;

import jef.database.cache.TransactionCache;
import jef.database.dialect.DatabaseDialect;
import jef.database.innerpool.IConnection;
import jef.database.innerpool.IUserManagedPool;
import jef.database.innerpool.PartitionSupport;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.database.support.DbOperatorListener;
import jef.database.support.SavepointNotSupportedException;
import jef.tools.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 事务状态下的数据库连接封装。
 * 
 * @author jiyi
 * 
 */
public abstract class Transaction extends Session implements TransactionStatus {
	protected static Logger log = LoggerFactory.getLogger(Transaction.class);

	/**
	 * ORM中部分操作会在非事务场景下进行，而JEF本身的一些内部操作则必须保证在事务环境下执行。（如级联操作、批操作）
	 * 为此JEF会在内部创建一个事务对象。这类内部的食物对象当操作完成后即提交并关闭。对用户透明。
	 * 
	 * @author jiyi
	 * 
	 */
	enum TransactionFlag {
		/**
		 * 因为操作悲观锁模式,为了阻止连接被释放（例如线程重入），因此使用一个事务来独占连接。
		 */
		ResultHolder,
		/**
		 * 级联写操作，为了保证一致性的隐含事务
		 */
		Cascade,
		/**
		 * 被外部管理的事务.
		 */
		Managed
	}

	/**
	 * Use the default timeout of the underlying transaction system, or none if
	 * timeouts are not supported.
	 */
	final static int TIMEOUT_DEFAULT = -1;
	/**
	 * 使用数据库的默认事务隔离级别。
	 */
	final static int ISOLATION_DEFAULT = -1;

	protected String parentName;// 当关闭后parent即为null，此时需要使用该变量显示日志
	protected DbClient parent;
	protected volatile IConnection conn;
	protected TransactionCache cache;

	@Override
	public String toString() {
		return getTransactionId(null);
	}

	void releaseConnection(IConnection conn) {
	}

	public boolean isOpen() {
		return parent != null;
	}

	@Override
	public <T> NativeQuery<T> createNamedQuery(String name, Class<T> resultWrapper) {
		if (parent.namedQueries == null)
			parent.initNQ();
		NQEntry nc = parent.namedQueries.get(name);
		if (nc == null)
			return null;
		return asOperateTarget(MetaHolder.getMappingSite(nc.getTag())).createNativeQuery(nc, resultWrapper);
	}

	@Override
	public <T> NativeQuery<T> createNamedQuery(String name, ITableMetadata resultMeta) {
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
	 * 
	 * @param savepointName
	 * @throws SQLException
	 * @throws SavepointNotSupportedException
	 */
	public Savepoint setSavepoint(String savepointName) throws SQLException, SavepointNotSupportedException {
		// Oracle SavePoint必须用字母开头，不能用数字开头
//		if (!parent.asOperateTarget(null).getMetaData().supportsSavepoints())
//			throw new SavepointNotSupportedException("Savepoints are not supported by your JDBC driver.");
		
		// XA模式下不支持savepoint;
		Savepoint sp = getConnection().setSavepoint(savepointName);// 如果不支持SP，返回null
		if (sp == null) {
			throw new SavepointNotSupportedException("Savepoints are not supported.");
		}
		return sp;
	}

	public Savepoint setSavepoint() throws SQLException, SavepointNotSupportedException {
		if (!parent.asOperateTarget(null).getMetaData().supportsSavepoints())
			throw new SavepointNotSupportedException("Savepoints are not supported by your JDBC driver.");
		return getConnection().setSavepoint();// 如果不支持SP，返回null
	}

	public void rollbackToSavepoint(Savepoint savepoint) throws SQLException, SavepointNotSupportedException {
		getConnection().releaseSavepoint(savepoint);
	}

	public void releaseSavepoint(Savepoint savepoint) throws SQLException {
		getConnection().releaseSavepoint(savepoint);
	}

	@Override
	protected OperateTarget asOperateTarget(String dbKey) {
		if (StringUtils.isEmpty(dbKey))
			return new OperateTarget(this, null);
		return new OperateTarget(this, dbKey);
	}

	@Override
	protected Collection<String> getAllDatasourceNames() {
		ensureOpen();
		return parent.getAllDatasourceNames();
	}

	@Override
	protected String getDbName(String dbKey) {
		return parent == null ? this.parentName : parent.getDbName(dbKey);
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

	private void ensureOpen() {
		if (parent == null) {
			throw new IllegalStateException("Current transaction is closed!|" + getTransactionId(null));
		}
	}

	@Override
	public PartitionSupport getPartitionSupport() {
		return parent.getPartitionSupport();
	}

	@Override
	protected String getTransactionId(String dbkey) {
		StringBuilder sb = new StringBuilder();
		TransactionFlag innerFlag = getTransactionFlag();
		if (innerFlag == null) {
			sb.append("[Tx");
		} else {
			sb.append('[').append(innerFlag.name());
		}
		sb.append(StringUtils.toFixLengthString(this.hashCode(), 8)).append('@');
		sb.append(parent != null ? parent.getDbName(dbkey) : parentName);
		sb.append('@').append(Thread.currentThread().getId()).append(']');
		return sb.toString();
	}

	abstract public void setReadonly(boolean flag);

	abstract public boolean isReadonly();

	abstract public int getIsolationLevel();

	abstract public void setIsolationLevel(int isolationLevel);
	
	abstract public Transaction setAutoCommit(boolean autoCommit);
	
	abstract public boolean isAutoCommit();
	

	/**
	 * 提交当前事务中的操作
	 * 
	 * 仅提交，不会关闭事务和释放连接
	 */
	public void commit() {
		commit(false);
	}
	
	/**
	 * 回滚当前事务中的所有操作
	 * 
	 * 仅回滚，不会关闭事务和释放连接
	 */
	public void rollback() {
		rollback(false);
	}
}
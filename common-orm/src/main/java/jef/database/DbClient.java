/*
named * JEF - Copyright 2009-2010 Jiyi (mr.jiyi@gmail.com)
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.persistence.PersistenceException;
import javax.sql.DataSource;

import org.easyframe.enterprise.spring.TransactionMode;

import jef.common.Callback;
import jef.common.log.LogUtil;
import jef.database.cache.Cache;
import jef.database.cache.CacheDummy;
import jef.database.cache.CacheImpl;
import jef.database.datasource.SimpleDataSource;
import jef.database.dialect.AbstractDialect;
import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.AutoIncrementMapping;
import jef.database.innerpool.IConnection;
import jef.database.innerpool.IUserManagedPool;
import jef.database.innerpool.PartitionSupport;
import jef.database.innerpool.PoolService;
import jef.database.jmx.JefFacade;
import jef.database.meta.AbstractMetadata;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.database.meta.Reference;
import jef.database.routing.PartitionResult;
import jef.database.support.DbOperatorListener;
import jef.database.support.DbOperatorListenerContainer;
import jef.database.support.DefaultDbOperListener;
import jef.database.support.MetadataEventListener;
import jef.tools.Assert;
import jef.tools.JefConfiguration;
import jef.tools.StringUtils;
import jef.tools.reflect.BeanUtils;

/**
 * 数据库操作句柄
 * 
 * DbClient是非事务状态下的数据库连接，每次操作后自动提交不能回滚。但可以执行建表、删表等DDL语句。
 * 
 * @author jiyi
 */
public class DbClient extends Session implements SessionFactory {
	/**
	 * 命名查询
	 */
	protected NamedQueryHolder namedQueries;
	/**
	 * 数据库操作监听器
	 */
	private DbOperatorListener listener;
	/**
	 * Sequence管理器
	 */
	private SequenceManager sequenceManager;

	/**
	 * 事务支持类型
	 */
	private TransactionMode txType = TransactionMode.JPA;

	/**
	 * 连接池和metadata服务
	 */
	private IUserManagedPool connPool;

	/**
	 * 构造当前对象的DataSource 也可能是RoutingDataSource
	 */
	private DataSource ds;
	/**
	 * 全局缓存
	 */
	private Cache golbalCache;

	/**
	 * 启动一个事务。
	 * 
	 * <h3>Example</h3>
	 * 
	 * <pre>
	 * <tt>
	 * Transaction session=dbClient.startTransaction();//开启一个事务
	 * String sql = "delete from ROOT where THE_NAME=:val";
	 * try{
	 *      session.executeSql("delete from table where id=?", id);//执行SQL语句
	 *      session.commit();   //提交事务
	 * }catch(SQLException e){
	 *      session.rollback(); //回滚事务
	 * }
	 * </tt>
	 * </pre>
	 * 
	 * @return 事务处理对象，继承了Session中的方法。
	 * @see Session
	 * @see Transaction
	 */
	public Transaction startTransaction() {
		return new TransactionImpl(this, null, false);
	}

	/**
	 * @param timeout
	 *            事务超时时间，单位秒
	 * @param isolationLevel
	 *            事务隔离级别 <li>TRANSACTION_READ_COMMITTED =1</li> <li>
	 *            TRANSACTION_READ_UNCOMMITTED = 2</li> <li>
	 *            TRANSACTION_REPEATABLE_READ = 4</li> <li>
	 *            TRANSACTION_SERIALIZABLE =8</li> <li>ISOLATION_DEFAULT = -1</li>
	 * 
	 * @param readOnly
	 *            是否为只读事务。部分数据库支持只读事务。可以针对只读进行优化。具体优化哪些特性取决于数据库。
	 * @return Transaction对象
	 */
	public Transaction startTransaction(int timeout, int isolationLevel, boolean readOnly) {
		return new TransactionImpl(this, timeout, isolationLevel, readOnly, false);
	}

	/**
	 * 使用Datasource 构造DbClient
	 * 
	 * @param datasource
	 *            数据源信息 如果datasource已经是一个连接池，那么不会再启动内嵌的连接池，否则会使用内建的连接池
	 */
	public DbClient(DataSource datasource) {
		this(datasource, JefConfiguration.getInt(DbCfg.DB_CONNECTION_POOL, 3), JefConfiguration.getInt(DbCfg.DB_CONNECTION_POOL_MAX, 50), null);
	}

	/**
	 * 使用DataSource构造DbClient
	 * 
	 * @param datasource
	 *            数据源信息
	 * @param max
	 *            内建连接池的最大值，如果DataSource已经是一个连接池，那么内建连接池不会启动，此参数无效。
	 */
	public DbClient(DataSource datasource, int min, int max, TransactionMode txType) {
		try {
			if (txType != null)
				this.txType = txType;
			if (datasource == null) {
				datasource = getDefaultDataSource();
			}
			init(datasource, min, max);
			JefFacade.registeEmf(this, null);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 构造，会使用jef.properties中配置的信息来连接数据库。
	 * 
	 * @deprecated use new DbClientBuilder().build() to create a new DbClient.
	 */
	public DbClient() {
		this(getDefaultDataSource(), JefConfiguration.getInt(DbCfg.DB_CONNECTION_POOL, 3), JefConfiguration.getInt(DbCfg.DB_CONNECTION_POOL_MAX, 50), null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jef.database.Session#createNamedQuery(java.lang.String,
	 * java.lang.Class)
	 */
	@Override
	public <T> NativeQuery<T> createNamedQuery(String name, Class<T> resultWrapper) {
		if (namedQueries == null)
			initNQ();
		NQEntry nc = namedQueries.get(name);
		if (nc == null) {
			throw new IllegalArgumentException("The query which named [" + name + "] was not found.");
		}
		return selectTarget(MetaHolder.getMappingSite(nc.getTag())).createNativeQuery(nc, resultWrapper);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jef.database.Session#createNamedQuery(java.lang.String,
	 * jef.database.meta.ITableMetadata)
	 */
	@Override
	public <T> NativeQuery<T> createNamedQuery(String name, ITableMetadata resultMeta) {
		if (namedQueries == null)
			initNQ();
		NQEntry nc = namedQueries.get(name);
		if (nc == null) {
			throw new IllegalArgumentException("The query which named [" + name + "] was not found.");
		}
		return selectTarget(MetaHolder.getMappingSite(nc.getTag())).createNativeQuery(nc, resultMeta);
	}

	/**
	 * 检测一个命名查询是否存在
	 * 
	 * @param name
	 * @return
	 */
	public boolean hasNamedQuery(String name) {
		if (namedQueries == null)
			initNQ();
		NQEntry nc = namedQueries.get(name);
		return nc != null;
	}

	/**
	 * 增加一个数据库操作监听器 操作监听器是可以为各种数据库操作编写事件的一个自定义的类。
	 * 
	 * @param lis
	 */
	public void addEventListener(DbOperatorListener lis) {
		DbOperatorListener old = getListener();
		if (old == DefaultDbOperListener.getInstance()) {
			this.listener = lis;
		} else if (old instanceof DbOperatorListenerContainer) {
			((DbOperatorListenerContainer) old).add(lis);
		} else {
			this.listener = new DbOperatorListenerContainer(old, lis);
		}
	}

	protected synchronized void initNQ() {
		namedQueries = new NamedQueryHolder(this);
	}

	@Override
	protected Cache getCache() {
		return golbalCache;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DbOperatorListener getListener() {
		if (listener == null) {
			String clz = JefConfiguration.get(DbCfg.DB_OPERATOR_LISTENER);
			if (StringUtils.isNotEmpty(clz)) {
				try {
					listener = (DbOperatorListener) BeanUtils.newInstance(Class.forName(clz));
				} catch (ClassNotFoundException e) {
					LogUtil.exception(e);
				}
			}
			if (listener == null) {
				listener = DefaultDbOperListener.getInstance();
			}
		}
		return listener;
	}

	protected String getTransactionId(String key) {
		StringBuilder sb = new StringBuilder();
		// 2016/7/8 优化，Map hash查找仅发生一次。
		ConnectInfo info = connPool.getInfo(key);
		sb.append('[').append(info.profile.getName()).append(':').append(info.dbname).append('@').append(Thread.currentThread().getId()).append(']');
		return sb.toString();
	}

	@Override
	protected OperateTarget selectTarget(String dbKey) {
		if (connPool == null) {
			throw new IllegalAccessError("The database client was closed!");
		}
		if (StringUtils.isEmpty(dbKey))
			dbKey = null;
		return new OperateTarget(this, dbKey);
	}

	@Override
	public DbClient getNoTransactionSession() {
		return this;
	}

	/**
	 * 得到指定表的所有分表
	 * 
	 * @param meta
	 *            表元模型
	 * @return 从数据库扫描得到的分表
	 */
	public PartitionResult[] getSubTableNames(ITableMetadata meta) {
		return getPartitionSupport().getSubTableNames(meta);
	}

	public boolean isOpen() {
		return connPool != null;
	}

	protected void finalize() throws Throwable {
		if (connPool != null) {
			LogUtil.show("Database will auto shut down at Java finalize thread. to avoid this message, you should manually close the DbClient.");
			close();
		}
		super.finalize();
	}

	public SequenceManager getSequenceManager() {
		return sequenceManager;
	}

	/*
	 * 初始化
	 */
	protected void init(DataSource ds, int min, int max) throws SQLException {
		DbUtils.tryAnalyzeInfo(ds, true);// 尝试解析并处理连接参数。
		this.ds = ds;
		this.connPool = PoolService.getPool(ds, min, max, txType);
		Assert.notNull(connPool);
		LogUtil.info("Init DB Connection: " + connPool.getInfo(null));
		afterPoolReady();
	}

	/*
	 * initlize:连接建立完成后执行初始化检查
	 */
	private void afterPoolReady() throws SQLException {
		// 初始化处理器
		DatabaseDialect profile = this.getProfile(null);
		this.preProcessor = new SqlProcessor.PrepareImpl(profile, this);
		this.selectp = SelectProcessor.get(profile, this);
		this.insertp = InsertProcessor.get(profile, this);
		this.updatep = UpdateProcessor.get(profile, this);
		this.deletep = DeleteProcessor.get(profile, this);
		this.sequenceManager = new SequenceManager(this);

		// 设置全局缓存
		if (ORMConfig.getInstance().getCacheLevel2() > 0) {
			this.golbalCache = new CacheImpl(preProcessor, selectp, ORMConfig.getInstance().getCacheLevel2(), "GLOBAL");
		} else {
			this.golbalCache = CacheDummy.getInstance();
		}
		// 设置分区加载器
		this.pm = new PartitionMetadata(connPool);
		// Named Query初始化
		String queryTable = JefConfiguration.get(DbCfg.DB_QUERY_TABLE_NAME);
		if (StringUtils.isNotEmpty(queryTable)) {
			MetaHolder.initMetadata(NamedQueryConfig.class, null, queryTable);
			try {
				refreshTable(MetaHolder.getMeta(NamedQueryConfig.class), null);// 创建表
			} catch (SQLException e) {
				LogUtil.warn("Named Query Table:" + queryTable + " was not refreshed. Error:" + e.getMessage());
			}
		}

		// 在每个数据库上都要执行的初始化任务
		this.connPool.registeDbInitCallback(new C(this));
	}

	/**
	 * 回调对象
	 * 
	 * @author Administrator
	 * 
	 */
	private static class C implements Callback<String, SQLException> {
		private DbClient session;

		C(DbClient session) {
			this.session = session;
		}

		public void call(String key) throws SQLException {
			for (String tableName : JefConfiguration.get(DbCfg.DB_TABLES).split(",")) {
				try {
					tableName = tableName.trim();
					if (tableName.length() > 0) {
						Class<?> c = Class.forName(tableName);
						Object o = c.newInstance();
						if (o instanceof IQueryableEntity) {
							if (session.createTable((IQueryableEntity) o)) {
								LogUtil.info("JEF has created table {} automaticlly.", tableName);
							}
						}
					}
				} catch (Exception e) {
					LogUtil.exception(e);
				}
			}
			DbMetaData meta = session.getMetaData(key);
			boolean showVersion = JefConfiguration.getBoolean(DbCfg.DB_SHOW_JDBC_VERSION, true);
			if (showVersion) {
				LogUtil.show(meta.getDbVersion());
			}
		}
	}

	/**
	 * Get the database metadata handler of assigned datasource. if there's only
	 * one datasource, input null is fine.
	 * 
	 * @param dbkey
	 *            the name of datasource. input null to access the metadata of
	 *            default datasource.
	 * @return the handler of database metadata.
	 * @throws SQLException
	 */
	public DbMetaData getMetaData(String dbkey) {
		return connPool.getMetadata(dbkey);
	}

	/**
	 * Check if the table exists.
	 * <p>
	 * 
	 * If there are multiple datasources , use {@link #getMetaData(String)} then
	 * call {@link DbMetaData#getExistTable(String)} to check.
	 * 
	 * @param tableName
	 *            支持Schema重定向
	 * @return
	 * @throws SQLException
	 * 
	 * 
	 */
	public boolean existsTable(String tableName) throws SQLException {
		Assert.notNull(tableName);
		tableName = MetaHolder.toSchemaAdjustedName(tableName);
		return getMetaData(null).existTable(tableName);
	}

	/**
	 * 给定一个对象，计算这个对象所对应的数据库表并判断这些表是否存在。（在分表分库条件下，一个查询对象可以对应多张表）
	 * 
	 * @param obj
	 *            检测的查询对象
	 * @return 不存在的表名称
	 * @throws SQLException
	 */
	public Collection<String> existsTable(IQueryableEntity obj) throws SQLException {
		PartitionResult[] result = DbUtils.toTableNames(obj, null, null, getPartitionSupport());
		List<String> s = new ArrayList<String>();
		for (PartitionResult pr : result) {
			DbMetaData meta = getMetaData(pr.getDatabase());
			if (meta != null) {
				for (String table : pr.getTables()) {
					if (!meta.existTable(table)) {
						s.add(table);
					}
				}
			}
		}
		return s;
	}

	/**
	 * 删除表
	 * 
	 * @param meta
	 *            要删除的表
	 * @return 删除的表数量
	 * @throws SQLException
	 */
	public int dropTable(ITableMetadata meta) throws SQLException {
		PartitionResult[] pr = DbUtils.toTableNames(meta, getPartitionSupport(), 4);
		return dropTable0(pr, meta);
	}

	/**
	 * 删除表
	 * 
	 * @param cls
	 *            要删除的表（对应的class）。如果是分库分表的class，会对应多张表
	 * @return 删除的表数量
	 * @throws SQLException
	 */
	public int dropTable(Class<?>... cls) throws SQLException {
		int count = 0;
		for (Class<?> c : cls) {
			ITableMetadata meta = MetaHolder.getMeta(c);
			count += dropTable(meta);
		}
		return count;
	}

	/**
	 * 删除表
	 * 
	 * @param obj
	 *            要删除的表对应的查询对象
	 * @return 删除的表数量
	 * @throws SQLException
	 */
	public int dropTable(IQueryableEntity obj) throws SQLException {
		PartitionResult[] pr = DbUtils.toTableNames(obj, null, obj.getQuery(), getPartitionSupport());
		ITableMetadata meta = MetaHolder.getMeta(obj);
		return dropTable0(pr, meta);
	}

	/**
	 * 删除表 (支持schema重定向)
	 * 
	 * @param tablename
	 *            表名
	 * @param seqNames
	 *            要关联删除的Sequence名称
	 * @return
	 * @throws SQLException
	 */
	public int dropTable(String tablename, String... seqNames) throws SQLException {
		tablename = MetaHolder.toSchemaAdjustedName(tablename);
		if (seqNames != null) {
			for (int i = 0; i < seqNames.length; i++) {
				seqNames[i] = MetaHolder.toSchemaAdjustedName(seqNames[i]);
			}
		}
		PartitionResult pr = new PartitionResult(tablename);
		return dropTable0(new PartitionResult[] { pr }, null, seqNames);
	}

	/**
	 * 删除指定表上的全部约束（主外键）
	 * 
	 * @param tablename
	 *            支持schema重定向
	 * @throws SQLException
	 */
	public void dropAllConstraint(String tablename) throws SQLException {
		getMetaData(null).dropAllConstraint(tablename);
	}

	/**
	 * 执行 truncate table XXXX 命令，快速清光表的数据
	 * 
	 * @param meta
	 *            表名
	 * @param route
	 *            路由结果。该表对应的所有实例。
	 * 
	 */
	public int truncate(ITableMetadata meta, PartitionResult[] route) throws SQLException {
		List<SQLException> errors = new ArrayList<SQLException>();
		int total = 0;
		for (PartitionResult site : route) {
			DbMetaData dbmeta = connPool.getMetadata(site.getDatabase());
			try {
				dbmeta.truncate(meta, site.getTablesEscaped(dbmeta.getProfile()));
				total += site.getTables().size();
			} catch (SQLException e) {
				errors.add(e);
			}

		}
		if (!errors.isEmpty()) {
			throw DbUtils.wrapExceptions(errors);
		}
		return total;
	}

	/**
	 * 执行 truncate table XXXX 命令，快速清光一张表的数据
	 * 
	 * @param meta
	 *            表结构元数据
	 * @throws SQLException
	 */
	public void truncate(ITableMetadata meta) throws SQLException {
		PartitionResult[] route;
		route = DbUtils.toTableNames(meta, getPartitionSupport(), 4);
		truncate(meta, route);
	}

	/**
	 * 执行 truncate table XXXX 命令，快速清光一张表的数据
	 * 
	 * @param meta
	 *            表对应的类
	 */
	public void truncate(Class<? extends IQueryableEntity> meta) throws SQLException {
		truncate(MetaHolder.getMeta(meta));
	}

	/**
	 * 根据传入的对象和指定的表名创建表，这个方法只会创建一张表
	 * 
	 * @param meta
	 *            表结构元数据
	 * @param tablename
	 *            表名，支持Schema重定向.
	 * @param dbName
	 *            数据源名
	 * @return true表示创建成功
	 * @throws SQLException
	 */
	public boolean createTable(ITableMetadata meta, String tablename, String dbName) throws SQLException {
		tablename = MetaHolder.toSchemaAdjustedName(tablename);
		dbName = MetaHolder.getMappingSite(dbName);
		return createTable0(meta, new PartitionResult(tablename).setDatabase(dbName)) > 0;
	}

	/**
	 * 根据传入的对象和指定的表名创建表，这个方法只会创建一张表
	 * 
	 * @param clz
	 *            表实体类
	 * @param tablename
	 *            表名，支持Schema重定向.
	 * @param dbName
	 *            数据源名
	 * @return true表示创建成功
	 * @throws SQLException
	 */
	public boolean createTable(Class<? extends IQueryableEntity> clz, String tablename, String dbName) throws SQLException {
		return createTable(MetaHolder.getMeta(clz), tablename, dbName);
	}

	/**
	 * 根据一个对象来创建表。如果是分库分表对象，会创建这个对象对应的表。
	 * 
	 * @param obj
	 *            分库对象
	 * @return 创建成功返回true，表已经存在无需创建返回false
	 * @throws SQLException
	 */
	public boolean createTable(IQueryableEntity obj) throws SQLException {
		AbstractMetadata meta = MetaHolder.getMeta(obj);
		PartitionResult[] result = DbUtils.partitionUtil.toTableNames(meta, obj, obj.getQuery(), getPartitionSupport(), false);
		if (ORMConfig.getInstance().isDebugMode()) {
			LogUtil.info("Partitions:" + Arrays.toString(result));
		}
		boolean ok = createTable0(meta, result) > 0;
		return ok;
	}

	/**
	 * 根据传入的类创建表，如果传入的类是分库分表对象，会创建这个对象所有的表。
	 * 
	 * @param cs
	 *            要创建的表对应的class
	 * @return 创建成功的表总数。
	 * @throws SQLException
	 */
	public int createTable(Class<?>... cs) throws SQLException {
		int n = 0;
		for (Class<?> c : cs) {
			ITableMetadata meta = MetaHolder.getMeta(c);
			PartitionResult[] result = DbUtils.toTableNames(meta, getPartitionSupport(), 2);
			n += createTable0(meta, result);
		}
		return n;
	}

	/**
	 * 根据传入的metadata创建表，如果传入的类是分库分表对象，会创建这个对象所有的表。
	 * 
	 * @param metas
	 *            要创建的表的元数据
	 * @return 创建成功的表的总数
	 * @throws SQLException
	 */
	public int createTable(ITableMetadata... metas) {
		int n = 0;
		for (ITableMetadata meta : metas) {
			PartitionResult[] result = DbUtils.toTableNames(meta, getPartitionSupport(), 2);
			n += createTable0(meta, result);
		}
		return n;
	}

	/**
	 * 检查并修改数据库中的表，使其和传入的实体模型保持一致。
	 * 
	 * @param clz
	 *            要更新的表对应的类
	 * @throws SQLException
	 */
	public void refreshTable(Class<?> clz) throws SQLException {
		refreshTable(MetaHolder.getMeta(clz), null, true, true);
	}

	/**
	 * 检查并修改数据库中的表，使其和传入的实体模型保持一致。
	 * 
	 * @param meta
	 *            要更新的表的元数据
	 * @param event
	 *            事件监听器，可以监听刷新过程的事件
	 * @throws SQLException
	 */
	public void refreshTable(ITableMetadata meta, MetadataEventListener event) throws SQLException {
		refreshTable(meta, event, true, true);
	}

	/**
	 * 检查并修改数据库中的表，使其和传入的实体模型保持一致。
	 * 
	 * @param meta
	 *            要更新的表的元数据
	 * @param event
	 *            事件监听器，可以监听刷新过程的事件
	 * @param modifyConstraint
	 *            更改约束
	 * @param modifyIndexes
	 *            更改索引
	 * @throws SQLException
	 * @see MetadataEventListener
	 */
	public void refreshTable(ITableMetadata meta, MetadataEventListener event, boolean modifyConstraint, boolean modifyIndexes) throws SQLException {
		Assert.notNull(meta, "The table definition which your want to resresh must not null.");
		ensureOpen();
		PartitionResult[] results = DbUtils.toTableNames(meta, this.getPartitionSupport(), 4);
		for (PartitionResult result : results) {
			DbMetaData dbmeta = getPool().getMetadata(result.getDatabase());
			for (String table : result.getTables()) {
				if (event == null || event.beforeTableRefresh(meta, table)) {
					dbmeta.refreshTable(meta, table, event, modifyConstraint, modifyIndexes);
				}
			}
		}
	}

	/**
	 * 检查并修改数据库中的表，使其和传入的实体模型保持一致。
	 * 
	 * @param clz
	 *            要更新的表对应的类
	 * @param listener
	 *            事件监听器，可以监听刷新过程的事件
	 * @throws SQLException
	 */
	public void refreshTable(Class<?> clz, MetadataEventListener listener) throws SQLException {
		refreshTable(MetaHolder.getMeta(clz), listener);
	}

	public void close() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jef.database.Session#getProfile(java.lang.String)
	 */
	public DatabaseDialect getProfile(String key) {
		ensureOpen();
		return connPool.getProfile(key);
	}

	@Override
	IUserManagedPool getPool() {
		return connPool;
	}

	@Override
	public Collection<String> getAllDatasourceNames() {
		ensureOpen();
		return connPool.getAllDatasourceNames();
	}

	/**
	 * 是否为路由场合下使用
	 * 
	 * @return true if the datasource is routing datasource
	 */
	public boolean isRoutingDataSource() {
		return connPool.isRouting();
	}

	/**
	 * 是否启用了EF-ORM的内部连接池
	 * 
	 * @return true if the inner pool is enabled.
	 */
	public boolean isInnerPoolEnabled() {
		return !connPool.isDummy();
	}

	/**
	 * 获得内部连接池的统计信息
	 * 
	 * @return
	 */
	public String getInnerPoolStatics() {
		ensureOpen();
		if (connPool.isDummy()) {
			return "InnerPool is Disabled.";
		}
		return connPool.getStatus().toString();
	}

	/**
	 * 强制进行命名查询的更新检查
	 */
	public void checkNamedQueryUpdate() {
		if (namedQueries == null) {
			initNQ();
		} else {
			namedQueries.checkUpdate(null);
		}
	}

	protected IConnection getConnection() throws SQLException {
		ensureOpen();
		IConnection conn = connPool.poll();
		if (!conn.getAutoCommit()) {
			conn.setAutoCommit(true);
		}
		return conn;
	}

	protected String getDbName(String dbKey) {
		return connPool.getInfo(dbKey).getDbname();
	}

	private int createTable0(ITableMetadata meta, PartitionResult... route) {
		List<SQLException> errors = new ArrayList<>();
		int total = 0;
		for (PartitionResult site : route) {
			DbMetaData dbmeta = connPool.getMetadata(site.getDatabase());
			for (String tablename : site.getTables()) {
				try {
					if (dbmeta.createTable(meta, tablename))
						total++;
				} catch (SQLException e) {
					errors.add(e);
				}
			}
		}
		if (meta != null) {
			for (Reference ref : meta.getRefFieldsByRef().keySet()) {
				if (ref.getHint() != null && ref.getHint().getRelationTable() != null) {
					createTable(ref.getHint().getRelationTable());
				}
			}
		}
		for (Exception e : errors) {
			LogUtil.exception(e);
		}
		if (!errors.isEmpty()) {
			throw new PersistenceException(DbUtils.wrapExceptions(errors));
		}
		return total;
	}

	private int dropTable0(PartitionResult[] prs, ITableMetadata meta, String... otherseq) throws SQLException {
		List<SQLException> errors = new ArrayList<SQLException>();
		int count = 0;
		for (PartitionResult pr : prs) {
			DbMetaData metaData = getMetaData(pr.getDatabase());
			try {
				for (String table : pr.getTables()) {
					metaData.dropTable(table);
					count++;
				}
				if (meta != null) {
					for (AutoIncrementMapping mapping : meta.getAutoincrementDef()) {
						sequenceManager.dropSequence(mapping, this.selectTarget(pr.getDatabase()));
					}
				}
				for (String s : otherseq) {
					metaData.dropSequence(s);
				}
			} catch (SQLException e) {
				errors.add(e);
			}
			if (meta != null)
				metaData.clearTableMetadataCache(meta);
		}
		if (meta != null) {
			for (Reference ref : meta.getRefFieldsByRef().keySet()) {
				if (ref.getHint() != null && ref.getHint().getRelationTable() != null) {
					try {
						dropTable(ref.getHint().getRelationTable());
					} catch (SQLException e) {
						errors.add(e);
					}
				}
			}
		}
		for (Exception e : errors) {
			LogUtil.exception(e);
		}
		if (!errors.isEmpty()) {
			throw DbUtils.wrapExceptions(errors);
		}
		return count;
	}

	private void ensureOpen() {
		if (connPool == null) {
			throw new IllegalStateException("The database client was closed.");
		}
	}

	public TransactionMode getTxType() {
		return txType;
	}

	public DataSource getDataSource() {
		return ds;
	}

	/**
	 * 获取缺省的DataSource配置
	 * 
	 * @return
	 */
	private static SimpleDataSource getDefaultDataSource() {
		String dbPath = JefConfiguration.get(DbCfg.DB_FILEPATH, "");
		String dbType = JefConfiguration.get(DbCfg.DB_TYPE, "derby");
		int port = JefConfiguration.getInt(DbCfg.DB_PORT, 0);
		String host = JefConfiguration.get(DbCfg.DB_HOST, "");
		String dbName = JefConfiguration.get(DbCfg.DB_NAME);
		DatabaseDialect features = AbstractDialect.getDialect(dbType);
		if (features == null) {
			throw new RuntimeException("The DBMS: " + dbType + "not support yet.");
		}
		dbPath = dbPath.replace('\\', '/');
		if (dbPath.length() > 0 && !dbPath.endsWith("/")) {
			dbPath += "/";
		}
		String url = features.generateUrl(host, port, dbPath + dbName);
		String user = JefConfiguration.get(DbCfg.DB_USER);
		String password = JefConfiguration.get(DbCfg.DB_PASSWORD);
		return DbUtils.createSimpleDataSource(url, user, password);
	}

	private PartitionMetadata pm;

	@Override
	PartitionSupport getPartitionSupport() {
		return pm;
	}

	public void printPool() {
		System.out.println(getPool().getStatus());
	}

	/*
	 * 从配置中读取命名查询的配置位置(文件)
	 */
	private String namedQueryFilename = JefConfiguration.get(DbCfg.NAMED_QUERY_RESOURCE_NAME, "named-queries.xml");
	/*
	 * 从配置中读取命名查询的配置位置(数据库表)
	 */
	private String namedQueryTablename = JefConfiguration.get(DbCfg.DB_QUERY_TABLE_NAME);

	/**
	 * 设置命名查询文件的名称
	 * 
	 * @param namedQueryFilename
	 */
	public void setNamedQueryFilename(String namedQueryFilename) {
		if (namedQueries != null) {
			throw new IllegalStateException("must set before named-query init");
		}
		this.namedQueryFilename = namedQueryFilename;
	}

	/**
	 * 设置命名查询表的名称
	 * 
	 * @param namedQueryTablename
	 */
	public void setNamedQueryTablename(String namedQueryTablename) {
		if (namedQueries != null) {
			throw new IllegalStateException("must set before named-query init");
		}
		this.namedQueryTablename = namedQueryTablename;
	}

	String getNamedQueryFile() {
		return namedQueryFilename;
	}

	String getNamedQueryTable() {
		return namedQueryTablename;
	}

	boolean isJpaTx() {
		return false;
	}

	@Override
	public void shutdown() {
		try {
			this.getListener().onDbClientClose();
		} catch (Exception e) {
			LogUtil.exception(e);
		}
		this.sequenceManager.close();
		if(connPool!=null) {
			try {
				connPool.close();
				JefFacade.unregisteEmf((DbClient) this);
			} catch (SQLException e) {
				throw DbUtils.toRuntimeException(e);
			} finally {
				connPool = null;
			}	
		}
	}

	@Override
	public Session getSession() {
		return this;
	}

	public void flush() {
		getCache().evictAll();
	}
}
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

import java.io.File;
import java.sql.SQLException;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.easyframe.enterprise.spring.TransactionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jef.codegen.EntityEnhancer;
import jef.common.log.LogUtil;
import jef.database.datasource.MapDataSourceLookup;
import jef.database.datasource.RoutingDataSource;
import jef.database.datasource.SimpleDataSource;
import jef.database.dialect.AbstractDialect;
import jef.database.dialect.DatabaseDialect;
import jef.database.jpa.JefEntityManagerFactory;
import jef.database.meta.MetaHolder;
import jef.database.support.DbInitHandler;
import jef.database.support.QuerableEntityScanner;
import jef.tools.JefConfiguration;

/**
 * 提供了创建DbClient的若干工厂方法
 * 
 * 
 */
public class DbClientBuilder {

	private Logger log = LoggerFactory.getLogger(DbClientBuilder.class);
	/**
	 * 多数据源。分库分表时可以使用。 在Spring配置时，可以使用这样的格式来配置
	 * 
	 * <pre>
	 * <code>
	 * &lt;property name="dataSources"&gt;
	 * 	&lt;map&gt;
	 * 	 &lt;entry key="dsname1" value-ref="ds1" /&gt;
	 * 	 &lt;entry key="dsname2" value-ref="ds2" /&gt;
	 * 	&lt;/map&gt;
	 * &lt;/property&gt;
	 * </code>
	 * </pre>
	 */
	private Map<String, DataSource> dataSources;

	/**
	 * 单数据源。
	 */
	protected DataSource dataSource;

	/**
	 * 多数据源时的缺省数据源名称
	 */
	private String defaultDatasource;

	/**
	 * 内置连接池最大连接数
	 */
	private int maxPoolSize = JefConfiguration.getInt(DbCfg.DB_CONNECTION_POOL_MAX, 50);

	/**
	 * 内置连接池最小连接数
	 */
	private int minPoolSize = JefConfiguration.getInt(DbCfg.DB_CONNECTION_POOL, 3);

	/**
	 * 命名查询所在的文件
	 */
	private String namedQueryFile;

	/**
	 * 命名查询所在的表
	 */
	private String namedQueryTable;

	/**
	 * 事务支持类型
	 * 
	 * @see #setTransactionMode(String)
	 */
	private TransactionMode transactionMode;

	/**
	 * 指定对以下包内的实体做一次增强扫描。多个包名之间逗号分隔。<br>
	 * 如果不配置此项，默认将对packagesToScan包下的类进行增强。<br>
	 * 不过不想进行类增强扫描，可配置为"none"。
	 * 
	 * @deprecated
	 */
	private String enhancePackages = "none";

	/**
	 * 指定扫描若干包,配置示例如下—— <code><pre>
	 * &lt;list&gt;
	 *  &lt;value&gt;org.easyframe.test&lt;/value&gt;
	 *  &lt;value&gt;org.easyframe.entity&lt;/value&gt;
	 * &lt;/list&gt;
	 * </pre></code>
	 */
	private String[] packagesToScan;

	/**
	 * 对配置了包扫描的路径进行增强检查，方便单元测试
	 */
	private boolean enhanceScanPackages = true;

	/**
	 * 扫描已知的若干注解实体类，配置示例如下—— <code><pre>
	 * &lt;list&gt;
	 *  &lt;value&gt;org.easyframe.testp.jta.Product&lt;/value&gt;
	 *  &lt;value&gt;org.easyframe.testp.jta.Users&lt;/value&gt;
	 * &lt;/list&gt;
	 * </pre></code>
	 */
	private String[] annotatedClasses;

	/**
	 * 指定扫描若干表作为动态表，此处配置表名，名称之间逗号分隔
	 */
	private String dynamicTables;

	/**
	 * 是否将所有未并映射的表都当做动态表进行映射。
	 */
	private boolean registeNonMappingTableAsDynamic;

	/**
	 * 扫描到实体后，如果数据库中不存在，是否建表 <br>
	 * 默认开启
	 */
	private boolean createTable = true;

	/**
	 * 扫描到实体后，如果数据库中存在对应表，是否修改表 <br>
	 * 默认开启
	 */
	private boolean alterTable = true;

	/**
	 * 扫描到实体后，如果准备修改表，如果数据库中的列更多，是否允许删除列 <br>
	 * 默认关闭
	 */
	private boolean allowDropColumn;

	/**
	 * 自定义一个类，当数据库连上后干一些初始化的事情。
	 * 
	 * @see DbInitHandler
	 */
	private String dbInitHandler;
	/**
	 * 在建表后插入初始化数据
	 * <p>
	 * 允许用户在和class相同的位置创建一个 <i>class-name</i>.txt的文件，记录了表中的初始化数据。
	 * 开启此选项后，在初始化建表时会插入这些数据。
	 */
	private boolean initData;

	/**
	 * 是否使用数据库初始化记录表
	 */
	private boolean useDataInitTable = JefConfiguration.getBoolean(DbCfg.USE_DATAINIT_FLAG_TABLE, false);;

	/**
	 * 初始化数据编码
	 */
	private String initDataCharset = "UTF-8";

	/**
	 * 初始化数据扩展名
	 */
	private String initDataExtension = JefConfiguration.get(DbCfg.INIT_DATA_EXTENSION, "txt");

	/**
	 *  初始化数据根路径
	 */
	private String initDataRoot = JefConfiguration.get(DbCfg.INIT_DATA_ROOT, "/");

	/**
	 * 最终构造出来的对象实例
	 */
	protected JefEntityManagerFactory instance;

	/**
	 * 空构造
	 */
	public DbClientBuilder() {
	}

	/**
	 * 构造
	 * 
	 * @param jdbcURL
	 * @param user
	 * @param password
	 * @param maxPool
	 */
	public DbClientBuilder(String jdbcURL, String user, String password, int maxPool) {
		this.dataSource = DbUtils.createSimpleDataSource(jdbcURL, user, password);
		this.maxPoolSize = maxPool;
	}

	/**
	 * 构造数据源连接信息
	 * 
	 * @param dbType
	 * @param host
	 * @param port
	 * @param pathOrName
	 * @param user
	 * @param password
	 * @return
	 */
	public DbClientBuilder(String dbType, String host, int port, String pathOrName, String user, String password) {
		DatabaseDialect profile = AbstractDialect.getDialect(dbType);
		if (profile == null) {
			throw new IllegalArgumentException("The DBMS:[" + dbType + "] is not supported yet.");
		}
		String dbURL = profile.generateUrl(host, port, pathOrName);
		this.dataSource = DbUtils.createSimpleDataSource(dbURL, user, password);
	}

	/**
	 * 工厂方法获得数据库实例（本地）
	 * 
	 * @param dbName
	 *            数据库名
	 * @param user
	 *            用户
	 * @param pass
	 *            密码
	 * @return
	 * @throws SQLException
	 */
	public DbClientBuilder(String dbType, File dbFolder, String user, String password) {
		int port = JefConfiguration.getInt(DbCfg.DB_PORT, 0);
		String host = JefConfiguration.get(DbCfg.DB_HOST, "");
		DatabaseDialect profile = AbstractDialect.getDialect(dbType);
		if (profile == null) {
			throw new IllegalArgumentException("The DBMS:[" + dbType + "] is not supported yet.");
		}
		String dbURL = profile.generateUrl(host, port, dbFolder.getAbsolutePath());
		this.dataSource = DbUtils.createSimpleDataSource(dbURL, user, password);
	}

	/**
	 * 根据JDBC连接字符串和用户名密码得到
	 * 
	 * @param jdbcUrl
	 * @param user
	 * @param password
	 * @throws SQLException
	 */
	public DbClientBuilder(String jdbcUrl, String user, String password) {
		this.setDataSource(DbUtils.createSimpleDataSource(jdbcUrl, user, password));
	}

	/**
	 * 获得构造完成的DbClient对象
	 * 
	 * @return DbClient
	 * @see DbClient
	 */
	public DbClient build() {
		if (instance == null) {
			instance = buildSessionFactory();
		}
		return instance.getDefault();
	}

	/**
	 * 获得当前的事务控制模式
	 * 
	 * @return 事务控制模式
	 * @see TransactionMode
	 */
	public String getTransactionMode() {
		return transactionMode == null ? null : transactionMode.name();
	}

	/**
	 * 事务管理模式，可配置为
	 * <ul>
	 * <li><strong>JPA</strong></li><br>
	 * 使用JPA的方式管理事务，对应Spring的
	 * {@linkplain org.springframework.orm.jpa.JpaTransactionManager
	 * JpaTransactionManager}, 适用于ef-orm单独作为数据访问层时使用。
	 * <li><strong>JTA</strong></li><br>
	 * 使用JTA的分布式事务管理。使用JTA可以在多个数据源、内存数据库、JMS目标之间保持事务一致性。<br>
	 * 推荐使用atomikos作为JTA管理器。 对应Spring的
	 * {@linkplain org.springframework.transaction.jta.JtaTransactionManager
	 * JtaTransactionManager}。<br>
	 * 当需要在多个数据库之间保持事务一致性时酌情使用。
	 * <li><strong>JDBC</strong></li><br>
	 * 使用JDBC事务管理。当和Hibernate一起使用时，可以利用Hibernate的连接共享Hibernate事务。
	 * 当与JdbcTemplate共同使用时， 也可以获得DataSource所绑定的连接从而共享JDBC事务。 对应Spring的
	 * {@linkplain org.springframework.orm.hibernate3.HibernateTransactionManager
	 * HibernateTransactionManager} 和
	 * {@linkplain org.springframework.jdbc.datasource.DataSourceTransactionManager
	 * DataSourceTransactionManager}。
	 * 一般用于和Hibernate/Ibatis/MyBatis/JdbcTemplate等共享同一个事务。
	 * </ul>
	 * 默认为{@code JPA}
	 * 
	 * @param txType
	 *            事务管理模式，可设置为JPA、JTA、JDBC
	 * 
	 * @see TransactionMode
	 */
	public DbClientBuilder setTransactionMode(TransactionMode txType) {
		this.transactionMode = txType;
		return this;
	}

	/**
	 * 设置内置连接池最大连接数，如果设置为0可以禁用内置连接池。
	 * 
	 * @param maxConnection
	 * @return
	 */
	public DbClientBuilder setMaxPoolSize(int maxConnection) {
		this.maxPoolSize = maxConnection;
		return this;
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	/**
	 * 设置数据源
	 * 
	 * @param dataSource
	 *            数据源
	 */
	public DbClientBuilder setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
		return this;
	}

	/**
	 * 设置数据源
	 * 
	 * @param url
	 * @param user
	 * @param password
	 * @return
	 */
	public DbClientBuilder setDataSource(String url, String user, String password) {
		this.dataSource = new SimpleDataSource(url, user, password);
		return this;
	}

	/**
	 * 设置要扫描的包
	 * 
	 * @return 要扫描的包，逗号分隔
	 */
	public DbClientBuilder setPackagesToScan(String[] scanPackages) {
		this.packagesToScan = scanPackages;
		return this;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isAlterTable() {
		return alterTable;
	}

	/**
	 * 扫描到实体后，是否修改数据库中与实体定义不同的表
	 * 
	 * @param alterTable
	 *            'true' , EF-ORM will alter tables in database.
	 */
	public DbClientBuilder setAlterTable(boolean alterTable) {
		this.alterTable = alterTable;
		return this;
	}

	/**
	 * 设置是否处于调试
	 * 
	 * @param debug
	 * @return
	 */
	public DbClientBuilder setDebug(boolean debug) {
		ORMConfig.getInstance().setDebugMode(debug);
		return this;
	}

	/**
	 * 查询是否为调试模式
	 * 
	 * @return
	 */
	public boolean isDebug() {
		return ORMConfig.getInstance().isDebugMode();
	}

	/**
	 * 查询是否会自动建表
	 * 
	 * @return
	 */
	public boolean isCreateTable() {
		return createTable;
	}

	/**
	 * 扫描到实体后，是否在数据库中创建不存在的表
	 * 
	 * @param createTable
	 *            true将会创建表
	 */
	public DbClientBuilder setCreateTable(boolean createTable) {
		this.createTable = createTable;
		return this;
	}

	public boolean isAllowDropColumn() {
		return allowDropColumn;
	}

	/**
	 * 扫描数据库中存在的表作为动态表模型
	 * 
	 * @param dynamicTables
	 *            表名，逗号分隔
	 */
	public DbClientBuilder setDynamicTables(String dynamicTables) {
		this.dynamicTables = dynamicTables;
		return this;
	}

	public boolean isRegisteNonMappingTableAsDynamic() {
		return registeNonMappingTableAsDynamic;
	}

	public String[] getAnnotatedClasses() {
		return annotatedClasses;
	}

	/**
	 * 扫描已知的若干注解实体类，配置示例如下——
	 * 
	 * <pre>
	 * <code>
	 * &lt;list&gt;
	 *  &lt;value&gt;org.easyframe.testp.jta.Product&lt;/value&gt;
	 *  &lt;value&gt;org.easyframe.testp.jta.Users&lt;/value&gt;
	 * &lt;/list&gt;
	 * </code>
	 * </pre>
	 */
	public DbClientBuilder setAnnotatedClasses(String[] annotatedClasses) {
		this.annotatedClasses = annotatedClasses;
		return this;
	}

	public String[] getPackagesToScan() {
		return packagesToScan;
	}

	public boolean isInitData() {
		return initData;
	}

	public DbClientBuilder setInitData(boolean initData) {
		this.initData = initData;
		return this;
	}

	public void setMinPoolSize(int minPoolSize) {
		this.minPoolSize = minPoolSize;
	}

	/**
	 * 扫描数据库中当前schema下的所有表，如果尚未有实体与该表对应，那么就将该表作为动态表建模。
	 * 
	 * @param registeNonMappingTableAsDynamic
	 */
	public DbClientBuilder setRegisteNonMappingTableAsDynamic(boolean registeNonMappingTableAsDynamic) {
		this.registeNonMappingTableAsDynamic = registeNonMappingTableAsDynamic;
		return this;
	}

	public Map<String, DataSource> getDataSources() {
		return dataSources;
	}

	public String getDefaultDatasource() {
		return defaultDatasource;
	}

	/**
	 * 设置多数据源时的缺省数据源名称
	 * 
	 * @param defaultDatasource
	 *            name of the datasource.
	 */
	public DbClientBuilder setDefaultDatasource(String defaultDatasource) {
		this.defaultDatasource = defaultDatasource;
		return this;
	}

	/**
	 * 多数据源。分库分表时可以使用。 在Spring配置时，可以使用这样的格式来配置
	 * 
	 * <pre>
	 * <code>
	 * &lt;property name="dataSources"&gt;
	 * 	&lt;map&gt;
	 * 	 &lt;entry key="dsname1" value-ref="ds1" /&gt;
	 * 	 &lt;entry key="dsname2" value-ref="ds2" /&gt;
	 * 	&lt;/map&gt;
	 * &lt;/property&gt;
	 * </code>
	 * </pre>
	 */
	public DbClientBuilder setDataSources(Map<String, DataSource> datasources) {
		this.dataSources = datasources;
		return this;
	}

	/**
	 * 设置存放命名查询的文件资源名（xml格式，将在classpath下查找）
	 * 
	 * @param namedQueryFile
	 *            命名查询文件名
	 */
	public DbClientBuilder setNamedQueryFile(String namedQueryFile) {
		this.namedQueryFile = namedQueryFile;
		return this;
	}

	public String getNamedQueryTable() {
		return namedQueryTable;
	}

	/**
	 * 设置存放命名查询的数据库表名
	 * 
	 * @param namedQueryTable
	 *            命名查询数据库表
	 */
	public DbClientBuilder setNamedQueryTable(String namedQueryTable) {
		this.namedQueryTable = namedQueryTable;
		return this;
	}

	/**
	 * 扫描到实体后，在Alter数据表时，是否允许删除列。
	 * 
	 * @param allowDropColumn
	 *            true允许删除列
	 */
	public DbClientBuilder setAllowDropColumn(boolean allowDropColumn) {
		this.allowDropColumn = allowDropColumn;
		return this;
	}

	public String getEnhancePackages() {
		return enhancePackages;
	}

	/**
	 * 是否检查并增强实体。 注意，增强实体仅对目录中的class文件生效，对jar包中的class无效。
	 * 
	 * @deprecated 1.12开始，推荐使用instrument动态增强，不推荐这种做法
	 * @param enhancePackages
	 *            要扫描的包
	 */
	public DbClientBuilder setEnhancePackages(String enhancePackages) {
		this.enhancePackages = enhancePackages;
		return this;
	}

	public String getDynamicTables() {
		return dynamicTables;
	}

	public String getNamedQueryFile() {
		return namedQueryFile;
	}

	protected JefEntityManagerFactory buildSessionFactory() {
		if (instance != null)
			return instance;

		// try enahcen entity if theres 'enhancePackages'.
		if (enhancePackages != null) {
			if (!enhancePackages.equalsIgnoreCase("none")) {
				new EntityEnhancer().enhance(StringUtils.split(enhancePackages, ","));
			}
		}
		if (enhanceScanPackages && ArrayUtils.isNotEmpty(this.packagesToScan)) {
			new EntityEnhancer().enhance(packagesToScan);
		} else if (enhanceScanPackages) {
			log.warn("EnhanceScanPackages flag was set to true. but property 'packagesToScan' was not assigned");
		}
		JefEntityManagerFactory sf;
		// check data sources.
		if (dataSource == null && dataSources == null) {
			LogUtil.info("No datasource found. Using default datasource in jef.properties.");
			sf = new JefEntityManagerFactory(null, minPoolSize, maxPoolSize, transactionMode);
		} else if (dataSource != null) {
			sf = new JefEntityManagerFactory(dataSource, minPoolSize, maxPoolSize, transactionMode);
		} else {
			RoutingDataSource rs = new RoutingDataSource(new MapDataSourceLookup(dataSources).setDefaultKey(this.defaultDatasource));
			sf = new JefEntityManagerFactory(rs, minPoolSize, maxPoolSize, transactionMode);
		}
		if (namedQueryFile != null) {
			sf.getDefault().setNamedQueryFilename(namedQueryFile);
		}
		if (namedQueryTable != null) {
			sf.getDefault().setNamedQueryTablename(namedQueryTable);
		}

		if (packagesToScan != null || annotatedClasses != null) {
			QuerableEntityScanner qe = new QuerableEntityScanner();
			if (transactionMode == TransactionMode.JTA) {
				// JTA事务下，DDL语句必须在已启动后立刻就做，迟了就被套进JTA是事务中，出错。
				qe.setCheckSequence(false);
			}
			qe.setImplClasses(DataObject.class);
			qe.setAllowDropColumn(allowDropColumn);
			qe.setAlterTable(alterTable);
			qe.setCheckIndex(alterTable);
			qe.setCreateTable(createTable);
			
			qe.setInitData(this.initData);
			qe.setEntityManagerFactory(sf, this.useDataInitTable, this.initDataCharset, this.initDataExtension, this.initDataRoot);
			if (annotatedClasses != null)
				qe.registeEntity(annotatedClasses);
			if (packagesToScan != null) {
				String joined = StringUtils.join(packagesToScan, ',');
				qe.setPackageNames(joined);
				LogUtil.info("Starting scan easyframe entity from package: {}", joined);
				qe.doScan();
			}
			qe.finish();
		}
		if (dynamicTables != null) {
			DbClient client = sf.getDefault();
			for (String s : StringUtils.split(dynamicTables, ",")) {
				String table = s.trim();
				registe(client, table);
			}
		}
		if (registeNonMappingTableAsDynamic) {
			DbClient client = sf.getDefault();
			try {
				for (String tableName : client.getMetaData(null).getTableNames()) {
					if (MetaHolder.lookup(null, tableName) != null) {
						registe(client, tableName);
					}
				}
			} catch (SQLException e) {
				LogUtil.exception(e);
			}

		}
		// 执行用户自行配制的初始化任务
		if (StringUtils.isNotBlank(this.dbInitHandler)) {
			for (String clzName : StringUtils.split(dbInitHandler, ',')) {
				try {
					Object initType = Class.forName(clzName).newInstance();
					if (initType instanceof DbInitHandler) {
						((DbInitHandler) initType).doDatabaseInit(sf.getDefault());
					}
				} catch (ClassNotFoundException e) {
					LogUtil.error("InitClass load failure: class not found - " + e.getMessage());
				} catch (InstantiationException e) {
					LogUtil.error("InitClass load failure - ", e);
				} catch (IllegalAccessException e) {
					LogUtil.error("InitClass load failure - ", e);
				}
			}
		}
		return sf;
	}

	private void registe(DbClient client, String table) {
		if (MetaHolder.getDynamicMeta(table) == null) {
			try {
				MetaHolder.initMetadata(client, table);
				LogUtil.show("DynamicEntity: [" + table + "] registed.");
			} catch (SQLException e) {
				LogUtil.exception(e);
			}
		}
	}

	public boolean isCacheDebug() {
		return ORMConfig.getInstance().isCacheDebug();
	}

	public DbClientBuilder setCacheDebug(boolean cacheDebug) {
		ORMConfig.getInstance().setCacheDebug(cacheDebug);
		return this;
	}

	public boolean isCacheLevel1() {
		return ORMConfig.getInstance().isCacheLevel1();
	}

	/**
	 * 
	 * @param cache
	 * @return this
	 * @see DbCfg#CACHE_LEVEL_1
	 */
	public DbClientBuilder setCacheLevel1(boolean cache) {
		ORMConfig.getInstance().setCacheLevel1(cache);
		return this;
	}

	/**
	 * 获得全局缓存的生存时间，单位秒。 全局缓存是类似于一级缓存的一个存储结构，可以自动分析数据库操作关联性并进行数据刷新。
	 * 
	 * @return 秒数
	 * @see DbCfg#CACHE_GLOBAL_EXPIRE_TIME
	 */
	public int getGlobalCacheLiveTime() {
		return ORMConfig.getInstance().getCacheLevel2();
	}

	/**
	 * 设置全局缓存的生存时间，单位秒。 全局缓存是类似于一级缓存的一个存储结构，可以自动分析数据库操作关联性并进行数据刷新。
	 * 
	 * @param second
	 * @return this
	 * @see DbCfg#CACHE_GLOBAL_EXPIRE_TIME
	 */
	public DbClientBuilder setGlobalCacheLiveTime(int second) {
		ORMConfig.getInstance().setCacheLevel2(second);
		return this;
	}

	public DbClientBuilder setUseSystemOut(boolean flag) {
		LogUtil.useSlf4j = !flag;
		return this;
	}

	/**
	 * 是否启用数据初始化信息记录表。 如果启用，会自动在数据库中创建表 allow_data_initialize，其中
	 * do_init设置为0时，启动时不进行数据初始化。 如果设置为1，启动时进行数据初始化。
	 * 
	 * @return whether use the datainit table or not.
	 */
	public boolean isUseDataInitTable() {
		return useDataInitTable;
	}

	/**
	 * 设置是否启用数据初始化信息记录表。 如果启用，会自动在数据库中创建表 allow_data_initialize，其中
	 * do_init设置为0时，启动时不进行数据初始化。 如果设置为1，启动时进行数据初始化。
	 * 
	 * @param useDataInitTable
	 *            whether use the datainit table or not.
	 * @return this
	 */
	public DbClientBuilder setUseDataInitTable(boolean useDataInitTable) {
		this.useDataInitTable = useDataInitTable;
		return this;
	}

	public static DbClientBuilder newBuilder() {
		return new DbClientBuilder();
	}

	public String getInitDataCharset() {
		return initDataCharset;
	}

	public DbClientBuilder setInitDataCharset(String initDataCharset) {
		this.initDataCharset = initDataCharset;
		return this;
	}

	public String getDbInitHandler() {
		return dbInitHandler;
	}

	public void setDbInitHandler(String dbInitHandler) {
		this.dbInitHandler = dbInitHandler;
	}

	public boolean isEnhanceScanPackages() {
		return enhanceScanPackages;
	}

	public DbClientBuilder setEnhanceScanPackages(boolean enhanceScanPackages) {
		this.enhanceScanPackages = enhanceScanPackages;
		return this;
	}

	public String getInitDataExtension() {
		return initDataExtension;
	}

	public DbClientBuilder setInitDataExtension(String initDataExtension) {
		this.initDataExtension = initDataExtension;
		return this;
	}

	public String getInitDataRoot() {
		return initDataRoot;
	}

	public DbClientBuilder setInitDataRoot(String initDataRoot) {
		this.initDataRoot = initDataRoot;
		return this;
	}
	
	
}

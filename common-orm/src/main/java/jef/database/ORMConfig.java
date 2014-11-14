package jef.database;

import java.nio.charset.Charset;

import jef.database.meta.MetaHolder;
import jef.database.meta.MetadataFacade;
import jef.jre5support.ProcessUtil;
import jef.tools.JefConfiguration;
import jef.tools.JefConfiguration.Item;

/**
 * all configuration values of ORM.
 * 
 * JMX支持
 * 
 * @author jiyi
 * 
 */
public class ORMConfig implements ORMConfigMBean {
	private static ORMConfig instance = new ORMConfig();
	private MetadataFacade metaFacade = new MetadataFacade();

	private String serverName;

	public static ORMConfig getInstance() {
		return instance;
	}

	public ORMConfig() {
		init();
	}

	/**
	 * 调试模式
	 */
	protected boolean debugMode;

	/**
	 * 打印出文本型数据字段的实际编码后长度。将字符编码一次来输出长度有不小的开销，默认关闭
	 */
	private boolean showStringLength;
	/**
	 * 过滤不存在的分区表
	 */
	private boolean filterAbsentTables;
	/**
	 * 按需建表（分区表）
	 */
	private boolean partitionCreateTableInneed;

	/**
	 * 内存排序和聚合等计算的最大支持结果数
	 */
	private int partitionInMemoryMaxRows;

	/**
	 * Lob等数据流映射到String时的编码
	 */
	private String dbEncoding;
	/**
	 * Lob等数据流映射到String时的编码
	 */
	private Charset dbEncodingCharset;
	/**
	 * 全局一次性查询限制
	 */
	private int globalMaxResults;
	/**
	 * 是否单数据源模式
	 */
	private boolean singleSite;
	/**
	 * 全局最大一次性查询数量限制
	 */
	private int globalFetchSize;
	/**
	 * 批操作下日志显示参数最大条数
	 */
	private int maxBatchLog;
	/**
	 * 全局查询超时
	 */
	private int selectTimeout;
	/**
	 * 全局更新超时
	 */
	private int updateTimeout;
	/**
	 * 全局删除超时
	 */
	private int deleteTimeout;
	/**
	 * 缓存结果集
	 */
	private boolean cacheResultset;
	/**
	 * 改写Oracle时允许移除StartWith子句
	 */
	private boolean allowRemoveStartWith;
	/**
	 * 检查类是否增强
	 */
	private boolean checkEnhancement;
	/**
	 * 不使用 t.*，指定每个列名
	 */
	private boolean specifyAllColumnName;
	/**
	 * 插入省略未赋值字段
	 */
	private boolean dynamicInsert;
	/**
	 * 更新时省略未赋值字段
	 */
	private boolean dynamicUpdate;
	/**
	 * 外连接一次查询出关系
	 */
	private boolean useOuterJoin;
	/**
	 * 为Postgres使用恢复点来保证事务延续
	 */
	private boolean keepTxForPG;
	/**
	 * 允许手工指定自增值
	 */
	private boolean manualSequence;
	/**
	 * 允许空查询
	 */
	private boolean allowEmptyQuery;
	/**
	 * 允许延迟加载
	 */
	private boolean enableLazyLoad;

	/**
	 * 启用/禁用一级缓存
	 */
	private boolean cacheLevel1;
	/**
	 * 缓存是否开启调试，这项配置不能在外部配置，默认是flase，只能通过代码或JMX开启
	 */
	private boolean cacheDebug;
	/**
	 * 定期检查连接
	 */
	private long heartBeatSleep;

	private boolean formatSQL;

	/**
	 * 可以创建序列
	 * 
	 */
	private boolean autoCreateSequence;

	/**
	 * 是否设置事务隔离级别
	 */
	private boolean setTxIsolation;
	/**
	 * 命名查询检查更新
	 */
	private boolean checkUpdateForNamedQueries;

	/**
	 * in条件中允许出现的最多参数。缺省500个。
	 */
	private int maxInConditions;

	private boolean jpaContinueCommitIfError;

	/**
	 * 多站点查询时启用并行查询
	 */
	private int parallelSelect;

	public String wrap = "";
	public String wrapt = "";

	private boolean checkSqlFunctions;

	private void init() {
		showStringLength = JefConfiguration.getBoolean(DbCfg.DB_ENCODING_SHOWLENGTH, false);
		setDbEncoding(JefConfiguration.get(DbCfg.DB_ENCODING, Charset.defaultCharset().name()));
		globalMaxResults = JefConfiguration.getInt(DbCfg.DB_MAX_RESULTS_LIMIT, 0);
		globalFetchSize = JefConfiguration.getInt(DbCfg.DB_FETCH_SIZE, 0);
		debugMode = JefConfiguration.getBoolean(Item.DB_DEBUG, false);
		maxBatchLog = JefConfiguration.getInt(DbCfg.DB_MAX_BATCH_LOG, 5);
		selectTimeout = JefConfiguration.getInt(DbCfg.DB_SELECT_TIMEOUT, 60);
		updateTimeout = JefConfiguration.getInt(DbCfg.DB_UPDATE_TIMEOUT, 60);
		deleteTimeout = JefConfiguration.getInt(DbCfg.DB_DELETE_TIMEOUT, 60);
		cacheResultset = JefConfiguration.getBoolean(DbCfg.DB_CACHE_RESULTSET, false);
		singleSite = JefConfiguration.getBoolean(DbCfg.DB_SINGLE_DATASOURCE, false);
		allowRemoveStartWith = JefConfiguration.getBoolean(DbCfg.ALLOW_REMOVE_START_WITH, false);
		checkEnhancement = JefConfiguration.getBoolean(DbCfg.DB_FORCE_ENHANCEMENT, true);
		specifyAllColumnName = JefConfiguration.getBoolean(DbCfg.DB_SPECIFY_ALLCOLUMN_NAME, false);
		dynamicInsert = JefConfiguration.getBoolean(DbCfg.DB_DYNAMIC_INSERT, false);
		dynamicUpdate = JefConfiguration.getBoolean(DbCfg.DB_DYNAMIC_UPDATE, true);
		useOuterJoin = JefConfiguration.getBoolean(DbCfg.DB_USE_OUTER_JOIN, true);
		keepTxForPG = JefConfiguration.getBoolean(DbCfg.DB_KEEP_TX_FOR_POSTGRESQL, true);
		manualSequence = JefConfiguration.getBoolean(DbCfg.DB_SUPPORT_MANUAL_GENERATE, false);
		allowEmptyQuery = JefConfiguration.getBoolean(DbCfg.ALLOW_EMPTY_QUERY, false);
		enableLazyLoad = JefConfiguration.getBoolean(DbCfg.DB_ENABLE_LAZY_LOAD, true);
		cacheLevel1 = JefConfiguration.getBoolean(DbCfg.CACHE_LEVEL_1, false);
		cacheDebug = System.getProperty("cache.debug") != null;
		setFormatSQL(JefConfiguration.getBoolean(DbCfg.DB_FORMAT_SQL, true));
		heartBeatSleep = JefConfiguration.getLong(DbCfg.DB_HEARTBEAT, 120000);
		setTxIsolation = JefConfiguration.getBoolean(DbCfg.DB_SET_ISOLATION, true);
		checkUpdateForNamedQueries = JefConfiguration.getBoolean(DbCfg.DB_NAMED_QUERY_UPDATE, debugMode);
		checkSqlFunctions = JefConfiguration.getBoolean(DbCfg.DB_CHECK_SQL_FUNCTIONS, true);
		filterAbsentTables = JefConfiguration.getBoolean(DbCfg.PARTITION_FILTER_ABSENT_TABLES, true);
		partitionCreateTableInneed = JefConfiguration.getBoolean(DbCfg.PARTITION_FILTER_ABSENT_TABLES, true);
		partitionInMemoryMaxRows = JefConfiguration.getInt(DbCfg.PARTITION_INMEMORY_MAXROWS, 0);
		autoCreateSequence = JefConfiguration.getBoolean(DbCfg.AUTO_SEQUENCE_CREATION, true);
		maxInConditions = JefConfiguration.getInt(DbCfg.DB_MAX_IN_CONDITIONS, 500);
		parallelSelect = JefConfiguration.getInt(DbCfg.PARTITION_PARALLEL_SELECT, 3);
		jpaContinueCommitIfError = JefConfiguration.getBoolean(DbCfg.DB_JPA_CONTINUE_COMMIT_IF_ERROR, false);
	}

	public int getMaxInConditions() {
		return maxInConditions;
	}

	public void setMaxInConditions(int maxInConditions) {
		this.maxInConditions = maxInConditions;
	}

	public boolean isAutoCreateSequence() {
		return autoCreateSequence;
	}

	public void setAutoCreateSequence(boolean autoCreateSequence) {
		this.autoCreateSequence = autoCreateSequence;
	}

	public boolean isPartitionCreateTableInneed() {
		return partitionCreateTableInneed;
	}

	public void setPartitionCreateTableInneed(boolean partitionCreateTableInneed) {
		this.partitionCreateTableInneed = partitionCreateTableInneed;
	}

	public boolean isFilterAbsentTables() {
		return filterAbsentTables;
	}

	public void setFilterAbsentTables(boolean filterAbsentTables) {
		this.filterAbsentTables = filterAbsentTables;
	}

	public boolean isCheckUpdateForNamedQueries() {
		return checkUpdateForNamedQueries;
	}

	public void setCheckUpdateForNamedQueries(boolean checkUpdateForNamedQueries) {
		this.checkUpdateForNamedQueries = checkUpdateForNamedQueries;
	}

	public boolean isEnableLazyLoad() {
		return enableLazyLoad;
	}

	public void setEnableLazyLoad(boolean enableLazyLoad) {
		this.enableLazyLoad = enableLazyLoad;
	}

	public boolean isAllowEmptyQuery() {
		return allowEmptyQuery;
	}

	public void setAllowEmptyQuery(boolean allowEmptyQuery) {
		this.allowEmptyQuery = allowEmptyQuery;
	}

	public boolean isManualSequence() {
		return manualSequence;
	}

	public void setManualSequence(boolean manualSequence) {
		this.manualSequence = manualSequence;
	}

	public boolean isKeepTxForPG() {
		return keepTxForPG;
	}

	public void setKeepTxForPG(boolean keepTxForPG) {
		this.keepTxForPG = keepTxForPG;
	}

	public boolean isUseOuterJoin() {
		return useOuterJoin;
	}

	public void setUseOuterJoin(boolean useOuterJoin) {
		this.useOuterJoin = useOuterJoin;
	}

	public String getDbEncoding() {
		return dbEncoding;
	}

	public void setDbEncoding(String dbEncoding) {
		this.dbEncoding = dbEncoding;
		this.dbEncodingCharset = Charset.forName(dbEncoding);
	}

	public boolean isShowStringLength() {
		return showStringLength;
	}

	public void setShowStringLength(boolean showStringLength) {
		this.showStringLength = showStringLength;
	}

	public Charset getDbEncodingCharset() {
		return dbEncodingCharset;
	}

	public int getGlobalMaxResults() {
		return globalMaxResults;
	}

	public boolean isCheckSqlFunctions() {
		return checkSqlFunctions;
	}

	public void setCheckSqlFunctions(boolean checkSqlFunctions) {
		this.checkSqlFunctions = checkSqlFunctions;
	}

	public void setGlobalMaxResults(int globalMaxResults) {
		this.globalMaxResults = globalMaxResults;
		QueryOption.DEFAULT.setMaxResult(globalMaxResults);
		QueryOption.DEFAULT_MAX1.setMaxResult(globalMaxResults);
	}

	public boolean isDebugMode() {
		return debugMode;
	}

	public void setDebugMode(boolean debugMode) {
		this.debugMode = debugMode;
	}

	public int getGlobalFetchSize() {
		return globalFetchSize;
	}

	public void setGlobalFetchSize(int globalFetchSize) {
		this.globalFetchSize = globalFetchSize;
		QueryOption.DEFAULT.setFetchSize(globalFetchSize);
		QueryOption.DEFAULT_MAX1.setFetchSize(globalFetchSize);
	}

	public int getMaxBatchLog() {
		return maxBatchLog;
	}

	public void setMaxBatchLog(int maxBatchLog) {
		this.maxBatchLog = maxBatchLog;
	}

	public int getSelectTimeout() {
		return selectTimeout;
	}

	public void setSelectTimeout(int selectTimeout) {
		this.selectTimeout = selectTimeout;
		QueryOption.DEFAULT.setQueryTimeout(selectTimeout);
		QueryOption.DEFAULT_MAX1.setQueryTimeout(selectTimeout);
	}

	public int getUpdateTimeout() {
		return updateTimeout;
	}

	public void setUpdateTimeout(int updateTimeout) {
		this.updateTimeout = updateTimeout;
	}

	public int getDeleteTimeout() {
		return deleteTimeout;
	}

	public void setDeleteTimeout(int deleteTimeout) {
		this.deleteTimeout = deleteTimeout;
	}

	public boolean isCacheResultset() {
		return cacheResultset;
	}

	public boolean isSingleSite() {
		return singleSite;
	}

	public int getParallelSelect() {
		return parallelSelect;
	}

	public void setParallelSelect(int parallelSelect) {
		this.parallelSelect = parallelSelect;
	}

	public void setSingleSite(boolean singleSite) {
		this.singleSite = singleSite;
	}

	public boolean isAllowRemoveStartWith() {
		return allowRemoveStartWith;
	}

	public void setAllowRemoveStartWith(boolean allowRemoveStartWith) {
		this.allowRemoveStartWith = allowRemoveStartWith;
	}

	public boolean isCheckEnhancement() {
		return checkEnhancement;
	}

	public void setCheckEnhancement(boolean checkEnhancement) {
		this.checkEnhancement = checkEnhancement;
	}

	public boolean isSpecifyAllColumnName() {
		return specifyAllColumnName;
	}

	public void setSpecifyAllColumnName(boolean specifyAllColumnName) {
		this.specifyAllColumnName = specifyAllColumnName;
	}

	public boolean isDynamicInsert() {
		return dynamicInsert;
	}

	public void setDynamicInsert(boolean dynamicInsert) {
		this.dynamicInsert = dynamicInsert;
	}

	public boolean isDynamicUpdate() {
		return dynamicUpdate;
	}

	public void setDynamicUpdate(boolean dynamicUpdate) {
		this.dynamicUpdate = dynamicUpdate;
	}

	public boolean isCacheLevel1() {
		return cacheLevel1;
	}

	public void setCacheLevel1(boolean cacheLevel1) {
		this.cacheLevel1 = cacheLevel1;
	}

	public void setCacheResultset(boolean cacheResultset) {
		this.cacheResultset = cacheResultset;
		QueryOption.DEFAULT.setCacheResultset(cacheResultset);
		QueryOption.DEFAULT_MAX1.setCacheResultset(cacheResultset);
	}

	public boolean isCacheDebug() {
		return cacheDebug;
	}

	public void setCacheDebug(boolean cacheDebug) {
		this.cacheDebug = cacheDebug;
	}

	public boolean isFormatSQL() {
		return formatSQL;
	}

	public void setFormatSQL(boolean value) {
		this.formatSQL = value;
		this.wrap = formatSQL ? "\n" : "";
		this.wrapt = formatSQL ? "\n\t" : "";
	}

	public long getHeartBeatSleep() {
		return heartBeatSleep;
	}

	public void setHeartBeatSleep(long heartBeatSleep) {
		this.heartBeatSleep = heartBeatSleep;
	}

	public String getServerName() {
		if (serverName == null) {
			serverName = ProcessUtil.getServerName();
		}
		return serverName;
	}

	public String getHostIp() {
		return ProcessUtil.getLocalIp();
	}

	public int getLoadedEntityCount() {
		return metaFacade.getLoadedEntityCount();
	}

	public void clearMetadatas() {
		metaFacade.clearMetadatas();
	}

	public String getSchemaMapping() {
		return metaFacade.getSchemaMapping();
	}

	public void setSchemaMapping(String data) {
		metaFacade.setSchemaMapping(data);
	}

	public String getSiteMapping() {
		return metaFacade.getSiteMapping();
	}

	public void setSiteMapping(String data) {
		metaFacade.setSiteMapping(data);
	}

	public boolean isSetTxIsolation() {
		return setTxIsolation;
	}

	public void setSetTxIsolation(boolean setTxIsolation) {
		this.setTxIsolation = setTxIsolation;
	}

	public String getMetadataResourcePattern() {
		if (metaFacade.getDefaultMeta() == null) {
			return "-";
		}
		return metaFacade.getDefaultMeta().getPattern();
	}

	public int getPartitionInMemoryMaxRows() {
		return partitionInMemoryMaxRows;
	}

	public void setPartitionInMemoryMaxRows(int partitionInMemoryMaxRows) {
		this.partitionInMemoryMaxRows = partitionInMemoryMaxRows;
	}

	public boolean isJpaContinueCommitIfError() {
		return jpaContinueCommitIfError;
	}

	public void setJpaContinueCommitIfError(boolean jpaContinueCommitIfError) {
		this.jpaContinueCommitIfError = jpaContinueCommitIfError;
	}

	public void setMetadataResourcePattern(String pattern) {
		MetaHolder.getMappingSchema("");
		if (metaFacade.getDefaultMeta() == null) {
			return;
		}
		metaFacade.getDefaultMeta().setPattern(pattern);
	}
}

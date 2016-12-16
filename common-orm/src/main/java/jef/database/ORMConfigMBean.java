package jef.database;

/**
 * JMX Bean for ORM Configuration.
 * 
 * Most of configuration items can be adjusted in runtime.
 * 
 * @author jiyi
 */
public interface ORMConfigMBean {

	/**
	 * 获得配置参数
	 * @return {@link DbCfg#DB_ENABLE_LAZY_LOAD}
	 */
	boolean isEnableLazyLoad();
	
	/**
	 * LOB字段延迟加载
	 * @return
	 */
	boolean isEnableLazyLob();

	/**
	 * LOB字段延迟加载开关
	 * @param enableLazyLob
	 */
	public void setEnableLazyLob(boolean enableLazyLob);
	
	/**
	 * 修改配置
	 * @param enableLazyLoad {@link DbCfg#DB_ENABLE_LAZY_LOAD}
	 */
	void setEnableLazyLoad(boolean enableLazyLoad);

	/**
	 * 获得配置参数
	 * @return {@link DbCfg#ALLOW_EMPTY_QUERY}
	 */
	boolean isAllowEmptyQuery();

	/**
	 * 修改配置
	 * @param allowEmptyQuery {@link DbCfg#ALLOW_EMPTY_QUERY}
	 */
	void setAllowEmptyQuery(boolean allowEmptyQuery);

	/**
	 * 获得配置参数
	 * @return {@link DbCfg#DB_SUPPORT_MANUAL_GENERATE}
	 */
	boolean isManualSequence();

	/**
	 * 修改配置
	 * @param manualSequence {@link DbCfg#DB_SUPPORT_MANUAL_GENERATE}
	 */
	void setManualSequence(boolean manualSequence);

	/**
	 * 获得配置参数
	 * @return {@link DbCfg#DB_KEEP_TX_FOR_POSTGRESQL}
	 */
	boolean isKeepTxForPG();

	/**
	 * 修改配置
	 * @param keepTxForPG {@link DbCfg#DB_KEEP_TX_FOR_POSTGRESQL}
	 */
	void setKeepTxForPG(boolean keepTxForPG);

	/**
	 * 获得配置参数
	 * @return {@link DbCfg#DB_USE_OUTER_JOIN}
	 */
	boolean isUseOuterJoin();

	/**
	 * 修改配置
	 * @param useOuterJoin {@link DbCfg#DB_USE_OUTER_JOIN}
	 */
	void setUseOuterJoin(boolean useOuterJoin);

	/**
	 * 获得配置参数
	 * @return {@link DbCfg#DB_ENCODING}
	 */
	String getDbEncoding();

	/**
	 * 修改配置
	 * @param dbEncoding {@link DbCfg#DB_ENCODING}
	 */
	void setDbEncoding(String dbEncoding);

	/**
	 * 获得配置参数
	 * @return {@link DbCfg#DB_ENCODING_SHOWLENGTH}
	 */
	boolean isShowStringLength();

	/**
	 * 修改配置
	 * @param showStringLength {@link DbCfg#DB_ENCODING_SHOWLENGTH}
	 */
	void setShowStringLength(boolean showStringLength);

	/**
	 * 获得配置参数
	 * @return {@link DbCfg#DB_MAX_RESULTS_LIMIT}
	 */
	int getGlobalMaxResults();

	/**
	 * 修改配置
	 * @param globalMaxResults {@link DbCfg#DB_MAX_RESULTS_LIMIT}
	 */
	void setGlobalMaxResults(int globalMaxResults);

	/**
	 * 获得配置参数
	 * @return 调试开关值
	 */
	boolean isDebugMode();

	/**
	 * 修改配置
	 * @param debugMode 是否调试
	 */
	void setDebugMode(boolean debugMode);

	/**
	 * 获得配置参数
	 * @return {@link DbCfg#DB_FETCH_SIZE}
	 */
	int getGlobalFetchSize();

	/**
	 * 修改配置
	 * @param globalFetchSize {@link DbCfg#DB_FETCH_SIZE}
	 */
	void setGlobalFetchSize(int globalFetchSize);

	/**
	 * 获得配置参数
	 * @return {@link DbCfg#DB_MAX_BATCH_LOG}
	 */
	int getMaxBatchLog();

	/**
	 * 修改配置
	 * @param maxBatchLog {@link DbCfg#DB_MAX_BATCH_LOG}
	 */
	void setMaxBatchLog(int maxBatchLog);

	/**
	 * 获得配置参数
	 * @return {@link DbCfg#DB_SELECT_TIMEOUT}
	 */
	int getSelectTimeout();

	/**
	 * 修改配置
	 * @param selectTimeout {@link DbCfg#DB_SELECT_TIMEOUT}
	 */
	void setSelectTimeout(int selectTimeout);

	/**
	 * 获得配置参数
	 * @return {@link DbCfg#DB_UPDATE_TIMEOUT}
	 */
	int getUpdateTimeout();

	/**
	 * 修改配置
	 * @param updateTimeout  {@link DbCfg#DB_UPDATE_TIMEOUT}
	 */
	void setUpdateTimeout(int updateTimeout);

	/**
	 * 获得配置参数
	 * @return {@link DbCfg#DB_DELETE_TIMEOUT}
	 */
	int getDeleteTimeout();

	/**
	 * 修改配置
	 * @param deleteTimeout {@link DbCfg#DB_DELETE_TIMEOUT}
	 */
	void setDeleteTimeout(int deleteTimeout);

	/**
	 * 获得配置参数
	 * @return {@link DbCfg#DB_CACHE_RESULTSET}
	 */
	boolean isCacheResultset();


	/**
	 * 修改配置
	 * @param cacheResultset {@link DbCfg#DB_CACHE_RESULTSET}
	 */
	void setCacheResultset(boolean cacheResultset);
	
	/**
	 * 获得配置参数
	 * @return {@link DbCfg#DB_SINGLE_DATASOURCE}
	 */
	boolean isSingleSite();

	/**
	 * 修改配置
	 * @param singleSite {@link DbCfg#DB_SINGLE_DATASOURCE}
	 */
	void setSingleSite(boolean singleSite);

	/**
	 * 获得配置参数
	 * @return {@link DbCfg#ALLOW_REMOVE_START_WITH}
	 */
	boolean isAllowRemoveStartWith();

	/**
	 * 修改配置
	 * @param allowRemoveStartWith {@link DbCfg#ALLOW_REMOVE_START_WITH}
	 */
	void setAllowRemoveStartWith(boolean allowRemoveStartWith);

	/**
	 * 获得配置参数
	 * @return {@link DbCfg#DB_FORCE_ENHANCEMENT}
	 */
	boolean isCheckEnhancement();

	/**
	 * 修改配置
	 * @param checkEnhancement {@link DbCfg#DB_FORCE_ENHANCEMENT}
	 */
	void setCheckEnhancement(boolean checkEnhancement);

	/**
	 * 获得配置参数
	 * @return {@link DbCfg#DB_SPECIFY_ALLCOLUMN_NAME}
	 */
	boolean isSpecifyAllColumnName();

	/**
	 * 修改配置
	 * @param specifyAllColumnName {@link DbCfg#DB_SPECIFY_ALLCOLUMN_NAME}
	 */
	void setSpecifyAllColumnName(boolean specifyAllColumnName);

	/**
	 * 获得配置参数
	 * @return {@link DbCfg#DB_DYNAMIC_INSERT}
	 */
	boolean isDynamicInsert();

	/**
	 * 修改配置
	 * @param dynamicInsert {@link DbCfg#DB_DYNAMIC_INSERT}
	 */
	void setDynamicInsert(boolean dynamicInsert);

	/**
	 * 获得配置参数
	 * @return {@link DbCfg#DB_DYNAMIC_UPDATE}
	 */
	boolean isDynamicUpdate();

	/**
	 * 修改配置
	 * @param dynamicUpdate  {@link DbCfg#DB_DYNAMIC_UPDATE}
	 */
	void setDynamicUpdate(boolean dynamicUpdate);

	/**
	 * 获得配置参数
	 * @return {@link DbCfg#CACHE_LEVEL_1}
	 */
	boolean isCacheLevel1();

	/**
	 * 修改配置
	 * @param cacheLevel1 {@link DbCfg#CACHE_LEVEL_1}
	 */
	void setCacheLevel1(boolean cacheLevel1);

	/**
	 * 获得配置参数
	 * @return 一级缓存调试开关，该开关只能通过API调节。主要供开发调试用。
	 */
	boolean isCacheDebug();

	/**
	 * 修改配置
	 * @param cacheDebug 一级缓存调试开关，该开关只能通过API调节。主要供开发调试用。
	 */
	void setCacheDebug(boolean cacheDebug);

	/**
	 * 获得配置参数
	 * @return {@link DbCfg#DB_FORMAT_SQL}
	 */
	boolean isFormatSQL();

	/**
	 * 修改配置
	 * @param value {@link DbCfg#DB_FORMAT_SQL}
	 */
	void setFormatSQL(boolean value);
	/**
	 * 获得配置参数 
	 * @return {@link DbCfg#DB_HEARTBEAT}
	 */
	long getHeartBeatSleep();

	/**
	 * 修改配置
	 * @param heartBeatSleep {@link DbCfg#DB_HEARTBEAT}
	 */
	void setHeartBeatSleep(long heartBeatSleep);

	/**
	 * 得到当前所在主机的IP地址
	 * @return
	 */
	String getHostIp();

	/**
	 * 得到当前所在主机的名称
	 * @return
	 */
	String getServerName();

	/**
	 * 得到已经加载的Entity的总数
	 * @return
	 */
	int getLoadedEntityCount();

	/**
	 * 清除缓存中的Entity Metadata.
	 */
	void clearMetadatas();

	/**
	 * 得到Schema映射配置字符串
	 * @return {@link DbCfg#SCHEMA_MAPPING}
	 */
	String getSchemaMapping();

	/**
	 * 修改配置
	 * @param data  {@link DbCfg#SCHEMA_MAPPING}
	 */
	void setSchemaMapping(String data);

	/**
	 * 获得配置参数 
	 * @return {@link DbCfg#DB_DATASOURCE_MAPPING}
	 */
	String getSiteMapping();

	/**
	 * 修改配置
	 * @param data {@link DbCfg#DB_DATASOURCE_MAPPING}
	 */
	void setSiteMapping(String data);

	/**
	 * 获得配置参数 
	 * @return {@link DbCfg#METADATA_RESOURCE_PATTERN}
	 */
	String getMetadataResourcePattern();

	/**
	 * 修改配置
	 * @param pattern {@link DbCfg#METADATA_RESOURCE_PATTERN}
	 */
	void setMetadataResourcePattern(String pattern);

	/**
	 * 获得配置参数 
	 * @return {@link DbCfg#DB_NAMED_QUERY_UPDATE}
	 */
	boolean isCheckUpdateForNamedQueries();

	/**
	 * 修改配置
	 * @param checkUpdateForNamedQueries {@link DbCfg#DB_NAMED_QUERY_UPDATE}
	 */
	void setCheckUpdateForNamedQueries(boolean checkUpdateForNamedQueries);

	/**
	 * 获得配置参数 
	 * @return {@link DbCfg#PARTITION_INMEMORY_MAXROWS}
	 */
	int getPartitionInMemoryMaxRows();

	/**
	 * 修改配置
	 * @param partitionInMemoryMaxRows {@link DbCfg#PARTITION_INMEMORY_MAXROWS}
	 */
	void setPartitionInMemoryMaxRows(int partitionInMemoryMaxRows);

	/**
	 * 获得配置参数 
	 * @return {@link DbCfg#DB_SET_ISOLATION}
	 */
	boolean isSetTxIsolation();

	/**
	 * 修改配置
	 * @param setTxIsolation {@link DbCfg#DB_SET_ISOLATION}
	 */
	void setSetTxIsolation(boolean setTxIsolation);

	/**
	 * 获得配置参数 
	 * @return {@link DbCfg#DB_CHECK_SQL_FUNCTIONS}
	 */
	boolean isCheckSqlFunctions();

	/**
	 * 修改配置
	 * @param checkSqlFunctions {@link DbCfg#DB_CHECK_SQL_FUNCTIONS}
	 */
	void setCheckSqlFunctions(boolean checkSqlFunctions);

	/**
	 * 获得配置参数 
	 * @return {@link DbCfg#AUTO_SEQUENCE_CREATION}
	 */
	boolean isAutoCreateSequence();

	/**
	 * 修改配置
	 * @param autoCreateSequence {@link DbCfg#AUTO_SEQUENCE_CREATION}
	 */
	void setAutoCreateSequence(boolean autoCreateSequence);

	/**
	 * 获得配置参数 
	 * @return {@link DbCfg#PARTITION_CREATE_TABLE_INNEED}
	 */
	boolean isPartitionCreateTableInneed();

	/**
	 * 修改配置
	 * @param partitionCreateTableInneed  {@link DbCfg#PARTITION_CREATE_TABLE_INNEED}
	 */
	void setPartitionCreateTableInneed(boolean partitionCreateTableInneed);

	/**
	 * 获得配置参数 
	 * @return {@link DbCfg#PARTITION_FILTER_ABSENT_TABLES}
	 */
	boolean isFilterAbsentTables();

	/**
	 * 修改配置
	 * @param filterAbsentTables {@link DbCfg#PARTITION_FILTER_ABSENT_TABLES}
	 */
	void setFilterAbsentTables(boolean filterAbsentTables);

	/**
	 * 获得配置参数 
	 * @return value of {@link DbCfg#DB_JPA_CONTINUE_COMMIT_IF_ERROR}.
	 */
	boolean isJpaContinueCommitIfError();

	/**
	 * 修改配置
	 * @param jpaContinueCommitIfError {@link DbCfg#DB_JPA_CONTINUE_COMMIT_IF_ERROR}
	 * 
	 */
	void setJpaContinueCommitIfError(boolean jpaContinueCommitIfError);
	
	boolean isGenerateBySequenceAndIdentityToAUTO();
	
	void setGenerateBySequenceAndIdentityToAUTO(boolean generateBySequenceAndIdentityToAUTO);

}

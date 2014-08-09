package jef.database;

/**
 * JMX Bean for ORM Configuration.
 * 
 * Most of configuration items can be adjusted runtime.
 * 
 * @author jiyi
 */
public interface ORMConfigMBean {

	boolean isEnableLazyLoad();

	void setEnableLazyLoad(boolean enableLazyLoad);

	boolean isAllowEmptyQuery();

	void setAllowEmptyQuery(boolean allowEmptyQuery);

	boolean isManualSequence();

	void setManualSequence(boolean manualSequence);

	boolean isKeepTxForPG();

	void setKeepTxForPG(boolean keepTxForPG);

	boolean isUseOuterJoin();

	void setUseOuterJoin(boolean useOuterJoin);

	String getDbEncoding();

	void setDbEncoding(String dbEncoding);

	boolean isShowStringLength();

	void setShowStringLength(boolean showStringLength);

	int getGlobalMaxResults();

	void setGlobalMaxResults(int globalMaxResults);

	boolean isDebugMode();

	void setDebugMode(boolean debugMode);

	int getGlobalFetchSize();

	void setGlobalFetchSize(int globalFetchSize);

	int getMaxBatchLog();

	void setMaxBatchLog(int maxBatchLog);

	int getSelectTimeout();

	void setSelectTimeout(int selectTimeout);

	int getUpdateTimeout();

	void setUpdateTimeout(int updateTimeout);

	int getDeleteTimeout();

	void setDeleteTimeout(int deleteTimeout);

	boolean isCacheResultset();

	boolean isSingleSite();

	void setSingleSite(boolean singleSite);

	boolean isAllowRemoveStartWith();

	void setAllowRemoveStartWith(boolean allowRemoveStartWith);

	boolean isCheckEnhancement();

	void setCheckEnhancement(boolean checkEnhancement);

	boolean isSpecifyAllColumnName();

	void setSpecifyAllColumnName(boolean specifyAllColumnName);

	boolean isDynamicInsert();

	void setDynamicInsert(boolean dynamicInsert);

	boolean isDynamicUpdate();

	void setDynamicUpdate(boolean dynamicUpdate);

	void setCacheResultset(boolean cacheResultset);

	boolean isCacheLevel1();

	void setCacheLevel1(boolean cacheLevel1);

	boolean isCacheDebug();

	void setCacheDebug(boolean cacheDebug);

	boolean isFormatSQL();

	void setFormatSQL(boolean value);

	long getHeartBeatSleep();

	void setHeartBeatSleep(long heartBeatSleep);

	String getHostIp();

	String getServerName();

	int getLoadedEntityCount();

	void clearMetadatas();

	String getSchemaMapping();

	void setSchemaMapping(String data);

	String getSiteMapping();

	void setSiteMapping(String data);

	String getMetadataResourcePattern();

	void setMetadataResourcePattern(String pattern);

	boolean isCheckUpdateForNamedQueries();

	void setCheckUpdateForNamedQueries(boolean checkUpdateForNamedQueries);

	int getPartitionInMemoryMaxRows();

	void setPartitionInMemoryMaxRows(int partitionInMemoryMaxRows);

	boolean isSetTxIsolation();

	void setSetTxIsolation(boolean setTxIsolation);

	boolean isCheckSqlFunctions();

	void setCheckSqlFunctions(boolean checkSqlFunctions);
}

//package jef.database.routing.jdbc;
//
//import java.io.PrintWriter;
//import java.sql.Connection;
//import java.sql.SQLException;
//import java.util.Collections;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Map;
//
//import javax.sql.DataSource;
//
//public class TDataSource extends TDataSourceConfig implements DataSource, Cloneable {
//	private final TDSProperties properties = new TDSProperties();
//	private static final long DEFAULT_TIMEOUT_THRESHOLD = 100;
//	private static DataSource indexMappingCacheDatasource = null;
//
//	private ReplicationConfig replicationConfig;
//	private String replicationConfigFile; // 新本地文件配置
//	@SuppressWarnings("unchecked")
//	private Map<String, DataSource> replicationTargetDataSources = Collections.EMPTY_MAP;
//	
//	public TDataSourceState state = new TDataSourceState(""); //安全冗余
//
//	public void init() {
//		state = new TDataSourceState(getAppName());
//		super.init();
//		
////		//试点去掉行复制，start by v3.4 20140321
////		initReplication();
//	}
//
//	/**
//	 * 初始化数据复制配置
//	 * @author junyu
//	 */
//	@SuppressWarnings("unused")
//	private void initReplication() {
//		if (this.isHandleReplication) {
//			this.replicationConfig = new ReplicationConfig();
//			this.replicationConfig.setAppName(this.getAppName());
//			this.replicationConfig.setUseLocalConfig(this.isUseLocalConfig());
//			this.replicationConfig.setReplicationConfigFile(
//			        doExternalResolve(this.replicationConfigFile)[0]); /* 使用外部  springContext 解析配置文件路径 */
//			this.replicationConfig.init(this);
//
//			if (this.defaultDispatcher instanceof SpringBasedDispatcherImpl) {
//				SpringBasedDispatcherImpl dispatcher = (SpringBasedDispatcherImpl) this.defaultDispatcher;
//				for (Map.Entry<String, BizTDDLContext> e : this.replicationConfig.getLogicTableName2TDDLContext()
//						.entrySet()) {
//					if(dispatcher.getRoot()!=null){
//						LogicTable logicTable = dispatcher.getRoot().getLogicTable(e.getKey());
//						if (logicTable != null) {
//							logicTable.setNeedRowCopy(true);
//							logicTable.setUniqueKeys(e.getValue().getUniqueKey());
//						}
//					}else if(dispatcher.getVtabroot()!=null){
//						TableRule logicTable = (TableRule) dispatcher.getVtabroot().getVirtualTable(e.getKey());
//						if (logicTable != null) {
//							logicTable.setNeedRowCopy(true);
//							logicTable.setUniqueKeys(e.getValue().getUniqueKey());
//						}
//					}else{
//						logger.error("[REPLICATION] rule error,no old rule or new rule!check the config");
//						//can not be here
//					}
//				}
//			}
//		}
//
//		try {
//			this.hookPoints = new HookPoints();
//			for (Adapter adapter : adapters) {
//				adapter.init(this, hookPoints);
//			}
//		} catch (SQLException e) {
//			throw new RuntimeException(e);
//		}
//	}
//
//	public Connection getConnection() throws SQLException {
//		TConnectionImp returnConnection = null;
//		if (TransactionCrossDb.NOT_ALLOW_READ == restrictionLEVEL) {
//			returnConnection = new TConnectionImp(getAppName(), enableProfileRealDBAndTables, pipelineFactory);
//		} else if (TransactionCrossDb.ALLOW_READ == restrictionLEVEL) {
//			returnConnection = new AllowReadLevelTConnection(getAppName(), enableProfileRealDBAndTables, pipelineFactory);
//		} else if (TransactionCrossDb.ALLOW_ALL == restrictionLEVEL) {
//			returnConnection = new AllowAllLevelTconnection(getAppName(), enableProfileRealDBAndTables, pipelineFactory);
//		}
//		buildConnection1(returnConnection);
//		return returnConnection;
//	}
//
//	private void buildConnection1(TConnectionImp connectionImp) {
//		TddlRuntime rt = super.runtimeConfigHolder.get();
//		connectionImp.setHookPoints(hookPoints);
//		if (rt == null) {
//			throw new IllegalArgumentException("rt is null");
//		}
//		connectionImp.setProperties(properties);
//		connectionImp.setDsMap(rt.dsMap);
//	}
//
//	public Connection getConnection(String username, String password) throws SQLException {
//		return getConnection();
//	}
//
//	public int getLoginTimeout() throws SQLException {
//		throw new UnsupportedOperationException("getLoginTimeout");
//	}
//
//	public void setLoginTimeout(int seconds) throws SQLException {
//		throw new UnsupportedOperationException("setLoginTimeout");
//	}
//
//	public PrintWriter getLogWriter() throws SQLException {
//		throw new UnsupportedOperationException("getLogWriter");
//	}
//
//	public void setLogWriter(PrintWriter out) throws SQLException {
//		throw new UnsupportedOperationException("setLogWriter");
//	}
//
//	@Override
//	protected TDataSource clone() throws CloneNotSupportedException {
//		return (TDataSource) super.clone();
//	}
//
//	@Override
//	public String toString() {
//		return new StringBuilder("{MasterOnly:").append(isMasterOnly).append(",SlaveOnly:").append(this.isSlaveOnly)
//				.append(",ReadOnly:").append(this.isReadOnly).append("}").toString();
//	}
//
//	public void setTimeoutThreshold(long timeoutThreshold) {
//		this.properties.timeoutThreshold = timeoutThreshold;
//	}
//
//	public static DataSource getIndexMappingCacheDatasource() {
//		return indexMappingCacheDatasource;
//	}
//
//	public static void setIndexMappingCacheDatasource(DataSource indexMappingCacheDatasource) {
//		TDataSource.indexMappingCacheDatasource = indexMappingCacheDatasource;
//	}
//
//	@SuppressWarnings("unchecked")
//	private List<Adapter> adapters = Collections.EMPTY_LIST;
//
//	public void setListenerAdapter(Adapter adapter) throws SQLException {
//		LinkedList<Adapter> adapters = new LinkedList<Adapter>();
//		adapters.add(adapter);
//		setListenerAdapters(adapters);
//	}
//
//	public void setListenerAdapters(List<Adapter> adapters) throws SQLException {
//		this.adapters = adapters;
//		for (Adapter adapter : adapters) {
//			if (adapter instanceof ReplicationAdapter) {
//				this.isHandleReplication = true;
//				break;
//			}
//		}
//	}
//
//	public ReplicationConfig getReplicationConfig() {
//		return replicationConfig;
//	}
//
//	public void setReplicationConfigFile(String replicationConfigFile) {
//		this.replicationConfigFile = replicationConfigFile;
//	}
//
//	public String getReplicationConfigFile() {
//		return replicationConfigFile;
//	}
//
//	public boolean isEnableReplaceTableSuffixByIdentifier() {
//		return properties.enableReplaceTableSuffixByIdentifier;
//	}
//
//	public void setEnableReplaceTableSuffixByIdentifier(boolean enableReplaceTableSuffixByIdentifier) {
//		this.properties.enableReplaceTableSuffixByIdentifier = enableReplaceTableSuffixByIdentifier;
//	}
//
//	public void setUsePlaceHolder(boolean usePlaceHolder) {
//		this.properties.usePlaceHolder = usePlaceHolder;
//	}
//
//	public boolean getUsePlaceHolder() {
//		return this.properties.usePlaceHolder;
//	}
//
//	public void setRemoveThreadLocalOnCloseConn(boolean re) {
//		this.properties.removeThreadLocalOnCloseConn = re;
//	}
//
//	public boolean getRemoveThreadLocalOnCloseConn() {
//		return properties.removeThreadLocalOnCloseConn;
//	}
//
//	public DataSource getDataSource(String dbIndex) {
//		return this.runtimeConfigHolder.get().dsMap.get(dbIndex);
//	}
//
//	public static class TDSProperties {
//		/**
//		 * 超时时间
//		 */
//		public long timeoutThreshold = DEFAULT_TIMEOUT_THRESHOLD;
//		/**
//		 * 是否允许replace后缀表名的模式
//		 */
//		public boolean enableReplaceTableSuffixByIdentifier = false;
//		/**
//		 * 是否使用@table@的模式来进行表名指定
//		 */
//		public boolean usePlaceHolder = false;
//		/**
//		 * 是否应该在关闭连接的时候移除ThreadLocal
//		 */
//		public boolean removeThreadLocalOnCloseConn = false;
//	}
//
//	/**
//	 * 多连接事务处理。默认为所有都不允许读
//	 */
//	private TransactionCrossDb restrictionLEVEL = TransactionCrossDb.NOT_ALLOW_READ;
//
//	public enum TransactionCrossDb {
//		/**
//		 * 在事务状态下，其他数据库的连接不允许读
//		 */
//		NOT_ALLOW_READ,
//		/**
//		 * 在事务状态下，其他数据库的连接只允许读，不允许写入，写入会抛错误
//		 */
//		ALLOW_READ,
//		/**
//		 * 在事务状态下，其他数据库连接允许写也允许读。
//		 */
//		ALLOW_ALL;
//	}
//
//	public TransactionCrossDb getTransactionCrossDb() {
//		return restrictionLEVEL;
//	}
//
//	public void setTransactionCrossDb(TransactionCrossDb restrictionLEVEL) {
//		this.restrictionLEVEL = restrictionLEVEL;
//	}
//
//	public Map<String, DataSource> getReplicationTargetDataSources() {
//		return replicationTargetDataSources;
//	}
//
//	public void setReplicationTargetDataSources(Map<String, DataSource> replicationTargetDataSources) {
//		this.replicationTargetDataSources = replicationTargetDataSources;
//	}
//
//
//	public boolean isWrapperFor(Class<?> iface) throws SQLException {
//		return this.getClass().isAssignableFrom(iface);
//	}
//
//	@SuppressWarnings("unchecked")
//	public <T> T unwrap(Class<T> iface) throws SQLException {
//		try {
//			return (T) this;
//		} catch (Exception e) {
//			throw new SQLException(e);
//		}
//	}
//}

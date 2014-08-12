package jef.database.routing.jdbc;


public abstract class ExecuteSQLProfiler {
	/**
	 * 是否允许验证所有的库和表
	 */
	private boolean enableProfileRealDBAndTables;

	protected long timeoutForEachTable = DEFAULT_TIMEOUT_FOR_EACH_TABLE;
	/**
	 * 默认的每个表执行sql的超时时间,主要用于打日志
	 */
	public static final long DEFAULT_TIMEOUT_FOR_EACH_TABLE = 100;

	private long timeoutThreshold;

//	protected void profileRealDatabaseAndTables(String database,
//			RealSqlContext targetSql, long during) {
//		if (isEnableProfileRealDBAndTables()) {
//			if (during >= timeoutForEachTable) {
//				matrixSqlAdd(buildExecuteDBAndTableKey1(database, targetSql
//						.getRealTable()), buildExecuteSqlKey2(targetSql
//						.getSql()), KEY3_EXECUTE_A_SQL_TIMEOUT, during, 1);
//			} else {
//				matrixSqlAdd(buildExecuteDBAndTableKey1(database, targetSql
//						.getRealTable()), buildExecuteSqlKey2(targetSql
//						.getSql()), KEY3_EXECUTE_A_SQL_SUCCESS, during, 1);
//			}
//		}
//	}
//
//	/**
//	 * 只对db+tab进行profile
//	 * 
//	 * @param virtualTableName
//	 * @param databaseSize
//	 * @param targetTablesSize
//	 * @param sql
//	 */
//	protected void profileNumberOfDBAndTablesOnly(String virtualTableName,
//			int databaseSize, int targetTablesSize, String sql) {
//		// timeout time consuming in Write or Read DB divide total time
//		matrixSqlAdd(buildTableKey1(virtualTableName), buildExecuteSqlKey2(sql),
//				KEY3_EXECUTE_A_SQL_SUCCESS_DBTAB, databaseSize,
//				targetTablesSize);
//	}
//
//	protected void profileNumberOfDBAndTablesAndDuringTime(
//			String virtualTableName, int databaseSize, int targetTablesSize,
//			String sql, long elapsedTime) {
//		if (elapsedTime > timeoutThreshold) {
//			// timeout time consuming in Write or Read DB divide total time
//			matrixSqlAdd(buildTableKey1(virtualTableName), buildExecuteSqlKey2(sql),
//					KEY3_EXECUTE_A_SQL_TIMEOUT, elapsedTime, 1);
//			matrixSqlAdd(buildTableKey1(virtualTableName), buildExecuteSqlKey2(sql),
//					KEY3_EXECUTE_A_SQL_TIMEOUT_DBTAB, databaseSize,
//					targetTablesSize);
//		} else {
//			// normal time consuming in Write or Read DB divide total time
//			matrixSqlAdd(buildTableKey1(virtualTableName), buildExecuteSqlKey2(sql),
//					KEY3_EXECUTE_A_SQL_SUCCESS, elapsedTime, 1);
//			matrixSqlAdd(buildTableKey1(virtualTableName), buildExecuteSqlKey2(sql),
//					KEY3_EXECUTE_A_SQL_SUCCESS_DBTAB, databaseSize,
//					targetTablesSize);
//		}
//		//TMonitor.addWithLimit(sql, elapsedTime, 1);
//	}
//
//	protected void profileDuringTime(List<SQLException> exceptions ,String virtualTableName,String sql, long elapsedTime) {
//		if (exceptions != null && !exceptions.isEmpty()) {
//			matrixSqlAdd(buildTableKey1(virtualTableName), buildExecuteSqlKey2(sql),
//					KEY3_EXECUTE_A_SQL_EXCEPTION, elapsedTime, 1);
//		} else {
//			if (elapsedTime > timeoutThreshold) {
//				// timeout time consuming in Write or Read DB divide total time
//				matrixSqlAdd(buildTableKey1(virtualTableName), buildExecuteSqlKey2(sql),
//						KEY3_EXECUTE_A_SQL_TIMEOUT, elapsedTime, 1);
//			} else {
//				// normal time consuming in Write or Read DB divide total time
//				matrixSqlAdd(buildTableKey1(virtualTableName), buildExecuteSqlKey2(sql),
//						KEY3_EXECUTE_A_SQL_SUCCESS, elapsedTime, 1);
//			}
//		}
//	}
//
//	protected void profileRealDatabaseAndTablesWithException(String database,
//			RealSqlContext targetSql, long during) {
//		if (isEnableProfileRealDBAndTables()) {
//			matrixSqlAdd(buildExecuteDBAndTableKey1(database, targetSql.getRealTable()),
//					buildExecuteSqlKey2(targetSql.getSql()),
//					KEY3_EXECUTE_A_SQL_EXCEPTION, during, 1);
//		}
//	}
//
//	/**
//	 * 异常情况下的profile，只有当exception链不为空，并且内部也非空list的情况下才会输出。
//	 * 
//	 * @param exceptions
//	 * @param virtualTableName
//	 * @param sql
//	 * @param elapsedTime
//	 */
//	protected void profileWithException(List<SQLException> exceptions,
//			String virtualTableName, String sql, long elapsedTime) {
//		if (exceptions != null && !exceptions.isEmpty()) {
//			matrixSqlAdd(buildTableKey1(virtualTableName), buildExecuteSqlKey2(sql),
//					KEY3_EXECUTE_A_SQL_EXCEPTION, elapsedTime, 1);
//		}
//		//TMonitor.addWithLimit(sql, elapsedTime, 1);
//	}
//
//	public boolean isEnableProfileRealDBAndTables() {
//		return Monitor.isStatRealDbInWrapperDs == null ? enableProfileRealDBAndTables : Monitor.isStatRealDbInWrapperDs;
//	}
//
//	public void setEnableProfileRealDBAndTables(
//			boolean enableProfileRealDBAndTables) {
//		this.enableProfileRealDBAndTables = enableProfileRealDBAndTables;
//	}
//
//	public long getTimeoutForEachTable() {
//		return timeoutForEachTable;
//	}
//
//	public void setTimeoutForEachTable(long timeoutForEachTable) {
//		this.timeoutForEachTable = timeoutForEachTable;
//	}
//
//	public long getTimeoutThreshold() {
//		return timeoutThreshold;
//	}
//
//	public void setTimeoutThreshold(long timeoutThreshold) {
//		this.timeoutThreshold = timeoutThreshold;
//	}
}

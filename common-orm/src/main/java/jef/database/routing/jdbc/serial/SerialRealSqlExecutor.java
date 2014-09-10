package jef.database.routing.jdbc.serial;


/**
 * @author junyu
 * 
 */
public class SerialRealSqlExecutor  {
//	private static final Log logger = LogFactory
//			.getLog(SerialRealSqlExecutor.class);
//
//	public SerialRealSqlExecutor(ConnectionManager connectionManager) {
//		super(connectionManager);
//	}
//
//	public void serialQuery(
//			ConcurrentLinkedQueue<QueryReturn> queryReturnQueue,
//			ExecutionPlan executionPlan, TStatementImp tStatementImp) {
//		setSpecialProperty(tStatementImp, executionPlan);
//
//		boolean isPrepareStatement = this.isPreparedStatement(tStatementImp);
//
//		Map<String, List<RealSqlContext>> sqlMap = executionPlan.getSqlMap();
//		for (final Entry<String, List<RealSqlContext>> dbEntry : sqlMap
//				.entrySet()) {
//			String dbSelectorId = dbEntry.getKey();
//			try {
//				Connection connection = connectionManager.getConnection(
//						dbSelectorId, executionPlan.isGoSlave());
//
//				List<RealSqlContext> sqlList = dbEntry.getValue();
//
//				for (RealSqlContext sql : sqlList) {
//					QueryReturn qr = null;
//					long start = System.currentTimeMillis();
//					if (isPrepareStatement) {
//						qr = executeQueryIntervalPST(connection, sql, executionPlan.getSqlMetaData());
//					} else {
//						qr = executeQueryIntervalST(connection, sql, executionPlan.getSqlMetaData());
//					}
//
//					long during = System.currentTimeMillis() - start;
//
//					qr.setCurrentDBIndex(dbSelectorId);
//					queryReturnQueue.add(qr);
//					profileRealDatabaseAndTables(dbSelectorId, sql, during);
//				}
//			} catch (SQLException e) {
//				//第一时间打印异常
//				logger.error(e);
//				// 发生异常后，将异常塞入一个QueryReturn里面直接返回，不再进行后续查询
//				QueryReturn qr = new QueryReturn();
//				qr.add2ExceptionList(e);
//				tryCloseConnection(qr.getExceptions(), dbSelectorId);
//				queryReturnQueue.add(qr);
//				break;
//			}
//		}
//	}
//
//	protected void tryCloseConnection(String dbIndex) {
//		tryCloseConnection(null, dbIndex);
//	}
//
//	public void serialUpdate(
//			ConcurrentLinkedQueue<UpdateReturn> updateReturnQueue,
//			ExecutionPlan executionPlan, TStatementImp tStatementImp) {
//		setSpecialProperty(tStatementImp, executionPlan);
//
//		boolean isPrepareStatement = this.isPreparedStatement(tStatementImp);
//
//		Map<String, List<RealSqlContext>> sqlMap = executionPlan.getSqlMap();
//		for (final Entry<String, List<RealSqlContext>> dbEntry : sqlMap
//				.entrySet()) {
//			UpdateReturn ur = null;
//			if (isPrepareStatement) {
//				ur = executeUpdateIntervalPST(executionPlan, dbEntry);
//			} else {
//				ur = executeUpdateIntervalST(executionPlan, dbEntry);
//			}
//
//			updateReturnQueue.add(ur);
//		}
//	}
}

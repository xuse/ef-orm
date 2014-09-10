package jef.database.routing.jdbc.serial;


/**
 * @author junyu
 *
 */
public class SimpleSerialRealSqlExecutor extends SerialRealSqlExecutor {
//	private static final Log logger = LogFactory
//	.getLog(SimpleSerialRealSqlExecutor.class);
//	
//	public SimpleSerialRealSqlExecutor(ConnectionManager connectionManager) {
//		super(connectionManager);
//	}
//	
//    protected String currentDBIndex = null;
//	
//	protected int tableIndex = 0;
//
//	private List<RealSqlContext> sqlContextToBeExecOnCurrentDB;
//	
//	protected ExecutionPlan executionPlanIn;
//	
//	private List<String> dbIndexList;
//	
//	protected Statement statement;
//
//	protected ResultSet resultSet;
//	
//	private final Random ran = new Random();
//	
//	private boolean isPreparedStatement=false;
//	
//	@Override
//	public void serialQuery(ConcurrentLinkedQueue<QueryReturn> queryReturnQueue,
//			ExecutionPlan executionPlan, TStatementImp tStatementImp){
//		if(null==this.executionPlanIn&&null!=executionPlan){
//			this.executionPlanIn=executionPlan;
//			isPreparedStatement = this.isPreparedStatement(tStatementImp);
//			dbIndexList = new LinkedList<String>(executionPlan.getSqlMap().keySet());
//			setSpecialProperty(tStatementImp, executionPlan);
//		}
//	
//		List<SQLException> sqlExceptions = null;
//		try {
//			while (select()) {
//				try {
//					Connection con = connectionManager.getConnection(
//							currentDBIndex, executionPlan.isGoSlave());
//					QueryReturn qr=null;
//					if (!isPreparedStatement) {
//						qr=executeQueryIntervalST(con, 
//								sqlContextToBeExecOnCurrentDB.get(tableIndex), executionPlan.getSqlMetaData());
//					} else { 
//						qr=executeQueryIntervalPST(con,
//								sqlContextToBeExecOnCurrentDB.get(tableIndex), executionPlan.getSqlMetaData());
//					}
//					
//					qr.setCurrentDBIndex(currentDBIndex);
//					this.resultSet=qr.getResultset();
//					this.statement=qr.getStatement();
//					
//					queryReturnQueue.add(qr);
//					return;
//				} catch (SQLException e) {
//					//第一时间打印异常
//					logger.error(e);
//					sqlExceptions = ExceptionUtils.appendToExceptionList(sqlExceptions, e);
//					sqlExceptions = tryCloseConnection(sqlExceptions,
//							currentDBIndex);
//					this.closeAndClearResources(sqlExceptions);
//					break;
//				}
//			}
//
//			// 执行到这里表示size已经空了，没有后续资源可以被使用了
//			writeLogOrThrowSQLException("TDDL print sqlException while retry :",
//					sqlExceptions);
//		} catch (SQLException e) {
//			QueryReturn qr = new QueryReturn();
//			qr.setExceptions(sqlExceptions);
//			queryReturnQueue.add(qr);
//		}
//	} 
//	
//	protected boolean select() throws SQLException {
//		List<SQLException> sqlExceptions = null;
//        
//		//这里不再清statement和resultset，由外部自己来维护。
//		if (currentDBIndex == null) {
//			if (tableIndex != 0) {
//				throw new IllegalStateException("tableIndex != 0 should not be here!");
//			}
//			// 第一次进来，先初始化一下
//			writeLogOrThrowSQLException(
//					"TDDL print sqlException while close resources:",
//					sqlExceptions);
//			return selectDBGroupByRandom();
//		}
//
//		// 如果当前executePlan的表的个数小于表名index自增值。表示当前ds的所有表用尽。
//		tableIndex++;
//		if (sqlContextToBeExecOnCurrentDB.size() <= tableIndex) {
//			// dbindex 随机换下一个。表示当前dbIndex所指代的数据已经用完.那么关闭当前连接
//			sqlExceptions = tryCloseConnection(sqlExceptions, currentDBIndex);
//			writeLogOrThrowSQLException(
//					"TDDL print sqlException while close resources:",
//					sqlExceptions);
//			return selectDBGroupByRandom();
//		}
//
//		writeLogOrThrowSQLException(
//				"TDDL print sqlException while close resources:", sqlExceptions);
//		return true;
//	}
//	
//	/**
//	 * 随机选择一个数据源，如果选择成功则返回true 选择失败则返回false 没有多余的可供选择的数据源后，会自动清除维持的指针。
//	 * 
//	 * @return
//	 */
//	private boolean selectDBGroupByRandom() {
//		// tableIndex重置
//		tableIndex = 0;
//		int size = dbIndexList.size();
//		if (size == 0) {
//			currentDBIndex = null;
//			sqlContextToBeExecOnCurrentDB = null;
//			return false;
//		}
//		currentDBIndex = dbIndexList.remove(ran.nextInt(size));
//		sqlContextToBeExecOnCurrentDB = executionPlanIn.getSqlMap().get(
//				currentDBIndex);
//		return true;
//	}
//	
//	/**
//	 * 清掉并关闭当前statement的资源
//	 * 
//	 * @param exceptions
//	 * @param closeConnection
//	 * @throws SQLException
//	 */
//	protected List<SQLException> closeAndClearResources(
//			List<SQLException> exceptions) {
//		if (resultSet != null) {
//			try {
//				resultSet.close();
//			} catch (SQLException e) {
//				exceptions = ExceptionUtils.appendToExceptionList(exceptions, e);
//			} finally {
//				resultSet = null;
//			}
//		}
//		if (statement != null) {
//			try {
//				statement.close();
//			} catch (SQLException e) {
//				exceptions = ExceptionUtils.appendToExceptionList(exceptions, e);
//			} finally {
//				statement = null;
//			}
//		}
//		// 打扫战场的时候不抛出异常，如果抛出异常，则肯定最少查了一次了，所以不会走到catch段中
//		return exceptions;
//	}
//	
//	private void writeLogOrThrowSQLException(String message,
//			List<SQLException> sqlExceptions) throws SQLException {
//		// 这时候抛出异常,如果有异常的话
//		ExceptionUtils.throwSQLException(sqlExceptions, executionPlanIn
//				.getOriginalSql(), executionPlanIn.getOriginalArgs());
//	}
//	
//	protected List<SQLException> tryCloseConnection(
//			List<SQLException> exceptions, String dbSelectorId) {
//		try {
//			connectionManager.tryClose(dbSelectorId);
//		} catch (SQLException e) {
//			exceptions = ExceptionUtils.appendToExceptionList(exceptions, e);
//		}
//		return exceptions;
//	}
}

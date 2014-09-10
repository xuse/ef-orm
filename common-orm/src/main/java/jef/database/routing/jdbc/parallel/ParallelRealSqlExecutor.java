package jef.database.routing.jdbc.parallel;


/**
 * 并行sql执行器，采用线程池模式并行执行不同库sql,从而
 * 充分利用数据库IO
 * <br>
 * 使用回调将结果设置到结果队列，如果某个线程执行发生异常，
 * 中断主线程，使主线程退出countdownlatch.await(),并且
 * 在异常块里取消此次任务的其他sql执行 ,并且清理队列内已
 * 经执行完毕返回的结果,关闭链接。（这段逻辑需要优化，
 * 取消任务我始终觉得有风险，增加复杂性）
 * 
 * @author junyu
 * 
 */
public class ParallelRealSqlExecutor {
//	private static final Log logger = LogFactory
//			.getLog(ParallelRealSqlExecutor.class);
//
//	public ParallelRealSqlExecutor(ConnectionManager connectionManager) {
//		super(connectionManager);
//	}
//
//	/**
//	 * 并行查询，使用固定线程池进行查询操作。
//	 * 
//	 * @param queryReturnList
//	 * @param executionPlan
//	 * @param tStatementImp
//	 * @param latch
//	 */
//	@SuppressWarnings("rawtypes")
//	public void parallelQuery(
//			ConcurrentLinkedQueue<QueryReturn> queryReturnQueue,
//			final ExecutionPlan executionPlan, TStatementImp tStatementImp,
//			final CountDownLatch latch, List<Future> futures) {
//
//		setSpecialProperty(tStatementImp, executionPlan);
//		
//		/**
//		 * 创建回调类
//		 */
//		final ExecuteCompleteListener<QueryReturn> ec = new ExecuteCompleteListener<QueryReturn>(
//				queryReturnQueue);
//
//		final boolean isPrepareStatement = this
//				.isPreparedStatement(tStatementImp);
//
//		final Thread mainThread = Thread.currentThread();
//		Map<String, List<RealSqlContext>> sqlMap = executionPlan.getSqlMap();
//		for (final Entry<String, List<RealSqlContext>> dbEntry : sqlMap
//				.entrySet()) {
//			Future future = null;
//			futures.add(future);
//			future = ParallelDiamondConfigManager.submit(dbEntry.getKey(),
//					new Runnable() {
//						public void run() {
//							String dbSelectorId = dbEntry.getKey();
//							
//							try {
//								/**
//								 * 第一次查询之前，检查下当前线程有没有被置为 interrupted
//								 */
//								checkThreadState();
//
//								Connection connection = connectionManager
//										.getConnection(dbSelectorId,
//												executionPlan.isGoSlave());
//
//								List<RealSqlContext> sqlList = dbEntry
//										.getValue();
//
//								for (RealSqlContext sql : sqlList) {
//									QueryReturn qr = null;
//
//									/**
//									 * 每一次查询之前，检查下当前线程有没有被置为 interrupted
//									 */
//									checkThreadState();
//
//									long start = System.currentTimeMillis();
//
//									if (isPrepareStatement) {
//										qr = executeQueryIntervalPST(
//												connection, sql, executionPlan.getSqlMetaData());
//									} else {
//										qr = executeQueryIntervalST(connection,
//												sql, executionPlan.getSqlMetaData());
//									}
//
//									long during = System.currentTimeMillis()
//											- start;
//									profileRealDatabaseAndTables(dbSelectorId,
//											sql, during);
//									
//                                    qr.setCurrentDBIndex(dbSelectorId);
//									ec.addResult(qr);
//								}
//							} catch (SQLException e) {
//								logger.error(
//										"Parallel Query SQLException Happen!",
//										e);
//						        
//								// 让countdownlatch响应interruptException;
//								if (!mainThread.isInterrupted()) {
//									mainThread.interrupt();
//								}
//							} catch(Exception e){
//								logger.error(
//										"Parallel Query Unknow Exception Happen!",
//										e);
//								
//								// 让countdownlatch响应interruptException;
//								if (!mainThread.isInterrupted()) {
//									mainThread.interrupt();
//								}
//							}
//
//							/**
//							 * 通知主线程完成查询
//							 */
//							latch.countDown();
//						}
//					});
//
//		}
//	}
//	
//	public void tryCloseConnection(String dbIndex){
//		tryCloseConnection(null,dbIndex);
//	}
//
//	/**
//	 * 并行更新，使用固定线程池进行update操作
//	 * 
//	 * @param updateReturnList
//	 * @param executionPlan
//	 * @param tStatementImp
//	 * @param latch
//	 */
//	@SuppressWarnings("rawtypes")
//	public void parallelUpdate(
//			ConcurrentLinkedQueue<UpdateReturn> updateReturnQueue,
//			final ExecutionPlan executionPlan,
//			final TStatementImp tStatementImp, final CountDownLatch latch,
//			List<Future> futures) {
//
//		setSpecialProperty(tStatementImp, executionPlan);
//		
//		/**
//		 * 创建回调类
//		 */
//		final ExecuteCompleteListener<UpdateReturn> ec = new ExecuteCompleteListener<UpdateReturn>(
//				updateReturnQueue);
//
//		final boolean isPrepareStatement = this
//				.isPreparedStatement(tStatementImp);
//
//		final Thread mainThread = Thread.currentThread();
//		Map<String, List<RealSqlContext>> sqlMap = executionPlan.getSqlMap();
//		for (final Entry<String, List<RealSqlContext>> dbEntry : sqlMap
//				.entrySet()) {
//			Future future = null;
//			futures.add(future);
//			future = ParallelDiamondConfigManager.submit(dbEntry.getKey(),
//					new Runnable() {
//						public void run() {
//							UpdateReturn ur = null;
//							if (isPrepareStatement) {
//								ur = executeUpdateIntervalPST(executionPlan,
//										dbEntry);
//							} else {
//								ur = executeUpdateIntervalST(executionPlan,
//										dbEntry);
//							}
//
//							if (null != ur.getExceptions()
//									&& !ur.getExceptions().isEmpty()) {
//								logger
//										.error("Parallel Update SQLException Happen!");
//
//								if (!mainThread.isInterrupted()) {
//									mainThread.interrupt();
//								}
//							}
//							
//							ec.addResult(ur);
//
//							/**
//							 * 通知主线程更新操作完成
//							 */
//							latch.countDown();
//						}
//					});
//		}
//	}
//
//	/**
//	 * 内部回调类，当子线程完成任务时完成结果设置
//	 * 
//	 * @author junyu
//	 * 
//	 * @param <T>
//	 */
//	private class ExecuteCompleteListener<T> {
//		private ConcurrentLinkedQueue<T> re = null;
//
//		public ExecuteCompleteListener(ConcurrentLinkedQueue<T> re) {
//			this.re = re;
//		}
//
//		public void addResult(T ele) {
//			re.add(ele);
//		}
//	}
}

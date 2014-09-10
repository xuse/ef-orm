//package com.taobao.tddl.client.jdbc.sqlexecutor;
//
//import java.sql.SQLException;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ConcurrentLinkedQueue;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.Future;
//import java.util.concurrent.RejectedExecutionException;
//
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
//
//import com.taobao.tddl.client.jdbc.RealSqlContext;
//import com.taobao.tddl.client.jdbc.TStatementImp;
//import com.taobao.tddl.client.jdbc.executeplan.ExecutionPlan;
//import com.taobao.tddl.client.jdbc.sqlexecutor.parallel.ParallelDiamondConfigManager;
//import com.taobao.tddl.client.jdbc.sqlexecutor.parallel.ParallelRealSqlExecutor;
//import com.taobao.tddl.client.jdbc.sqlexecutor.serial.SerialRealSqlExecutor;
//
///**
// * 对外的SQL执行器，每次虚拟查询或者更新都会实例化
// * 
// * @author junyu
// * 
// */
//public class RealSqlExecutorImp implements RealSqlExecutor {
//	private static final Log logger = LogFactory
//			.getLog(RealSqlExecutorImp.class);
//	protected final ParallelRealSqlExecutor parallelExecutor;
//	protected final SerialRealSqlExecutor serialExecutor;
//	protected final TStatementImp tStatementImp;
//	protected final ExecutionPlan executionPlan;
//
//	protected ConcurrentLinkedQueue<UpdateReturn> updateReturnQueue = null;
//	protected ConcurrentLinkedQueue<QueryReturn> queryReturnQueue = null;
//
//	public RealSqlExecutorImp(ParallelRealSqlExecutor parallelExecutor,
//			SerialRealSqlExecutor serialExecutor, TStatementImp tStatementImp,
//			ExecutionPlan executionPlan) {
//		this.parallelExecutor = parallelExecutor;
//		this.serialExecutor = serialExecutor;
//		this.tStatementImp = tStatementImp;
//		this.executionPlan = executionPlan;
//	}
//
//	@SuppressWarnings("rawtypes")
//	public QueryReturn query() throws SQLException {
//		if (null == queryReturnQueue) {
//			queryReturnQueue = new ConcurrentLinkedQueue<QueryReturn>();
//			boolean useParallel = useParallel();
//			if (useParallel && null != this.parallelExecutor) {
//				int dbSize = getDbSize(executionPlan);
//				CountDownLatch cdl = new CountDownLatch(dbSize);
//				List<Future> futures = new ArrayList<Future>(dbSize);
//				try {
//					parallelExecutor.parallelQuery(queryReturnQueue,
//							executionPlan, tStatementImp, cdl, futures);
//					cdl.await();
//				} catch (RejectedExecutionException e1) {
//					logger.error("some task rejected,this query failed!", e1);
//					parallelQueryExceptionHandle(futures);
//					throw new SQLException("[RealSqlExecutorImp exception caught]some task rejected,this query failed!");
//				} catch (InterruptedException e) {
//					logger.error("parrallel query error!", e);
//					parallelQueryExceptionHandle(futures);
//					throw new SQLException("[RealSqlExecutorImp exception caught]parrallel query error!");
//				} catch (Exception e) {
//					logger.error("some error happen!", e);
//					parallelQueryExceptionHandle(futures);
//					throw new SQLException("[RealSqlExecutorImp exception caught]unknow error happen!");
//				}
//			}
//
//			if (!useParallel && null != this.serialExecutor) {
//				serialExecutor.serialQuery(queryReturnQueue, executionPlan,
//						tStatementImp);
//			}
//		}
//
//		if (null != queryReturnQueue && !queryReturnQueue.isEmpty()) {
//			QueryReturn qr = queryReturnQueue.poll();
//			return qr;
//		} else {
//			return null;
//		}
//	}
//
//	@SuppressWarnings("rawtypes")
//	public UpdateReturn update() throws SQLException {
//		if (null == updateReturnQueue) {
//			boolean useParallel = useParallel();
//			updateReturnQueue = new ConcurrentLinkedQueue<UpdateReturn>();
//
//			if (useParallel && null != this.parallelExecutor) {
//				int dbSize = getDbSize(executionPlan);
//				CountDownLatch cdl = new CountDownLatch(dbSize);
//				List<Future> futures = new ArrayList<Future>(dbSize);
//				try {
//					parallelExecutor.parallelUpdate(updateReturnQueue,
//							executionPlan, tStatementImp, cdl, futures);
//					cdl.await();
//				} catch (RejectedExecutionException e1) {
//					logger.error("some task rejected,this query failed!", e1);
//					parallelUpdateExceptionHandle(futures);
//					throw new SQLException("[RealSqlExecutorImp exception caught]some task rejected,this update failed!");
//				} catch (InterruptedException e) {
//					logger.error("parrallel update error!", e);
//					parallelUpdateExceptionHandle(futures);
//					throw new SQLException("[RealSqlExecutorImp exception caught]parrallel update error!");
//				} catch (Exception e) {
//					logger.error("main thread some error happen!", e);
//					parallelUpdateExceptionHandle(futures);
//					throw new SQLException("[RealSqlExecutorImp exception caught]unknow error happen!");
//				}
//			}
//
//			if (!useParallel && null != this.serialExecutor) {
//				serialExecutor.serialUpdate(updateReturnQueue, executionPlan,
//						tStatementImp);
//			}
//		}
//
//		if (null != updateReturnQueue && !updateReturnQueue.isEmpty()) {
//			UpdateReturn ur = updateReturnQueue.poll();
//			return ur;
//		} else {
//			return null;
//		}
//	}
//
//	/**
//	 * 确定是否使用并行查询
//	 * 
//	 * @param executionPlan
//	 * @return
//	 */
//	protected boolean useParallel() {
//		if (!ParallelDiamondConfigManager.isUseParallel()) {
//			return false;
//		} else if (executionPlan.isUseParallel()) {
//			return true;
//		} else {
//			return false;
//		}
//	}
//
//	/**
//	 * 取得需要到几个库上执行
//	 * 
//	 * @param executionPlan
//	 * @return
//	 */
//	private int getDbSize(ExecutionPlan executionPlan) {
//		Map<String, List<RealSqlContext>> sqlMap = executionPlan.getSqlMap();
//		return sqlMap.size();
//	}
//
//	/**
//	 * 主要为了防止查询到一半直接放弃查询
//	 * 另外出现异常时为了确保安全，也需要调用
//	 * 这个方法。从而确保正确的关闭了资源
//	 * 
//	 * @throws SQLException
//	 */
//	public void clearQueryResource(){
//		if (null != this.queryReturnQueue && !this.queryReturnQueue.isEmpty()) {
//			QueryReturn qr = null;
//			//每个try catch
//			while (null != (qr = queryReturnQueue.poll())) {
//				if (qr.getResultset() != null) {
//					try {
//						qr.getResultset().close();
////						//测试用
////						logger.info(Thread.currentThread()+"resultset close success!");
//					} catch (SQLException e) {
//						logger.error("resultset close error!",e);
//					}
//				}
//
//				if (qr.getStatement() != null) {
//					try {
//						qr.getStatement().close();
////						//测试用
////						logger.info(Thread.currentThread()+"statement close success!");
//					} catch (SQLException e) {
//						logger.error("statement close error!",e);
//					}
//				}
//
//				if (qr.getCurrentDBIndex() != null) {
//					// 这里无论用哪个都是OK的，因为两者的connectionManager一样
////					//测试用
////					logger.info(Thread.currentThread()+"try close connection!");
//					parallelExecutor.tryCloseConnection(qr.getCurrentDBIndex());
//				}
//			}
//		}
//	}
//
//	/**
//	 * 并行查询回收资源
//	 * 
//	 * @param futures
//	 * @throws SQLException
//	 */
//	@SuppressWarnings("rawtypes")
//	private void parallelQueryExceptionHandle(List<Future> futures)
//			throws SQLException {
//		logger.warn("start to cancel all future!");
//		for (Future future : futures) {
//			if (null != future) {
//				future.cancel(true);
//			}
//		}
//        
//		logger.warn("start to collect query resources!");
//		clearQueryResource();
//	}
//
//	/**
//	 * 并行更新回收资源
//	 * 
//	 * @param futures
//	 * @throws SQLException
//	 */
//	@SuppressWarnings("rawtypes")
//	private void parallelUpdateExceptionHandle(List<Future> futures)
//			throws SQLException {
//		logger.warn("start to cancel all future!");
//		for (Future future : futures) {
//			if (null != future) {
//				future.cancel(true);
//			}
//		}
//	}
//}

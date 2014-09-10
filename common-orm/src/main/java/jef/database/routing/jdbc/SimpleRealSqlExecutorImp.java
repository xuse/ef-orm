//package jef.database.routing.jdbc;
//
//import java.sql.SQLException;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ConcurrentLinkedQueue;
//
//import com.taobao.tddl.client.jdbc.RealSqlContext;
//import com.taobao.tddl.client.jdbc.TStatementImp;
//import com.taobao.tddl.client.jdbc.executeplan.ExecutionPlan;
//import com.taobao.tddl.client.jdbc.sqlexecutor.parallel.ParallelRealSqlExecutor;
//import com.taobao.tddl.client.jdbc.sqlexecutor.serial.SerialRealSqlExecutor;
//
///**
// * @author junyu
// * 
// */
//public class SimpleRealSqlExecutorImp extends RealSqlExecutorImp {
//	public SimpleRealSqlExecutorImp(ParallelRealSqlExecutor parallelExecutor,
//			SerialRealSqlExecutor serialExecutor, TStatementImp tStatementImp,
//			ExecutionPlan executionPlan) {
//		super(parallelExecutor, serialExecutor, tStatementImp, executionPlan);
//	}
//
//	private boolean alwaysUseParallel = false;
//
//	@Override
//	public QueryReturn query() throws SQLException {
//		// 说明第一次进来，否则不允许中途走超类查询
//		if (null == queryReturnQueue && useParallel()) {
//			alwaysUseParallel = true;
//			QueryReturn qr = super.query();
//			queryConnectionManage(qr);
//			return qr;
//		}
//
//		// 如果走并行，那么一直走并行，切换为串行后，这次查询也会走完并行。
//		if (alwaysUseParallel) {
//			QueryReturn qr = super.query();
//			queryConnectionManage(qr);
//			return qr;
//		}
//
//		if (null == queryReturnQueue) {
//			queryReturnQueue = new ConcurrentLinkedQueue<QueryReturn>();
//		}
//
//		serialExecutor.serialQuery(queryReturnQueue, executionPlan,
//				tStatementImp);
//
//		if (queryReturnQueue != null && !queryReturnQueue.isEmpty()) {
//			return queryReturnQueue.poll();
//		} else {
//			return null;
//		}
//	}
//
//	private String needCloseConection = null;
//	Map<String, Integer> conWithStatements = null;
//
//	/**
//	 * 使用并行方式，并且拿一个，关一个，需要考虑连接关闭问题。
//	 * 
//	 * @param qr
//	 */
//	private void queryConnectionManage(QueryReturn qr) {
//		if (null != needCloseConection) {
//			// 测试使用
//			// logger.debug("--------needCloseConection-------"+needCloseConection);
//			// logger.debug("--------needCloseConection statement num-------"+conWithStatements.get(needCloseConection));
//			parallelExecutor.tryCloseConnection(needCloseConection);
//			needCloseConection = null;
//		}
//
//		if (null != qr) {
//			if (null == conWithStatements) {
//				conWithStatements = new HashMap<String, Integer>();
//				Map<String, List<RealSqlContext>> map = executionPlan
//						.getSqlMap();
//				for (Map.Entry<String, List<RealSqlContext>> entry : map
//						.entrySet()) {
//					conWithStatements.put(entry.getKey(), entry.getValue()
//							.size());
//				}
//			}
//
//			// 如果当前ds resultset计数-1后小于等于0,当前ds resultset取尽
//			int remain = conWithStatements.get(qr.getCurrentDBIndex()) - 1;
//			conWithStatements.put(qr.getCurrentDBIndex(), remain);
//			if (remain <= 0) {
//				needCloseConection = qr.getCurrentDBIndex();
//			}
//		}
//	}
//}

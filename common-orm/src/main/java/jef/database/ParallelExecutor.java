package jef.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

import jef.common.PairSO;
import jef.common.log.LogUtil;
import jef.database.annotation.PartitionResult;
import jef.database.jdbc.statement.ResultSetLaterProcess;
import jef.database.query.ConditionQuery;
import jef.database.routing.sql.InMemoryOperateProvider;
import jef.database.routing.sql.SelectExecutionPlan;
import jef.database.wrapper.clause.QueryClause;
import jef.database.wrapper.populator.ResultSetExtractor;
import jef.database.wrapper.processor.BindVariableContext;
import jef.database.wrapper.processor.BindVariableTool;
import jef.database.wrapper.result.MultipleResultSet;
import jef.tools.Assert;

@SuppressWarnings("rawtypes")
public class ParallelExecutor {

	private CountDownLatch latch;
	private final Queue<SQLException> exceptions=new ConcurrentLinkedQueue<SQLException>();
	private final Queue<Throwable> throwables=new ConcurrentLinkedQueue<Throwable>();
	
	private class Task1 implements Runnable {
		private QueryClause sql;
		private SelectProcessor selectp;
		private Session session;
		private ConditionQuery queryObj;
		private MultipleResultSet rs;
		private QueryOption option;
		private PartitionResult site;

		Task1(QueryClause sql, SelectProcessor selectp, Session session, ConditionQuery queryObj, MultipleResultSet rs, QueryOption option, PartitionResult site) {
			this.sql = sql;
			this.selectp = selectp;
			this.session = session;
			this.queryObj = queryObj;
			this.rs = rs;
			this.option = option;
			this.site = site;
		}

		@Override
		public void run() {
			try {
				selectp.processSelect(session.asOperateTarget(site.getDatabase()), sql, site, queryObj, rs, option);
			} catch (SQLException e) {
				exceptions.add(e);
			} catch (Throwable e) {
				throwables.add(e);
			} finally {
				latch.countDown();
			}
		}
	}

	private class Task2 implements Runnable {
		private OperateTarget db;
		private SelectExecutionPlan plan;
		private boolean noOrder;
		private ResultSetExtractor rst;
		private MultipleResultSet mrs;
		private InMemoryOperateProvider sqlContext;
		private PartitionResult site;
		
		public Task2(OperateTarget db, SelectExecutionPlan plan, boolean noOrder, ResultSetExtractor rst, MultipleResultSet mrs, InMemoryOperateProvider sqlContext, PartitionResult site) {
			this.db = db;
			this.plan = plan;
			this.noOrder = noOrder;
			this.rst = rst;
			this.mrs = mrs;
			this.sqlContext = sqlContext;
			this.site = site;
		}

		@Override
		public void run() {
			try {
				processQuery(db.getTarget(site.getDatabase()), plan.getSql(site, noOrder), rst, mrs, sqlContext.isReverseResult());
			} catch (SQLException e) {
				exceptions.add(e);
			} catch (Throwable e) {
				throwables.add(e);	
			} finally {
				latch.countDown();
			}
		}
	}
	
	/*
	 * 执行查询动作，将查询结果放入mrs
	 */
	private static void processQuery(OperateTarget db, PairSO<List<Object>> sql, ResultSetExtractor rst, MultipleResultSet mrs, ResultSetLaterProcess reverseResult) throws SQLException {
		StringBuilder sb = null;
		PreparedStatement psmt = null;
		ResultSet rs = null;
		if (mrs.isDebug())
			sb = new StringBuilder(sql.first.length() + 150).append(sql.first).append(" | ").append(db.getTransactionId());
		try {
			psmt = db.prepareStatement(sql.first, reverseResult, false);
			BindVariableContext context = new BindVariableContext(psmt, db.getProfile(), sb);
			BindVariableTool.setVariables(context, sql.second);
			rst.apply(psmt);
			rs = psmt.executeQuery();
			mrs.add(rs, psmt, db);
		} finally {
			if (mrs.isDebug())
				LogUtil.show(sb);
		}
	}

	public void executeSelect(QueryClause sql, SelectProcessor selectp, Session session, ConditionQuery queryObj, MultipleResultSet rs, QueryOption option) throws SQLException {
		initLatch(sql.getTables().length);
		for (PartitionResult site : sql.getTables()) {
			Task1 t1 = new Task1(sql, selectp, session, queryObj, rs, option, site);
			DbUtils.es.execute(t1);
		}
		try {
			latch.await();
		} catch (InterruptedException e) {
			throw new SQLException(e);
		}
		if(!exceptions.isEmpty()){
			throw DbUtils.wrapExceptions(exceptions);
		}
		if(!throwables.isEmpty()){
			throw DbUtils.toRuntimeException(throwables.peek());
		}
	}

	private void initLatch(int length) {
		Assert.isNull(latch);
		latch = new CountDownLatch(length);
	}

	public void executeQuery(OperateTarget db, SelectExecutionPlan plan, boolean noOrder, ResultSetExtractor rst, MultipleResultSet mrs, InMemoryOperateProvider sqlContext) throws SQLException {
		initLatch(plan.getSites().length);
		for (PartitionResult site : plan.getSites()) {
			Task2 t1 = new Task2(db, plan, noOrder, rst, mrs, sqlContext, site);
			DbUtils.es.execute(t1);
		}
		try {
			latch.await();
		} catch (InterruptedException e) {
			throw new SQLException(e);
		}
		if(!exceptions.isEmpty()){
			throw DbUtils.wrapExceptions(exceptions);
		}
		if(!throwables.isEmpty()){
			throw DbUtils.toRuntimeException(throwables.peek());
		}
	}

}

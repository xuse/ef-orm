package jef.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

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
import jef.database.wrapper.result.ResultSetContainer;



public class SerialExecutor {
	public static SerialExecutor INSTANCE = new SerialExecutor();

	public void executeSelect(QueryClause sql,SelectProcessor selectp,Session session,ConditionQuery queryObj,ResultSetContainer rs,QueryOption option) throws SQLException {
		for (PartitionResult site : sql.getTables()) {
			selectp.processSelect(session.asOperateTarget(site.getDatabase()), sql, site, queryObj, rs, option);
		}
	}
	@SuppressWarnings("rawtypes") 
	public void executeQuery(OperateTarget db,SelectExecutionPlan plan,boolean noOrder,ResultSetExtractor rst,ResultSetContainer mrs,InMemoryOperateProvider sqlContext ) throws SQLException {
		for (PartitionResult site : plan.getSites()) {
			processQuery(db.getTarget(site.getDatabase()), plan.getSql(site, noOrder), rst, mrs, sqlContext.getRsLaterProcessor());
		}
	}
	
	/*
	 * 执行查询动作，将查询结果放入mrs
	 */
	@SuppressWarnings("rawtypes") 
	private static void processQuery(OperateTarget db, PairSO<List<Object>> sql, ResultSetExtractor rst,ResultSetContainer mrs,ResultSetLaterProcess reverseResult) throws SQLException {
		StringBuilder sb = null;
		PreparedStatement psmt = null;
		ResultSet rs = null;
		if (mrs.isDebug())
			sb = new StringBuilder(sql.first.length() + 150).append(sql.first).append(" | ").append(db.getTransactionId());
		try {
			psmt = db.prepareStatement(sql.first,reverseResult,false);
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
}

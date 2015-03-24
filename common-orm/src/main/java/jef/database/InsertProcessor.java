package jef.database;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.ColumnMapping;
import jef.database.meta.Feature;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.database.routing.PartitionResult;
import jef.database.support.SqlLog;
import jef.database.wrapper.clause.InsertSqlClause;
import jef.database.wrapper.processor.AutoIncreatmentCallBack;
import jef.database.wrapper.processor.AutoIncreatmentCallBack.OracleRowidKeyCallback;
import jef.database.wrapper.processor.BindVariableContext;
import jef.database.wrapper.processor.BindVariableTool;
import jef.tools.ArrayUtils;
import jef.tools.StringUtils;
import jef.tools.reflect.BeanWrapper;

abstract class InsertProcessor {
	protected DbClient db;
	protected SqlProcessor parent;

	static InsertProcessor get(DatabaseDialect profile, DbClient db) {
		if (profile.has(Feature.NO_BIND_FOR_INSERT)) {
			return new NormalImpl(db, db.rProcessor);
		} else {
			return new PreparedImpl(db, db.rProcessor);
		}
	}

	/**
	 * generate a insert SQL.
	 * 
	 * @param obj
	 * @param tableName
	 * @param dynamic
	 * @param extreme
	 * @return
	 * @throws SQLException
	 */
	abstract InsertSqlClause toInsertSql(IQueryableEntity obj, String tableName, boolean dynamic, PartitionResult pr) throws SQLException;

	abstract InsertSqlClause toInsertSqlBatch(IQueryableEntity obj, String tableName, boolean dynamic, boolean extreme, PartitionResult pr) throws SQLException;

	/**
	 * process insert operate
	 * 
	 * @param db
	 * @param obj
	 * @param sqls
	 * @param start
	 * @param parse
	 * @throws SQLException
	 */
	abstract void processInsert(OperateTarget db, IQueryableEntity obj, InsertSqlClause sqls, long start, long parse) throws SQLException;

	/**
	 * 构造
	 * 
	 * @param parentDbClient
	 * @param rProcessor
	 */
	InsertProcessor(DbClient parentDbClient, SqlProcessor rProcessor) {
		this.db = parentDbClient;
		this.parent = rProcessor;
	}

	static final class NormalImpl extends InsertProcessor {
		private PreparedImpl batchImpl;

		NormalImpl(DbClient parentDbClient, SqlProcessor rProcessor) {
			super(parentDbClient, rProcessor);
			batchImpl = new PreparedImpl(parentDbClient, rProcessor);
		}

		@Override
		InsertSqlClause toInsertSql(IQueryableEntity obj, String tableName, boolean dynamic, PartitionResult pr) throws SQLException {
			@SuppressWarnings("deprecation")
			DatabaseDialect profile = pr == null ? db.getProfile() : db.getProfile(pr.getDatabase());
			List<String> cStr = new ArrayList<String>();// 字段列表
			List<String> vStr = new ArrayList<String>();// 值列表
			ITableMetadata meta = MetaHolder.getMeta(obj);
			InsertSqlClause result = new InsertSqlClause();
			result.parent = db;
			result.profile = profile;
			result.setTableNames(pr);
			for (ColumnMapping entry : meta.getColumns()) {
				BeanWrapper wrapper = BeanWrapper.wrap(obj);
				Object value = wrapper.getPropertyValue(entry.fieldName());
				entry.processInsert(value, result, cStr, vStr, dynamic, obj);
			}
			result.setColumnsPart(StringUtils.join(cStr, ','));
			result.setValuesPart(StringUtils.join(vStr, ','));
			if (profile.has(Feature.SELECT_ROW_NUM)) {
				result.setCallback(new OracleRowidKeyCallback(result.getCallback()));
			}
			return result;
		}

		@Override
		void processInsert(OperateTarget db, IQueryableEntity obj, InsertSqlClause sqls, long start, long parse) throws SQLException {
			SqlLog sb = ORMConfig.getInstance().newLogger();
			Statement st = null;
			try {
				st = db.createStatement();
				String sql = sqls.getSql();
				sb.ensureCapacity(sql.length()+150);
				sb.append(sql).append(db);
				
				if (sqls.getCallback() == null) {
					st.executeUpdate(sql);
				} else {
					AutoIncreatmentCallBack cb = sqls.getCallback();
					cb.executeUpdate(st, sql);
					cb.callAfter(obj);
				}
				sb.append("\nInsert:1\tTime cost([ParseSQL]:",parse - start).append("ms, [DbAccess]:",System.currentTimeMillis() - parse).append("ms)").append(db);
			} catch (SQLException e) {
				DbUtils.processError(e, ArrayUtils.toString(sqls.getTable(), true), db);
				throw e;
			} finally {
				sb.output();
				if (st != null)
					st.close();
				db.releaseConnection();
			}
		}

		@Override
		InsertSqlClause toInsertSqlBatch(IQueryableEntity obj, String tableName, boolean dynamic, boolean extreme, PartitionResult pr) throws SQLException {
			return batchImpl.toInsertSqlBatch(obj, tableName, dynamic, extreme, pr);
		}
	}

	static final class PreparedImpl extends InsertProcessor {
		PreparedImpl(DbClient parentDbClient, SqlProcessor rProcessor) {
			super(parentDbClient, rProcessor);
		}

		@Override
		InsertSqlClause toInsertSql(IQueryableEntity obj, String tableName, boolean dynamic, PartitionResult pr) throws SQLException {
			return toInsertSqlBatch(obj, tableName, dynamic, false, pr);
		}

		@Override
		InsertSqlClause toInsertSqlBatch(IQueryableEntity obj, String tableName, boolean dynamic, boolean extreme, PartitionResult pr) throws SQLException {
			DatabaseDialect profile = pr == null ? db.getProfile() : db.getProfile(pr.getDatabase());
			List<String> cStr = new ArrayList<String>();// 字段列表
			List<String> vStr = new ArrayList<String>();// 值列表
			ITableMetadata meta = MetaHolder.getMeta(obj);
			InsertSqlClause result = new InsertSqlClause(extreme);
			result.parent = db;
			result.profile = profile;
			result.setTableNames(pr);
			for (ColumnMapping entry : meta.getColumns()) {
				entry.processPreparedInsert(obj, cStr, vStr, result, dynamic);
			}
			if (profile.has(Feature.SELECT_ROW_NUM) && !extreme) {
				result.setCallback(new OracleRowidKeyCallback(result.getCallback()));
			}
			result.setColumnsPart(StringUtils.join(cStr, ','));
			result.setValuesPart(StringUtils.join(vStr, ','));
			return result;
		}

		@Override
		void processInsert(OperateTarget db, IQueryableEntity obj, InsertSqlClause sqls, long start, long parse) throws SQLException {
			SqlLog sb = ORMConfig.getInstance().newLogger();
			String sql = sqls.getSql();
			sb.ensureCapacity(sql.length() + 128);
			sb.append(sql).append(db);
			PreparedStatement psmt = null;
			try {
				AutoIncreatmentCallBack callback = sqls.getCallback();
				if (callback == null) {
					psmt = db.prepareStatement(sql);
				} else {
					psmt = callback.doPrepareStatement(db, sql);
				}
				BindVariableContext context = new BindVariableContext(psmt, db.getProfile(), sb);
				BindVariableTool.setInsertVariables(obj, sqls.getFields(), context);
				psmt.execute();
				sb.append("\nInsert:1\tTime cost([ParseSQL]:", parse - start).append("ms, [DbAccess]:", System.currentTimeMillis() - parse).append("ms)").append(db);

				if (callback != null) {
					callback.callAfter(obj);
				}
			} catch (SQLException e) {
				DbUtils.processError(e, ArrayUtils.toString(sqls.getTable(), true), db);
				throw e;
			} finally {
				sb.output();
				if (psmt != null)
					psmt.close();
				db.releaseConnection();
			}
		}
	}
}

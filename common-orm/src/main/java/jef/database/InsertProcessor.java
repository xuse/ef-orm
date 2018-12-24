package jef.database;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;

import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.ColumnMapping;
import jef.database.innerpool.WrapableConnection;
import jef.database.meta.Feature;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.database.routing.PartitionResult;
import jef.database.support.SqlLog;
import jef.database.wrapper.clause.InsertSqlClause;
import jef.database.wrapper.processor.InsertStep.OracleRowidKeyCallback;
import jef.database.wrapper.variable.BindVariableContext;
import jef.tools.StringUtils;

abstract class InsertProcessor {
	protected DbClient db;

	static InsertProcessor get(DatabaseDialect profile, DbClient db) {
		return new PreparedImpl(db);
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
	abstract InsertSqlClause toInsertSql(Object obj, String tableName, boolean dynamic, PartitionResult pr) throws SQLException;

	abstract InsertSqlClause toInsertSqlBatch(Object obj, String tableName, boolean dynamic, boolean extreme, PartitionResult pr) throws SQLException;

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
	abstract void processInsert(OperateTarget db, Object obj, InsertSqlClause sqls, long start, long parse) throws SQLException;

	/**
	 * 构造
	 * 
	 * @param parentDbClient
	 * @param rProcessor
	 */
	InsertProcessor(DbClient parentDbClient) {
		this.db = parentDbClient;
	}

	static final class PreparedImpl extends InsertProcessor {
		PreparedImpl(DbClient parentDbClient) {
			super(parentDbClient);
		}

		@Override
		InsertSqlClause toInsertSql(Object obj, String tableName, boolean dynamic, PartitionResult pr) throws SQLException {
			return toInsertSqlBatch(obj, tableName, dynamic, false, pr);
		}

		@Override
		InsertSqlClause toInsertSqlBatch(Object obj, String tableName, boolean dynamic, boolean extreme, PartitionResult pr) throws SQLException {
			DatabaseDialect profile = pr == null ? db.getProfile(null) : db.getProfile(pr.getDatabase());
			List<String> cStr = new ArrayList<String>();// 字段列表
			List<String> vStr = new ArrayList<String>();// 值列表
			ITableMetadata meta = MetaHolder.getMeta(obj);
			InsertSqlClause result = new InsertSqlClause(extreme);
			result.parent = db;
			result.profile = profile;
			result.setTableNames(pr);
			for (ColumnMapping entry : meta.getColumns()) {
				if(entry.isNotInsert())
					continue;
				entry.processPreparedInsert(obj, cStr, vStr, result, dynamic);
			}
			if (profile.has(Feature.SELECT_ROW_NUM) && !extreme) {
				result.getCallback().addProcessor(new OracleRowidKeyCallback());
			}
			result.setColumnsPart(StringUtils.join(cStr, ','));
			result.setValuesPart(StringUtils.join(vStr, ','));
			return result;
		}

		@Override
		void processInsert(OperateTarget provider, Object obj, InsertSqlClause sqls, long start, long parse) throws SQLException {
			SqlLog sb = ORMConfig.getInstance().newLogger();
			String sql = sqls.getSql();
			sb.ensureCapacity(sql.length() + 128);
			sb.append(sql).append(provider.toString());
			DatabaseDialect profile = provider.getProfile();
			PreparedStatement psmt = null;
			try (WrapableConnection db=provider.get()){
				psmt = sqls.getCallback().doPrepareStatement(db, sql);
				BindVariableContext context = new BindVariableContext(psmt, profile, sb);
				context.setInsertVariables(obj, sqls.getFields());
				psmt.execute();
				sb.append("\nInsert:1\tTime cost([ParseSQL]:", parse - start).append("ms, [DbAccess]:", System.currentTimeMillis() - parse).append("ms)").append(provider.toString());
				sqls.getCallback().callAfter(obj);
//			} catch (SQLIntegrityConstraintViolationException e) {
//				throw e;
			} catch (SQLException e) {
				String s = profile.getViolatedConstraintNameExtracter().extractConstraintName(e);
				if (s != null) {
					throw new SQLIntegrityConstraintViolationException(s);
				}
				throw e;
			} finally {
				sb.output();
				DbUtils.close(psmt);
			}
		}
	}
}

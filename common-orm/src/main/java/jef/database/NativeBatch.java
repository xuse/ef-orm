package jef.database;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import jef.common.log.LogUtil;
import jef.database.cache.Cache;
import jef.database.dialect.type.ColumnMapping;
import jef.database.meta.ITableMetadata;
import jef.database.routing.PartitionResult;
import jef.database.support.DbOperatorListener;
import jef.database.support.SqlLog;
import jef.database.wrapper.clause.BindSql;
import jef.database.wrapper.clause.InsertSqlClause;
import jef.database.wrapper.clause.UpdateClause;
import jef.database.wrapper.processor.AutoIncreatmentCallBack;
import jef.database.wrapper.processor.BindVariableContext;
import jef.database.wrapper.processor.BindVariableDescription;
import jef.tools.StringUtils;

abstract class NativeBatch<T extends IQueryableEntity> extends Batch<T>{
	NativeBatch(Session parent, ITableMetadata meta) throws SQLException {
		super(parent, meta);
	}
	
	protected PartitionResult getTableName(T obj) {
		return DbUtils.toTableName(obj, null, obj.getQuery(), parent.getPartitionSupport());
	}
	
	static final class NativeInsert<T extends IQueryableEntity> extends NativeBatch<T> {
		/**
		 * SQL片段,Insert部分(INSERT语句使用)
		 */
		private InsertSqlClause insertPart;

		NativeInsert(Session parent, ITableMetadata meta) throws SQLException {
			super(parent, meta);
		}

		protected String toSql(String tablename) {
			return insertPart.getSql(tablename);
		}

		public void setInsertPart(InsertSqlClause insertPart) {
			this.insertPart = insertPart;
		}

		@Override
		protected void callEventListenerBefore(List<T> listValue) throws SQLException {
			Session parent = this.parent;
			DbOperatorListener listener = parent.getListener();
			for (T t : listValue) {
				try {
					listener.beforeInseret(t, parent);
				} catch (Exception e) {
					LogUtil.exception(e);
				}
			}
		}

		@Override
		protected void callEventListenerAfter(List<T> listValue) throws SQLException {
			if (insertPart.getCallback() != null) {
				insertPart.getCallback().callAfterBatch(listValue);
			}
			Cache cache = parent.getCache();
			DbOperatorListener listener = parent.getListener();
			if (extreme) {
				for (T t : listValue) {
					try {
						listener.afterInsert(t, parent);
					} catch (Exception e) {
						LogUtil.exception(e);
					}
				}
			} else {
				for (T t : listValue) {
					try {
						cache.onInsert(t, forceTableName);
						listener.afterInsert(t, parent);
					} catch (Exception e) {
						LogUtil.exception(e);
					}
					t.clearUpdate();
				}
			}

		}

		@Override
		protected void processJdbcParams(PreparedStatement psmt, List<T> listValue, OperateTarget db) throws SQLException {
			List<ColumnMapping> writeFields = insertPart.getFields();
			int len = listValue.size();
			SqlLog log = ORMConfig.getInstance().newLogger(this.extreme);
			int maxLog = ORMConfig.getInstance().getMaxBatchLog();
			for (int i = 0; i < len; i++) {
				T t = listValue.get(i);
				BindVariableContext context = new BindVariableContext(psmt, db.getProfile(), log.append("Batch Parameters: ", i + 1).append('/').append(len));
				context.setInsertVariables(t, writeFields);
				psmt.addBatch();
				if (log.isDebug()) {
					log.output();
					if (i + 1 == maxLog) {
						log.directLog("Batch Parameters: After " + maxLog + "th are ignored to reduce the size of log file.");
						log = SqlLog.DUMMY;
					}
				}
			}
		}

		protected long innerCommit(List<T> objs, String site, String tablename, String dbName) throws SQLException {
			OperateTarget db = parent.selectTarget(site);
			if (extreme) {
				db.getProfile().toExtremeInsert(insertPart);
			}
			String sql = toSql(tablename);
			if (ORMConfig.getInstance().isDebugMode())
				LogUtil.show(sql + " | " + dbName);
			AutoIncreatmentCallBack callback = insertPart.getCallback();
			PreparedStatement p = callback == null ? db.prepareStatement(sql) : callback.doPrepareStatement(db, sql);
			try {
				long dbAccess = doCommit(p, db, objs);
				return dbAccess;
			} finally {
				p.close();
				db.releaseConnection();
			}
		}

		@Override
		protected void callVeryBefore(List<T> objs) throws SQLException {
			if (insertPart.getCallback() != null) {
				insertPart.getCallback().callBefore(objs);
			}
		}
	}

	static final class NativeUpdate<T extends IQueryableEntity> extends NativeBatch<T> {
		/**
		 * SQL片段，update部分(UPDATE语句使用)
		 */
		private UpdateClause updatePart;

		/**
		 * SQL片段，where部分(UPDATE和DELETE语句使用)
		 */
		private BindSql wherePart;

		NativeUpdate(Session parent, ITableMetadata meta) throws SQLException {
			super(parent, meta);
		}

		@Override
		protected String toSql(String tablename) {
			return StringUtils.concat("update ", tablename, " set ", updatePart.getSql(), wherePart.getSql());
		}

		@Override
		protected void callEventListenerBefore(List<T> listValue) {
			Session parent = this.parent;
			DbOperatorListener listener = parent.getListener();
			for (T t : listValue) {
				listener.beforeUpdate(t, parent);
			}
		}

		@Override
		protected void callEventListenerAfter(List<T> listValue) {
			Session parent = this.parent;
			DbOperatorListener listener = parent.getListener();
			if (extreme) {
				for (T t : listValue) {
					t.clearUpdate();
					listener.afterUpdate(t, 1, parent);
				}
			} else {
				for (T t : listValue) {
					t.applyUpdate();
					listener.afterUpdate(t, 1, parent);
				}
			}

		}

		public void setUpdatePart(UpdateClause updatePart) {
			this.updatePart = updatePart;
		}

		public void setWherePart(BindSql wherePart) {
			this.wherePart = wherePart;
		}

		@Override
		protected void processJdbcParams(PreparedStatement psmt, List<T> listValue, OperateTarget db) throws SQLException {
			List<BindVariableDescription> bindVar = wherePart.getBind();
			int len = listValue.size();
			SqlLog log = ORMConfig.getInstance().newLogger(this.extreme);
			int maxLog = ORMConfig.getInstance().getMaxBatchLog();
			for (int i = 0; i < len; i++) {
				T t = listValue.get(i);
				BindVariableContext context = new BindVariableContext(psmt, db.getProfile(), log.append("Batch Parameters: ", i + 1).append('/').append(len));
				List<Object> whereBind = context.setVariables(t.getQuery(), updatePart.getVariables(), bindVar);
				psmt.addBatch();
				String baseTableName=forceTableName == null ? meta.getTableName(false) : forceTableName;
				parent.getCache().onUpdate(baseTableName, wherePart.getSql(), whereBind);

				if (log.isDebug()) {
					log.output();
					if (i + 1 == maxLog) {
						log.directLog("Batch Parameters: After " + maxLog + "th are ignored to reduce the size of log file.");
						log = SqlLog.DUMMY;
					}

				}

			}

		}

		@Override
		protected void callVeryBefore(List<T> objs) throws SQLException {
		}
	}

	static final class NativeDelete<T extends IQueryableEntity> extends NativeBatch<T> {
		/**
		 * SQL片段，where部分(UPDATE和DELETE语句使用)
		 */
		private BindSql wherePart;

		NativeDelete(Session parent, ITableMetadata meta) throws SQLException {
			super(parent, meta);
		}

		@Override
		protected String toSql(String tablename) {
			return "delete from " + tablename + wherePart.getSql();
		}

		@Override
		protected void callEventListenerBefore(List<T> listValue) {
			Session parent = this.parent;
			DbOperatorListener listener = parent.getListener();
			for (T t : listValue) {
				listener.beforeDelete(t, parent);
			}
		}

		@Override
		protected void callEventListenerAfter(List<T> listValue) {
			Session parent = this.parent;
			DbOperatorListener listener = parent.getListener();
			for (T t : listValue) {
				listener.afterDelete(t, 1, parent);
			}
		}

		public void setWherePart(BindSql wherePart) {
			this.wherePart = wherePart;
		}

		@Override
		protected void processJdbcParams(PreparedStatement psmt, List<T> listValue, OperateTarget db) throws SQLException {
			List<BindVariableDescription> bindVar = wherePart.getBind();
			int len = listValue.size();
			SqlLog log = ORMConfig.getInstance().newLogger(this.extreme);
			int maxLog = ORMConfig.getInstance().getMaxBatchLog();
			for (int i = 0; i < len; i++) {
				T t = listValue.get(i);
				if (t.getQuery().getConditions().isEmpty()) {
					DbUtils.fillConditionFromField(t, t.getQuery(), true, pkMpode);
				}
				BindVariableContext context = new BindVariableContext(psmt, db.getProfile(), log.append("Batch Parameters: ", i + 1).append('/').append(len));
				List<Object> whereBind = context.setVariables(t.getQuery(), null, bindVar);
				String baseTableName=(forceTableName == null ? meta.getTableName(false) : forceTableName);
				parent.getCache().onDelete(baseTableName, wherePart.getSql(), whereBind);

				psmt.addBatch();
				if (log.isDebug()) {
					log.output();
					if (i + 1 == maxLog) {
						log.directLog("Batch Parameters: After " + maxLog + "th are ignored to reduce the size of log file.");
						log = SqlLog.DUMMY;// 关闭日志开关
					}
				}

			}

		}

		@Override
		protected void callVeryBefore(List<T> objs) throws SQLException {
		}
	}
}

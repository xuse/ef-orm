/*
 * JEF - Copyright 2009-2010 Jiyi (mr.jiyi@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jef.database;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jef.common.PairSS;
import jef.common.log.LogUtil;
import jef.database.annotation.PartitionResult;
import jef.database.cache.TransactionCache;
import jef.database.dialect.type.ColumnMapping;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.database.support.DbOperatorListener;
import jef.database.support.SqlLog;
import jef.database.wrapper.clause.BindSql;
import jef.database.wrapper.clause.InsertSqlClause;
import jef.database.wrapper.clause.UpdateClause;
import jef.database.wrapper.processor.AutoIncreatmentCallBack;
import jef.database.wrapper.processor.BindVariableContext;
import jef.database.wrapper.processor.BindVariableDescription;
import jef.database.wrapper.processor.BindVariableTool;
import jef.tools.StringUtils;

/**
 * 批操作工具。由一个已经编译好的SQL语句构成。<br>
 * Batch对象一旦建立，其对应的SQL语句是固定不变的，传入不同批次的数据，可以作为该SQL语句的参数，使用JDBC的批量执行接口执行。
 * <p>
 * 可以使用{@link #execute(List)}方法执行批量插入、更新或删除操作。
 * <p>
 * 可以使用{@link #setGroupForPartitionTable(boolean)}方法指定是否要对每条参数进行路由计算，根据路由结果重新分组后再执行插入、更新或删除操作。(仅当分库分表后才需要)
 * 
 * 
 * @author Administrator
 * 
 * @param <T>
 */
public abstract class Batch<T extends IQueryableEntity> {
	/**
	 * 操作执行数据库对象
	 */
	protected Session parent;
	/**
	 * 元数据
	 */
	protected ITableMetadata meta;
	/**
	 * 批量删除或更新中采用主键模式操作
	 */
	boolean pkMpode = false;
	/**
	 * 重新分组模式 开启后，对于批操作的所有对象，都进行分表的表名计算，然后按不同的表进行分组，分组后在分别执行批操作。
	 * 适用于对于分表后多组对象的操作。
	 * 
	 * 关闭时，按照不分表场景下的表名称，适用于用户能确保操作的所有数据位于一张表中的情况，以及用户自定义表名的情况
	 */
	private boolean groupForPartitionTable;
	/**
	 * 执行影响的总记录行数
	 */
	private int executeResult;
	/**
	 * 极限模式，极限模式下，会使用数据库本地特性来尽可能加速操作。 极限模式下，禁用数据回写功能。
	 */
	protected boolean extreme;

	/**
	 * 固定使用的表名<br>
	 * 当指定了固定的表名后，则认为要操作的表就是指定的表名，框架本身计算的表名（包含通过分表规则计算的表名）就会失效。
	 */
	String forceTableName;

	/**
	 * 固定操作指定的数据源
	 */
	String forcrSite;

	long parseTime;

	public boolean isExtreme() {
		return extreme;
	}

	/**
	 * 构造
	 * 
	 * @param parent
	 * @param operType
	 * @param isSpecTransaction
	 * @param needGroup
	 * @throws SQLException
	 */
	Batch(Session parent, ITableMetadata meta) throws SQLException {
		this.parent = parent;
		this.groupForPartitionTable = meta.getPartition() != null;
		this.meta = meta;
	}

	/**
	 * 获取指定的要在哪个数据库上执行操作
	 * 
	 * @return 要在哪个数据库上执行操作
	 */
	public String getForcrSite() {
		return forcrSite;
	}

	/**
	 * 指定要在哪个数据库上执行操作
	 * 
	 * @param forcrSite
	 */
	public void setForcrSite(String forcrSite) {
		this.forcrSite = forcrSite;
	}

	/**
	 * 获得当前指定的 固定表名
	 * 
	 * 当设置了固定的表名后，则认为要操作的表就是指定的表名，框架本身计算的表名（包含通过分表规则计算的表名）就会失效。
	 * 
	 * @return
	 */
	public String getForceTableName() {
		return forceTableName;
	}

	/**
	 * 设置固定表名 当指定了固定的表名后，则认为要操作的表就是指定的表名，框架本身计算的表名（包含通过分表规则计算的表名）就会失效。
	 * 
	 * @param forceTableName
	 */
	public void setForceTableName(String forceTableName) {
		if (forceTableName != null) {
			this.forceTableName = MetaHolder.toSchemaAdjustedName(forceTableName);
		}
	}

	/**
	 * 是否重新分组再插入<br>
	 * <li>为什么要重新分组</li><br>
	 * 当支持多多库多表时，由于对象不一定都写入到同一张表中，因此不能在一个批上操作。<br>
	 * EF-ORM会对每个对象进行路由计算，将相同路由结果的对象分为一组，再批量写入数据库。
	 * <p>
	 * 显然，对每个对象单独路由一次是有相当的性能损耗的，因此用户可以设置分组功能是否开启。
	 * <p>
	 * 关闭时，则将批的第一个对象对应的表作为写入表，适用非分表的对象，以及虽然分表但用户能自行确保操作的所有数据位于一张表中的情况。<br>
	 * <br>
	 * <li>此外。如果用户设置了固定表名({@link #setForceTableName(String)})，那么分组操作无效。</li>
	 * 
	 * 
	 * @return 如果重分组开关开启返回true，反之false。
	 */
	public boolean isGroupForPartitionTable() {
		return groupForPartitionTable;
	}

	/**
	 * 设置是否重新分组再插入<br>
	 * <li>为什么要重新分组</li><br>
	 * 当支持多多库多表时，由于对象不一定都写入到同一张表中，因此不能在一个批上操作。<br>
	 * EF-ORM会对每个对象进行路由计算，将相同路由结果的对象分为一组，再批量写入数据库。
	 * <p>
	 * 显然，对每个对象单独路由一次是有相当的性能损耗的，因此用户可以设置分组功能是否开启。
	 * <p>
	 * 关闭时，则将批的第一个对象对应的表作为写入表，适用非分表的对象，以及虽然分表但用户能自行确保操作的所有数据位于一张表中的情况。<br>
	 * <br>
	 * <li>此外。如果用户设置了固定表名({@link #setForceTableName(String)})，那么分组操作无效。</li>
	 * 
	 * 
	 * @param regroupForPartitionTable
	 *            重分组功能开关
	 */
	public void setGroupForPartitionTable(boolean regroupForPartitionTable) {
		this.groupForPartitionTable = regroupForPartitionTable;
	}

	/**
	 * 提交并执行批数据。
	 * 注意Batch对应的SQL语句是固定的。因此此处传入的对象只会影响参数中的绑定变量和SQL语句中的表名。对where条件、Update中的set子句不会构成影响。
	 * 
	 * @throws SQLException
	 */
	public int execute(List<T> objs) throws SQLException {
		if (objs.isEmpty()) {
			return 0;
		}
		boolean debugMode = ORMConfig.getInstance().isDebugMode();
		String tablename=null;
		
		try {
			if (this.groupForPartitionTable && forceTableName == null) {// 需要分组
				callVeryBefore(objs);
				int total = 0;
				Map<PairSS, List<T>> data = doGroup(objs);
				for (Map.Entry<PairSS, List<T>> entry : data.entrySet()) {
					long start = System.currentTimeMillis();
					PairSS target = entry.getKey();
					String dbName = parent.getTransactionId(target.first);
					tablename=target.second;
					long dbAccess = innerCommit(entry.getValue(),target.first, tablename, dbName);
					total += executeResult;
					if (debugMode) {
						LogUtil.info(StringUtils.concat(this.getClass().getSimpleName(), " Batch executed:", String.valueOf(entry.getValue().size()), "/on ", String.valueOf(executeResult), " record(s) on [" + entry.getKey() + "]\t Time cost([ParseSQL]:",
								String.valueOf(parseTime / 1000), "us, [DbAccess]:", String.valueOf(dbAccess - start), "ms) |", dbName));
					}
				}
				if (debugMode) {
					LogUtil.info(StringUtils.concat(this.getClass().getSimpleName(), " Batch executed:", String.valueOf(objs.size()), "/on ", String.valueOf(total), " record(s) and ", String.valueOf(data.size()), " tables. |  @", String.valueOf(Thread.currentThread().getId())));
				}
			} else {// 不分组
				long start = System.currentTimeMillis();
				T obj = objs.get(0);
				String site = null;
				callVeryBefore(objs);
				if (forceTableName != null) {
					tablename = forceTableName;
				} else {
					PartitionResult pr = DbUtils.toTableName(obj, null, obj.getQuery(), parent.getPartitionSupport());
					site = forcrSite != null ? forcrSite : pr.getDatabase();
					tablename = pr.getAsOneTable();
				}
				String dbName = parent.getTransactionId(null);
				long dbAccess = innerCommit(objs, site, tablename, dbName);
				if (debugMode) {
					LogUtil.info(StringUtils.concat(this.getClass().getSimpleName(), " Batch executed total:", String.valueOf(objs.size()), "/on ", String.valueOf(executeResult), " record(s)\t Time cost([ParseSQL]:", String.valueOf(parseTime / 1000), "us, [DbAccess]:",
							String.valueOf(dbAccess - start), "ms) |", dbName));
				}
			}
		} catch (SQLException e) {
			DebugUtil.setSqlState(e, tablename);
			throw e;
		}
		return executeResult;
	}

	protected long innerCommit(List<T> objs, String site, String tablename, String dbName) throws SQLException {
		String sql = toSql(tablename);
		if (ORMConfig.getInstance().isDebugMode())
			LogUtil.show(sql + " | " + dbName);
		OperateTarget db = parent.asOperateTarget(site);
		PreparedStatement p = db.prepareStatement(sql);
		try {
			return doCommit(p, db, objs);
		} finally {
			p.close();
			db.releaseConnection();
		}
	}

	/*
	 * 按计算的表名进行分组
	 * 
	 * @return 返回的Map中，key为计算出的表名(数据源名称加上-表名)，value为该表上的操作对象
	 */
	private Map<PairSS, List<T>> doGroup(List<T> objs) {
		Map<PairSS, List<T>> result = new HashMap<PairSS, List<T>>();
		for (T obj : objs) {
			PartitionResult partitionResult = DbUtils.toTableName(obj, null, obj.getQuery(), parent.getPartitionSupport());
			if (this.forcrSite != null) {
				partitionResult.setDatabase(forcrSite);
			}
			PairSS tablename = new PairSS(partitionResult.getDatabase(),partitionResult.getAsOneTable());
			List<T> list = result.get(tablename);
			if (list == null) {
				list = new ArrayList<T>();
				result.put(tablename, list);
			}
			list.add(obj);
		}
		return result;
	}

	/*
	 * 提交每批数据，返回本次数据库完成后的时间
	 */
	protected long doCommit(PreparedStatement psmt, OperateTarget db, List<T> listValue) throws SQLException {
		callEventListenerBefore(listValue);
		processJdbcParams(psmt, listValue, db);
		int[] result = psmt.executeBatch();
		int total = 0;
		if (result[0] < 0) {
			total = result[0];
		} else {
			for (int i : result) {
				total += i;
			}
		}
		this.executeResult = total;
		long dbAccess = System.currentTimeMillis();
		callEventListenerAfter(listValue);
		return dbAccess;
	}
	
	/*
	 * 分库分表前执行，调用主键回调
	 */
	protected abstract void callVeryBefore(List<T> objs) throws SQLException;
	/*
	 * 操作前执行，调用监听器
	 */
	protected abstract void callEventListenerBefore(List<T> listValue) throws SQLException;
	/*
	 * 操作后执行，返回主键，以及执行监听器
	 */
	protected abstract void callEventListenerAfter(List<T> listValue) throws SQLException;

	protected abstract void processJdbcParams(PreparedStatement psmt, List<T> listValue, OperateTarget db) throws SQLException;

	/**
	 * 根据传入的表名，计算针对该表的SQL语句
	 * 
	 * @param tablename
	 * @return
	 */
	protected abstract String toSql(String tablename);

	static final class Insert<T extends IQueryableEntity> extends Batch<T> {
		/**
		 * SQL片段,Insert部分(INSERT语句使用)
		 */
		private InsertSqlClause insertPart;

		Insert(Session parent, ITableMetadata meta) throws SQLException {
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
			TransactionCache cache = parent.getCache();
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
						cache.onInsert(t,forceTableName);
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
			List<ColumnMapping<?>> writeFields = insertPart.getFields();
			int len = listValue.size();
			SqlLog log=ORMConfig.getInstance().newLogger(this.extreme);
			int maxLog = ORMConfig.getInstance().getMaxBatchLog();
			for (int i = 0; i < len; i++) {
				T t = listValue.get(i);
				BindVariableContext context = new BindVariableContext(psmt, db.getProfile(), log.append("Batch Parameters: ",i + 1).append('/').append(len));
				BindVariableTool.setInsertVariables(t, writeFields, context);
				psmt.addBatch();
				if (log.isDebug()) {
					log.output();
					if (i + 1 == maxLog) {
						log.directLog("Batch Parameters: After " + maxLog + "th are ignored to reduce the size of log file.");
						log=SqlLog.DUMMY;
					}
				}
			}
		}

		protected long innerCommit(List<T> objs, String site, String tablename, String dbName) throws SQLException {
			OperateTarget db = parent.asOperateTarget(site);
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

	static final class Update<T extends IQueryableEntity> extends Batch<T> {
		/**
		 * SQL片段，update部分(UPDATE语句使用)
		 */
		private UpdateClause updatePart;

		/**
		 * SQL片段，where部分(UPDATE和DELETE语句使用)
		 */
		private BindSql wherePart;

		Update(Session parent, ITableMetadata meta) throws SQLException {
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
			SqlLog log=ORMConfig.getInstance().newLogger(this.extreme);
			int maxLog = ORMConfig.getInstance().getMaxBatchLog();
			for (int i = 0; i < len; i++) {
				T t = listValue.get(i);
				BindVariableContext context = new BindVariableContext(psmt, db.getProfile(), log.append("Batch Parameters: ",i + 1).append('/').append(len));
				List<Object> whereBind = BindVariableTool.setVariables(t.getQuery(), updatePart.getVariables(), bindVar, context);
				psmt.addBatch();
				parent.getCache().onUpdate(forceTableName == null ? meta.getName() : forceTableName, wherePart.getSql(), whereBind);

				if (log.isDebug()) {
					log.output();
					if (i + 1 == maxLog) {
						log.directLog("Batch Parameters: After " + maxLog + "th are ignored to reduce the size of log file.");
						log=SqlLog.DUMMY;
					}

				}

			}

		}

		@Override
		protected void callVeryBefore(List<T> objs) throws SQLException {
		}
	}

	static final class Delete<T extends IQueryableEntity> extends Batch<T> {
		/**
		 * SQL片段，where部分(UPDATE和DELETE语句使用)
		 */
		private BindSql wherePart;

		Delete(Session parent, ITableMetadata meta) throws SQLException {
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
			SqlLog log=ORMConfig.getInstance().newLogger(this.extreme);
			int maxLog = ORMConfig.getInstance().getMaxBatchLog();
			for (int i = 0; i < len; i++) {
				T t = listValue.get(i);
				if (t.getQuery().getConditions().isEmpty()) {
					DbUtils.fillConditionFromField(t, t.getQuery(), true, pkMpode);
				}
				BindVariableContext context = new BindVariableContext(psmt, db.getProfile(), log.append("Batch Parameters: ",i + 1).append('/').append(len));
				List<Object> whereBind = BindVariableTool.setVariables(t.getQuery(), null, bindVar, context);
				parent.getCache().onDelete(forceTableName == null ? meta.getName() : forceTableName, wherePart.getSql(), whereBind);

				psmt.addBatch();
				if (log.isDebug()) {
					log.output();
					if (i + 1 == maxLog) {
						log.directLog("Batch Parameters: After " + maxLog + "th are ignored to reduce the size of log file.");
						log=SqlLog.DUMMY;// 关闭日志开关
					}
				}

			}

		}

		@Override
		protected void callVeryBefore(List<T> objs) throws SQLException {
		}
	}
}

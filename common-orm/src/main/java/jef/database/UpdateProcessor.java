package jef.database;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jef.common.Entry;
import jef.common.log.LogUtil;
import jef.database.annotation.PartitionResult;
import jef.database.dialect.ColumnType;
import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.AbstractTimeMapping;
import jef.database.dialect.type.ColumnMapping;
import jef.database.jsqlparser.parser.ParseException;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.meta.Feature;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.database.query.BindVariableField;
import jef.database.query.JoinElement;
import jef.database.query.JpqlExpression;
import jef.database.query.ParameterProvider.MapProvider;
import jef.database.query.Query;
import jef.database.query.SqlContext;
import jef.database.query.SqlExpression;
import jef.database.wrapper.clause.BindSql;
import jef.database.wrapper.clause.UpdateClause;
import jef.database.wrapper.executor.DbTask;
import jef.database.wrapper.processor.BindVariableContext;
import jef.database.wrapper.processor.BindVariableTool;
import jef.tools.Assert;
import jef.tools.StringUtils;
import jef.tools.reflect.BeanWrapper;

/**
 * 基本数据库操作
 * 
 * @author jiyi
 * 
 */
public abstract class UpdateProcessor {
	/**
	 * 执行更新操作
	 * 
	 * @param db
	 * @param obj
	 * @param setValues
	 * @param whereValues
	 * @param p
	 * @param parseCost
	 * @return
	 * @throws SQLException
	 */
	abstract int processUpdate0(OperateTarget db, IQueryableEntity obj, UpdateClause setValues, BindSql whereValues, PartitionResult site) throws SQLException;

	/**
	 * 形成update语句
	 * 
	 * @param obj
	 *            更新请求
	 * @param dynamic
	 *            动态更新标记
	 * @return SQL片段
	 */
	abstract UpdateClause toUpdateClause(IQueryableEntity obj, PartitionResult[] prs, boolean dynamic) throws SQLException;

	/**
	 * 形成update语句
	 * 
	 * @param obj
	 *            更新请求
	 * @param dynamic
	 *            动态更新标记
	 * @return SQL片段
	 */
	abstract UpdateClause toUpdateClauseBatch(IQueryableEntity obj, PartitionResult[] prs, boolean dynamic) throws SQLException;

	/**
	 * 形成whrer部分语句
	 * 
	 * @param joinElement
	 * @param context
	 * @param update
	 * @param profile
	 * @return
	 */
	abstract BindSql toWhereClause(JoinElement joinElement, SqlContext context, boolean update, DatabaseDialect profile);

	static UpdateProcessor get(DatabaseDialect profile, DbClient db) {
		if (profile.has(Feature.NO_BIND_FOR_UPDATE)) {
			return new NormalImpl(db);
		} else {
			return new PreparedImpl(db);
		}
	}

	protected DbClient parent;

	UpdateProcessor(DbClient parent) {
		this.parent = parent;
	}

	final static class NormalImpl extends UpdateProcessor {
		private UpdateProcessor prepared;

		public NormalImpl(DbClient db) {
			super(db);
			prepared = new PreparedImpl(db);
		}

		int processUpdate0(OperateTarget db, IQueryableEntity obj, UpdateClause update, BindSql where, PartitionResult site) throws SQLException {
			int result = 0;
			for (String tablename : site.getTables()) {
				String sql = "update " + tablename + " set " + update.getSql() + where;
				Statement st = null;
				try {
					st = db.createStatement();
					int updateTimeout = ORMConfig.getInstance().getUpdateTimeout();
					if (updateTimeout > 0)
						st.setQueryTimeout(updateTimeout);
					int currentUpdateCount = st.executeUpdate(sql);
					result += currentUpdateCount;
					obj.applyUpdate();
				} catch (SQLException e) {
					DbUtils.processError(e, tablename, db);
					db.releaseConnection();
					throw e;
				} finally {
					if (ORMConfig.getInstance().isDebugMode())
						LogUtil.show(sql + " | " + db.getTransactionId());

					if (st != null)
						st.close();
				}
			}
			db.releaseConnection();
			return result;
		}

		/**
		 * 转换成Update字句
		 */
		@SuppressWarnings("unchecked")
		public UpdateClause toUpdateClause(IQueryableEntity obj, PartitionResult[] prs, boolean dynamic) throws SQLException {
			DatabaseDialect profile = getProfile(prs);
			UpdateClause result = new UpdateClause();
			ITableMetadata meta = MetaHolder.getMeta(obj);
			Map<Field, Object> map = obj.getUpdateValueMap();

			Map.Entry<Field, Object>[] fields;
			if (dynamic) {
				fields = map.entrySet().toArray(new Map.Entry[map.size()]);
				moveLobFieldsToLast(fields, meta);

				// 增加时间戳自动更新的列
				AbstractTimeMapping<?>[] autoUpdateTime = meta.getUpdateTimeDef();
				if (autoUpdateTime != null) {
					for (AbstractTimeMapping<?> tm : autoUpdateTime) {
						if (!map.containsKey(tm.field())) {
							Object value = tm.getAutoUpdateValue(profile, obj);
							if (value != null) {
								result.addEntry(tm.getColumnName(profile, true), tm.getSqlStr(value, profile));
							}
						}
					}
				}
			} else {
				fields = getAllFieldValues(meta, map, BeanWrapper.wrap(obj), profile);
			}

			// 其他列
			for (Map.Entry<Field, Object> entry : fields) {
				Field field = entry.getKey();
				Object value = entry.getValue();
				ColumnMapping<?> vType = meta.getColumnDef(field);

				if (value == null) {
					result.addEntry(vType.getColumnName(profile, true), "null");
				} else {
					result.addEntry(vType.getColumnName(profile, true), vType.getSqlStr(value, profile));
				}
			}
			return result;
		}

		@Override
		UpdateClause toUpdateClauseBatch(IQueryableEntity obj, PartitionResult[] prs, boolean dynamic) throws SQLException {
			return prepared.toUpdateClauseBatch(obj, prs, dynamic);
		}

		@Override
		BindSql toWhereClause(JoinElement joinElement, SqlContext context, boolean update, DatabaseDialect profile) {
			return parent.rProcessor.toWhereClause(joinElement, context, update, profile);
		}
	}

	final static class PreparedImpl extends UpdateProcessor {
		public PreparedImpl(DbClient db) {
			super(db);
		}

		int processUpdate0(OperateTarget db, IQueryableEntity obj, UpdateClause setValues, BindSql whereValues, PartitionResult site) throws SQLException {
			boolean debugMode = ORMConfig.getInstance().isDebugMode();
			int result = 0;
			for (String tablename : site.getTables()) {
				String updateSql = StringUtils.concat("update ", tablename, " set ", setValues.getSql(), whereValues.getSql());
				StringBuilder sb = null;
				if (debugMode)
					sb = new StringBuilder(updateSql.length() + 150).append(updateSql).append(" | ").append(db.getTransactionId());
				PreparedStatement psmt = null;
				try {

					psmt = db.prepareStatement(updateSql);
					int updateTimeout = ORMConfig.getInstance().getUpdateTimeout();
					if (updateTimeout > 0) {
						psmt.setQueryTimeout(updateTimeout);
					}
					BindVariableContext context = new BindVariableContext(psmt, db.getProfile(), sb);
					BindVariableTool.setVariables(obj.getQuery(), setValues.getVariables(), whereValues.getBind(), context);
					psmt.execute();
					int currentUpdateCount = psmt.getUpdateCount();
					result += currentUpdateCount;
					obj.applyUpdate();
				} catch (SQLException e) {
					DbUtils.processError(e, tablename, db);
					db.releaseConnection();
					throw e;
				} finally {
					if (debugMode)
						LogUtil.show(sb);

					if (psmt != null)
						psmt.close();
				}
			}
			db.releaseConnection();
			return result;
		}

		/**
		 * 返回2个参数 第一个为带？的SQL String 第二个为 update语句中所用的Field
		 * 
		 * @param obj
		 * @return
		 */
		@SuppressWarnings("unchecked")
		public UpdateClause toUpdateClauseBatch(IQueryableEntity obj, PartitionResult[] prs, boolean dynamic) {
			DatabaseDialect profile = getProfile(prs);
			UpdateClause result = new UpdateClause();

			Map<Field, Object> map = obj.getUpdateValueMap();
			Map.Entry<Field, Object>[] fields;
			ITableMetadata meta = MetaHolder.getMeta(obj);
			if (dynamic) {
				fields = map.entrySet().toArray(new Map.Entry[map.size()]);
				moveLobFieldsToLast(fields, meta);

				// 增加时间戳自动更新的列
				AbstractTimeMapping<?>[] autoUpdateTime = meta.getUpdateTimeDef();
				if (autoUpdateTime != null) {
					for (AbstractTimeMapping<?> tm : autoUpdateTime) {
						if (!map.containsKey(tm.field())) {
							tm.processAutoUpdate(profile, result);
						}
					}
				}
			} else {
				fields = getAllFieldValues(meta, map, BeanWrapper.wrap(obj), profile);
			}

			for (Map.Entry<Field, Object> e : fields) {
				Field field = e.getKey();
				Object value = e.getValue();
				String columnName = meta.getColumnName(field, profile, true);
				if (value instanceof SqlExpression) {
					String sql = ((SqlExpression) value).getText();
					if (obj.hasQuery()) {
						Map<String, Object> attrs = ((Query<?>) obj.getQuery()).getAttributes();
						if (attrs != null && attrs.size() > 0) {
							try {
								Expression ex = DbUtils.parseExpression(sql);
								Entry<String, List<Object>> fieldInExpress = NamedQueryConfig.applyParam(ex, new MapProvider(attrs));
								if (fieldInExpress.getValue().size() > 0) {
									sql = fieldInExpress.getKey();
									for (Object v : fieldInExpress.getValue()) {
										result.addField(new BindVariableField(v));
									}
								}
							} catch (ParseException e1) {
								// 如果解析异常就不修改sql语句
							}
						}
					}
					result.addEntry(columnName, sql);
				} else if (value instanceof JpqlExpression) {
					JpqlExpression je = (JpqlExpression) value;
					if (!je.isBind())
						je.setBind(obj.getQuery());
					result.addEntry(columnName, je.toSqlAndBindAttribs(null, profile));
				} else if (value instanceof jef.database.Field) {// FBI
																	// Field不可能在此
					String setColumn = meta.getColumnName((Field) value, profile, true);
					result.addEntry(columnName, setColumn);
				} else {
					result.addEntry(columnName, field);
				}
			}
			return result;
		}

		@Override
		UpdateClause toUpdateClause(IQueryableEntity obj, PartitionResult[] prs, boolean dynamic) throws SQLException {
			return toUpdateClauseBatch(obj, prs, dynamic);
		}

		@Override
		BindSql toWhereClause(JoinElement joinElement, SqlContext context, boolean update, DatabaseDialect profile) {
			return parent.rProcessor.toPrepareWhereSql(joinElement, context, update, profile);
		}
	}

	/**
	 * 更新前，将所有LLOB字段都移动到最后去
	 * 
	 * @param fields
	 * @param meta
	 */
	static void moveLobFieldsToLast(java.util.Map.Entry<Field, Object>[] fields, final ITableMetadata meta) {
		Arrays.sort(fields, new Comparator<Map.Entry<Field, Object>>() {
			public int compare(Map.Entry<Field, Object> o1, Map.Entry<Field, Object> o2) {
				Field field1 = o1.getKey();
				Field field2 = o2.getKey();
				Assert.notNull(meta.getColumnDef(field1));
				Assert.notNull(meta.getColumnDef(field2));

				Class<? extends ColumnType> type1 = meta.getColumnDef(field1).get().getClass();
				Class<? extends ColumnType> type2 = meta.getColumnDef(field2).get().getClass();
				Boolean b1 = (type1 == ColumnType.Blob.class || type1 == ColumnType.Clob.class);
				Boolean b2 = (type2 == ColumnType.Blob.class || type2 == ColumnType.Clob.class);
				return b1.compareTo(b2);
			}
		});
	}

	protected DatabaseDialect getProfile(PartitionResult[] prs) {
		if (prs == null || prs.length == 0) {
			return parent.getProfile();
		}
		return this.parent.getProfile(prs[0].getDatabase());
	}

	// 将所有非主键字段作为update的值
	@SuppressWarnings("unchecked")
	static java.util.Map.Entry<Field, Object>[] getAllFieldValues(ITableMetadata meta, Map<Field, Object> map, BeanWrapper wrapper, DatabaseDialect profile) {
		List<Entry<Field, Object>> result = new ArrayList<Entry<Field, Object>>();
		for (ColumnMapping<?> vType : meta.getColumns()) {
			Field field = vType.field();
			if (map.containsKey(field)) {
				result.add(new Entry<Field, Object>(field, map.get(field)));
			} else {
				if (vType.isPk()) {
					continue;
				}
				if (vType instanceof AbstractTimeMapping<?>) {
					AbstractTimeMapping<?> times = (AbstractTimeMapping<?>) vType;
					if (times.isForUpdate()) {
						Object value = times.getAutoUpdateValue(profile, wrapper.getWrapped());
						result.add(new Entry<Field, Object>(field, value));
						continue;
					}
				}
				result.add(new Entry<Field, Object>(field, wrapper.getPropertyValue(field.name())));
			}
		}
		return result.toArray(new Map.Entry[result.size()]);
	}

	public int processUpdate(Session session, final IQueryableEntity obj,final UpdateClause updateClause,final BindSql whereClause, PartitionResult[] sites, long parseCost) throws SQLException {
		int total = 0;
		long access=System.currentTimeMillis();
		String dbName=null;
		if (sites.length >= ORMConfig.getInstance().getParallelSelect()) {
			List<DbTask> tasks = new ArrayList<DbTask>();
			final AtomicInteger count=new AtomicInteger();
			for (final PartitionResult site : sites) {
				final OperateTarget db = session.asOperateTarget(site.getDatabase());
				dbName=db.getTransactionId();
				tasks.add(new DbTask(){
					@Override
					public void execute() throws SQLException {
						count.addAndGet(processUpdate0(db, obj, updateClause, whereClause, site));
					}
				});
			}
			DbUtils.parallelExecute(tasks);
			total=count.get();
		} else {
			for (PartitionResult site : sites) {
				OperateTarget db = session.asOperateTarget(site.getDatabase());
				dbName=db.getTransactionId();
				total += processUpdate0(db, obj, updateClause, whereClause, site);
			}
		}
		if (ORMConfig.getInstance().debugMode){
			access=System.currentTimeMillis();
			LogUtil.show(StringUtils.concat("Updated:", String.valueOf(total), "\t Time cost([ParseSQL]:", String.valueOf(parseCost), "ms, [DbAccess]:", String.valueOf(access), "ms) |", dbName));
		}
		return total;
	}
}

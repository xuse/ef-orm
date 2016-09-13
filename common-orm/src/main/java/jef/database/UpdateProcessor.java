package jef.database;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jef.common.Entry;
import jef.common.PairSO;
import jef.common.log.LogUtil;
import jef.database.Session.UpdateContext;
import jef.database.dialect.ColumnType;
import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.ColumnMapping;
import jef.database.dialect.type.VersionSupportColumn;
import jef.database.jsqlparser.parser.ParseException;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.database.query.BindVariableField;
import jef.database.query.JoinElement;
import jef.database.query.JpqlExpression;
import jef.database.query.ParameterProvider.MapProvider;
import jef.database.query.Query;
import jef.database.query.SqlContext;
import jef.database.query.SqlExpression;
import jef.database.routing.PartitionResult;
import jef.database.support.SqlLog;
import jef.database.wrapper.clause.BindSql;
import jef.database.wrapper.clause.UpdateClause;
import jef.database.wrapper.executor.DbTask;
import jef.database.wrapper.processor.BindVariableContext;
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
	abstract int processUpdate0(OperateTarget db, IQueryableEntity obj, UpdateClause setValues, BindSql whereValues, PartitionResult site, SqlLog log) throws SQLException;

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
	abstract BindSql toWhereClause(JoinElement joinElement, SqlContext context, UpdateContext update, DatabaseDialect profile);

	static UpdateProcessor get(DatabaseDialect profile, DbClient db) {
		return new PreparedImpl(db.preProcessor);
	}

	protected SqlProcessor processor;

	UpdateProcessor(SqlProcessor parent) {
		this.processor = parent;
	}

	final static class PreparedImpl extends UpdateProcessor {
		public PreparedImpl(SqlProcessor db) {
			super(db);
		}

		int processUpdate0(OperateTarget db, IQueryableEntity obj, UpdateClause setValues, BindSql whereValues, PartitionResult site, SqlLog log) throws SQLException {
			int result = 0;
			for (String tablename : site.getTables()) {
				tablename = DbUtils.escapeColumn(db.getProfile(), tablename);
				String updateSql = StringUtils.concat("update ", tablename, " set ", setValues.getSql(), whereValues.getSql());
				log.ensureCapacity(updateSql.length() + 150);
				log.append(updateSql).append(db);
				PreparedStatement psmt = null;
				try {
					psmt = db.prepareStatement(updateSql);
					int updateTimeout = ORMConfig.getInstance().getUpdateTimeout();
					if (updateTimeout > 0) {
						psmt.setQueryTimeout(updateTimeout);
					}
					BindVariableContext context = new BindVariableContext(psmt, db.getProfile(), log);
					context.setVariables(obj.getQuery(), setValues.getVariables(), whereValues.getBind());
					psmt.execute();
					int currentUpdateCount = psmt.getUpdateCount();
					result += currentUpdateCount;
					obj.applyUpdate();
				} catch (SQLException e) {
					DbUtils.processError(e, tablename, db);
					db.releaseConnection();
					throw e;
				} finally {
					log.output();
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
			DatabaseDialect profile = processor.getProfile(prs);
			UpdateClause result = new UpdateClause();

			Map<Field, Object> map = obj.getUpdateValueMap();
			Map.Entry<Field, Object>[] fields;
			ITableMetadata meta = MetaHolder.getMeta(obj);
			if (dynamic) {
				fields = map.entrySet().toArray(new Map.Entry[map.size()]);
				moveLobFieldsToLast(fields, meta);

				// 增加时间戳自动更新的列
				VersionSupportColumn[] autoUpdateTime = meta.getAutoUpdateColumnDef();
				if (autoUpdateTime != null) {
					for (VersionSupportColumn tm : autoUpdateTime) {
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
				ColumnMapping column=meta.getColumnDef(field);
				if(column!=null && column.isNotUpdate()){
					continue;
				}
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
					PairSO<List<BindVariableField>> entry =je.toSqlAndBindAttribs2(new SqlContext(null, obj.getQuery()), profile);
					result.addEntry(columnName, entry.first);
					for(BindVariableField binder: entry.second){
						result.addField(binder);
					}
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
		BindSql toWhereClause(JoinElement joinElement, SqlContext context, UpdateContext update, DatabaseDialect profile) {
			return processor.toWhereClause(joinElement, context, update, profile);
		}
	}

	/**
	 * 更新前，将所有LOB字段都移动到最后去。之前碰到一个BUG(Oracle)可能和驱动有关，LOB 不在最后，更新会出错。
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

	/*
	 * 在更新数据的时候，如果无法确定哪些字段作了修改，那么就将所有非主键字段作为update的值
	 */
	@SuppressWarnings("unchecked")
	static java.util.Map.Entry<Field, Object>[] getAllFieldValues(ITableMetadata meta, Map<Field, Object> map, BeanWrapper wrapper, DatabaseDialect profile) {
		List<Entry<Field, Object>> result = new ArrayList<Entry<Field, Object>>();
		for (ColumnMapping vType : meta.getColumns()) {
			Field field = vType.field();
			if (map.containsKey(field)) {
				result.add(new Entry<Field, Object>(field, map.get(field)));
			} else {
				if (vType.isPk()) {
					continue;
				}
				if (vType instanceof VersionSupportColumn) {
					VersionSupportColumn timeColumn = (VersionSupportColumn) vType;
					if (timeColumn.isUpdateAlways()) {
						Object value = timeColumn.getAutoUpdateValue(profile, wrapper.getWrapped());
						result.add(new Entry<Field, Object>(field, value));
						continue;
					}
				}
				result.add(new Entry<Field, Object>(field, wrapper.getPropertyValue(field.name())));
			}
		}
		return result.toArray(new Map.Entry[result.size()]);
	}

	public int processUpdate(Session session, final IQueryableEntity obj, final UpdateClause updateClause, final BindSql whereClause, PartitionResult[] sites, long parseCost)
			throws SQLException {
		int total = 0;
		long access = System.currentTimeMillis();
		String dbName = null;
		final SqlLog log = ORMConfig.getInstance().newLogger();
		if (sites.length >= ORMConfig.getInstance().getParallelSelect()) {
			List<DbTask> tasks = new ArrayList<DbTask>();
			final AtomicInteger count = new AtomicInteger();
			for (final PartitionResult site : sites) {
				final OperateTarget db = session.selectTarget(site.getDatabase());
				dbName = db.getTransactionId();
				tasks.add(new DbTask() {
					@Override
					public void execute() throws SQLException {
						count.addAndGet(processUpdate0(db, obj, updateClause, whereClause, site, log));
					}
				});
			}
			DbUtils.parallelExecute(tasks);
			total = count.get();
		} else {
			for (PartitionResult site : sites) {
				OperateTarget db = session.selectTarget(site.getDatabase());
				dbName = db.getTransactionId();
				total += processUpdate0(db, obj, updateClause, whereClause, site, log);
			}
		}
		if (ORMConfig.getInstance().debugMode) {
			access = System.currentTimeMillis() - access;
			LogUtil.show(StringUtils.concat("Updated:", String.valueOf(total), "\t Time cost([ParseSQL]:", String.valueOf(parseCost), "ms, [DbAccess]:", String.valueOf(access),
					"ms) |", dbName));
		}
		return total;
	}
}

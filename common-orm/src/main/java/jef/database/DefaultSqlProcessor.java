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

import java.lang.reflect.Array;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jef.common.Entry;
import jef.database.annotation.PartitionResult;
import jef.database.dialect.ColumnType;
import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.AbstractTimeMapping;
import jef.database.dialect.type.MappingType;
import jef.database.innerpool.PartitionSupport;
import jef.database.jsqlparser.parser.ParseException;
import jef.database.jsqlparser.statement.select.PlainSelect;
import jef.database.jsqlparser.statement.select.Union;
import jef.database.jsqlparser.visitor.DeParserAdapter;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.SelectBody;
import jef.database.jsqlparser.visitor.ToCountDeParser;
import jef.database.meta.FBIField;
import jef.database.meta.Feature;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.database.query.AbstractJoinImpl;
import jef.database.query.BindVariableField;
import jef.database.query.JoinElement;
import jef.database.query.JpqlExpression;
import jef.database.query.ParameterProvider.MapProvider;
import jef.database.query.Query;
import jef.database.query.SqlContext;
import jef.database.query.SqlExpression;
import jef.database.wrapper.clause.BindSql;
import jef.tools.StringUtils;
import jef.tools.reflect.BeanWrapper;

import org.apache.commons.lang.RandomStringUtils;

/**
 * 用于将数据库相关的SQL处理和查询结果处理
 * 
 * @author Administrator
 * 
 */
public class DefaultSqlProcessor implements SqlProcessor {
	private DatabaseDialect profile;
	protected DbClient parent;
	private Expression EXP_ROWID;

	DefaultSqlProcessor(DatabaseDialect profile, DbClient parent) {
		this.profile = profile;
		this.parent = parent;
		try {
			this.EXP_ROWID = DbUtils.parseExpression("ROWID");
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 转换到Where子句
	 */
	public String toWhereClause(JoinElement obj, SqlContext context, boolean update,DatabaseDialect profile) {
		String sb = innerToWhereClause(obj, context, update,profile);
		if (sb.length() > 0) {
			return " where " + sb;
		} else {
			return sb;
		}
	}

	/**
	 * 转换到绑定类Where字句
	 */
	public BindSql toPrepareWhereSql(JoinElement obj, SqlContext context, boolean update,DatabaseDialect profile) {
		BindSql result = innerToPrepareWhereSql(obj, context, update,profile);
		if (result.getSql().length() > 0) {
			result.setSql(" where ".concat(result.getSql()));
		}
		return result;
	}

	/**
	 * 转换成Update字句
	 */
	@SuppressWarnings("unchecked")
	public String toUpdateClause(IQueryableEntity obj, boolean dynamic) throws SQLException {
		DatabaseDialect profile = getProfile();
		StringBuilder sb = new StringBuilder();
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
							if (sb.length() > 0)
								sb.append(", ");
							sb.append(tm.getColumnName(profile, true)).append(" = ");
							sb.append(tm.getSqlStr(value, profile));
						}
					}
				}
			}
		} else {
			fields = getAllFieldValues(meta, map, BeanWrapper.wrap(obj));
		}

		if (dynamic) {
			// 其他列
			for (Map.Entry<Field, Object> entry : fields) {
				Field field = entry.getKey();
				Object value = entry.getValue();
				MappingType<?> vType = meta.getColumnDef(field);
				if (sb.length() > 0)
					sb.append(", ");
				sb.append(vType.getColumnName(profile, true)).append(" = ");
				if (value == null) {
					sb.append("null");
				} else {
					sb.append(vType.getSqlStr(value, profile));
				}
			}
		}
		return sb.toString();
	}

	// 获得容器需要的值
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Object collectValueToContainer(List<? extends IQueryableEntity> records, Class<?> containerType, String targetField) {
		Collection c = null;
		if (containerType == Set.class) {
			c = new HashSet();
		} else if (containerType == List.class || containerType.isArray()) {
			c = new ArrayList();
		} else {
			if (!records.isEmpty()) {
				BeanWrapper bean = BeanWrapper.wrap(records.get(0));
				return bean.getPropertyValue(targetField);
			}
			return null;
			// throw new IllegalArgumentException(containerType +
			// " is not a known collection type.");
		}
		for (IQueryableEntity d : records) {
			BeanWrapper bean = BeanWrapper.wrap(d);
			c.add(bean.getPropertyValue(targetField));
		}
		if (containerType.isArray()) {
			return c.toArray((Object[]) Array.newInstance(containerType.getComponentType(), c.size()));
		} else {
			return c;
		}
	}

	public static final String wrapCount(String sql) {
		return StringUtils.concat("select count(*) from (", sql, ") t", RandomStringUtils.randomAlphanumeric(3));
	}

	public String toCountSql(String sql) throws SQLException {
		// 重新解析
		try {
			SelectBody select = DbUtils.parseNativeSelect(sql).getSelectBody();
			if (select instanceof Union) {
				// 对于union是没有好的办法来count的……，union all则可以多次查询后累加
				return wrapCount(sql);
			}
			PlainSelect plain = (PlainSelect) select;
			DeParserAdapter deparse = new ToCountDeParser(profile);
			plain.accept(deparse);
			return deparse.getBuffer().toString();
		} catch (ParseException e) {
			throw new SQLException("Parser error:" + sql);
		}
	}

	/**
	 * 返回2个参数 第一个为带？的SQL String 第二个为 update语句中所用的Field
	 * 
	 * @param obj
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Entry<List<String>, List<Field>> toPrepareUpdateClause(IQueryableEntity obj, PartitionResult[] prs,boolean dynamic) {
		DatabaseDialect profile = getProfile(prs);
		List<String> cstr = new ArrayList<String>();
		List<Field> params = new ArrayList<Field>();
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
						tm.processAutoUpdate(profile, cstr, params);
					}
				}
			}
		} else {
			fields = getAllFieldValues(meta, map, BeanWrapper.wrap(obj));
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
									params.add(new BindVariableField(v));
								}
							}
						} catch (ParseException e1) {
							// 如果解析异常就不修改sql语句
						}
					}
				}
				cstr.add(columnName + " = " + sql);
			} else if (value instanceof JpqlExpression) {
				JpqlExpression je = (JpqlExpression) value;
				if (!je.isBind())
					je.setBind(obj.getQuery());
				cstr.add(columnName + " = " + je.toSqlAndBindAttribs(null, this));
			} else if (value instanceof jef.database.Field) {// FBI Field不可能在此
				cstr.add(columnName + " = " + DbUtils.toColumnName((Field) value, profile,null));
			} else {
				cstr.add(columnName + " = ?");
				params.add(field);
			}
		}
		return new Entry<List<String>, List<Field>>(cstr, params);
	}

	//将所有非主键字段作为update的值
	@SuppressWarnings("unchecked")
	private java.util.Map.Entry<Field, Object>[] getAllFieldValues(ITableMetadata meta, Map<Field, Object> map, BeanWrapper wrapper) {
		List<Entry<Field, Object>> result = new ArrayList<Entry<Field, Object>>();
		for (MappingType<?> vType : meta.getMetaFields()) {
			Field field = vType.field();
			if (map.containsKey(field)) {
				result.add(new Entry<Field, Object>(field, map.get(field)));
			} else {
				if(vType.isPk()){
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
				Class<? extends ColumnType> type1 = meta.getColumnDef(field1).get().getClass();
				Class<? extends ColumnType> type2 = meta.getColumnDef(field2).get().getClass();
				Boolean b1 = (type1 == ColumnType.Blob.class || type1 == ColumnType.Clob.class);
				Boolean b2 = (type2 == ColumnType.Blob.class || type2 == ColumnType.Clob.class);
				return b1.compareTo(b2);
			}
		});
	}

	/**
	 * 获取数据库属性简要表
	 * @deprecated use getProfile(PartitionResult[])
	 */
	public DatabaseDialect getProfile() {
		return profile;
	}
	
	public DatabaseDialect getProfile(PartitionResult[] prs) {
		if(prs==null || prs.length==0){
			return profile;
		}
		return this.parent.getProfile(prs[0].getDatabase());
	}

	private String innerToWhereClause(JoinElement obj, SqlContext context, boolean removePKUpdate,DatabaseDialect profile) {
		if (obj instanceof AbstractJoinImpl) {
			AbstractJoinImpl join = (AbstractJoinImpl) obj;
			StringBuilder sb = new StringBuilder();
			for (Query<?> ele : join.elements()) {
				String condStr = generateWhereClause0(ele, context.getContextOf(ele), removePKUpdate, ele.getConditions(),profile);
				if (StringUtils.isEmpty(condStr)) {
					continue;
				}
				if (sb.length() > 0) {
					sb.append(" and ");
				}
				sb.append(condStr);
			}
			for (Map.Entry<Query<?>, List<Condition>> entry : join.getRefConditions().entrySet()) {
				Query<?> q = entry.getKey();
				String condStr = generateWhereClause0(q, context.getContextOf(q), false, entry.getValue(),profile);
				if (StringUtils.isEmpty(condStr)) {
					continue;
				}
				if (sb.length() > 0) {
					sb.append(" and ");
				}
				sb.append(condStr);
			}
			return sb.toString();
		} else if (obj instanceof Query<?>) {
			return generateWhereClause0((Query<?>) obj, context, removePKUpdate, obj.getConditions(),profile);
		} else {
			throw new IllegalArgumentException("Unknown Query class:" + obj.getClass().getName());
		}
	}

	private String generateWhereClause0(Query<?> q, SqlContext context, boolean removePKUpdate, List<Condition> conds,DatabaseDialect profile) {
		if (q.isAll())
			return "";
		if (conds.isEmpty()) {
			IQueryableEntity instance = q.getInstance();
			if (profile.has(Feature.SELECT_ROW_NUM) && instance.rowid() != null) {
				q.addCondition(new FBIField(EXP_ROWID, q), instance.rowid());
			} else {// 自动将主键作为条件
				DbUtils.fillConditionFromField(q.getInstance(), q, removePKUpdate, false);
			}
		}
		if (conds.isEmpty() && ORMConfig.getInstance().isAllowEmptyQuery()) {
			return "";
		}
		ITableMetadata meta = MetaHolder.getMeta(q.getInstance());
		StringBuilder sb = new StringBuilder();
		for (Condition c : conds) {
			if (sb.length() > 0)
				sb.append(" and ");
			sb.append(c.toSqlClause(meta, context, this, q.getInstance(),profile)); // 递归的，当do是属于Join中的一部分时，需要为其增加前缀
		}
		if (sb.length() == 0)
			throw new NullPointerException("Illegal usage of query:" + q.getClass().getName() + " object, must including any condition in query. or did you forget to set the primary key for the entity?");
		return sb.toString();
	}

	private BindSql innerToPrepareWhereSql(JoinElement query, SqlContext context, boolean removePKUpdate,DatabaseDialect profile) {
		if (query instanceof AbstractJoinImpl) {
			List<BindVariableDescription> params = new ArrayList<BindVariableDescription>();
			AbstractJoinImpl join = (AbstractJoinImpl) query;
			StringBuilder sb = new StringBuilder();
			for (Query<?> ele : join.elements()) {
				BindSql result = generateWhereCondition(ele, context.getContextOf(ele), ele.getConditions(), false);
				if (result.getBind() != null) {
					params.addAll(result.getBind());
				}
				if (StringUtils.isEmpty(result.getSql())) {
					continue;
				}
				if (sb.length() > 0) {
					sb.append(" and ");
				}
				sb.append(result.getSql());
			}
			for (Map.Entry<Query<?>, List<Condition>> entry : join.getRefConditions().entrySet()) {
				Query<?> q = entry.getKey();
				BindSql result = generateWhereCondition(q, context.getContextOf(q), entry.getValue(), false);
				if (result.getBind() != null) {
					params.addAll(result.getBind());
				}
				if (StringUtils.isEmpty(result.getSql())) {
					continue;
				}
				if (sb.length() > 0) {
					sb.append(" and ");
				}
				sb.append(result.getSql());
			}
			return new BindSql(sb.toString(), params);
		} else if (query instanceof Query<?>) {
			return generateWhereCondition((Query<?>) query, context, query.getConditions(), removePKUpdate);
		} else {
			throw new IllegalArgumentException("Unknown Query class:" + query.getClass().getName());
		}
	}

	private BindSql generateWhereCondition(Query<?> q, SqlContext context, List<Condition> conditions, boolean removePKUpdate) {
		List<BindVariableDescription> params = new ArrayList<BindVariableDescription>();
		IQueryableEntity obj = q.getInstance();
		if (q.isAll())
			return new BindSql("", params);

		if (conditions.isEmpty()) {
			if (getProfile().has(Feature.SELECT_ROW_NUM) && obj.rowid() != null) {
				q.addCondition(new FBIField(EXP_ROWID, q), obj.rowid());
			} else {// 自动将主键作为条件
				DbUtils.fillConditionFromField(obj, q, removePKUpdate, false);
			}
		}
		
		
		
		StringBuilder sb = new StringBuilder();
		for (Condition c : conditions) {
			if (sb.length() > 0)
				sb.append(" and ");
			sb.append(c.toPrepareSqlClause(params, q.getMeta(), context, this, obj,profile));
		}

		if (sb.length() > 0 || ORMConfig.getInstance().isAllowEmptyQuery()) {
			return new BindSql(sb.toString(), params);
		} else {
			throw new NullPointerException("Illegal usage of Query object, must including any condition in query.");
		}
	}

	public PartitionSupport getPartitionSupport() {
		return parent.getPartitionSupport();
	}
}

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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.NoResultException;

import jef.database.dialect.DatabaseDialect;
import jef.database.innerpool.PartitionSupport;
import jef.database.jsqlparser.SelectToCountWrapper;
import jef.database.jsqlparser.parser.ParseException;
import jef.database.jsqlparser.statement.select.PlainSelect;
import jef.database.jsqlparser.statement.select.Union;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.SelectBody;
import jef.database.meta.FBIField;
import jef.database.meta.Feature;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.database.query.AbstractJoinImpl;
import jef.database.query.JoinElement;
import jef.database.query.Query;
import jef.database.query.SqlContext;
import jef.database.routing.PartitionResult;
import jef.database.wrapper.clause.BindSql;
import jef.database.wrapper.processor.BindVariableDescription;
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
	private static Expression EXP_ROWID;
	{
		try {
			EXP_ROWID = DbUtils.parseExpression("ROWID");
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	DefaultSqlProcessor(DatabaseDialect profile, DbClient parent) {
		this.profile = profile;
		this.parent = parent;
	}

	/**
	 * 转换到Where子句
	 */
	public BindSql toWhereClause(JoinElement obj, SqlContext context, boolean update, DatabaseDialect profile) {
		String sb = innerToWhereClause(obj, context, update, profile);
		if (sb.length() > 0) {
			return new BindSql(" where " + sb);
		} else {
			return new BindSql(sb);
		}
	}

	/**
	 * 转换到绑定类Where字句
	 */
	public BindSql toPrepareWhereSql(JoinElement obj, SqlContext context, boolean update, DatabaseDialect profile) {
		BindSql result = innerToPrepareWhereSql(obj, context, update, profile);
		if (result.getSql().length() > 0) {
			result.setSql(" where ".concat(result.getSql()));
		}
		return result;
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
			SelectToCountWrapper sw;
			if (select instanceof Union) {
				sw = new SelectToCountWrapper((Union) select);
			} else {
				sw = new SelectToCountWrapper((PlainSelect) select, getProfile());
			}
			return sw.toString();
		} catch (ParseException e) {
			throw new SQLException("Parser error:" + sql);
		}
	}




	/**
	 * 获取数据库属性简要表
	 * 
	 * @deprecated use getProfile(PartitionResult[])
	 */
	public DatabaseDialect getProfile() {
		return profile;
	}

	public DatabaseDialect getProfile(PartitionResult[] prs) {
		if (prs == null || prs.length == 0) {
			return profile;
		}
		return this.parent.getProfile(prs[0].getDatabase());
	}

	private String innerToWhereClause(JoinElement obj, SqlContext context, boolean removePKUpdate, DatabaseDialect profile) {
		if (obj instanceof AbstractJoinImpl) {
			AbstractJoinImpl join = (AbstractJoinImpl) obj;
			StringBuilder sb = new StringBuilder();
			for (Query<?> ele : join.elements()) {
				String condStr = generateWhereClause0(ele, context.getContextOf(ele), removePKUpdate, ele.getConditions(), profile);
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
				String condStr = generateWhereClause0(q, context.getContextOf(q), false, entry.getValue(), profile);
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
			return generateWhereClause0((Query<?>) obj, context, removePKUpdate, obj.getConditions(), profile);
		} else {
			throw new IllegalArgumentException("Unknown Query class:" + obj.getClass().getName());
		}
	}

	private String generateWhereClause0(Query<?> q, SqlContext context, boolean removePKUpdate, List<Condition> conds, DatabaseDialect profile) {
		if (q.isAll() && conds.isEmpty())
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
			sb.append(c.toSqlClause(meta, context, this, q.getInstance(), profile)); // 递归的，当do是属于Join中的一部分时，需要为其增加前缀
		}
		if (sb.length() == 0)
			throw new NoResultException("Illegal usage of query:" + q.getClass().getName() + " object, must including any condition in query. or did you forget to set the primary key for the entity?");
		return sb.toString();
	}

	private BindSql innerToPrepareWhereSql(JoinElement query, SqlContext context, boolean removePKUpdate, DatabaseDialect profile) {
		if (query instanceof AbstractJoinImpl) {
			List<BindVariableDescription> params = new ArrayList<BindVariableDescription>();
			AbstractJoinImpl join = (AbstractJoinImpl) query;
			StringBuilder sb = new StringBuilder();
			for (Query<?> ele : join.elements()) {
				BindSql result = generateWhereCondition(ele, context.getContextOf(ele), ele.getConditions(), false, profile);
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
				BindSql result = generateWhereCondition(q, context.getContextOf(q), entry.getValue(), false, profile);
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
			return generateWhereCondition((Query<?>) query, context, query.getConditions(), removePKUpdate, profile);
		} else {
			throw new IllegalArgumentException("Unknown Query class:" + query.getClass().getName());
		}
	}

	private BindSql generateWhereCondition(Query<?> q, SqlContext context, List<Condition> conditions, boolean removePKUpdate, DatabaseDialect profile) {
		List<BindVariableDescription> params = new ArrayList<BindVariableDescription>();
		IQueryableEntity obj = q.getInstance();
		//这里必须用双条件判断，因为Join当中的RefCondition是额外增加的条件，如果去除将造成RefCondition丢失。
		if (q.isAll() && conditions.isEmpty())
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
			sb.append(c.toPrepareSqlClause(params, q.getMeta(), context, this, obj, profile));
		}

		if (sb.length() > 0 || ORMConfig.getInstance().isAllowEmptyQuery()) {
			return new BindSql(sb.toString(), params);
		} else {
			throw new NoResultException("Illegal usage of Query object, must including any condition in query.");
		}
	}

	public PartitionSupport getPartitionSupport() {
		return parent.getPartitionSupport();
	}
}

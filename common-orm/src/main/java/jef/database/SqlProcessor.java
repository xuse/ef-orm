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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.NoResultException;

import jef.database.Session.UpdateContext;
import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.type.ColumnMapping;
import jef.database.innerpool.PartitionSupport;
import jef.database.jsqlparser.parser.ParseException;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.meta.FBIField;
import jef.database.meta.Feature;
import jef.database.meta.ITableMetadata;
import jef.database.query.AbstractJoinImpl;
import jef.database.query.JoinElement;
import jef.database.query.Query;
import jef.database.query.SqlContext;
import jef.database.routing.PartitionResult;
import jef.database.wrapper.clause.BindSql;
import jef.database.wrapper.clause.SqlBuilder;
import jef.tools.reflect.BeanWrapper;

/**
 * SQL语句生成器
 * 
 * @author jiyi
 *
 */
public abstract class SqlProcessor {
	private DatabaseDialect profile;
	private DbClient parent;

	public SqlProcessor(DatabaseDialect profile, DbClient parent) {
		this.profile = profile;
		this.parent = parent;
	}

	private static Expression EXP_ROWID;
	static {
		try {
			EXP_ROWID = DbUtils.parseExpression("ROWID");
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 转换为Where子句
	 * 
	 * @param joinElement
	 * @param context
	 *            SQL语句上下文
	 * @param update
	 * @param profile
	 * @return
	 */
	public abstract BindSql toWhereClause(JoinElement joinElement, SqlContext context, UpdateContext update, DatabaseDialect profile,boolean isBatch);

	/**
	 * 获取数据库Dialect
	 * 
	 * @return
	 * @deprecated
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

	public PartitionSupport getPartitionSupport() {
		return parent.getPartitionSupport();
	}

	// 获得容器需要的值
	@SuppressWarnings({ "unchecked", "rawtypes" })
	static Object collectValueToContainer(List<? extends IQueryableEntity> records, Class<?> containerType, String targetField) {
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

	private static boolean checkPKCondition(List<Condition> conditions, ITableMetadata meta) {
		List<ColumnMapping> pks = meta.getPKFields();
		if (conditions.size() != pks.size())
			return false;
		for (Condition c : conditions) {
			if (c.getOperator() != jef.database.Condition.Operator.EQUALS) {
				return false;
			}
			if (!c.getField().getClass().isEnum()) {
				return false;
			}
			if (!isFieldOfPK(c.getField(), pks)) {
				return false;
			}
		}
		return true;
	}

	private static boolean isFieldOfPK(Field field, List<ColumnMapping> pks) {
		for (ColumnMapping c : pks) {
			if (field == c.field()) {
				return true;
			}
		}
		return false;
	}

	static final class PrepareImpl extends SqlProcessor {
		public PrepareImpl(DatabaseDialect profile, DbClient parent) {
			super(profile, parent);
		}

		/**
		 * 转换到绑定类Where字句
		 */
		public BindSql toWhereClause(JoinElement obj, SqlContext context, UpdateContext update, DatabaseDialect profile, boolean batch) {
			SqlBuilder builder=new SqlBuilder();
			toWhere1(builder,obj, context, update, profile,batch);
			if (builder.isNotEmpty()) {
				builder.addBefore(" where ");
			}
			return builder.build();
		}

		private void toWhere1(SqlBuilder builder,JoinElement query, SqlContext context, UpdateContext update, DatabaseDialect profile, boolean batch) {
			if (query instanceof AbstractJoinImpl) {
				AbstractJoinImpl join = (AbstractJoinImpl) query;
				for (Query<?> ele : join.elements()) {
					builder.startSection(" and ");
					toWhereElement1(builder,ele, context.getContextOf(ele), ele.getConditions(), null, profile, batch);
					builder.endSection();
				}
				for (Map.Entry<Query<?>, List<Condition>> entry : join.getRefConditions().entrySet()) {
					Query<?> q = entry.getKey();
					builder.startSection(" and ");
					toWhereElement1(builder, q, context.getContextOf(q), entry.getValue(), null, profile, batch);
					builder.endSection();
				}
			} else if (query instanceof Query<?>) {
				Query<?> q=(Query<?>) query; 
				toWhereElement1(builder,q, context, query.getConditions(), update, profile,batch);
				if(update!=null && update.needVersionCondition()){
					update.appendVersionCondition(builder,context,this,((Query<?>) query).getInstance(),profile, batch);
				}
				if (builder.isNotEmpty() || ORMConfig.getInstance().isAllowEmptyQuery()) {
					return;
				} else {
					throw new NoResultException("Illegal usage of Query object, must including any condition in query:" + q.getInstance().getClass());
				}
			} else {
				throw new IllegalArgumentException("Unknown Query class:" + query.getClass().getName());
			}
		}

		/**
		 * 
		 * @param obj
		 * @param context
		 * @param removePKUpdate
		 * @param profile
		 * @param checkIsPK
		 *            需要监测该条件是否恰好等于主键条件，如果是则返回true，如果为null则说明不需要检查
		 * @return
		 */
		private void toWhereElement1(SqlBuilder builder,Query<?> q, SqlContext context, List<Condition> conditions, UpdateContext update, DatabaseDialect profile, boolean batch) {
			IQueryableEntity obj = q.getInstance();
			// 这里必须用双条件判断，因为Join当中的RefCondition是额外增加的条件，如果去除将造成RefCondition丢失。
			if (q.isAll() && conditions.isEmpty())
				return;

			if (conditions.isEmpty()) {
				if (getProfile().has(Feature.SELECT_ROW_NUM) && obj.rowid() != null) {
					q.addCondition(new FBIField(EXP_ROWID, q), obj.rowid());
				} else {// 自动将主键作为条件
					DbUtils.fillConditionFromField(obj, q, update, false);
				}
			}
			// 检查当前的查询条件是否为一个主键条件
			if (update != null && update.checkIsPKCondition()) {
				update.setIsPkQuery(checkPKCondition(conditions, q.getMeta()));
			}

			for (Condition c : conditions) {
				builder.startSection(" and ");
				c.toPrepareSqlClause(builder, q.getMeta(), context, this, obj, profile, batch);
				builder.endSection();
			}
		}
	}

//	static class NormalImpl extends SqlProcessor {
//		NormalImpl(DatabaseDialect profile, DbClient parent) {
//			super(profile, parent);
//		}
//
//		/**
//		 * 转换到Where子句
//		 */
//		public BindSql toWhereClause(JoinElement obj, SqlContext context, UpdateContext update, DatabaseDialect profile) {
//			String sb = toWhere0(obj, context, update, profile);
//			if (sb.length() > 0) {
//				return new BindSql(" where " + sb);
//			} else {
//				return new BindSql(sb);
//			}
//		}
//
//		private String toWhere0(JoinElement obj, SqlContext context, UpdateContext update, DatabaseDialect profile) {
//			if (obj instanceof AbstractJoinImpl) {
//				AbstractJoinImpl join = (AbstractJoinImpl) obj;
//				StringBuilder sb = new StringBuilder();
//				for (Query<?> ele : join.elements()) {
//					String condStr = toWhereElement(ele, context.getContextOf(ele), ele.getConditions(), null, profile);
//					if (StringUtils.isEmpty(condStr)) {
//						continue;
//					}
//					if (sb.length() > 0) {
//						sb.append(" and ");
//					}
//					sb.append(condStr);
//				}
//				for (Map.Entry<Query<?>, List<Condition>> entry : join.getRefConditions().entrySet()) {
//					Query<?> q = entry.getKey();
//					String condStr = toWhereElement(q, context.getContextOf(q), entry.getValue(), null, profile);
//					if (StringUtils.isEmpty(condStr)) {
//						continue;
//					}
//					if (sb.length() > 0) {
//						sb.append(" and ");
//					}
//					sb.append(condStr);
//				}
//				return sb.toString();
//			} else if (obj instanceof Query<?>) {
//				return toWhereElement((Query<?>) obj, context, obj.getConditions(), update, profile);
//			} else {
//				throw new IllegalArgumentException("Unknown Query class:" + obj.getClass().getName());
//			}
//		}
//
//		private String toWhereElement(Query<?> q, SqlContext context, List<Condition> conditions, UpdateContext update, DatabaseDialect profile) {
//			if (q.isAll() && conditions.isEmpty())
//				return "";
//			if (conditions.isEmpty()) {
//				IQueryableEntity instance = q.getInstance();
//				if (profile.has(Feature.SELECT_ROW_NUM) && instance.rowid() != null) {
//					q.addCondition(new FBIField(EXP_ROWID, q), instance.rowid());
//				} else {// 自动将主键作为条件
//					DbUtils.fillConditionFromField(q.getInstance(), q, update, false);
//				}
//			}
//			// 检查当前的查询条件是否为一个主键条件
//			if (update != null && update.checkIsPKCondition()) {
//				update.setIsPkQuery(checkPKCondition(conditions, q.getMeta()));
//			}
//
//			ITableMetadata meta = MetaHolder.getMeta(q.getInstance());
//			StringBuilder sb = new StringBuilder();
//			for (Condition c : conditions) {
//				if (sb.length() > 0)
//					sb.append(" and ");
//				sb.append(c.toSqlClause(meta, context, this, q.getInstance(), profile)); // 递归的，当do是属于Join中的一部分时，需要为其增加前缀
//			}
//			if (sb.length() > 0 || ORMConfig.getInstance().isAllowEmptyQuery()) {
//				return sb.toString();
//			} else {
//				throw new NoResultException("Illegal usage of query:" + q.getClass().getName()
//						+ " object, must including any condition in query. or did you forget to set the primary key for the entity?");
//			}
//		}
//	}
}
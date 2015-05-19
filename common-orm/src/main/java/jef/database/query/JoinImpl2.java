package jef.database.query;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

import jef.database.DbUtils;
import jef.database.ORMConfig;
import jef.database.QueryAlias;
import jef.database.SelectProcessor;
import jef.database.SqlProcessor;
import jef.database.annotation.JoinType;
import jef.database.dialect.DatabaseDialect;
import jef.database.meta.AbstractRefField;
import jef.database.meta.IReferenceAllTable;
import jef.database.meta.IReferenceColumn;
import jef.database.meta.ISelectProvider;
import jef.database.meta.ITableMetadata;
import jef.database.meta.JoinKey;
import jef.database.meta.JoinPath;
import jef.database.meta.Reference;
import jef.database.wrapper.clause.BindSql;
import jef.database.wrapper.clause.GroupClause;
import jef.database.wrapper.clause.QueryClause;
import jef.database.wrapper.clause.QueryClauseImpl;
import jef.tools.Assert;
import jef.tools.StringUtils;

/**
 * Join Query实现
 * 
 * @author jiyi
 * 
 */
final class JoinImpl2 extends AbstractJoinImpl {

	private final List<JoinCondition> conditions = new ArrayList<JoinCondition>();

	private final List<Query<?>> qlist = new AbstractList<Query<?>>() {
		public Query<?> get(int index) {
			return context.queries.get(index).getQuery();
		}

		public int size() {
			return context.queries.size();
		}
	};

	JoinImpl2(Query<?> left, Query<?> right, JoinCondition joinCondition, Reference ref) {
		List<QueryAlias> qs = new ArrayList<QueryAlias>(6);
		qs.add(new QueryAlias(null, left));

		QueryAlias rightTableDef = new QueryAlias(null, right);
		rightTableDef.setStaticRef(ref);
		qs.add(rightTableDef);

		SqlContext context = new SqlContext(-1, qs, null);
		this.context = context;
		this.conditions.add(joinCondition);

		if (ref != null) {
			List<AbstractRefField> referenceOfRight = ref.getAllRefFields();
			for (ISelectProvider p : referenceOfRight) {
				if (p.isSingleColumn()) {
					rightTableDef.addField((IReferenceColumn) p);
				} else {
					rightTableDef.addField((IReferenceAllTable) p);
				}
			}
		}
	}

	private void add(Query<?> right, JoinCondition joinCondition, Reference ref, QueryAlias refFromLeft) {
		QueryAlias rightTableDef = new QueryAlias(null, right);
		rightTableDef.setStaticRef(ref);
		context.queries.add(rightTableDef);
		this.conditions.add(joinCondition);
		if (ref != null) {
			setReferenceOfRight(rightTableDef, refFromLeft.getReferenceObj(), ref.getAllRefFields());
		}
	}

	/**
	 * 间接引用的关系描述
	 * 
	 * @param reference
	 * @param p
	 */
	private void setReferenceOfRight(QueryAlias rightTableDef, IReferenceAllTable reference, List<AbstractRefField> p) {
		if (p == null || p.isEmpty())
			return;
		String lastName = reference == null ? null : reference.getName();
		if (StringUtils.isEmpty(lastName)) {
			for (ISelectProvider select : p) {
				if (select.isSingleColumn()) {
					rightTableDef.addField((IReferenceColumn) select);
				} else {
					rightTableDef.addField((IReferenceAllTable) select);
				}
			}
		} else {
			// if(p==null || p.size()!=1){
			// return;
			// }
			for (ISelectProvider select : p) {
				AbstractRefField ref = (AbstractRefField) select;
				ISelectProvider result = ref.toNestedDesc(lastName);
				if (result.isSingleColumn()) {
					rightTableDef.addField((IReferenceColumn) result);
				} else {
					rightTableDef.addField((IReferenceAllTable) result);
				}
			}
		}
	}

	static AbstractJoinImpl create(JoinElement parent, Query<?> e, JoinPath pathHint, JoinType forceType, boolean reverse) {
		if (parent instanceof Query<?>) {
			Query<?> query = (Query<?>) parent;
			JoinPath path = pathHint == null ? null : pathHint.accept(query, e);
			Reference ref = null;
			if (reverse) {
				if (path == null) {// 反向查找
					ITableMetadata meta = e.getMeta();
					ref = DbUtils.findPath(meta, query.getMeta());
					if (ref != null) {
						path = ref.getHint().flip();// 颠倒一下
						ref = Reference.createRevse(path, ref);
					}
				}
			} else {
				if (path == null) {// 正向查找
					ITableMetadata meta = query.getMeta();
					ref = DbUtils.findPath(meta, e.getMeta());
					if (ref != null)
						path = ref.getHint();
				}
			}
			if (path != null) {
				JoinCondition condition = new JoinCondition(query, path);
				condition.setType(forceType);
				return new JoinImpl2(query, e, condition, ref);
			}
		} else if (parent instanceof JoinImpl2) {
			JoinImpl2 join = (JoinImpl2) parent;
			if (pathHint != null) {
				pathHint = pathHint.accept(parent, e);
				if (pathHint != null) {
					JoinCondition con = new JoinCondition(null, pathHint);
					con.setType(forceType);
					join.add(e, con, null, null);
					return join;
				}
			} else {
				return join.findMatchReferenceJoin(e, forceType, reverse);
			}
		} else {
			throw new IllegalArgumentException("parent join invalid:" + parent.getClass().getName());
		}
		return null;
	}

	static AbstractJoinImpl create(JoinElement parent, Reference e, Query<?> tQuery) {
		if (parent instanceof Query<?>) {
			Query<?> left = (Query<?>) parent;
			if (e.getThisType() != left.getMeta()) {
				throw new IllegalArgumentException(left.getMeta().getName() + " ::" + e.toString());
			}
			JoinPath path = e.toJoinPath();
			tQuery = checkAndCreateTarget(path, tQuery, e);
			return new JoinImpl2(left, tQuery, new JoinCondition(left, path), e);

		} else if (parent instanceof JoinImpl2) {
			JoinPath path = e.toJoinPath();
			tQuery = checkAndCreateTarget(path, tQuery, e);
			JoinImpl2 join = (JoinImpl2) parent;

			for (QueryAlias querya : join.allElements()) {
				Query<?> left = querya.getQuery();
				if (e.getThisType() == left.getMeta()) {// 只找第一个符合类型的表就可以了。
					JoinCondition joinCondition = new JoinCondition(left, path);
					join.add(tQuery, joinCondition, e, querya);
					return join;
				}
			}
		}
		throw new IllegalArgumentException("No path from " + parent + " to table " + e.getTargetType().getName());
	}

	private static Query<?> checkAndCreateTarget(JoinPath path, Query<?> right, Reference e) {
		Assert.notNull(path);
		if (right == null) {
			return ReadOnlyQuery.getEmptyQuery(e.getTargetType());// QB.create(hint.getTargetType().getThisType());//;//为什么不用空查询？
		} else {
			// "输入的连接条件对象和Hint的目标不是同一张表的";
			Assert.isTrue(e.getTargetType().getThisType().isAssignableFrom(right.getInstance().getClass()), "The right side is not match to the target type of join key.");
			return right;
		}
	}

	// 成功返回自身，不成功返回null
	private AbstractJoinImpl findMatchReferenceJoin(Query<?> e, JoinType forceType, boolean reverse) {
		List<QueryAlias> ds = allElements();
		Reference ref = null;
		JoinPath path = null;

		for (QueryAlias querya : ds) {
			Query<?> query = querya.getQuery();
			if (reverse) {
				ITableMetadata meta = e.getMeta();
				ref = DbUtils.findPath(meta, query.getMeta());
				if (ref != null) {
					path = ref.getHint().flip();// 颠倒一下
					ref = Reference.createRevse(path, ref);
				}
			} else {
				ITableMetadata meta = query.getMeta();
				ref = DbUtils.findPath(meta, e.getMeta());
				if (ref != null)
					path = ref.getHint();

			}
			// 最后在全局配置中查找
			if (path != null) {
				JoinCondition cond = new JoinCondition(query, path);
				cond.setType(forceType);
				add(e, cond, ref, querya);
				return this;
			}
		}
		return null;
	}

	/**
	 * 返回SQL的表名部分 select xxx from [.....] where xxx，包括 join ... on ...等部分
	 * 
	 * @return
	 */
	public String toTableDefinitionSql(SqlProcessor processor, SqlContext context, DatabaseDialect profile) {
		StringBuilder sb = new StringBuilder(64);
		toTableDefSql(sb, context.queries.get(0), processor, context);
		// sb.append(' ');
		String wrap = ORMConfig.getInstance().wrap;

		for (int i = 0; i < conditions.size(); i++) {
			JoinCondition relations = conditions.get(i);
			sb.append(wrap);
			sb.append(' ').append(relations.getType().nameLower()).append(" join ");
			QueryAlias right = context.queries.get(i + 1);
			toTableDefSql(sb, right, processor, context);
			sb.append(" ON ");
			relations.toOnExpression(sb, context, right, processor, profile);
		}
		return sb.toString();
	}

	private void toTableDefSql(StringBuilder sb, QueryAlias obj, SqlProcessor processor, SqlContext context) {
		String alias = obj.getAlias();
		Assert.notNull(alias);
		Query<?> q = obj.getQuery();
		String table = DbUtils.toTableName(q.getInstance(), null, q, processor.getPartitionSupport()).getAsOneTable();
		sb.append(DbUtils.escapeColumn(processor.getProfile(), table)).append(' ').append(alias);
	}

	private void checkInstance(Query<?> right2) {
		if (this.allElements().contains(right2)) {
			throw new IllegalArgumentException("The query instanceof " + right2 + " has been added into this join already");
		}
	}

	public Join leftJoin(Query<?> right, JoinKey... keys) {
		processAddJoin(JoinType.LEFT, right, keys);
		return this;
	}

	public Join rightJoin(Query<?> right, JoinKey... keys) {
		processAddJoin(JoinType.RIGHT, right, keys);
		return this;
	}

	public Join innerJoin(Query<?> right, JoinKey... keys) {
		processAddJoin(JoinType.INNER, right, keys);
		return this;
	}

	public Join fullJoin(Query<?> right, JoinKey... keys) {
		processAddJoin(JoinType.FULL, right, keys);
		return this;
	}

	private void processAddJoin(JoinType type, Query<?> right, JoinKey[] keys) {
		checkInstance(right);
		JoinPath path = null;
		if (keys.length > 0) {
			path = new JoinPath(type, keys);
		}
		path = path.accept(this, right);

		if (path != null) {
			JoinCondition con = new JoinCondition(null, path);
			add(right, con, null, null);
		} else {
			throw new IllegalArgumentException("Can not create join, the join key not match the input table");
		}
	}

	public List<Query<?>> elements() {
		return qlist;
	}

	public List<QueryAlias> allElements() {
		return context.queries;
	}

	private int prepareSize = 0;

	public SqlContext prepare() {
		if (prepareSize != context.queries.size()) {
			int i = 0;
			for (QueryAlias q : context.queries) {
				q.setAlias("T" + (++i));
			}
			prepareSize = i;
		}

		return context;
	}

	public List<Reference> getIncludedCascadeOuterJoin() {
		ArrayList<Reference> result = new ArrayList<Reference>();
		for (QueryAlias qa : context.queries) {
			Reference ref = qa.getStaticRef();
			if (ref != null)
				result.add(ref);
		}
		return result;
	}

	@Override
	public QueryClause toQuerySql(SelectProcessor processor, SqlContext context, boolean order) {
		@SuppressWarnings("deprecation")
		DatabaseDialect profile = processor.getProfile();
		QueryClauseImpl clause = new QueryClauseImpl(profile);
		GroupClause groupClause = SelectProcessor.toGroupAndHavingClause(this, context, profile);
		clause.setGrouphavingPart(groupClause);
		clause.setSelectPart(SelectProcessor.toSelectSql(context, groupClause, profile));
		clause.setTableDefinition(toTableDefinitionSql(processor.parent, context, profile));
		clause.setWherePart(processor.parent.toWhereClause(this, context, false, profile).getSql());
		if (order)
			clause.setOrderbyPart(SelectProcessor.toOrderClause(this, context, profile));
		return clause;
	}

	@Override
	public QueryClause toPrepareQuerySql(SelectProcessor processor, SqlContext context, boolean order) {
		@SuppressWarnings("deprecation")
		DatabaseDialect profile = processor.getProfile();
		GroupClause groupClause = SelectProcessor.toGroupAndHavingClause(this, context, profile);
		QueryClauseImpl result = new QueryClauseImpl(profile);
		result.setSelectPart(SelectProcessor.toSelectSql(context, groupClause, profile));
		result.setTableDefinition(toTableDefinitionSql(processor.parent, context, profile));
		BindSql whereResult = processor.parent.toPrepareWhereSql(this, context, false, profile);
		result.setWherePart(whereResult.getSql());
		result.setBind(whereResult.getBind());
		result.setGrouphavingPart(groupClause);
		if (order)
			result.setOrderbyPart(SelectProcessor.toOrderClause(this, context, profile));
		return result;
	}

}

package jef.database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import jef.database.Condition.Operator;
import jef.database.dialect.DatabaseDialect;
import jef.database.meta.ITableMetadata;
import jef.database.query.Query;
import jef.database.query.SqlContext;
import jef.database.wrapper.clause.BindSql;
import jef.database.wrapper.clause.SqlBuilder;

/**
 * 用于代替Field来描述特定的复杂条件 正常情况下这些对象都容纳了若干的条件甚至查询实例。
 * 
 * 如果不考虑IConditionField的出现，那么RefField/FBIField就构成了基本Field的所有变种。
 * 
 * 而对于IConditionField的引入，为批操作的绑定变量增加了新的难度，虽然目前没有计划在批操作中支持IConditionField。
 * 但是从模型角度来解析，每个绑定变量描述应该对应一个条件树的节点，条件树指向的根节点的产生的一个路径是匹配条件树的根本。（条件树的解析） 即——
 * Level1：Join[组合查询] - DataObject[查询实例] (二叉树) Level2：DataObject[查询实例] -
 * 条件集合（无序集合）-条件 （基本条件/容器条件） Level3 容器条件 - 条件集合（无序集合）-条件 （基本条件/容器条件）形成一棵条件树。层次不限
 * 
 * @author Jiyi
 * @see Or
 * @see And
 * @see Not
 * @see Exists
 * @see NotExists
 */
public interface IConditionField extends jef.database.Field {
	/**
	 * 获得所有的条件
	 * 
	 * @return 条件集合
	 */
	List<Condition> getConditions();

	/**
	 * 生成非绑定变量下的SQL
	 * 
	 * @param meta
	 * @param processor
	 * @param context
	 * @param instance
	 * @return sql
	 * @deprecated To be deleted
	 */

	public String toSql(ITableMetadata meta, SqlProcessor processor, SqlContext context, Object instance,
			DatabaseDialect profile, boolean batch);

	/**
	 * 生成绑定变量的SQL，
	 * 
	 * @param fields    输出参数，会被添加一个
	 * @param meta
	 * @param processor
	 * @param context
	 * @param instance
	 * @return sql
	 */
	public void toPrepareSql(SqlBuilder builder, ITableMetadata meta, SqlProcessor processor, SqlContext context,
			Object instance, DatabaseDialect profile, boolean batch);

	static abstract class AbstractAndOr implements IConditionField {
		private static final long serialVersionUID = 1L;

		abstract String getName();

		final List<Condition> conditions = new ArrayList<Condition>();

		@Override
		public int hashCode() {
			HashCodeBuilder hash = new HashCodeBuilder();
			hash.append(getName());
			int h = 0;
			for (Condition c : conditions) {
				h += c.hashCode();
			}
			hash.append(h);
			return hash.toHashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof AbstractAndOr)) {
				return false;
			}
			AbstractAndOr rhs = (AbstractAndOr) obj;
			EqualsBuilder eb = new EqualsBuilder();
			eb.append(getName(), rhs.getName());
			eb.append(this.conditions, rhs.conditions);
			return eb.isEquals();
		}

		public String name() {
			return new StringBuilder(getName()).append(conditions.toString()).toString();
		}

		public void addCondition(IConditionField field) {
			conditions.add(Condition.get(field, Operator.EQUALS, null));
		}

		public void addCondition(Condition condition) {
			conditions.add(condition);
		}

		public void addCondition(Field field, Operator oper, Object value) {
			conditions.add(Condition.get(field, oper, value));
		}

		public void addCondition(Field field, Object value) {
			conditions.add(Condition.get(field, Operator.EQUALS, value));
		}

		public String toSql(ITableMetadata meta, SqlProcessor processor, SqlContext context, Object instance,
				DatabaseDialect profile, boolean batch) {
			StringBuilder sb = new StringBuilder();
			for (Condition c : conditions) {
				if (sb.length() > 0)
					sb.append(getName());
				sb.append(c.toSqlClause(meta, context, processor, instance, profile, batch));
			}
			return conditions.size() > 1 ? "(" + sb.toString() + ")" : sb.toString();
		}

		public void toPrepareSql(SqlBuilder builder, ITableMetadata meta, SqlProcessor processor, SqlContext context,
				Object instance, DatabaseDialect profile, boolean batch) {
			Iterator<Condition> cond = conditions.iterator();
			if (conditions.size() > 1) {
				builder.append("(");
			}
			if (cond.hasNext()) {
				cond.next().toPrepareSqlClause(builder, meta, context, processor, instance, profile, batch);
			}
			for (; cond.hasNext();) {
				builder.append(getName());
				cond.next().toPrepareSqlClause(builder, meta, context, processor, instance, profile, batch);
			}
			if (conditions.size() > 1) {
				builder.append(")");
			}
		}

		public List<Condition> getConditions() {
			return conditions;
		}
	}

	/**
	 * Or条件容器
	 * 
	 * @author Administrator
	 * 
	 */
	public static class Or extends AbstractAndOr {
		private static final long serialVersionUID = 1L;

		public Or(Condition... conditionsArg) {
			for (Condition c : conditionsArg) {
				conditions.add(c);
			}
		}

		String getName() {
			return " or ";
		}
	}

	/**
	 * Not条件容器
	 * 
	 * @author Administrator
	 * 
	 */
	public static class Not implements IConditionField {
		private static final long serialVersionUID = 3453370626698025387L;
		Condition condition;

		public Not() {
		}

		public Not(Condition condition) {
			this.condition = condition;
		}

		public Not(IConditionField condition) {
			this.condition = Condition.get(condition, Operator.EQUALS, null);
		}

		public String name() {
			return new StringBuilder().append("not ").append(condition.toString()).toString();
		}

		public String toSql(ITableMetadata meta, SqlProcessor processor, SqlContext context, Object instance,
				DatabaseDialect profile, boolean batch) {
			String sql = "not ".concat(condition.toSqlClause(meta, context, processor, instance, profile, batch));
			return sql;
		}

		public void toPrepareSql(SqlBuilder builder, ITableMetadata meta, SqlProcessor processor, SqlContext context,
				Object instance, DatabaseDialect profile, boolean batch) {
			builder.append("not ");
			condition.toPrepareSqlClause(builder, meta, context, processor, instance, profile, batch);
		}

		public List<Condition> getConditions() {
			return Arrays.asList(condition);
		}

		@Override
		public int hashCode() {
			HashCodeBuilder hash = new HashCodeBuilder();
			hash.append(20000);
			hash.append(condition);
			return hash.toHashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Not)) {
				return false;
			}
			Not rhs = (Not) obj;
			EqualsBuilder eb = new EqualsBuilder();
			eb.append(this.condition, rhs.condition);
			return eb.isEquals();
		}

		public void set(Condition cond) {
			this.condition = cond;
		}

		public Condition get() {
			return this.condition;
		}
	}

	/**
	 * And条件容器
	 * 
	 * @author Administrator
	 * 
	 */
	public static class And extends AbstractAndOr {
		private static final long serialVersionUID = -4023661686645164036L;

		public And(Condition... conditions1) {
			for (Condition c : conditions1) {
				conditions.add(c);
			}
		}

		String getName() {
			return " and ";
		}
	}

	/**
	 * NotExists条件容器
	 * 
	 * @author Administrator
	 * 
	 */
	public static class NotExists implements IConditionField {
		private static final long serialVersionUID = -4000282148613580766L;
		Query<?> query;

		public String name() {
			return "not exists";
		}

		public NotExists(Query<?> subQuery) {
			this.query = subQuery;
		}

		public String toSql(ITableMetadata meta, SqlProcessor processor, SqlContext context, Object instance,
				DatabaseDialect profile, boolean batch) {
			StringBuilder sb = new StringBuilder();
			sb.append(name()).append("(");
			sb.append("select 1 from ").append(DbUtils.toTableName(query, null, processor.getPartitionSupport()));
			sb.append(" et ");

			sb.append(processor.toWhereClause(query, new SqlContext(context, "et", query), null, profile, batch));
			sb.append(")");
			return sb.toString();
		}

		public void toPrepareSql(SqlBuilder sb, ITableMetadata meta, SqlProcessor processor, SqlContext context,
				Object instance, DatabaseDialect profile, boolean batch) {
			sb.append(name(), "(");
			String table = DbUtils.toTableName(query, null, processor.getPartitionSupport()).toString();
			sb.append("select 1 from ", table);
			sb.append(" et ");
			BindSql bind = processor.toWhereClause(query, new SqlContext(context, "et", query), null, profile, batch);
			sb.append(bind.getSql());
			sb.append(")");
			sb.addAllBind(bind.getBind());
		}

		public List<Condition> getConditions() {
			return Arrays.asList();
		}

		@Override
		public int hashCode() {
			HashCodeBuilder hash = new HashCodeBuilder();
			hash.append(40000);
			hash.append(query);
			return hash.toHashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof NotExists)) {
				return false;
			}
			NotExists rhs = (NotExists) obj;
			EqualsBuilder eb = new EqualsBuilder();
			eb.append(this.query, rhs.query);
			return eb.isEquals();
		}
	}

	/**
	 * Exists条件容器
	 * 
	 * @author Administrator
	 * 
	 */
	public static class Exists implements IConditionField {
		private static final long serialVersionUID = 6146531660079302188L;
		Query<?> query;

		public String name() {
			return "exists";
		}

		public Exists(Query<?> subQuery) {
			this.query = subQuery;
		}

		public String toSql(ITableMetadata meta, SqlProcessor processor, SqlContext context, Object instance,
				DatabaseDialect profile, boolean batch) {
			StringBuilder sb = new StringBuilder();
			sb.append(name()).append("(");
			sb.append("select 1 from ").append(DbUtils.toTableName(query, null, processor.getPartitionSupport()));
			sb.append(" et ");

			sb.append(processor.toWhereClause(query, new SqlContext(context, "et", query), null, profile, batch));
			sb.append(")");
			return sb.toString();
		}

		public void toPrepareSql(SqlBuilder sb, ITableMetadata meta, SqlProcessor processor, SqlContext context,
				Object instance, DatabaseDialect profile, boolean batch) {
			sb.append(name(), "(");
			String table = DbUtils.toTableName(query, null, processor.getPartitionSupport()).toString();
			sb.append("select 1 from ", table);
			sb.append(" et ");
			BindSql bind = processor.toWhereClause(query, new SqlContext(context, "et", query), null, profile, batch);
			sb.append(bind.getSql());
			sb.append(")");
			sb.addAllBind(bind.getBind());
		}

		public List<Condition> getConditions() {
			return Arrays.asList();
		}

		@Override
		public int hashCode() {
			HashCodeBuilder hash = new HashCodeBuilder();
			hash.append(50000);
			hash.append(query);
			return hash.toHashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Exists)) {
				return false;
			}
			Exists rhs = (Exists) obj;
			EqualsBuilder eb = new EqualsBuilder();
			eb.append(this.query, rhs.query);
			return eb.isEquals();
		}
	}
}

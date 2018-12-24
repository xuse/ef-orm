package jef.database.query;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.Transient;

import jef.database.DbUtils;
import jef.database.Field;
import jef.database.SelectProcessor;
import jef.database.dialect.DatabaseDialect;
import jef.database.meta.ITableMetadata;
import jef.database.routing.PartitionResult;
import jef.database.wrapper.clause.BindSql;
import jef.database.wrapper.clause.GroupClause;
import jef.database.wrapper.clause.QueryClause;
import jef.database.wrapper.clause.QueryClauseImpl;

@SuppressWarnings("serial")
public abstract class AbstractQuery<T> implements Query<T>, Serializable {

	static final Query<?>[] EMPTY_Q = new Query[0];

	/**
	 * 实例
	 */
	@Transient
	transient T instance;
	/**
	 * 类型
	 */
	@Transient
	transient ITableMetadata type;

	private int maxResult;
	private int fetchSize;
	private int queryTimeout;
	protected boolean cacheable = true;

	private transient Map<Field, Object> updateValueMap;

	public void setMaxResult(int size) {
		this.maxResult = size;
	}

	public void setFetchSize(int fetchszie) {
		this.fetchSize = fetchszie;
	}

	public void setQueryTimeout(int timeout) {
		this.queryTimeout = timeout;
	}

	public int getMaxResult() {
		return maxResult;
	}

	public int getFetchSize() {
		return fetchSize;
	}

	public int getQueryTimeout() {
		return queryTimeout;
	}

	public Query<T> orderByAsc(Field... ascFields) {
		addOrderBy(true, ascFields);
		return this;
	}

	public Query<T> orderByDesc(Field... descFields) {
		addOrderBy(false, descFields);
		return this;
	}

	public ITableMetadata getMeta() {
		return type;
	}

	public T getInstance() {
		return instance;
	}

	@SuppressWarnings("unchecked")
	public Class<T> getType() {
		return (Class<T>) type.getThisType();
	}

	@Override
	public QueryClause toQuerySql(SelectProcessor processor, SqlContext context, boolean order) {
		String tableName = (String) getAttribute(JoinElement.CUSTOM_TABLE_NAME);
		if (tableName != null)
			tableName = DbUtils.toSchemaAdjustedName(tableName);
		PartitionResult[] prs = DbUtils.toTableNames(this, tableName, processor.getPartitionSupport());
		DatabaseDialect profile = processor.getProfile(prs);

		GroupClause groupClause = SelectProcessor.toGroupAndHavingClause(this, context, profile);
		BindSql whereResult = processor.parent.toWhereClause(this, context, null, profile, false);

		QueryClauseImpl result = new QueryClauseImpl(profile);
		result.setGrouphavingPart(groupClause);

		result.setSelectPart(SelectProcessor.toSelectSql(context, groupClause, profile));
		result.setGrouphavingPart(groupClause);
		result.setTables(type.getTableName(false), prs);
		result.setWherePart(whereResult.getSql());
		result.setBind(whereResult.getBind());
		if (order)
			result.setOrderbyPart(SelectProcessor.toOrderClause(this, context, profile));
		return result;
	}

	@Override
	public void setCacheable(boolean cacheable) {
		this.cacheable = cacheable;
	}

	public boolean isCacheable() {
		return cacheable;
	}

	@Override
	public Map<Field, Object> getUpdateValueMap() {
		if (updateValueMap == null)
			return Collections.emptyMap();
		return updateValueMap;
	}

	public final void prepareUpdate(Field field, Object newValue) {
		if (updateValueMap == null)
			updateValueMap = new TreeMap<Field, Object>(cmp);
		updateValueMap.put(field, newValue);
	}

	@Override
	public void clearUpdateMap() {
		updateValueMap = null;
	}

	public boolean needUpdate() {
		return (updateValueMap != null) && this.updateValueMap.size() > 0;
	}

	/*
	 * 用于与条件做排序 为什么要对条件做排序？是为了避免让条件因为HashCode变化或者加入先后顺序变化而排序。最终引起SQL硬解析。 比如
	 * where name = ? and index = ? 和 where index = ? and name = ?
	 * 本质上是一个SQL条件，但是因为顺序不同变成了两个SQL语句。
	 */
	private static final ConditionComparator cmp = new ConditionComparator();

	private static class ConditionComparator implements Comparator<Field>, Serializable {
		public int compare(Field o1, Field o2) {
			if (o1 == o2)
				return 0;
			if (o1 == null)
				return 1;
			if (o2 == null)
				return -1;
			return o1.name().compareTo(o2.name());
		}
	}

	public Terms terms() {
		throw new UnsupportedOperationException();
	}
}

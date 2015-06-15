package jef.database.query;

import java.io.Serializable;

import javax.persistence.Transient;

import jef.database.DbUtils;
import jef.database.Field;
import jef.database.IQueryableEntity;
import jef.database.SelectProcessor;
import jef.database.dialect.DatabaseDialect;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.database.routing.PartitionResult;
import jef.database.wrapper.clause.BindSql;
import jef.database.wrapper.clause.GroupClause;
import jef.database.wrapper.clause.QueryClause;
import jef.database.wrapper.clause.QueryClauseImpl;

@SuppressWarnings("serial")
public abstract class AbstractQuery<T extends IQueryableEntity> implements Query<T>, Serializable {

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

	public void setAutoOuterJoin(boolean cascadeOuterJoin) {
		setCascadeViaOuterJoin(cascadeOuterJoin);
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
			tableName = MetaHolder.toSchemaAdjustedName(tableName);
		PartitionResult[] prs = DbUtils.toTableNames(getInstance(), tableName, this, processor.getPartitionSupport());
		DatabaseDialect profile = processor.getProfile(prs);

		QueryClauseImpl clause = new QueryClauseImpl(profile);

		GroupClause groupClause = SelectProcessor.toGroupAndHavingClause(this, context, profile);
		clause.setGrouphavingPart(groupClause);

		clause.setSelectPart(SelectProcessor.toSelectSql(context, groupClause, profile));
		clause.setTables(type.getTableName(false),prs);
		clause.setWherePart(processor.parent.toWhereClause(this, context, false, profile).getSql());
		if (order)
			clause.setOrderbyPart(SelectProcessor.toOrderClause(this, context, profile));
		return clause;
	}

	@Override
	public QueryClause toPrepareQuerySql(SelectProcessor processor, SqlContext context, boolean order) {
		String tableName = (String) getAttribute(JoinElement.CUSTOM_TABLE_NAME);
		if (tableName != null)
			tableName = MetaHolder.toSchemaAdjustedName(tableName);
		PartitionResult[] prs = DbUtils.toTableNames(getInstance(), tableName, this, processor.getPartitionSupport());

		DatabaseDialect profile = processor.getProfile(prs);

		GroupClause groupClause = SelectProcessor.toGroupAndHavingClause(this, context, profile);
		BindSql whereResult = processor.parent.toPrepareWhereSql(this, context, false, profile);

		QueryClauseImpl result = new QueryClauseImpl(profile);
		result.setSelectPart(SelectProcessor.toSelectSql(context, groupClause, profile));
		result.setGrouphavingPart(groupClause);
		result.setTables(type.getTableName(false),prs);
		result.setWherePart(whereResult.getSql());
		result.setBind(whereResult.getBind());
		if (order)
			result.setOrderbyPart(SelectProcessor.toOrderClause(this, context, profile));
		return result;
	}

	@Override
	public Terms terms() {
		throw new UnsupportedOperationException();
	}
}

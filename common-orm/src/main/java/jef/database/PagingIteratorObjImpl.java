package jef.database;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jef.database.meta.AbstractRefField;
import jef.database.meta.Reference;
import jef.database.query.ConditionQuery;
import jef.database.query.JoinElement;
import jef.database.query.Query;
import jef.database.wrapper.clause.CountClause;
import jef.tools.ArrayUtils;
import jef.tools.Assert;
import jef.tools.PageInfo;
import jef.tools.PageLimit;

final public class PagingIteratorObjImpl<T> extends PagingIterator<T> {
	private ConditionQuery queryObj; // 1 使用API查询的情况
	private Session db;

	/**
	 * 是否外连接获取
	 * 
	 * @return
	 */
	public boolean isRefQuery() {
		return transformer.isLoadVsOne();
	}

	/**
	 * 设置是否用外连接获取级联字段
	 * 
	 * @param refQuery
	 */
	public void setRefQuery(boolean refQuery) {
		transformer.setLoadVsOne(refQuery);
	}

	/**
	 * 返回结果是否填充 OneToMany,ManyToMany的属性（需要多次查询）
	 * 
	 * @return
	 */
	public boolean isFillVsNField() {
		return transformer.isLoadVsMany();
	}

	/**
	 * 设置返回结果是否填充 OneToMany,ManyToMany的属性（需要多次查询）
	 * 
	 * @param fillVsNField
	 */
	public void setFillVsNField(boolean fillVsNField) {
		transformer.setLoadVsMany(fillVsNField);
	}

	/*
	 * 从查询对象构造
	 */
	PagingIteratorObjImpl(IQueryableEntity query, int pageSize, Session db) {
		this(query.getQuery(), pageSize, db);
	}

	/*
	 * 从Query对象构造
	 */
	PagingIteratorObjImpl(ConditionQuery query, int pageSize, Session db) {
		Assert.notNull(query);
		this.queryObj = query;
		this.transformer = query.getResultTransformer();
		Assert.notNull(transformer.getResultClazz());
		this.db = db;
		page = new PageInfo();
		page.setRowsPerPage(pageSize);
	}

	private long getTotal(ConditionQuery j) throws SQLException {
		CountClause countResult = db.selectp.toCountSql(j);
		return db.selectp.processCount(db, countResult);
	}

	/*
	 * 处理由dataobject作为查询条件的查询处理
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected List<T> doQuery(boolean pageFlag) throws SQLException {
		calcPage();
		List<T> result;
		QueryOption option;
		if (queryObj instanceof JoinElement) {
			option = QueryOption.createFrom((JoinElement) queryObj);
		} else {
			option = QueryOption.DEFAULT;
		}
		PageLimit range = page.getCurrentRecordRange();
		if (range.getOffset() == 0 && range.getEnd() >= page.getTotal()) {
			range = null;
		}
		if (queryObj instanceof Query<?>) {
			Query q = (Query) queryObj;
			result = db.typedSelect(q, pageFlag ? range : null, option);
		} else {
			result = db.innerSelect(queryObj, pageFlag ? range : null, null, option);
		}
		if (result.isEmpty()) {
			recordEmpty();
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected long doCount() throws SQLException {
		if (queryObj instanceof Query<?>) {
			Query<?> query = (Query<?>) queryObj;
			Map<Reference, List<AbstractRefField>> map;
			if (isRefQuery() && query.isCascadeViaOuterJoin()) {
				map = DbUtils.getMergeAsOuterJoinRef(query);
			} else {
				map = Collections.EMPTY_MAP;
			}
			JoinElement j = (Query<?>) queryObj;
			if (query.getOtherQueryProvider().length > 0 || !map.isEmpty()) {// 检查补充外部链接
				// 拼装出带连接的查询请求
				j = DbUtils.getJoin(query, map, ArrayUtils.asList(query.getOtherQueryProvider()), null);
			}
			long total = getTotal(j);
			page.setTotal(total);
			return total;
		} else {
			long total = getTotal(queryObj);
			page.setTotal(total);
			return total;
		}
	}
}

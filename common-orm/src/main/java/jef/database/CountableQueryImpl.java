package jef.database;

import java.sql.SQLException;
import java.util.List;

import jef.database.query.ConditionQuery;
import jef.database.query.JoinElement;
import jef.database.query.Query;
import jef.database.wrapper.clause.CountClause;
import jef.tools.PageLimit;

public class CountableQueryImpl<X> implements CountableQuery<X> {
	private Session session;
	private ConditionQuery queryObj;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private List<X> getResultList0(PageLimit range) throws SQLException {
		// calcPage();
		// List<T> result;
		QueryOption option;
		if (queryObj instanceof JoinElement) {
			option = QueryOption.createFrom((JoinElement) queryObj);
		} else {
			option = QueryOption.DEFAULT;
		}
		if (queryObj instanceof Query<?>) {
			Query q = (Query) queryObj;
			return session.typedSelect(q, range, option);
		} else {
			return session.innerSelect(queryObj, range, null, option);
		}
	}

	@Override
	public List<X> getResultList(long start, int limit) {
		PageLimit range = new PageLimit(start, limit);
		try {
			return getResultList0(range);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	@Override
	public long getResultCount() {
		try {
			CountClause countResult = session.selectp.toCountSql(queryObj);
			return session.selectp.processCount(session, countResult);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	@Override
	public List<X> getResultList() {
		try {
			return getResultList0(null);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

}

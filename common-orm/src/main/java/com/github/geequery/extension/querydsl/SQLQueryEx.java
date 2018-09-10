package com.github.geequery.extension.querydsl;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import javax.inject.Provider;

import jef.database.DbUtils;
import jef.database.Session;

import com.querydsl.core.DefaultQueryMetadata;
import com.querydsl.core.QueryMetadata;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.sql.AbstractSQLQuery;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.QueryDSLVistor;
import com.querydsl.sql.SQLBindings;
import com.querydsl.sql.SQLCommonQuery;
import com.querydsl.sql.SQLTemplates;

/**
 * {@code SQLQuery} is a JDBC based implementation of the {@link SQLCommonQuery}
 * interface
 *
 * @param <T>
 * @author tiwe
 */
public class SQLQueryEx<T> extends AbstractSQLQuery<T, SQLQueryEx<T>> {
	private Session session;

	/**
	 * Create a detached SQLQuery instance The query can be attached via the
	 * clone method
	 *
	 * @param templates
	 *            SQLTemplates to use
	 */
	public SQLQueryEx(SQLTemplates templates) {
		super((Connection) null, new Configuration(templates), new DefaultQueryMetadata());
	}

	/**
	 * Create a new SQLQuery instance
	 *
	 * @param conn
	 *            Connection to use
	 * @param templates
	 *            SQLTemplates to use
	 */
	public SQLQueryEx(Connection conn, SQLTemplates templates) {
		super(conn, new Configuration(templates), new DefaultQueryMetadata());
	}

	/**
	 * Create a new SQLQuery instance
	 *
	 * @param conn
	 *            Connection to use
	 * @param templates
	 *            SQLTemplates to use
	 * @param metadata
	 *            metadata
	 */
	public SQLQueryEx(Connection conn, SQLTemplates templates, QueryMetadata metadata) {
		super(conn, new Configuration(templates), metadata);
	}

	/**
	 * Create a new SQLQuery instance
	 *
	 * @param configuration
	 *            configuration
	 */
	public SQLQueryEx(Configuration configuration, Session session) {
		this((Connection) null, configuration, session);
	}

	/**
	 * Create a new SQLQuery instance
	 *
	 * @param conn
	 *            Connection to use
	 * @param configuration
	 *            configuration
	 */
	public SQLQueryEx(Connection conn, Configuration configuration, Session session) {
		super(conn, configuration, new DefaultQueryMetadata());
		this.session = session;
	}

	/**
	 * Create a new SQLQuery instance
	 *
	 * @param conn
	 *            Connection to use
	 * @param configuration
	 *            configuration
	 * @param metadata
	 *            metadata
	 */
	public SQLQueryEx(Connection conn, Configuration configuration, QueryMetadata metadata) {
		super(conn, configuration, metadata);
	}

	/**
	 * Create a new SQLQuery instance
	 *
	 * @param connProvider
	 *            Connection to use
	 * @param configuration
	 *            configuration
	 */
	public SQLQueryEx(Provider<Connection> connProvider, Configuration configuration, Session session) {
		super(connProvider, configuration, new DefaultQueryMetadata());
		this.session = session;
	}

	/**
	 * Create a new SQLQuery instance
	 *
	 * @param connProvider
	 *            Connection to use
	 * @param configuration
	 *            configuration
	 * @param metadata
	 *            metadata
	 */
	public SQLQueryEx(Provider<Connection> connProvider, Configuration configuration, QueryMetadata metadata) {
		super(connProvider, configuration, metadata);
	}

	@Override
	public SQLQueryEx<T> clone(Connection conn) {
		SQLQueryEx<T> q = new SQLQueryEx<T>(conn, getConfiguration(), getMetadata().clone());
		q.clone(this);
		return q;
	}

	@Override
	public <U> SQLQueryEx<U> select(Expression<U> expr) {
		queryMixin.setProjection(expr);
		@SuppressWarnings("unchecked")
		// This is the new type
		SQLQueryEx<U> newType = (SQLQueryEx<U>) this;
		return newType;
	}

	@Override
	public SQLQueryEx<Tuple> select(Expression<?>... exprs) {
		queryMixin.setProjection(exprs);
		@SuppressWarnings("unchecked")
		// This is the new type
		SQLQueryEx<Tuple> newType = (SQLQueryEx<Tuple>) this;
		return newType;
	}

	public <X> List<X> fetchAs(Class<X> resultClass) {
		SQLBindings sqlBindings = getSQL();
		Object[] valuesArr = sqlBindings.getBindings().toArray();
		fixTypes(valuesArr);
		try {
			List<X> list = session.selectBySql(sqlBindings.getSQL(), resultClass, valuesArr);
			return list;
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	public long count() {
		SQLBindings sqlBindings = getSQL();
		String countSql = QueryDSLVistor.serialize(this, true).toString();
		Object[] values = sqlBindings.getBindings().toArray();
		fixTypes(values);
		try {
			Long rownum = session.loadBySql(countSql, Long.class, values);
			return rownum == null ? 0 : rownum.longValue();
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	private void fixTypes(Object[] values) {
		if (values != null) {
			for (int i = 0; i < values.length; i++) {
				Object val = values[i];
				if (val != null) {
					if (val.getClass() == java.util.Date.class) {
						val = new Timestamp(((java.util.Date) val).getTime());
					}
					values[i] = val;
				}
			}
		}
	}
}
package com.github.geequery.extension.querydsl;

import java.sql.Connection;
import java.sql.SQLException;

import javax.inject.Provider;
import javax.sql.DataSource;

import jef.database.Session;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.sql.AbstractSQLQueryFactory;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.RelationalPath;
import com.querydsl.sql.SQLCloseListener;
import com.querydsl.sql.SQLTemplates;

public class SQLQueryFactoryEx extends AbstractSQLQueryFactory<SQLQueryEx<?>> {
	static class DataSourceProvider implements Provider<Connection> {

		private final DataSource ds;

		public DataSourceProvider(DataSource ds) {
			this.ds = ds;
		}

		@Override
		public Connection get() {
			try {
				return ds.getConnection();
			} catch (SQLException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		}

	}

	public SQLQueryFactoryEx(SQLTemplates templates, Provider<Connection> connection) {
		this(new Configuration(templates), connection);
	}

	public SQLQueryFactoryEx(Configuration configuration, Provider<Connection> connProvider) {
		super(configuration, connProvider);
	}

	public SQLQueryFactoryEx(Configuration configuration, DataSource dataSource) {
		this(configuration, dataSource, true);
	}

	public SQLQueryFactoryEx(Configuration configuration, DataSource dataSource, boolean release) {
		super(configuration, new DataSourceProvider(dataSource));
		if (release) {
			configuration.addListener(SQLCloseListener.DEFAULT);
		}
	}
	private Session session;

	public SQLQueryFactoryEx(Session session, String datasourceName,Provider<Connection> provider) {
		super(new Configuration(session.getProfile(datasourceName).getQueryDslDialect()),provider);
		this.session=session;
	}

	@Override
	public SQLQueryEx<?> query() {
		return new SQLQueryEx<Void>(connection, configuration, session);
	}

	@Override
	public <T> SQLQueryEx<T> select(Expression<T> expr) {
		return query().select(expr);
	}

	@Override
	public SQLQueryEx<Tuple> select(Expression<?>... exprs) {
		return query().select(exprs);
	}

	@Override
	public <T> SQLQueryEx<T> selectDistinct(Expression<T> expr) {
		return query().select(expr).distinct();
	}

	@Override
	public SQLQueryEx<Tuple> selectDistinct(Expression<?>... exprs) {
		return query().select(exprs).distinct();
	}

	@Override
	public SQLQueryEx<Integer> selectZero() {
		return select(Expressions.ZERO);
	}

	@Override
	public SQLQueryEx<Integer> selectOne() {
		return select(Expressions.ONE);
	}

	@Override
	public <T> SQLQueryEx<T> selectFrom(RelationalPath<T> expr) {
		return select(expr).from(expr);
	}
}

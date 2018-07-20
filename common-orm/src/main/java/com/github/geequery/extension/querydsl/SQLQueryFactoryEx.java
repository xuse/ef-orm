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



/**
 * <H3>写在前面</H3>
 * 这是一个试验性功能，小友夏锋的想法是利用Query作为Criteria的生成方式，转换为SQL以后再利用到GeeQuery的selectBySql()上来，使用EF进行查询执行与对象拼装。
 * 我称之为“移花接木”法。
 * 这种方式仅利用了GeeQuery的JDBC操作和对象拼装功能。而QueryDSL在SQL语句的处理和对象拼装上其实是有自己的套路的，因此个人感觉不是太有必要。
 * 所以在提供原生的QueryDSL的QueryFactory的基础上，也将这种操作方式提供出来，后续看这种移花接木法是否有更充足的理由和必要。
 * <p>
 * <H3>功能说明</H3>
 * 和QueryDSL标准的SQLQueryFactory的区别是，生成的是SQLQueryEx对象。
 * 该对象额外提供两个方法
 * <ul> 
 * <li><code>fetchAs()</code>可以使用GeeQuery的逻辑将对象拼装成需要的格式。</li>
 * <li><code>count()</code>使用count()语句查询结果数量. 和fetchCount()其实没什么区别...</li>
 * </ul>
 * 
 * @author jiyi
 *
 */
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

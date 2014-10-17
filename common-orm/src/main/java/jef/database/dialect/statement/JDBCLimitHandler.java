package jef.database.dialect.statement;

import java.sql.ResultSet;

import jef.common.wrapper.IntRange;

/**
 * 使用JDBC对结果集进行限制的实现
 * @author jiyi
 *
 */
public class JDBCLimitHandler implements LimitHandler{

	@Override
	public String toPageSQL(String sql, IntRange offsetLimit) {
		return sql;
	}

	@Override
	public String toPageSQL(String sql, IntRange offsetLimit, boolean isUnion) {
		return sql;
	}

	@Override
	public ResultSet afterProcess(ResultSet rs, IntRange range) {
		int[] offsetLimit=range.toStartLimitSpan();
		return new LimitOffsetResultSet(rs,offsetLimit);
	}

}

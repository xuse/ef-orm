package jef.database.dialect.statement;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 返回查询结果会被颠倒的Statement
 * @author jiyi
 *
 */
public final class ReverseStatement extends DelegatingStatement{
	public ReverseStatement(Statement s) {
		super(s);
	}

	@Override
	public ResultSet executeQuery(String sql) throws SQLException {
		return new ReverseResultSet(_stmt.executeQuery(sql));
	}

	@Override
	public ResultSet getResultSet() throws SQLException {
		return new ReverseResultSet(_stmt.getResultSet());
	}
}

package jef.database.dialect.statement;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class ReversePreparedStatement extends DelegatingPreparedStatement{
	public ReversePreparedStatement(PreparedStatement s) {
		super(s);
	}

	@Override
	public ResultSet executeQuery() throws SQLException {
		return new ReverseResultSet(((PreparedStatement) _stmt).executeQuery());
	}

	@Override
	public ResultSet executeQuery(String sql) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ResultSet getResultSet() throws SQLException {
		return new ReverseResultSet(_stmt.getResultSet());
	}
}

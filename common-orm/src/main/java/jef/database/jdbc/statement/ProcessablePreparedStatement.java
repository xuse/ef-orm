package jef.database.jdbc.statement;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jef.database.jdbc.result.ProcessableResultSet;


/**
 * 返回查询结果会被颠倒的PreparedStatement
 * @author jiyi
 *
 */
public final class ProcessablePreparedStatement extends DelegatingPreparedStatement{
	private ResultSetLaterProcess rslp;
	
	public ProcessablePreparedStatement(PreparedStatement s,ResultSetLaterProcess rslp) {
		super(s);
		this.rslp=rslp;
	}

	@Override
	public ResultSet executeQuery() throws SQLException {
		return new ProcessableResultSet(((PreparedStatement) _stmt).executeQuery(),rslp);
	}

	@Override
	public ResultSet executeQuery(String sql) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ResultSet getResultSet() throws SQLException {
		return new ProcessableResultSet(_stmt.getResultSet(),rslp);
	}
}

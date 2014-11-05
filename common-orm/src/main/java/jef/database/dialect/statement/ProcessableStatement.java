package jef.database.dialect.statement;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 返回查询结果会被颠倒的Statement
 * @author jiyi
 *
 */
public final class ProcessableStatement extends DelegatingStatement{
	private ResultSetLaterProcess rslp;
	
	public ProcessableStatement(Statement s,ResultSetLaterProcess rslp) {
		super(s);
		this.rslp=rslp;
	}

	@Override
	public ResultSet executeQuery(String sql) throws SQLException {
		return new ProcessableResultSet(_stmt.executeQuery(sql),rslp);
	}

	@Override
	public ResultSet getResultSet() throws SQLException {
		return new ProcessableResultSet(_stmt.getResultSet(),rslp);
	}
}

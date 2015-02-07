package jef.database.jdbc.result;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import jef.database.DbUtils;

public class CloseableResultSet extends AbstractResultSet{
	private Statement st;
	private ResultSet rs;
	
	public CloseableResultSet(Statement st,ResultSet rs){
		this.st=st;
		this.rs=rs;
	}
	
	@Override
	public boolean next() throws SQLException {
		return rs.next();
	}

	@Override
	public void close() throws SQLException {
		rs.close();
		DbUtils.close(st);
	}

	@Override
	public ResultSetMetaData getMetaData() throws SQLException {
		return rs.getMetaData();
	}

	@Override
	public boolean isBeforeFirst() throws SQLException {
		return rs.next();
	}

	@Override
	public boolean isAfterLast() throws SQLException {
		return rs.isAfterLast();
	}

	@Override
	public boolean isFirst() throws SQLException {
		return rs.isFirst();
	}

	@Override
	public boolean isLast() throws SQLException {
		return rs.isLast();
	}

	@Override
	public void beforeFirst() throws SQLException {
		rs.beforeFirst();
	}

	@Override
	public void afterLast() throws SQLException {
		rs.afterLast();
	}

	@Override
	public boolean first() throws SQLException {
		return rs.first();
	}

	@Override
	public boolean last() throws SQLException {
		return rs.last();
	}

	@Override
	public boolean previous() throws SQLException {
		return rs.previous();
	}

	@Override
	public boolean isClosed() throws SQLException {
		return rs.isClosed();
	}

	@Override
	protected ResultSet get() throws SQLException {
		return rs;
	}
}

package jef.database.jdbc.result;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import jef.database.jdbc.statement.ResultSetLaterProcess;

/**
 * 倒序获取结果的ResultSet
 * @author jiyi
 *
 */
public final class ProcessableResultSet extends AbstractResultSet {
	private ResultSet rs;

	public ProcessableResultSet(ResultSet rs, ResultSetLaterProcess rslp) {
		if(rslp.getSkipResults()>0){
			rs=new LimitOffsetResultSet(rs, rslp.getSkipResults(),0);
		}
		this.rs=rs;
	}

	@Override
	public boolean next() throws SQLException {
		return rs.next();
	}

	@Override
	public void close() throws SQLException {
		rs.close();
	}

	@Override
	public ResultSetMetaData getMetaData() throws SQLException {
		return rs.getMetaData();
	}

	@Override
	public boolean isBeforeFirst() throws SQLException {
		return rs.isBeforeFirst();
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

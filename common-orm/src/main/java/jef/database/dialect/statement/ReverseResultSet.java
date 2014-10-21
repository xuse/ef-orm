package jef.database.dialect.statement;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import javax.persistence.PersistenceException;

import jef.database.wrapper.result.AbstractResultSet;

/**
 * 倒序获取结果的ResultSet
 * @author jiyi
 *
 */
public final class ReverseResultSet extends AbstractResultSet {
	private ResultSet rs;

	public ReverseResultSet(ResultSet rs) {
		this.rs=rs;
		try {
			rs.afterLast();
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
	}

	@Override
	public void afterLast() throws SQLException {
		rs.beforeFirst();
	}

	@Override
	public void beforeFirst() throws SQLException {
		rs.afterLast();
	}

	@Override
	public void close() throws SQLException {
		rs.close();
	}

	@Override
	public boolean first() throws SQLException {
		return rs.last();
	}

	@Override
	public ResultSetMetaData getMetaData() throws SQLException {
		return rs.getMetaData();
	}

	@Override
	public boolean isClosed() throws SQLException {
		return rs.isClosed();
	}

	@Override
	public boolean next() throws SQLException {
		return rs.previous();
	}

	@Override
	public boolean previous() throws SQLException {
		return rs.next();
	}

	@Override
	public boolean isFirst() throws SQLException {
		return rs.isLast();
	}

	@Override
	public boolean isLast() throws SQLException {
		return rs.isFirst();
	}

	@Override
	public boolean last() throws SQLException {
		return rs.first();
	}

	@Override
	protected ResultSet get() throws SQLException {
		return rs;
	}

}

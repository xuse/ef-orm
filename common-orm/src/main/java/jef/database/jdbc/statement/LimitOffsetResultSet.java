package jef.database.jdbc.statement;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import javax.persistence.PersistenceException;

import jef.database.wrapper.result.AbstractResultSet;

/**
 * 对结果集跳过若干记录，并且限定其最大数的过滤器
 * @author jiyi
 *
 */
final class LimitOffsetResultSet extends AbstractResultSet {
	private int offset;
	private int limit;
	private ResultSet rs;
	private int position; 

	public LimitOffsetResultSet(ResultSet rs, int[] offsetLimit) {
		this.offset = offsetLimit[0];
		this.limit = offsetLimit[1];
		this.rs = rs;
		try {
			skipOffset(rs,offset);
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
	}

	private void skipOffset(ResultSet rs,int offset) throws SQLException {
		for (int i = 0; i < offset; i++) {
			if(!rs.next()){
				break;
			}
		}
	}

	@SuppressWarnings("all")
	@Override
	public boolean next() throws SQLException {
		if(position>=limit){
			return false;
		}
		boolean next;
		if(next=rs.next()){
			position++;
		}
		return next;
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
	public void beforeFirst() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void afterLast() throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean first() throws SQLException {
		if(rs.first()){
			skipOffset(rs, offset);
			return true;
		}
		return false;
	}

	@Override
	public boolean previous() throws SQLException {
		if(rs.previous()){
			position--;
			return true;
		}
		return false;
	}

	@Override
	public boolean isClosed() throws SQLException {
		return rs.isClosed();
	}

	@Override
	protected ResultSet get() throws SQLException {
		return rs;
	}
	

	@Override
	public boolean isFirst() throws SQLException {
		throw new UnsupportedOperationException("isFirst");
	}

	@Override
	public boolean isLast() throws SQLException {
		throw new UnsupportedOperationException("isLast");
	}

	@Override
	public boolean last() throws SQLException {
		throw new UnsupportedOperationException("last");
	}
}

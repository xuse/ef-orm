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
public final class ProcessableResultSet extends AbstractResultSet {
	private ResultSet rs;
	private int offset;
	private int limit;
	
	//记录当前位置
	private int position; 

	public ProcessableResultSet(ResultSet rs, ResultSetLaterProcess rslp) {
		this.offset = rslp.getSkipResults();
		this.limit = 0;
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
//		LogUtil.debug("[{}] records skiped.", offset);
	}

	@SuppressWarnings("all")
	@Override
	public boolean next() throws SQLException {
		if(limit>0 && position>=limit){
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

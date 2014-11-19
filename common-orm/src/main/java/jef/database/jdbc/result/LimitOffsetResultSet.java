package jef.database.jdbc.result;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.persistence.PersistenceException;

import jef.database.Condition;
import jef.database.dialect.DatabaseDialect;
import jef.database.meta.Reference;
import jef.database.wrapper.populator.ColumnMeta;

/**
 * 对结果集跳过若干记录，并且限定其最大数的ResultSet实现
 * 
 * @author jiyi
 * 
 */
final class LimitOffsetResultSet extends AbstractResultSet implements IResultSet{
	private int offset;
	private int limit;
	private ResultSet rs;
	/**
	 * 当前游标所处位置，从0开始。0表示在第一条结果之前。 如果position=Integer.MAX_VALUE则表示在最后一条结果之后
	 */
	private int position;

	/**
	 * 构造一个可用于跳过条数，以及限制结果数量的ResultSet。
	 * 
	 * @param rs
	 * @param offset
	 * @param limit
	 */
	public LimitOffsetResultSet(ResultSet rs, int offset, int limit) {
		this.offset = offset;
		this.limit = limit == 0 ? Integer.MAX_VALUE : limit;
		this.rs = rs;
		try {
			skipOffset(rs, offset);
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
	}
	
	public LimitOffsetResultSet(IResultSet rs, int offset, int limit) {
		this.offset = offset;
		this.limit = limit == 0 ? Integer.MAX_VALUE : limit;
		this.meta=rs.getColumns();
		this.profile=rs.getProfile();
		this.filters=rs.getFilters();
		this.rs = rs;
		try {
			skipOffset(rs, offset);
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
	}

	private void skipOffset(ResultSet rs, int offset) throws SQLException {
		for (int i = 0; i < offset; i++) {
			if (!rs.next()) {
				break;
			}
		}
	}

	@SuppressWarnings("all")
	@Override
	public boolean next(){
		if (position >= limit) {
			position = Integer.MAX_VALUE;
			return false;
		}
		try{
			final boolean next;
			if (next = rs.next()) {
				position++;
			} else {
				position = Integer.MAX_VALUE;
			}
			return next;	
		}catch(SQLException e){
			throw new PersistenceException(e);
		}
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
		return position == 0;
	}

	@Override
	public boolean isAfterLast() throws SQLException {
		return position == Integer.MAX_VALUE;
	}

	@Override
	public void beforeFirst() throws SQLException {
		rs.beforeFirst();
		skipOffset(rs, offset);
		position = 0;
	}

	@Override
	public void afterLast() throws SQLException {
		while (next());
	}

	@Override
	public boolean first() throws SQLException {
		if (rs.first()) {
			skipOffset(rs, offset);
			position = 1;
			return true;
		}
		return false;
	}

	@Override
	public boolean last() throws SQLException {
		while (position<limit){
			if(rs.isLast())break;
			next();
		}
		return true;
	}
	
	@Override
	public boolean previous() throws SQLException {
		if (rs.previous()) {
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
		return position == 1;
	}

	@Override
	public boolean isLast() throws SQLException {
		return position == limit || rs.isLast();
	}

	private Map<Reference, List<Condition>> filters;
	private DatabaseDialect profile;
	private ColumnMeta meta ;
	@Override
	public Map<Reference, List<Condition>> getFilters() {
		return filters;
	}

	@Override
	public DatabaseDialect getProfile() {
		return profile;
	}

	@Override
	public ColumnMeta getColumns() {
		return meta;
	}
}

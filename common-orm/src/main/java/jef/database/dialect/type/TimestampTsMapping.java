package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

import jef.database.dialect.DatabaseDialect;
import jef.database.query.Func;
import jef.database.wrapper.IResultSet;

/**
 * TIMESTMP <-> java.sql.Timestamp
 * @author jiyi
 *
 */
public class TimestampTsMapping extends AbstractTimeMapping<java.sql.Timestamp>{
	public Object set(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		st.setTimestamp(index, (Timestamp)value);
		return value;
	}

	public int getSqlType() {
		return java.sql.Types.TIMESTAMP;
	}

	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		if(value instanceof java.util.Date){
			return profile.getSqlTimestampExpression((Date)value);
		}
		throw new IllegalArgumentException("The input param can not cast to Date.");
	}

	public Object getProperObject(IResultSet rs, int n) throws SQLException {
		return rs.getTimestamp(n);
	}

	@Override
	public String getFunctionString(DatabaseDialect profile) {
		return profile.getFunction(Func.current_timestamp);
	}
	
	@Override
	public Object getCurrentValue() {
		return new java.sql.Timestamp(System.currentTimeMillis());
	}
}

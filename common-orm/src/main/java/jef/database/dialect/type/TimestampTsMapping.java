package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.IResultSet;
import jef.database.query.Func;

/**
 * TIMESTMP <-> java.sql.Timestamp
 * @author jiyi
 *
 */
public class TimestampTsMapping extends AbstractTimeMapping{
	public Object jdbcSet(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
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

	public Object jdbcGet(IResultSet rs, int n) throws SQLException {
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

	@Override
	protected Class<?> getDefaultJavaType() {
		return java.sql.Timestamp.class;
	}
}

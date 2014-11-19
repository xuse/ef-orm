package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.IResultSet;
import jef.database.query.Func;

/**
 * TIMESTMP <-> java.util.Date
 * @author jiyi
 *
 */
public class TimestampDateMapping extends AbstractTimeMapping<Date>{
	public Object set(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		if(value==null){
			st.setNull(index, java.sql.Types.TIMESTAMP);
			return null;
		}else{
			Timestamp ts=session.toTimestampSqlParam((Date)value);
			st.setTimestamp(index, ts);
			return ts;
		}
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
		Timestamp ts=rs.getTimestamp(n);
		return ts;
	}

	@Override
	public String getFunctionString(DatabaseDialect profile) {
		return profile.getFunction(Func.current_timestamp);
	}

	@Override
	public Object getCurrentValue() {
		return new Date();
	}
}

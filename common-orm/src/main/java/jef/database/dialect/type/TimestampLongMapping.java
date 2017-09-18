package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.IResultSet;
import jef.database.query.Func;

/**
 * TIMESTMP <-> java.lang.Long (毫秒数)
 * @author jiyi
 *
 */
public class TimestampLongMapping extends AbstractTimeMapping{
	public Object jdbcSet(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		if(value==null){
			st.setNull(index, java.sql.Types.TIMESTAMP);
			return null;
		}else{
			Timestamp ts=new java.sql.Timestamp(((Number)value).longValue());
			st.setTimestamp(index, ts);
			return ts;
		}
	}


	
	public int getSqlType() {
		return java.sql.Types.TIMESTAMP;
	}

	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		if(value instanceof Long){
			value=new Date((Long) value);
		}
		return profile.getSqlTimestampExpression((Date)value);
	}

	public Object jdbcGet(IResultSet rs, int n) throws SQLException {
		Timestamp ts=rs.getTimestamp(n);
		if(ts==null)return null;
		return ts.getTime();
	}



	@Override
	public String getFunctionString(DatabaseDialect profile) {
		return profile.getFunction(Func.current_timestamp);
	}

	@Override
	public Object getCurrentValue() {
		return System.currentTimeMillis();
	}



	@Override
	protected Class<?> getDefaultJavaType() {
		return Long.class;
	}



	@Override
	public void jdbcUpdate(ResultSet rs, String columnIndex, Object value, DatabaseDialect dialect) throws SQLException {
		Timestamp ts=new java.sql.Timestamp(((Number)value).longValue());
		rs.updateTimestamp(columnIndex, ts);
	}
	
	
}

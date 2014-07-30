package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

import jef.database.dialect.DatabaseDialect;
import jef.database.query.Func;
import jef.database.wrapper.IResultSet;

/**
 * TIMESTMP <-> java.lang.Long (毫秒数)
 * @author jiyi
 *
 */
public class TimestampLongMapping extends AbstractTimeMapping<Long>{
	public Object set(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
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

	public Object getProperObject(IResultSet rs, int n) throws SQLException {
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
	
	
}

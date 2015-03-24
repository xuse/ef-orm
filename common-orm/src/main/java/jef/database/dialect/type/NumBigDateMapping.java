package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;

import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.IResultSet;

public class NumBigDateMapping extends AColumnMapping{

	public Object jdbcSet(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		if(value==null){
			st.setNull(index, java.sql.Types.BIGINT);
			return null;
		}else{
			long time=((Date)value).getTime();
			st.setLong(index, time);
			return time;
		}
	}

	public int getSqlType() {
		return java.sql.Types.BIGINT;
	}

	public Object jdbcGet(IResultSet rs, int n) throws SQLException {
		Object o=rs.getObject(n);
		if(o==null)return null;
		long l=((Number)o).longValue();
		return new Date(l);
	}

	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		Date date=(Date)value;
		return String.valueOf(date.getTime());
	}

	@Override
	protected Class<?> getDefaultJavaType() {
		return Date.class;
	}
}

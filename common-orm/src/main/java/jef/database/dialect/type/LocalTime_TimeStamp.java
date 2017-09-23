package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.IResultSet;
import jef.tools.DateUtils;

public class LocalTime_TimeStamp extends AColumnMapping{

	public Object jdbcSet(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		st.setTimestamp(index, DateUtils.toSqlTimeStamp((LocalTime)value));
		return value;
	}

	public int getSqlType() {
		return java.sql.Types.TIMESTAMP;
	}

	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		return profile.getSqlTimestampExpression(DateUtils.fromLocalTime((LocalTime)value));
	}

	public Object jdbcGet(IResultSet rs, int n) throws SQLException {
		return DateUtils.toLocalTime(rs.getTimestamp(n));
	}

	@Override
	protected Class<?> getDefaultJavaType() {
		return LocalTime.class;
	}

	@Override
	public void jdbcUpdate(ResultSet rs, String columnIndex, Object value, DatabaseDialect dialect) throws SQLException {
		rs.updateTimestamp(columnIndex, DateUtils.toSqlTimeStamp((LocalTime)value));
	}

}

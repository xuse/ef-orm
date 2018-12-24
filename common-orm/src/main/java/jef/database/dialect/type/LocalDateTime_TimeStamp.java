package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.IResultSet;
import jef.database.query.Func;
import jef.tools.DateUtils;

public class LocalDateTime_TimeStamp extends AbstractTimeMapping{

	public Object jdbcSet(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		st.setTimestamp(index, DateUtils.toSqlTimeStamp((LocalDateTime)value));
		return value;
	}

	public int getSqlType() {
		return java.sql.Types.TIMESTAMP;
	}

	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		return profile.getSqlTimestampExpression(DateUtils.fromLocalDateTime((LocalDateTime)value));
	}

	public Object jdbcGet(IResultSet rs, int n) throws SQLException {
		return DateUtils.toLocalDateTime(rs.getTimestamp(n));
	}

	@Override
	protected Class<?> getDefaultJavaType() {
		return LocalDateTime.class;
	}

	@Override
	public void jdbcUpdate(ResultSet rs, String columnIndex, Object value, DatabaseDialect dialect) throws SQLException {
		rs.updateTimestamp(columnIndex, DateUtils.toSqlTimeStamp((LocalDateTime)value));
	}

	@Override
	public String getFunctionString(DatabaseDialect dialect) {
		 return dialect.getFunction(Func.current_timestamp);
	}

	@Override
	public Object getCurrentValue() {
		return LocalDateTime.now();
	}

}

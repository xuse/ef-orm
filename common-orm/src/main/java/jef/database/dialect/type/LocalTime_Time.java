package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;

import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.IResultSet;
import jef.tools.DateUtils;

public class LocalTime_Time extends AColumnMapping{

	public Object jdbcSet(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		st.setTime(index, DateUtils.toSqlTime((LocalTime)value));
		return value;
	}

	public int getSqlType() {
		return java.sql.Types.TIME;
	}

	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		return profile.getSqlTimeExpression(DateUtils.fromLocalTime((LocalTime)value));
	}

	public Object jdbcGet(IResultSet rs, int n) throws SQLException {
		return DateUtils.toLocalTime(rs.getTime(n));
	}

	@Override
	protected Class<?> getDefaultJavaType() {
		return LocalTime.class;
	}

	@Override
	public void jdbcUpdate(ResultSet rs, String columnIndex, Object value, DatabaseDialect dialect) throws SQLException {
		rs.updateTime(columnIndex,  DateUtils.toSqlTime((LocalTime)value));
	}

}

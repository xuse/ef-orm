package jef.database.dialect.type;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.IResultSet;
import jef.tools.StringUtils;

public class NumBigDecimalMapping extends AColumnMapping{

	public Object jdbcSet(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		if(StringUtils.isEmpty(value)){
			st.setNull(index, java.sql.Types.DECIMAL);
		}else{
			st.setBigDecimal(index, ((BigDecimal)value));
		}
		return value;
	}

	public int getSqlType() {
		return java.sql.Types.DECIMAL;
	}

	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		return value.toString();
	}
	
	public Object jdbcGet(IResultSet rs, int n) throws SQLException {
		return rs.getBigDecimal(n);
	}

	@Override
	protected Class<?> getDefaultJavaType() {
		return BigDecimal.class;
	}

	@Override
	public void jdbcUpdate(ResultSet rs, String columnIndex, Object value, DatabaseDialect dialect) throws SQLException {
		rs.updateBigDecimal(columnIndex, (BigDecimal)value);
	}
}

package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.IResultSet;
import jef.tools.StringUtils;

/**
 * BIGINT <-> String.class
 * @author jiyi
 *
 */
public class NumBigStringMapping extends AbstractVersionNumberMapping {

	public Object jdbcSet(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		if (StringUtils.isEmpty(value)) {
			st.setNull(index, java.sql.Types.BIGINT);
		} else {
			st.setLong(index, Long.parseLong((String)value));
		}
		return value;
	}

	public int getSqlType() {
		return java.sql.Types.BIGINT;
	}

	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		return value.toString();
	}

	public Object jdbcGet(IResultSet rs, int n) throws SQLException {
		Object obj = rs.getObject(n);
		if (obj == null)
			return null;
		return obj.toString();
	}

	@Override
	protected Class<?> getDefaultJavaType() {
		return String.class;
	}
	
	@Override
	Object increament(Object value) {
		if (value == null)
			return 1;
		int i = ((Number) value).intValue();
		return String.valueOf(i + 1);
	}

	@Override
	public void jdbcUpdate(ResultSet rs, String columnIndex, Object value, DatabaseDialect dialect) throws SQLException {
		String s=(String)value;
		if(s.length()==0){
			rs.updateNull(columnIndex);
		}else{
			rs.updateLong(columnIndex, Long.parseLong(s));
		}
	}
}

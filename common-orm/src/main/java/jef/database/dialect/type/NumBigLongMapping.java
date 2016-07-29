package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.IResultSet;

/**
 * BIGINT <-> Long.class
 * @author jiyi
 *
 */
public class NumBigLongMapping extends AbstractVersionNumberMapping{
	public Object jdbcSet(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		if(value==null){
			st.setNull(index,getSqlType());
		}else{
			st.setLong(index, ((Number)value).longValue());
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
		Object obj=rs.getObject(n);
		if(obj==null)return null;
		if(obj instanceof Long)return obj;
		return ((Number)obj).longValue();
	}

	@Override
	protected Class<?> getDefaultJavaType() {
		return Long.class;
	}
	
	@Override
	Object increament(Object value) {
		if (value == null)
			return 1;
		long i = ((Number) value).longValue();
		return i + 1L;
	}
}

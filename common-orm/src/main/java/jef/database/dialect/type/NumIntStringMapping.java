package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.IResultSet;
import jef.tools.StringUtils;

/**
 * INT <-> java.lang.String
 * @author jiyi
 *
 */
public class NumIntStringMapping  extends AbstractVersionNumberMapping{
	public Object jdbcSet(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		if(StringUtils.isEmpty(value)){
			st.setNull(index, java.sql.Types.INTEGER);
		}else{
			st.setInt(index, Integer.parseInt((String)value));
		}
		return value;
	}
	

	@Override
	public void jdbcUpdate(ResultSet rs, String columnIndex, Object value, DatabaseDialect dialect) throws SQLException {
		rs.updateInt(columnIndex, Integer.parseInt((String)value));
	}

	public int getSqlType() {
		return java.sql.Types.INTEGER;
	}

	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		return value.toString();
	}

	public Object jdbcGet(IResultSet rs, int n) throws SQLException {
		Object obj=rs.getObject(n);
		if(obj==null)return null;
		return obj.toString();
	}

	@Override
	protected Class<?> getDefaultJavaType() {
		return String.class;
	}

	@Override
	Object increament(Object value) {
		if(value==null)return 1;
		int i=Integer.parseInt(String.valueOf(value));
		return String.valueOf(i+1);
	}


	@Override
	protected Object transfer(long n) {
		return String.valueOf(n);
	}
}

package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.IResultSet;

/**
 * Java类中的ENUM对应数据库中的INT
 * 按照枚举在类中定义的顺序，第一个枚举值为0.以此类推
 * 
 * INT <-> enum. ordinal()
 * 
 * @author jiyi
 *
 */
public class NumIntEnumMapping extends AColumnMapping{

	public Object jdbcSet(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		if(value==null){
			st.setNull(index, java.sql.Types.INTEGER);
		}else{
			st.setInt(index, ((Enum<?>)value).ordinal());
		}
		return value;
	}

	public int getSqlType() {
		return java.sql.Types.INTEGER;
	}

	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		return String.valueOf(((Enum<?>)value).ordinal());
	}

	public Object jdbcGet(IResultSet rs, int n) throws SQLException {
		Object obj=rs.getObject(n);
		if(obj==null)return null;
		Enum<?>[] enums=clz.asSubclass(Enum.class).getEnumConstants(); 
		return enums[rs.getInt(n)];
	}

	@Override
	protected Class<?> getDefaultJavaType() {
		return Enum.class;
	}

	@Override
	public void jdbcUpdate(ResultSet rs, String columnIndex, Object value, DatabaseDialect dialect) throws SQLException {
		rs.updateInt(columnIndex, ((Enum<?>)value).ordinal());
	}
}

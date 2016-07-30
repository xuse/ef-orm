package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.IResultSet;

/**
 * BIGINT <-> java.util.Date
 * 这个映射关系用来表示在Java内是日期时间型，在数据库中用NUMBER型，记录自1970年以来的毫秒数
 * @author jiyi
 *
 */
public class NumBigDateMapping extends AbstractTimeMapping{

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

	@Override
	public String getFunctionString(DatabaseDialect profile) {
		//将current_timestamp转换为毫秒数的数据库函数比较烦，暂不支持。
		throw new UnsupportedOperationException("When using [Database:Number] <-> [java,util.Date] mapping, supports 'create-sys' and 'modified-sys' generator only.");
	}

	@Override
	public Object getCurrentValue() {
		return new Date();
	}

	@Override
	public void jdbcUpdate(ResultSet rs, String columnIndex, Object value, DatabaseDialect dialect) throws SQLException {
		long time=((Date)value).getTime();
		rs.updateLong(columnIndex, time);
	}
}

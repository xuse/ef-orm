package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.IResultSet;
import jef.tools.DateFormats;
import jef.tools.StringUtils;

public class CharTimestampMapping extends AColumnMapping{
	private ThreadLocal<? extends DateFormat> format;
	
	public CharTimestampMapping(String format){
		this.format=DateFormats.create(format);
	}
	
	public CharTimestampMapping(){
		this.format=DateFormats.DATE_TIME_CS;
	}
	
	public int getSqlType() {
		return java.sql.Types.CHAR;
	}
	
	public Object jdbcSet(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		String s=format.get().format((Date)value);
		st.setString(index, s);
		return value;
	}

	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		return super.wrapSqlStr(value.toString());
	}

	public Object jdbcGet(IResultSet rs, int n) throws SQLException {
		String s=rs.getString(n);
		if(StringUtils.isEmpty(s)){
			return null;
		}else{
			Date d;
			try {
				d = format.get().parse(s);
			} catch (ParseException e) {
				throw new SQLException(e);
			}
			return new java.sql.Timestamp(d.getTime());
		}
	}

	@Override
	protected Class<?> getDefaultJavaType() {
		return java.sql.Timestamp.class;
	}

	@Override
	public void jdbcUpdate(ResultSet rs, String columnIndex, Object value, DatabaseDialect dialect) throws SQLException {
		String s=format.get().format((Date)value);
		rs.updateString(columnIndex,s);
	}
}

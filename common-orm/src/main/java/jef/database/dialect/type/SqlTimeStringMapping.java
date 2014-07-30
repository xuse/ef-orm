package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;

import jef.database.dialect.DatabaseDialect;
import jef.database.wrapper.IResultSet;
import jef.tools.DateFormats;

public class SqlTimeStringMapping extends ATypeMapping<String>{
	private ThreadLocal<DateFormat> format=DateFormats.TIME_ONLY;
	
	public SqlTimeStringMapping(){
	}
	
	public SqlTimeStringMapping(final String pattern){
		this.format=DateFormats.getThreadLocalDateFormat(pattern);
	}
	
	public Object set(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		if(value==null){
			st.setNull(index, java.sql.Types.TIME);
			return value;
		}else{
			try {
				java.sql.Time time=new java.sql.Time(format.get().parse((String)value).getTime());
				st.setTime(index, time);
				return time;
			} catch (ParseException e) {
				throw new IllegalArgumentException((String)value,e);
			}
		}
		
	}

	public int getSqlType() {
		return java.sql.Types.TIME;
	}

	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		try {
			java.util.Date date=format.get().parse((String)value);
			return profile.getSqlTimeExpression(date);
		} catch (ParseException e) {
			throw new IllegalArgumentException("Error cast date string.",e);
		}
	}

	public Object getProperObject(IResultSet rs, int n) throws SQLException {
		java.sql.Time t=rs.getTime(n);
		if(t==null)return null;
		return format.get().format(t);
	}

}

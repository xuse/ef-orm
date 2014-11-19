package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.IResultSet;
import jef.database.query.Func;
import jef.tools.DateFormats;

/**
 * DATE <-> java.lang.String
 * @author jiyi
 *
 */
public class DateStringMapping extends AbstractTimeMapping<String>{
	
	private ThreadLocal<DateFormat> format=DateFormats.DATE_CS;
	
	public DateStringMapping(){
	}
	
	public DateStringMapping(final String pattern){
		this.format=DateFormats.getThreadLocalDateFormat(pattern);
	}
	
	
	public Object set(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		if(value==null){
			st.setNull(index, java.sql.Types.DATE);
		}else{
			try {
				st.setDate(index,new java.sql.Date(format.get().parse((String)value).getTime()));
			} catch (ParseException e) {
				throw new IllegalArgumentException((String)value,e);
			}
		}
		return value;
	}

	public int getSqlType() {
		return java.sql.Types.DATE;
	}

	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		try {
			java.util.Date date=format.get().parse((String)value);
			return profile.getSqlDateExpression(date);
		} catch (ParseException e) {
			throw new IllegalArgumentException("Error cast date string.",e);
		}
	}

	public Object getProperObject(IResultSet rs, int n) throws SQLException {
		java.sql.Date date= rs.getDate(n);
		if(date==null)return null;
		return format.get().format(date);
	}

	@Override
	public String getFunctionString(DatabaseDialect profile) {
		return profile.getFunction(Func.current_date);
	}

	@Override
	public Object getCurrentValue() {
		return format.get().format(new Date());
	}
}

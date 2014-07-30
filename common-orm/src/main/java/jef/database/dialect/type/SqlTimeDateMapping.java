package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;
import java.util.Date;

import jef.database.dialect.DatabaseDialect;
import jef.database.wrapper.IResultSet;

public class SqlTimeDateMapping extends ATypeMapping<Date>{

	public Object set(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		if(value==null){
			st.setNull(index, java.sql.Types.TIME);
			return value;
		}else{
			Time time=new java.sql.Time(((Date)value).getTime());
			st.setTime(index, time);
			return time;
		}
	}

	public int getSqlType() {
		return java.sql.Types.TIME;
	}

	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		if(value instanceof java.util.Date){
			return profile.getSqlTimeExpression((Date)value);
		}
		throw new IllegalArgumentException("The input param can not cast to Date.");
	}

	public Object getProperObject(IResultSet rs, int n) throws SQLException {
		java.sql.Time obj=rs.getTime(n);
		if(obj==null)return null;
		return new Date(obj.getTime()); 
	}
}

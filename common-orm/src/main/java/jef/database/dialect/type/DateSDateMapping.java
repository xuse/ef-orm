package jef.database.dialect.type;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.IResultSet;
import jef.database.query.Func;

/**
 * DATE <-> java.sql.Date
 * @author jiyi
 *
 */
public class DateSDateMapping extends AbstractTimeMapping<java.sql.Date>{
	
	public Object set(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		st.setDate(index, (java.sql.Date)value);
		return value;
	}
	

	public int getSqlType() {
		return java.sql.Types.DATE;
	}

	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		if(value instanceof java.util.Date){
			return profile.getSqlDateExpression((Date)value);
		}
		throw new IllegalArgumentException("The input param can not cast to Date.");
	}

	public Object getProperObject(IResultSet rs, int n) throws SQLException {
		return rs.getDate(n);
	}
	
	@Override
	public String getFunctionString(DatabaseDialect profile) {
		return profile.getFunction(Func.current_date);
	}


	@Override
	public Object getCurrentValue() {
		return new java.sql.Date(System.currentTimeMillis());
	}
}

package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;

import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.IResultSet;
import jef.database.query.Func;

/**
 * DataMapping
 *  DATE (无时分秒) <-> java.util.Date
 * @author jiyi
 *
 */
public class DateDateMapping extends AbstractTimeMapping<java.util.Date>{
	
	public Object set(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		if(value==null){
			st.setNull(index, java.sql.Types.DATE);
			return null;
		}else{
			java.sql.Date da= new java.sql.Date(((Date)value).getTime());
			st.setDate(index,da);
			return da;
		}
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
		return new java.util.Date();
	}
}

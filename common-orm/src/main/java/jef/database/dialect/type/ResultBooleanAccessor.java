package jef.database.dialect.type;

import java.sql.SQLException;

import jef.database.wrapper.IResultSet;

final  class ResultBooleanAccessor implements ResultSetAccessor{
	public Object getProperObject(IResultSet rs,int n) throws SQLException {
		Object value=rs.getObject(n);
		if(value==null){
			return null;
		}
		if(value instanceof Boolean){
			return value;
		}
		if(value instanceof Number){
			return ((Number) value).shortValue()>0;
		}
		String s=String.valueOf(value);
		char c=s.charAt(0);
		return Boolean.valueOf(c=='1' || c=='T');
	}

	public Class<?> getReturnType() {
		return Boolean.class;
	}

	public boolean applyFor(int type) {
		return true;
	}
	
	
}

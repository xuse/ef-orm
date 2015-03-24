package jef.database.dialect.type;

import java.sql.SQLException;

import jef.database.jdbc.result.IResultSet;

final  class ResultCharacterAccessor implements ResultSetAccessor{
	private Character defaultValue;
	
	public Object jdbcGet(IResultSet rs,int n) throws SQLException {
		String value=rs.getString(n);
		if(value!=null && value.length()>0){
			return value.charAt(0);
		}
		return defaultValue;
	}

	public Class<?> getReturnType() {
		return Character.class;
	}

	public boolean applyFor(int type) {
		return true;
	}
	
	public ResultCharacterAccessor(Character defaultValue){
		this.defaultValue=defaultValue;
	}
}

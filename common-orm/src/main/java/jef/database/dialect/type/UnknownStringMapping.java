package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.IResultSet;

public class UnknownStringMapping extends AColumnMapping<String>{
	private String name;
	private int sqlType;
	public UnknownStringMapping(String name, int sqlType) {
		this.name=name;
		this.sqlType=sqlType;
	}

	@Override
	public Object set(PreparedStatement st, Object value, int index, DatabaseDialect dialect) throws SQLException {
		if(value==null){
			st.setNull(index, sqlType);
		}else{
			st.setObject(index, value);
		}
		return value;
	}

	@Override
	public int getSqlType() {
		return sqlType;
	}

	@Override
	public Object getProperObject(IResultSet rs, int n) throws SQLException {
		return String.valueOf(rs.getObject(n));
	}

	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		return super.wrapSqlStr(value.toString());
	}

	@Override
	public String toString() {
		return name+"|"+super.toString();
	}
	
	
}

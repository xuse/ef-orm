package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.IResultSet;
import jef.tools.StringUtils;

public class NumIntStringMapping  extends AColumnMapping<String>{
	public Object set(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		if(StringUtils.isEmpty(value)){
			st.setNull(index, java.sql.Types.INTEGER);
		}else{
			st.setInt(index, Integer.parseInt((String)value));
		}
		return value;
	}

	public int getSqlType() {
		return java.sql.Types.INTEGER;
	}

	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		return value.toString();
	}

	public Object getProperObject(IResultSet rs, int n) throws SQLException {
		Object obj=rs.getObject(n);
		if(obj==null)return null;
		return obj.toString();
	}
}

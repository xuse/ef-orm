package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLXML;

import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.IResultSet;

public class XmlStringMapping extends AColumnMapping{
	public Object jdbcSet(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		String s=String.valueOf(value);
		if(s==null || s.length()==0){
			st.setNull(index,java.sql.Types.SQLXML);
		}else{
			SQLXML xml=st.getConnection().createSQLXML();
			xml.setString(String.valueOf(value));
			st.setSQLXML(index, xml);
		}
		return value;
	}

	public int getSqlType() {
		return java.sql.Types.SQLXML;
	}

	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		return super.wrapSqlStr(value.toString());
	}

	public Object jdbcGet(IResultSet rs, int n) throws SQLException {
		return rs.getString(n);
	}

	@Override
	protected Class<?> getDefaultJavaType() {
		return String.class;
	}

	@Override
	public void jdbcUpdate(ResultSet rs, String columnIndex, Object value, DatabaseDialect dialect) throws SQLException {
//		SQLXML xml=st.getConnection().createSQLXML();
//		xml.setString(String.valueOf(value));
//		rs.updateSQLXML(columnIndex, xml);
		rs.updateObject(columnIndex, value);
		
	}
}

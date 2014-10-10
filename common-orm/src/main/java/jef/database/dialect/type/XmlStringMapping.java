package jef.database.dialect.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLXML;

import jef.database.dialect.DatabaseDialect;
import jef.database.wrapper.result.IResultSet;

public class XmlStringMapping extends ATypeMapping<String>{
	public Object set(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
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

	public Object getProperObject(IResultSet rs, int n) throws SQLException {
		return rs.getString(n);
	}
}

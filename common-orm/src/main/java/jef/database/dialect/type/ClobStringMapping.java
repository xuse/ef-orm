package jef.database.dialect.type;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import jef.database.dialect.DatabaseDialect;
import jef.database.wrapper.result.IResultSet;
import jef.tools.IOUtils;

public class ClobStringMapping extends ATypeMapping<String>{
	public Object set(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		if(value==null){
			
			st.setNull(index, session.getImplementationSqlType(Types.CLOB));
		}else{
			String str=(String)value;
			st.setCharacterStream(index, new StringReader(str), str.length());
		}
		return value;
	}

	public int getSqlType() {
		return java.sql.Types.CLOB;
	}

	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isLob() {
		return true;
	}

	public Object getProperObject(IResultSet rs, int n) throws SQLException {
		Object obj=rs.getObject(n);
		if(obj==null)return null;
		if(obj instanceof String){
			return obj;
		}
		Reader reader = ((Clob)obj).getCharacterStream();
		try {
			return IOUtils.asString(reader);
		} catch (IOException e) {
			throw new SQLException("Error at reading clob",e);
		}
	}
}

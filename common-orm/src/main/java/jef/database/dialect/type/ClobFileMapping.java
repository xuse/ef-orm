package jef.database.dialect.type;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import javax.persistence.PersistenceException;

import jef.database.dialect.DatabaseDialect;
import jef.database.jdbc.result.IResultSet;
import jef.tools.IOUtils;

public class ClobFileMapping extends AColumnMapping{
	public Object jdbcSet(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		File file=(File)value;
		if(value==null || !file.exists()){
			st.setNull(index, session.getImplementationSqlType(Types.CLOB));
		}else{
			try {
				st.setCharacterStream(index, IOUtils.getReader(file, (String)null));//这个方法在JDBC4才支持。
			} catch (IOException e) {
				throw new PersistenceException(e);
			}	
		}
		return value;
	}

	public int getSqlType() {
		return Types.CLOB;
	}

	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isLob() {
		return true;
	}

	public Object jdbcGet(IResultSet rs, int n) throws SQLException {
		Object obj=rs.getObject(n);
		if(obj==null)return null;
		if(obj instanceof String){
			try{
				File file=File.createTempFile("tmp", ".io");
				IOUtils.saveAsFile(file, (String)obj);
				return file;	
			}catch(IOException e){
				throw new SQLException("Error at save clob to file.",e);
			}
		}
		Reader reader = ((Clob)obj).getCharacterStream();
		try {
			return IOUtils.saveAsTempFile(reader);
		} catch (IOException e) {
			throw new SQLException("Error at save clob to file.",e);
		}
	}

	@Override
	protected Class<?> getDefaultJavaType() {
		return File.class;
	}

	@Override
	public void jdbcUpdate(ResultSet rs, String columnIndex, Object value, DatabaseDialect dialect) throws SQLException {
		File file=(File)value;
		try {
			rs.updateCharacterStream(columnIndex, IOUtils.getReader(file, (String)null));//这个方法在JDBC4才支持。
		} catch (IOException e) {
			throw new PersistenceException(e);
		}	
	}
}

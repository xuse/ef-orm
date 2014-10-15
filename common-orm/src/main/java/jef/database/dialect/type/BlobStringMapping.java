package jef.database.dialect.type;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import jef.database.ORMConfig;
import jef.database.dialect.DatabaseDialect;
import jef.database.wrapper.result.IResultSet;
import jef.tools.IOUtils;

public class BlobStringMapping extends AColumnMapping<String>{
	
	public Object set(PreparedStatement st, Object value, int index, DatabaseDialect session) throws SQLException {
		if(value==null){
			st.setNull(index, session.getImplementationSqlType(Types.BLOB));
		}else{
			byte[] buf = ((String)value).getBytes(ORMConfig.getInstance().getDbEncodingCharset());
			st.setBytes(index,buf);
		}
		return value;
	}

	public int getSqlType() {
		return Types.BLOB;
	}
	
	@Override
	protected String getSqlExpression(Object value, DatabaseDialect profile) {
		throw new UnsupportedOperationException();
	}

	public boolean isLob() {
		return true;
	}
	
	public static int getLength(String str){
		return ORMConfig.getInstance().getDbEncodingCharset().encode(str).limit(); 
	}
	
	public Object getProperObject(IResultSet rs, int n) throws SQLException {
		Object obj=rs.getObject(n);
		if(obj==null)return null;
		if(obj.getClass().isArray()){
			byte[] data=(byte[])obj;
			return new String(data,0,data.length,ORMConfig.getInstance().getDbEncodingCharset());
		}
		Blob blob=(Blob) obj;
		InputStream in = blob.getBinaryStream();
		try {
			return IOUtils.asString(in,ORMConfig.getInstance().getDbEncoding(),true);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}
